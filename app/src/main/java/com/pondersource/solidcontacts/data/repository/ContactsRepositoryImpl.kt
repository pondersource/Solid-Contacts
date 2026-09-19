package com.pondersource.solidcontacts.data.repository

import com.pondersource.solidcontacts.data.local.db.AddressBookDao
import com.pondersource.solidcontacts.data.local.db.AddressBookEntity
import com.pondersource.solidcontacts.data.local.db.ContactDao
import com.pondersource.solidcontacts.data.local.db.ContactEntity
import com.pondersource.solidcontacts.data.local.db.GroupDao
import com.pondersource.solidcontacts.data.local.db.GroupEntity
import com.pondersource.solidcontacts.data.local.db.GroupMemberDao
import com.pondersource.solidcontacts.data.local.PhotoScaler
import com.pondersource.solidcontacts.data.local.PhotoStore
import com.pondersource.solidcontacts.data.local.db.GroupMemberEntity
import com.pondersource.solidcontacts.data.local.prefs.UserPreferences
import com.pondersource.solidcontacts.data.mapper.toContactData
import com.pondersource.solidcontacts.data.mapper.toDetail
import com.pondersource.solidcontacts.data.mapper.toDomain
import com.pondersource.solidcontacts.data.mapper.toEntity
import com.pondersource.solidcontacts.data.mapper.toSummary
import com.pondersource.solidcontacts.data.outbox.DrainResult
import com.pondersource.solidcontacts.data.outbox.OpType
import com.pondersource.solidcontacts.data.outbox.Outbox
import com.pondersource.solidcontacts.data.outbox.OutboxPayload
import com.pondersource.solidcontacts.data.remote.SolidContactsSource
import com.pondersource.solidcontacts.domain.model.AddressBookSummary
import com.pondersource.solidcontacts.domain.model.ContactDetail
import com.pondersource.solidcontacts.domain.model.ContactGroup
import com.pondersource.solidcontacts.domain.model.ContactSummary
import com.pondersource.solidcontacts.domain.model.DuplicateCluster
import com.pondersource.solidcontacts.domain.model.SyncState
import com.pondersource.solidcontacts.sync.SyncScheduler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** The vCard category that marks a contact as a favourite, so the star travels with the pod. */
const val FAVORITES_CATEGORY: String = "Favorites"

/** The title given to the address book created for a pod that has none. */
private const val DEFAULT_BOOK_TITLE = "Contacts"

/** Photos are fetched over IPC one at a time in small batches, to stay well inside the binder budget. */
private const val PHOTO_FETCH_CONCURRENCY = 4

@Singleton
class ContactsRepositoryImpl @Inject constructor(
    private val remote: SolidContactsSource,
    private val bookDao: AddressBookDao,
    private val contactDao: ContactDao,
    private val groupDao: GroupDao,
    private val memberDao: GroupMemberDao,
    private val outbox: Outbox,
    private val prefs: UserPreferences,
    private val scheduler: SyncScheduler,
    private val photoScaler: PhotoScaler,
    private val photoStore: PhotoStore,
    private val io: CoroutineDispatcher,
) : ContactsRepository {

    override fun connectionState(): Flow<Boolean> = remote.connectionState()

    override fun observePendingWrites(): Flow<Int> =
        forWebId(0) { webId -> outbox.observePendingCount(webId) }

    // --- Reads ---------------------------------------------------------------------------

    override fun observeBooks(): Flow<List<AddressBookSummary>> =
        forWebId(emptyList()) { webId ->
            bookDao.observeAll(webId).map { rows -> rows.map { it.toDomain() } }
        }

    override fun observeBook(bookId: String): Flow<AddressBookSummary?> =
        bookDao.observeById(bookId).map { it?.toDomain() }

    override fun observeAllContacts(): Flow<List<ContactSummary>> =
        forWebId(emptyList()) { webId ->
            contactDao.observeAll(webId).map { rows -> rows.map { it.toSummary() } }
        }

    override fun observeContactsInBook(bookId: String): Flow<List<ContactSummary>> =
        forWebId(emptyList()) { webId ->
            contactDao.observeInBook(webId, bookId).map { rows -> rows.map { it.toSummary() } }
        }

    override fun searchContacts(query: String): Flow<List<ContactSummary>> =
        forWebId(emptyList()) { webId ->
            val needle = query.trim().lowercase()
            val rows = if (needle.isEmpty()) {
                contactDao.observeAll(webId)
            } else {
                contactDao.search(webId, needle)
            }
            rows.map { list -> list.map { it.toSummary() } }
        }

    override fun observeFavorites(): Flow<List<ContactSummary>> =
        forWebId(emptyList()) { webId ->
            contactDao.observeFavorites(webId).map { rows -> rows.map { it.toSummary() } }
        }

    override fun observeContact(contactId: String): Flow<ContactDetail?> =
        contactDao.observeById(contactId).map { it?.toDetail() }

    override fun observeGroups(): Flow<List<ContactGroup>> =
        forWebId(emptyList()) { webId -> groupsWithCounts(webId, groupDao.observeAll(webId)) }

    override fun observeGroupsInBook(bookId: String): Flow<List<ContactGroup>> =
        forWebId(emptyList()) { webId -> groupsWithCounts(webId, groupDao.observeInBook(webId, bookId)) }

    override fun observeGroup(groupId: String): Flow<ContactGroup?> =
        groupDao.observeById(groupId).map { it?.toDomain() }

    override fun observeGroupMembers(groupId: String): Flow<List<ContactSummary>> =
        contactDao.observeInGroup(groupId).map { rows -> rows.map { it.toSummary() } }

    override fun observeGroupIdsOf(contactId: String): Flow<List<String>> =
        memberDao.observeGroupIdsOf(contactId)

    override fun observeContactCount(): Flow<Int> =
        forWebId(0) { webId -> contactDao.observeCount(webId) }

    override suspend fun contactPhoto(contactId: String): ByteArray? = withContext(io) {
        photoStore.read(contactId)?.let { return@withContext it }
        val row = contactDao.findById(contactId) ?: return@withContext null
        val photoUri = row.photoUri ?: return@withContext null
        val webId = prefs.webId().ifEmpty { return@withContext null }
        val photo = remote.getPhotoOrNull(webId, photoUri) ?: return@withContext null
        storePhoto(contactId, photoUri, photo.bytes)
    }

    override suspend fun getContact(contactId: String): ContactDetail? = withContext(io) {
        contactDao.findById(contactId)?.toDetail()
    }

    // --- Sync ----------------------------------------------------------------------------

    override suspend fun refresh(): Result<Unit> = withContext(io) {
        runCatching {
            val webId = prefs.webId()
            check(webId.isNotEmpty()) { "No Solid account is connected." }

            // Send anything queued before pulling, so the pull does not overwrite a local edit
            // that has not reached the pod yet.
            runCatching { drainOutboxFor(webId) }

            val list = remote.ensureBookContainer(webId)
            val books = list.privateAddressBookUris.map { it to true } +
                list.publicAddressBookUris.map { it to false }

            val now = System.currentTimeMillis()
            val keptBookIds = mutableListOf<String>()

            for ((bookUri, isPrivate) in books) {
                val book = remote.getBookOrNull(webId, bookUri) ?: continue
                val bookId = localBookId(webId, bookUri)
                keptBookIds += bookId
                bookDao.upsert(
                    AddressBookEntity(
                        id = bookId,
                        webId = webId,
                        uri = bookUri,
                        title = book.title.ifBlank { DEFAULT_BOOK_TITLE },
                        isPrivate = isPrivate,
                        syncState = SyncState.SYNCED,
                        updatedAt = now,
                    ),
                )
                syncBookContents(webId, bookId, bookUri, now)
            }

            if (keptBookIds.isNotEmpty()) {
                bookDao.deleteSyncedNotIn(webId, keptBookIds)
            }
            prefs.setLastSyncAt(now)
        }
    }

    override suspend fun drainOutbox(): Result<DrainResult> = withContext(io) {
        runCatching {
            val webId = prefs.webId()
            check(webId.isNotEmpty()) { "No Solid account is connected." }
            drainOutboxFor(webId)
        }
    }

    override suspend fun retryFailedWrites() = withContext(io) {
        val webId = prefs.webId()
        if (webId.isNotEmpty()) {
            outbox.retryAllNow(webId)
            scheduler.requestDrain()
        }
    }

    // --- Address books -------------------------------------------------------------------

    override suspend fun ensureDefaultBook(): String = withContext(io) {
        val webId = prefs.webId()
        check(webId.isNotEmpty()) { "No Solid account is connected." }

        prefs.current().defaultBookId
            .takeIf { it.isNotEmpty() && bookDao.findById(it) != null }
            ?.let { return@withContext it }

        bookDao.getAll(webId).firstOrNull()?.let { existing ->
            prefs.setDefaultBookId(existing.id)
            return@withContext existing.id
        }

        // Nothing cached. Ask the pod for its default book; if we cannot reach it, start one
        // locally and let the outbox create it on the pod later.
        val remoteBook = runCatching { remote.ensureDefaultBook(webId, DEFAULT_BOOK_TITLE) }.getOrNull()
        val now = System.currentTimeMillis()
        if (remoteBook != null) {
            val id = localBookId(webId, remoteBook.uri)
            bookDao.upsert(
                AddressBookEntity(
                    id = id,
                    webId = webId,
                    uri = remoteBook.uri,
                    title = remoteBook.title.ifBlank { DEFAULT_BOOK_TITLE },
                    isPrivate = true,
                    syncState = SyncState.SYNCED,
                    updatedAt = now,
                ),
            )
            prefs.setDefaultBookId(id)
            id
        } else {
            createBook(DEFAULT_BOOK_TITLE, isPrivate = true).also { prefs.setDefaultBookId(it) }
        }
    }

    override suspend fun createBook(title: String, isPrivate: Boolean): String = withContext(io) {
        val webId = prefs.webId()
        check(webId.isNotEmpty()) { "No Solid account is connected." }
        val id = "book-${UUID.randomUUID()}"
        bookDao.upsert(
            AddressBookEntity(
                id = id,
                webId = webId,
                uri = null,
                title = title.trim(),
                isPrivate = isPrivate,
                syncState = SyncState.PENDING_CREATE,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        enqueue(webId, OpType.CREATE_BOOK, OutboxPayload(bookId = id))
        id
    }

    override suspend fun renameBook(bookId: String, title: String) = withContext(io) {
        val webId = prefs.webId()
        val row = bookDao.findById(bookId) ?: return@withContext
        val state = if (row.syncState == SyncState.PENDING_CREATE) {
            SyncState.PENDING_CREATE
        } else {
            SyncState.PENDING_UPDATE
        }
        bookDao.rename(bookId, title.trim(), state, System.currentTimeMillis())
        if (state == SyncState.PENDING_UPDATE) {
            enqueue(webId, OpType.RENAME_BOOK, OutboxPayload(bookId = bookId, title = title.trim()))
        }
    }

    override suspend fun deleteBook(bookId: String) = withContext(io) {
        val webId = prefs.webId()
        val row = bookDao.findById(bookId) ?: return@withContext
        if (row.uri == null) {
            // It never reached the pod, so there is nothing to withdraw.
            purgeBookLocally(webId, bookId)
        } else {
            bookDao.setSyncState(bookId, SyncState.PENDING_DELETE)
            enqueue(webId, OpType.DELETE_BOOK, OutboxPayload(bookId = bookId, uri = row.uri))
        }
    }

    // --- Contacts ------------------------------------------------------------------------

    override suspend fun createContact(
        bookId: String,
        detail: ContactDetail,
        groupIds: List<String>,
    ): String = withContext(io) {
        val webId = prefs.webId()
        check(webId.isNotEmpty()) { "No Solid account is connected." }
        val id = "contact-${UUID.randomUUID()}"
        val now = System.currentTimeMillis()
        val prepared = detail.copy(
            id = id,
            uri = null,
            bookId = bookId,
            syncState = SyncState.PENDING_CREATE,
        ).withFavoriteCategory(detail.isFavorite)

        contactDao.upsert(prepared.toEntity(webId, now, syncState = SyncState.PENDING_CREATE))
        groupIds.forEach { memberDao.insert(GroupMemberEntity(it, id, webId)) }
        enqueue(webId, OpType.CREATE_CONTACT, OutboxPayload(contactId = id, bookId = bookId))
        id
    }

    override suspend fun updateContact(detail: ContactDetail) = withContext(io) {
        val webId = prefs.webId()
        val row = contactDao.findById(detail.id) ?: return@withContext
        val state = if (row.syncState == SyncState.PENDING_CREATE || row.uri == null) {
            SyncState.PENDING_CREATE
        } else {
            SyncState.PENDING_UPDATE
        }
        val prepared = detail
            .copy(uri = row.uri, bookId = row.bookId, syncState = state)
            .withFavoriteCategory(detail.isFavorite)
        contactDao.upsert(
            prepared.toEntity(webId, System.currentTimeMillis(), row.photoThumb, state),
        )
        // A row still queued for creation needs no second op: the create sends the latest text.
        if (state == SyncState.PENDING_UPDATE) {
            enqueue(webId, OpType.UPDATE_CONTACT, OutboxPayload(contactId = detail.id))
        }
    }

    override suspend fun deleteContact(contactId: String) = withContext(io) {
        val webId = prefs.webId()
        val row = contactDao.findById(contactId) ?: return@withContext
        if (row.uri == null) {
            memberDao.clearContact(contactId)
            contactDao.deleteById(contactId)
            photoStore.delete(contactId)
        } else {
            contactDao.setSyncState(contactId, SyncState.PENDING_DELETE)
            enqueue(
                webId,
                OpType.DELETE_CONTACT,
                OutboxPayload(contactId = contactId, bookId = row.bookId, uri = row.uri),
            )
        }
    }

    override suspend fun deleteContacts(contactIds: List<String>) {
        contactIds.forEach { deleteContact(it) }
    }

    override suspend fun moveContact(contactId: String, targetBookId: String) = withContext(io) {
        val webId = prefs.webId()
        val row = contactDao.findById(contactId) ?: return@withContext
        if (row.bookId == targetBookId) return@withContext
        val sourceBookId = row.bookId
        contactDao.setBook(contactId, targetBookId, System.currentTimeMillis())
        if (row.uri == null) {
            // Not on the pod yet: the queued create will simply use the new book.
            return@withContext
        }
        contactDao.setSyncState(contactId, SyncState.PENDING_UPDATE)
        enqueue(
            webId,
            OpType.MOVE_CONTACT,
            OutboxPayload(
                contactId = contactId,
                bookId = sourceBookId,
                targetBookId = targetBookId,
                uri = row.uri,
            ),
        )
    }

    override suspend fun setFavorite(contactId: String, favorite: Boolean) = withContext(io) {
        val detail = contactDao.findById(contactId)?.toDetail() ?: return@withContext
        updateContact(detail.copy(isFavorite = favorite))
    }

    override suspend fun setPhoto(contactId: String, bytes: ByteArray, contentType: String) =
        withContext(io) {
            val webId = prefs.webId()
            val row = contactDao.findById(contactId) ?: return@withContext
            storePhoto(contactId, row.photoUri, bytes)
            enqueue(
                webId,
                OpType.SET_PHOTO,
                OutboxPayload(contactId = contactId, contentType = contentType),
            )
        }

    override suspend fun removePhoto(contactId: String) = withContext(io) {
        val webId = prefs.webId()
        val row = contactDao.findById(contactId) ?: return@withContext
        contactDao.setPhoto(contactId, null, null)
        photoStore.delete(contactId)
        if (row.photoUri != null) {
            enqueue(webId, OpType.REMOVE_PHOTO, OutboxPayload(contactId = contactId))
        }
    }

    override suspend fun importContacts(bookId: String, contacts: List<ContactDetail>): Int =
        withContext(io) {
            var saved = 0
            for (contact in contacts) {
                if (contact.isEmpty()) continue
                createContact(bookId, contact)
                saved++
            }
            saved
        }

    // --- Groups --------------------------------------------------------------------------

    override suspend fun createGroup(
        bookId: String,
        name: String,
        memberIds: List<String>,
    ): String = withContext(io) {
        val webId = prefs.webId()
        check(webId.isNotEmpty()) { "No Solid account is connected." }
        val id = "group-${UUID.randomUUID()}"
        groupDao.upsert(
            GroupEntity(
                id = id,
                webId = webId,
                bookId = bookId,
                uri = null,
                name = name.trim(),
                syncState = SyncState.PENDING_CREATE,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        memberDao.insertAll(memberIds.map { GroupMemberEntity(id, it, webId) })
        enqueue(webId, OpType.CREATE_GROUP, OutboxPayload(groupId = id, bookId = bookId))
        id
    }

    override suspend fun deleteGroup(groupId: String) = withContext(io) {
        val webId = prefs.webId()
        val row = groupDao.findById(groupId) ?: return@withContext
        if (row.uri == null) {
            memberDao.clearGroup(groupId)
            groupDao.deleteById(groupId)
        } else {
            groupDao.setSyncState(groupId, SyncState.PENDING_DELETE)
            enqueue(
                webId,
                OpType.DELETE_GROUP,
                OutboxPayload(groupId = groupId, bookId = row.bookId, uri = row.uri),
            )
        }
    }

    override suspend fun addToGroup(groupId: String, contactId: String) = withContext(io) {
        val webId = prefs.webId()
        memberDao.insert(GroupMemberEntity(groupId, contactId, webId))
        enqueue(webId, OpType.ADD_MEMBER, OutboxPayload(groupId = groupId, contactId = contactId))
    }

    override suspend fun removeFromGroup(groupId: String, contactId: String) = withContext(io) {
        val webId = prefs.webId()
        memberDao.remove(groupId, contactId)
        enqueue(webId, OpType.REMOVE_MEMBER, OutboxPayload(groupId = groupId, contactId = contactId))
    }

    // --- Duplicates ----------------------------------------------------------------------

    override suspend fun findDuplicates(): List<DuplicateCluster> = withContext(io) {
        val webId = prefs.webId()
        if (webId.isEmpty()) return@withContext emptyList()
        DuplicateFinder.cluster(contactDao.getAll(webId).map { it.toDetail() })
    }

    override suspend fun mergeContacts(
        survivorId: String,
        loserIds: List<String>,
    ): ContactDetail? = withContext(io) {
        val survivor = contactDao.findById(survivorId)?.toDetail() ?: return@withContext null
        val losers = loserIds.mapNotNull { contactDao.findById(it)?.toDetail() }
        if (losers.isEmpty()) return@withContext survivor

        val merged = DuplicateFinder.merge(survivor, losers)
        updateContact(merged)

        // Carry over a photo and any group the survivor is not already in, then retire the rest.
        if (photoStore.read(survivorId) == null) {
            losers.firstNotNullOfOrNull { loser -> photoStore.read(loser.id) }
                ?.let { bytes -> setPhoto(survivorId, bytes, "image/jpeg") }
        }
        val survivorGroups = memberDao.groupIdsOf(survivorId).toSet()
        for (loser in losers) {
            memberDao.groupIdsOf(loser.id)
                .filterNot { it in survivorGroups }
                .forEach { addToGroup(it, survivorId) }
            deleteContact(loser.id)
        }
        contactDao.findById(survivorId)?.toDetail()
    }

    // --- Account -------------------------------------------------------------------------

    override suspend fun clearLocalData() = withContext(io) {
        val webId = prefs.webId()
        if (webId.isEmpty()) return@withContext
        memberDao.deleteAllFor(webId)
        contactDao.deleteAllFor(webId)
        groupDao.deleteAllFor(webId)
        bookDao.deleteAllFor(webId)
        outbox.clear(webId)
        photoStore.clear()
    }

    // --- The queued writes, replayed --------------------------------------------------------

    /**
     * Sends one queued write.
     *
     * An op whose subject has not reached the pod yet throws, which leaves it queued: the create
     * it depends on sits in front of it in the same queue and will have run by the next attempt.
     */
    private suspend fun execute(webId: String, type: OpType, payload: OutboxPayload) {
        when (type) {
            OpType.CREATE_BOOK -> {
                val row = payload.bookId?.let { bookDao.findById(it) } ?: return
                if (row.uri != null) return
                val created = remote.createBook(webId, row.title, row.isPrivate)
                bookDao.setUri(row.id, created.uri, SyncState.SYNCED)
            }

            OpType.RENAME_BOOK -> {
                val row = payload.bookId?.let { bookDao.findById(it) } ?: return
                val uri = row.uri ?: error("The address book has not reached the pod yet.")
                remote.renameBook(webId, uri, row.title)
                bookDao.setSyncState(row.id, SyncState.SYNCED)
            }

            OpType.DELETE_BOOK -> {
                val bookId = payload.bookId ?: return
                val uri = payload.uri ?: bookDao.findById(bookId)?.uri
                if (uri != null) remote.deleteBook(webId, uri)
                purgeBookLocally(webId, bookId)
            }

            OpType.CREATE_CONTACT -> {
                val row = payload.contactId?.let { contactDao.findById(it) } ?: return
                if (row.uri != null) return
                val bookUri = bookUriOf(row.bookId)
                val groupUris = memberDao.groupIdsOf(row.id).mapNotNull { groupDao.findById(it)?.uri }
                val created = remote.createContact(webId, bookUri, row.toDetail().toContactData(), groupUris)
                contactDao.setUri(row.id, created.uri, SyncState.SYNCED)
                pushPhotoIfAny(webId, row.id, created.uri)
            }

            OpType.UPDATE_CONTACT -> {
                val row = payload.contactId?.let { contactDao.findById(it) } ?: return
                val uri = row.uri ?: error("The contact has not reached the pod yet.")
                val bookUri = bookUriOf(row.bookId)
                remote.updateContact(webId, bookUri, uri, row.toDetail().toContactData())
                contactDao.setSyncState(row.id, SyncState.SYNCED)
            }

            OpType.DELETE_CONTACT -> {
                val contactId = payload.contactId ?: return
                val uri = payload.uri ?: contactDao.findById(contactId)?.uri
                val bookId = payload.bookId ?: contactDao.findById(contactId)?.bookId
                if (uri != null && bookId != null) {
                    val bookUri = bookDao.findById(bookId)?.uri
                    if (bookUri != null) remote.deleteContact(webId, bookUri, uri)
                }
                memberDao.clearContact(contactId)
                contactDao.deleteById(contactId)
                photoStore.delete(contactId)
            }

            OpType.MOVE_CONTACT -> {
                val contactId = payload.contactId ?: return
                val row = contactDao.findById(contactId) ?: return
                val fromUri = payload.uri ?: row.uri ?: return
                val fromBookUri = payload.bookId?.let { bookDao.findById(it)?.uri }
                val toBookUri = bookUriOf(payload.targetBookId ?: row.bookId)
                val created = remote.createContact(webId, toBookUri, row.toDetail().toContactData())
                if (fromBookUri != null) {
                    runCatching { remote.deleteContact(webId, fromBookUri, fromUri) }
                }
                contactDao.setUri(contactId, created.uri, SyncState.SYNCED)
                pushPhotoIfAny(webId, contactId, created.uri)
            }

            OpType.SET_PHOTO -> {
                val contactId = payload.contactId ?: return
                val row = contactDao.findById(contactId) ?: return
                val uri = row.uri ?: error("The contact has not reached the pod yet.")
                val bytes = photoStore.read(contactId) ?: return
                // The stored copy is already scaled, so it fits the host's IPC budget.
                val updated = remote.setPhoto(webId, uri, bytes, "image/jpeg")
                contactDao.setPhoto(contactId, updated.photoUri, row.photoThumb)
            }

            OpType.REMOVE_PHOTO -> {
                val contactId = payload.contactId ?: return
                val uri = contactDao.findById(contactId)?.uri
                    ?: error("The contact has not reached the pod yet.")
                remote.removePhoto(webId, uri)
                contactDao.setPhoto(contactId, null, null)
                photoStore.delete(contactId)
            }

            OpType.CREATE_GROUP -> {
                val row = payload.groupId?.let { groupDao.findById(it) } ?: return
                if (row.uri != null) return
                val bookUri = bookUriOf(row.bookId)
                val memberUris = memberDao.contactIdsIn(row.id)
                    .mapNotNull { contactDao.findById(it)?.uri }
                val created = remote.createGroup(webId, bookUri, row.name, memberUris)
                groupDao.setUri(row.id, created.uri, SyncState.SYNCED)
            }

            OpType.DELETE_GROUP -> {
                val groupId = payload.groupId ?: return
                val uri = payload.uri ?: groupDao.findById(groupId)?.uri
                val bookUri = payload.bookId?.let { bookDao.findById(it)?.uri }
                if (uri != null && bookUri != null) remote.deleteGroup(webId, bookUri, uri)
                memberDao.clearGroup(groupId)
                groupDao.deleteById(groupId)
            }

            OpType.ADD_MEMBER -> {
                val groupUri = payload.groupId?.let { groupDao.findById(it)?.uri }
                    ?: error("The group has not reached the pod yet.")
                val contactUri = payload.contactId?.let { contactDao.findById(it)?.uri }
                    ?: error("The contact has not reached the pod yet.")
                remote.addGroupMember(webId, groupUri, contactUri)
            }

            OpType.REMOVE_MEMBER -> {
                val groupUri = payload.groupId?.let { groupDao.findById(it)?.uri } ?: return
                val contactUri = payload.contactId?.let { contactDao.findById(it)?.uri } ?: return
                remote.removeGroupMember(webId, groupUri, contactUri)
            }
        }
    }

    // --- Helpers -------------------------------------------------------------------------

    private suspend fun drainOutboxFor(webId: String): DrainResult =
        outbox.drain(webId) { type, payload -> execute(webId, type, payload) }

    private suspend fun enqueue(webId: String, type: OpType, payload: OutboxPayload) {
        if (webId.isEmpty()) return
        outbox.enqueue(webId, type, payload)
        scheduler.requestDrain()
    }

    /** Pulls one book's contacts and groups into the local database. */
    private suspend fun syncBookContents(
        webId: String,
        bookId: String,
        bookUri: String,
        now: Long,
    ) {
        val remoteContacts = remote.listContacts(webId, bookUri)
        val rows = remoteContacts.map { solid ->
            val id = localContactId(webId, solid.uri)
            val existing = contactDao.findById(id)
            val detail = solid.toDomain(id, bookId)
            detail.copy(isFavorite = detail.categories.any { it.equals(FAVORITES_CATEGORY, true) })
                .toEntity(webId, now, existing?.photoThumb, SyncState.SYNCED)
        }
        // Rows still carrying a local write stay as they are; the outbox owns them until it lands.
        val pendingIds = contactDao.getInBook(webId, bookId)
            .filter { it.syncState != SyncState.SYNCED }
            .map { it.id }
            .toSet()
        contactDao.upsertAll(rows.filterNot { it.id in pendingIds })
        contactDao.deleteSyncedInBookNotIn(webId, bookId, rows.map { it.id } + pendingIds)

        fetchMissingPhotos(webId, rows.filterNot { it.id in pendingIds })

        val book = remote.getBookOrNull(webId, bookUri) ?: return
        val groupRows = book.groups.map { group ->
            GroupEntity(
                id = localGroupId(webId, group.uri),
                webId = webId,
                bookId = bookId,
                uri = group.uri,
                name = group.name,
                syncState = SyncState.SYNCED,
                updatedAt = now,
            )
        }
        val pendingGroupIds = groupDao.getInBook(webId, bookId)
            .filter { it.syncState != SyncState.SYNCED }
            .map { it.id }
            .toSet()
        groupDao.upsertAll(groupRows.filterNot { it.id in pendingGroupIds })
        groupDao.deleteSyncedInBookNotIn(webId, bookId, groupRows.map { it.id } + pendingGroupIds)

        for (group in groupRows.filterNot { it.id in pendingGroupIds }) {
            val uri = group.uri ?: continue
            val full = remote.getGroupOrNull(webId, uri) ?: continue
            val members = full.contacts.mapNotNull { member ->
                contactDao.findByUri(webId, member.uri)?.id
            }.map { GroupMemberEntity(group.id, it, webId) }
            memberDao.replaceGroup(group.id, members)
        }
    }

    /** Fetches the photos we do not hold yet, a few at a time, and never fails the sync over one. */
    private suspend fun fetchMissingPhotos(webId: String, rows: List<ContactEntity>) {
        val wanted = rows.filter { it.photoUri != null && it.photoThumb == null }
        if (wanted.isEmpty()) return
        val gate = Semaphore(PHOTO_FETCH_CONCURRENCY)
        coroutineScope {
            wanted.map { row ->
                async {
                    gate.withPermit {
                        val uri = row.photoUri ?: return@withPermit
                        val photo = runCatching { remote.getPhotoOrNull(webId, uri) }.getOrNull()
                        if (photo != null) storePhoto(row.id, uri, photo.bytes)
                    }
                }
            }.awaitAll()
        }
    }

    /** A contact created offline may have gained a photo before it had a URI. Send it now. */
    private suspend fun pushPhotoIfAny(webId: String, contactId: String, contactUri: String) {
        val row = contactDao.findById(contactId) ?: return
        val bytes = photoStore.read(contactId) ?: return
        runCatching {
            val updated = remote.setPhoto(webId, contactUri, bytes, "image/jpeg")
            contactDao.setPhoto(contactId, updated.photoUri, row.photoThumb)
        }
    }

    /**
     * Keeps a photo at the two sizes the app uses and throws the original away.
     *
     * Returns the display copy, which is what a detail screen draws and what the outbox sends on.
     */
    private suspend fun storePhoto(
        contactId: String,
        photoUri: String?,
        original: ByteArray,
    ): ByteArray? {
        val display = photoScaler.displayCopy(original) ?: return null
        photoStore.write(contactId, display)
        contactDao.setPhoto(contactId, photoUri, photoScaler.thumbnail(original))
        return display
    }

    private suspend fun purgeBookLocally(webId: String, bookId: String) {
        contactDao.getInBook(webId, bookId).forEach { memberDao.clearContact(it.id) }
        groupDao.getInBook(webId, bookId).forEach { memberDao.clearGroup(it.id) }
        contactDao.deleteInBook(webId, bookId)
        groupDao.getInBook(webId, bookId).forEach { groupDao.deleteById(it.id) }
        bookDao.deleteById(bookId)
    }

    private suspend fun bookUriOf(bookId: String): String =
        bookDao.findById(bookId)?.uri
            ?: error("The address book has not reached the pod yet.")

    /** Local ids are derived from the pod URI, so a refresh recognises a row it already holds. */
    private suspend fun localBookId(webId: String, uri: String): String =
        bookDao.findByUri(webId, uri)?.id ?: stableId("book", uri)

    private suspend fun localContactId(webId: String, uri: String): String =
        contactDao.findByUri(webId, uri)?.id ?: stableId("contact", uri)

    private suspend fun localGroupId(webId: String, uri: String): String =
        groupDao.findByUri(webId, uri)?.id ?: stableId("group", uri)

    private fun stableId(prefix: String, uri: String): String =
        "$prefix-${UUID.nameUUIDFromBytes(uri.toByteArray())}"

    private fun groupsWithCounts(
        webId: String,
        groups: Flow<List<GroupEntity>>,
    ): Flow<List<ContactGroup>> =
        combine(groups, memberDao.observeMemberCounts(webId)) { rows, counts ->
            val byId = counts.associate { it.groupId to it.count }
            rows.map { it.toDomain(byId[it.id] ?: 0) }
        }

    /**
     * Re-runs [block] whenever the signed-in account changes, so every flow follows the account.
     *
     * With no account connected it emits [empty] rather than nothing at all: a flow that never
     * emits would hang any caller that awaits its first value.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun <T> forWebId(empty: T, block: (String) -> Flow<T>): Flow<T> =
        prefs.webIdFlow.distinctUntilChanged().flatMapLatest { webId ->
            if (webId.isEmpty()) flowOf(empty) else block(webId)
        }

    /** Keeps the favourite star and the vCard category saying the same thing. */
    private fun ContactDetail.withFavoriteCategory(favorite: Boolean): ContactDetail {
        val others = categories.filterNot { it.equals(FAVORITES_CATEGORY, ignoreCase = true) }
        return copy(
            categories = if (favorite) others + FAVORITES_CATEGORY else others,
            isFavorite = favorite,
        )
    }
}

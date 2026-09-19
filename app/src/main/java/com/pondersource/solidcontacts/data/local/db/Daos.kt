package com.pondersource.solidcontacts.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.pondersource.solidcontacts.domain.model.SyncState
import kotlinx.coroutines.flow.Flow

/** A book row plus the counts the list screen shows, computed in SQL rather than in Kotlin. */
data class AddressBookWithCounts(
    val id: String,
    val webId: String,
    val uri: String?,
    val title: String,
    val isPrivate: Boolean,
    val syncState: SyncState,
    val updatedAt: Long,
    val contactCount: Int,
    val groupCount: Int,
)

@Dao
interface AddressBookDao {

    @Query(
        """
        SELECT b.*,
               (SELECT COUNT(*) FROM contact c
                 WHERE c.bookId = b.id AND c.syncState != 'PENDING_DELETE') AS contactCount,
               (SELECT COUNT(*) FROM contact_group g
                 WHERE g.bookId = b.id AND g.syncState != 'PENDING_DELETE') AS groupCount
          FROM address_book b
         WHERE b.webId = :webId AND b.syncState != 'PENDING_DELETE'
         ORDER BY b.title COLLATE NOCASE ASC
        """,
    )
    fun observeAll(webId: String): Flow<List<AddressBookWithCounts>>

    @Query("SELECT * FROM address_book WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<AddressBookEntity?>

    @Query("SELECT * FROM address_book WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): AddressBookEntity?

    @Query("SELECT * FROM address_book WHERE webId = :webId AND uri = :uri LIMIT 1")
    suspend fun findByUri(webId: String, uri: String): AddressBookEntity?

    @Query("SELECT * FROM address_book WHERE webId = :webId AND syncState != 'PENDING_DELETE'")
    suspend fun getAll(webId: String): List<AddressBookEntity>

    @Upsert
    suspend fun upsert(book: AddressBookEntity)

    @Upsert
    suspend fun upsertAll(books: List<AddressBookEntity>)

    @Query("UPDATE address_book SET uri = :uri, syncState = :state WHERE id = :id")
    suspend fun setUri(id: String, uri: String, state: SyncState)

    @Query("UPDATE address_book SET syncState = :state WHERE id = :id")
    suspend fun setSyncState(id: String, state: SyncState)

    @Query("UPDATE address_book SET title = :title, syncState = :state, updatedAt = :now WHERE id = :id")
    suspend fun rename(id: String, title: String, state: SyncState, now: Long)

    @Query("DELETE FROM address_book WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM address_book WHERE webId = :webId")
    suspend fun deleteAllFor(webId: String)

    @Query(
        "DELETE FROM address_book WHERE webId = :webId AND syncState = 'SYNCED' " +
            "AND id NOT IN (:keepIds)",
    )
    suspend fun deleteSyncedNotIn(webId: String, keepIds: List<String>)
}

@Dao
interface ContactDao {

    @Query(
        "SELECT * FROM contact WHERE webId = :webId AND syncState != 'PENDING_DELETE' " +
            "ORDER BY sortKey COLLATE NOCASE ASC",
    )
    fun observeAll(webId: String): Flow<List<ContactEntity>>

    @Query(
        "SELECT * FROM contact WHERE webId = :webId AND bookId = :bookId " +
            "AND syncState != 'PENDING_DELETE' ORDER BY sortKey COLLATE NOCASE ASC",
    )
    fun observeInBook(webId: String, bookId: String): Flow<List<ContactEntity>>

    /** Matches the one lower-cased haystack column, so a search hits any field at index speed. */
    @Query(
        "SELECT * FROM contact WHERE webId = :webId AND syncState != 'PENDING_DELETE' " +
            "AND searchText LIKE '%' || :query || '%' ORDER BY sortKey COLLATE NOCASE ASC",
    )
    fun search(webId: String, query: String): Flow<List<ContactEntity>>

    @Query(
        "SELECT * FROM contact WHERE webId = :webId AND isFavorite = 1 " +
            "AND syncState != 'PENDING_DELETE' ORDER BY sortKey COLLATE NOCASE ASC",
    )
    fun observeFavorites(webId: String): Flow<List<ContactEntity>>

    @Query(
        """
        SELECT c.* FROM contact c
          JOIN group_member m ON m.contactId = c.id
         WHERE m.groupId = :groupId AND c.syncState != 'PENDING_DELETE'
         ORDER BY c.sortKey COLLATE NOCASE ASC
        """,
    )
    fun observeInGroup(groupId: String): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contact WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<ContactEntity?>

    @Query("SELECT * FROM contact WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ContactEntity?

    @Query("SELECT * FROM contact WHERE webId = :webId AND uri = :uri LIMIT 1")
    suspend fun findByUri(webId: String, uri: String): ContactEntity?

    @Query("SELECT * FROM contact WHERE webId = :webId AND syncState != 'PENDING_DELETE'")
    suspend fun getAll(webId: String): List<ContactEntity>

    @Query("SELECT * FROM contact WHERE webId = :webId AND bookId = :bookId")
    suspend fun getInBook(webId: String, bookId: String): List<ContactEntity>

    @Query("SELECT COUNT(*) FROM contact WHERE webId = :webId AND syncState != 'PENDING_DELETE'")
    fun observeCount(webId: String): Flow<Int>

    @Upsert
    suspend fun upsert(contact: ContactEntity)

    @Upsert
    suspend fun upsertAll(contacts: List<ContactEntity>)

    @Query("UPDATE contact SET uri = :uri, syncState = :state WHERE id = :id")
    suspend fun setUri(id: String, uri: String, state: SyncState)

    @Query("UPDATE contact SET syncState = :state WHERE id = :id")
    suspend fun setSyncState(id: String, state: SyncState)

    @Query("UPDATE contact SET isFavorite = :favorite, updatedAt = :now WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean, now: Long)

    @Query("UPDATE contact SET photoUri = :photoUri, photoThumb = :thumb WHERE id = :id")
    suspend fun setPhoto(id: String, photoUri: String?, thumb: ByteArray?)

    @Query("UPDATE contact SET bookId = :bookId, updatedAt = :now WHERE id = :id")
    suspend fun setBook(id: String, bookId: String, now: Long)

    @Query("DELETE FROM contact WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM contact WHERE webId = :webId AND bookId = :bookId")
    suspend fun deleteInBook(webId: String, bookId: String)

    @Query("DELETE FROM contact WHERE webId = :webId")
    suspend fun deleteAllFor(webId: String)

    @Query(
        "DELETE FROM contact WHERE webId = :webId AND bookId = :bookId " +
            "AND syncState = 'SYNCED' AND id NOT IN (:keepIds)",
    )
    suspend fun deleteSyncedInBookNotIn(webId: String, bookId: String, keepIds: List<String>)

    /**
     * Replaces the synced rows of one book with what the pod just returned, leaving rows that
     * still carry an un-drained local write alone.
     */
    @Transaction
    suspend fun replaceBookContents(webId: String, bookId: String, rows: List<ContactEntity>) {
        upsertAll(rows)
        deleteSyncedInBookNotIn(webId, bookId, rows.map { it.id })
    }
}

@Dao
interface GroupDao {

    @Query(
        "SELECT * FROM contact_group WHERE webId = :webId AND syncState != 'PENDING_DELETE' " +
            "ORDER BY name COLLATE NOCASE ASC",
    )
    fun observeAll(webId: String): Flow<List<GroupEntity>>

    @Query(
        "SELECT * FROM contact_group WHERE webId = :webId AND bookId = :bookId " +
            "AND syncState != 'PENDING_DELETE' ORDER BY name COLLATE NOCASE ASC",
    )
    fun observeInBook(webId: String, bookId: String): Flow<List<GroupEntity>>

    @Query("SELECT * FROM contact_group WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<GroupEntity?>

    @Query("SELECT * FROM contact_group WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): GroupEntity?

    @Query("SELECT * FROM contact_group WHERE webId = :webId AND uri = :uri LIMIT 1")
    suspend fun findByUri(webId: String, uri: String): GroupEntity?

    @Query("SELECT * FROM contact_group WHERE webId = :webId AND bookId = :bookId")
    suspend fun getInBook(webId: String, bookId: String): List<GroupEntity>

    @Upsert
    suspend fun upsert(group: GroupEntity)

    @Upsert
    suspend fun upsertAll(groups: List<GroupEntity>)

    @Query("UPDATE contact_group SET uri = :uri, syncState = :state WHERE id = :id")
    suspend fun setUri(id: String, uri: String, state: SyncState)

    @Query("UPDATE contact_group SET syncState = :state WHERE id = :id")
    suspend fun setSyncState(id: String, state: SyncState)

    @Query("UPDATE contact_group SET name = :name, syncState = :state, updatedAt = :now WHERE id = :id")
    suspend fun rename(id: String, name: String, state: SyncState, now: Long)

    @Query("DELETE FROM contact_group WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM contact_group WHERE webId = :webId")
    suspend fun deleteAllFor(webId: String)

    @Query(
        "DELETE FROM contact_group WHERE webId = :webId AND bookId = :bookId " +
            "AND syncState = 'SYNCED' AND id NOT IN (:keepIds)",
    )
    suspend fun deleteSyncedInBookNotIn(webId: String, bookId: String, keepIds: List<String>)
}

@Dao
interface GroupMemberDao {

    @Query("SELECT groupId FROM group_member WHERE contactId = :contactId")
    fun observeGroupIdsOf(contactId: String): Flow<List<String>>

    @Query("SELECT groupId FROM group_member WHERE contactId = :contactId")
    suspend fun groupIdsOf(contactId: String): List<String>

    @Query("SELECT contactId FROM group_member WHERE groupId = :groupId")
    suspend fun contactIdsIn(groupId: String): List<String>

    @Query("SELECT COUNT(*) FROM group_member WHERE groupId = :groupId")
    suspend fun memberCount(groupId: String): Int

    @Query("SELECT groupId, COUNT(*) AS count FROM group_member WHERE webId = :webId GROUP BY groupId")
    fun observeMemberCounts(webId: String): Flow<List<GroupMemberCount>>

    @Insert(onConflict = androidx.room.OnConflictStrategy.IGNORE)
    suspend fun insert(member: GroupMemberEntity)

    @Insert(onConflict = androidx.room.OnConflictStrategy.IGNORE)
    suspend fun insertAll(members: List<GroupMemberEntity>)

    @Query("DELETE FROM group_member WHERE groupId = :groupId AND contactId = :contactId")
    suspend fun remove(groupId: String, contactId: String)

    @Query("DELETE FROM group_member WHERE groupId = :groupId")
    suspend fun clearGroup(groupId: String)

    @Query("DELETE FROM group_member WHERE contactId = :contactId")
    suspend fun clearContact(contactId: String)

    @Query("DELETE FROM group_member WHERE webId = :webId")
    suspend fun deleteAllFor(webId: String)

    @Transaction
    suspend fun replaceGroup(groupId: String, members: List<GroupMemberEntity>) {
        clearGroup(groupId)
        insertAll(members)
    }
}

data class GroupMemberCount(val groupId: String, val count: Int)

@Dao
interface OutboxDao {

    @Insert
    suspend fun insert(op: OutboxOpEntity): Long

    @Query(
        "SELECT * FROM outbox_op WHERE webId = :webId AND status IN ('PENDING', 'FAILED') " +
            "AND nextRetryAt <= :now ORDER BY id ASC",
    )
    suspend fun due(webId: String, now: Long): List<OutboxOpEntity>

    @Query("SELECT * FROM outbox_op WHERE webId = :webId ORDER BY id ASC")
    suspend fun allFor(webId: String): List<OutboxOpEntity>

    @Query("SELECT DISTINCT webId FROM outbox_op WHERE status IN ('PENDING', 'FAILED')")
    suspend fun webIdsWithWork(): List<String>

    @Query(
        "SELECT COUNT(*) FROM outbox_op WHERE webId = :webId AND status IN ('PENDING', 'FAILED')",
    )
    fun observePendingCount(webId: String): Flow<Int>

    @Update
    suspend fun update(op: OutboxOpEntity)

    @Query("DELETE FROM outbox_op WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM outbox_op WHERE webId = :webId")
    suspend fun deleteAllFor(webId: String)
}

package com.pondersource.solidcontacts.data.repository

import com.pondersource.solidcontacts.data.outbox.DrainResult
import com.pondersource.solidcontacts.domain.model.AddressBookSummary
import com.pondersource.solidcontacts.domain.model.ContactDetail
import com.pondersource.solidcontacts.domain.model.ContactGroup
import com.pondersource.solidcontacts.domain.model.ContactSummary
import com.pondersource.solidcontacts.domain.model.DuplicateCluster
import kotlinx.coroutines.flow.Flow

/**
 * Contacts, as the app sees them.
 *
 * Every `observe*` reads the local database, so a screen paints immediately and works offline.
 * Every write lands locally first and is queued for the pod; none of them waits for a network
 * round trip, and none of them is lost if the device is offline when it happens.
 */
interface ContactsRepository {

    /** `true` once the IPC link to the Solid host app is up. Drives the "connecting" state only. */
    fun connectionState(): Flow<Boolean>

    /** Writes still waiting for the pod. */
    fun observePendingWrites(): Flow<Int>

    // --- Reads ---------------------------------------------------------------------------

    fun observeBooks(): Flow<List<AddressBookSummary>>

    fun observeBook(bookId: String): Flow<AddressBookSummary?>

    fun observeAllContacts(): Flow<List<ContactSummary>>

    fun observeContactsInBook(bookId: String): Flow<List<ContactSummary>>

    /** Contacts whose name, number, address, company or note contains [query]. */
    fun searchContacts(query: String): Flow<List<ContactSummary>>

    fun observeFavorites(): Flow<List<ContactSummary>>

    fun observeContact(contactId: String): Flow<ContactDetail?>

    fun observeGroups(): Flow<List<ContactGroup>>

    fun observeGroupsInBook(bookId: String): Flow<List<ContactGroup>>

    fun observeGroup(groupId: String): Flow<ContactGroup?>

    fun observeGroupMembers(groupId: String): Flow<List<ContactSummary>>

    fun observeGroupIdsOf(contactId: String): Flow<List<String>>

    fun observeContactCount(): Flow<Int>

    suspend fun contactPhoto(contactId: String): ByteArray?

    suspend fun getContact(contactId: String): ContactDetail?

    // --- Sync ----------------------------------------------------------------------------

    /** Pulls the pod's state into the local database. Safe to call often. */
    suspend fun refresh(): Result<Unit>

    /** Sends every queued write the pod will accept right now. */
    suspend fun drainOutbox(): Result<DrainResult>

    /** Puts every failed write back in line immediately. */
    suspend fun retryFailedWrites()

    // --- Address books -------------------------------------------------------------------

    /** Returns the id of the book new contacts go into, creating it when the pod has none. */
    suspend fun ensureDefaultBook(): String

    suspend fun createBook(title: String, isPrivate: Boolean): String

    suspend fun renameBook(bookId: String, title: String)

    suspend fun deleteBook(bookId: String)

    // --- Contacts ------------------------------------------------------------------------

    /** Saves a new contact and returns its local id. */
    suspend fun createContact(bookId: String, detail: ContactDetail, groupIds: List<String> = emptyList()): String

    suspend fun updateContact(detail: ContactDetail)

    suspend fun deleteContact(contactId: String)

    suspend fun deleteContacts(contactIds: List<String>)

    suspend fun moveContact(contactId: String, targetBookId: String)

    suspend fun setFavorite(contactId: String, favorite: Boolean)

    suspend fun setPhoto(contactId: String, bytes: ByteArray, contentType: String)

    suspend fun removePhoto(contactId: String)

    /** Adds many contacts at once, as an import does. Returns how many were saved. */
    suspend fun importContacts(bookId: String, contacts: List<ContactDetail>): Int

    // --- Groups --------------------------------------------------------------------------

    suspend fun createGroup(bookId: String, name: String, memberIds: List<String> = emptyList()): String

    suspend fun deleteGroup(groupId: String)

    suspend fun addToGroup(groupId: String, contactId: String)

    suspend fun removeFromGroup(groupId: String, contactId: String)

    // --- Duplicates ----------------------------------------------------------------------

    suspend fun findDuplicates(): List<DuplicateCluster>

    /** Folds [loserIds] into [survivorId] and deletes them. Returns the merged contact. */
    suspend fun mergeContacts(survivorId: String, loserIds: List<String>): ContactDetail?

    // --- Account -------------------------------------------------------------------------

    /** Drops every cached row and queued write for the signed-in account. */
    suspend fun clearLocalData()
}

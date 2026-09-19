package com.pondersource.solidcontacts.data.remote

import com.erfangholami.androidsolidservices.client.sdk.SolidContactsDataModule
import com.erfangholami.androidsolidservices.shared.model.contacts.AddressBook
import com.erfangholami.androidsolidservices.shared.model.contacts.AddressBookList
import com.erfangholami.androidsolidservices.shared.model.contacts.ContactData
import com.erfangholami.androidsolidservices.shared.model.contacts.ContactMatch
import com.erfangholami.androidsolidservices.shared.model.contacts.ContactPhoto
import com.erfangholami.androidsolidservices.shared.model.contacts.FullGroup
import com.erfangholami.androidsolidservices.shared.model.contacts.SolidContact
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The pod, as this app talks to it.
 *
 * Wraps the contacts data module so the repository has one place to reach the network, and so
 * every "the service returned nothing" case becomes a thrown [IllegalStateException] rather than
 * a `null` each caller has to re-check. Callers that genuinely tolerate a miss use the `orNull`
 * variants.
 */
@Singleton
class SolidContactsSource @Inject constructor(
    private val module: SolidContactsDataModule,
) {

    /** Emits `true` once the IPC binding to the host app is up. */
    fun connectionState(): Flow<Boolean> = module.contactsDataModuleServiceConnectionState()

    // --- Address books -------------------------------------------------------------------

    suspend fun listBooks(webId: String): AddressBookList =
        module.books.list(webId).require("list address books")

    /** Creates the contacts container when the pod has none yet, then lists what is in it. */
    suspend fun ensureBookContainer(webId: String): AddressBookList =
        module.books.ensureContainer(webId).require("prepare the contacts container")

    suspend fun getBook(webId: String, bookUri: String): AddressBook =
        module.books.get(webId, bookUri).require("read the address book")

    suspend fun getBookOrNull(webId: String, bookUri: String): AddressBook? =
        runCatching { module.books.get(webId, bookUri) }.getOrNull()

    suspend fun createBook(webId: String, title: String, isPrivate: Boolean): AddressBook =
        module.books.create(webId, title, isPrivate).require("create the address book")

    suspend fun renameBook(webId: String, bookUri: String, newTitle: String): AddressBook =
        module.books.rename(webId, bookUri, newTitle).require("rename the address book")

    suspend fun deleteBook(webId: String, bookUri: String): AddressBook =
        module.books.delete(webId, bookUri).require("delete the address book")

    suspend fun ensureDefaultBook(webId: String, title: String): AddressBook =
        module.books.ensureDefault(webId, title = title).require("prepare the default address book")

    // --- Contacts ------------------------------------------------------------------------

    suspend fun listContacts(webId: String, bookUri: String): List<SolidContact> =
        module.contacts.list(webId, bookUri).require("list contacts").contacts

    suspend fun getContact(webId: String, contactUri: String): SolidContact =
        module.contacts.get(webId, contactUri).require("read the contact")

    suspend fun createContact(
        webId: String,
        bookUri: String,
        data: ContactData,
        groupUris: List<String> = emptyList(),
    ): SolidContact =
        module.contacts.create(webId, bookUri, data, groupUris).require("create the contact")

    suspend fun updateContact(
        webId: String,
        bookUri: String,
        contactUri: String,
        data: ContactData,
    ): SolidContact =
        module.contacts.update(webId, bookUri, contactUri, data).require("update the contact")

    suspend fun deleteContact(webId: String, bookUri: String, contactUri: String): SolidContact =
        module.contacts.delete(webId, bookUri, contactUri).require("delete the contact")

    suspend fun setPhoto(
        webId: String,
        contactUri: String,
        photo: ByteArray,
        contentType: String,
    ): SolidContact =
        module.contacts.setPhoto(webId, contactUri, photo, contentType).require("set the photo")

    suspend fun removePhoto(webId: String, contactUri: String): SolidContact =
        module.contacts.removePhoto(webId, contactUri).require("remove the photo")

    suspend fun getPhotoOrNull(webId: String, photoUri: String): ContactPhoto? =
        runCatching { module.contacts.getPhoto(webId, photoUri) }.getOrNull()

    suspend fun findByWebId(webId: String, targetWebId: String): ContactMatch? =
        runCatching { module.contacts.findByWebId(webId, targetWebId) }.getOrNull()

    // --- Groups --------------------------------------------------------------------------

    suspend fun createGroup(
        webId: String,
        bookUri: String,
        title: String,
        contactUris: List<String> = emptyList(),
    ): FullGroup =
        module.groups.create(webId, bookUri, title, contactUris).require("create the group")

    suspend fun getGroup(webId: String, groupUri: String): FullGroup =
        module.groups.get(webId, groupUri).require("read the group")

    suspend fun getGroupOrNull(webId: String, groupUri: String): FullGroup? =
        runCatching { module.groups.get(webId, groupUri) }.getOrNull()

    suspend fun deleteGroup(webId: String, bookUri: String, groupUri: String): FullGroup =
        module.groups.delete(webId, bookUri, groupUri).require("delete the group")

    suspend fun addGroupMember(webId: String, groupUri: String, contactUri: String): FullGroup =
        module.groups.addMember(webId, groupUri, contactUri).require("add the group member")

    suspend fun removeGroupMember(webId: String, groupUri: String, contactUri: String): FullGroup =
        module.groups.removeMember(webId, groupUri, contactUri).require("remove the group member")

    private fun <T : Any> T?.require(what: String): T =
        this ?: throw IllegalStateException("The Solid host returned no result when asked to $what.")
}

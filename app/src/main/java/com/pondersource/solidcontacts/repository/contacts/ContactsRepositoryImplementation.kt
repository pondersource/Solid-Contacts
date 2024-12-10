package com.pondersource.solidcontacts.repository.contacts

import android.util.Log
import com.pondersource.shared.data.datamodule.contact.AddressBook
import com.pondersource.shared.data.datamodule.contact.AddressBookList
import com.pondersource.shared.data.datamodule.contact.FullContact
import com.pondersource.shared.data.datamodule.contact.FullGroup
import com.pondersource.shared.data.datamodule.contact.NewContact
import com.pondersource.solidandroidclient.sdk.SolidContactsDataModule
import kotlinx.coroutines.flow.Flow

class ContactsRepositoryImplementation (
    private val contactDataModule: SolidContactsDataModule,
): ContactsRepository {

    override fun contactsServiceConnectionState(): Flow<Boolean> {
        return contactDataModule.contactsDataModuleServiceConnectionState()
    }

    override suspend fun getAddressBooks(): AddressBookList? {
        try {
            return contactDataModule.getAddressBooks()
        } catch (e: Exception) {
            Log.d("TAG", "service is not connected")
            return null
        }
    }

    override suspend fun createNewAddressBook(name: String, isPrivate: Boolean): AddressBook? {
        return contactDataModule.createAddressBook(
            title = name,
            isPrivate = isPrivate
        )
    }

    override suspend fun getAddressBook(addressBookUri: String): AddressBook? {
        return contactDataModule.getAddressBook(addressBookUri)
    }

    override suspend fun deleteAddressBook(addressBookUri: String): AddressBook? {
        return contactDataModule.deleteAddressBook(addressBookUri)
    }

    override suspend fun getContact(contactUri: String): FullContact? {
        return contactDataModule.getContact(contactUri)
    }

    override suspend fun createContact(addressBookUri: String, name: String, email: String, phone: String, groups: List<String>): FullContact? {
        val newContact = NewContact(name, email, phone)
        return contactDataModule.createNewContact(addressBookUri, newContact, groups)
    }

    override suspend fun deleteContact(addressBookUri: String, contactUri: String): FullContact? {
        return contactDataModule.deleteContact(addressBookUri, contactUri)
    }

    override suspend fun createGroup(
        addressBookUri: String,
        title: String,
        contacts: List<String>
    ): FullGroup? {
        return contactDataModule.createNewGroup(
            addressBookUri,
            title,
            contacts
        )
    }

    override suspend fun getGroup(groupUri: String): FullGroup? {
        return contactDataModule.getGroup(groupUri)
    }

    override suspend fun deleteGroup(addressBookUri: String, groupUri: String): FullGroup? {
        return contactDataModule.deleteGroup(addressBookUri, groupUri)
    }
}
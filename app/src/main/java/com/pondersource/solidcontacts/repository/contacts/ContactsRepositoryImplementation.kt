package com.pondersource.solidcontacts.repository.contacts

import com.pondersource.shared.data.datamodule.contact.AddressBook
import com.pondersource.shared.data.datamodule.contact.AddressBookList
import com.pondersource.shared.data.datamodule.contact.FullContact
import com.pondersource.shared.data.datamodule.contact.FullGroup
import com.pondersource.shared.data.datamodule.contact.NewContact
import com.pondersource.solidandroidclient.sdk.SolidContactsDataModule

class ContactsRepositoryImplementation (
    private val contactDataModule: SolidContactsDataModule,
): ContactsRepository {
    override suspend fun getAddressBooks(): AddressBookList? {
        return contactDataModule.getAddressBooks()
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

    override suspend fun getContact(contactUri: String): FullContact? {
        return contactDataModule.getContact(contactUri)
    }

    override suspend fun createContact(addressBookUri: String, name: String, email: String, phone: String, groups: List<String>): FullContact? {
        val newContact = NewContact(name, email, phone)
        return contactDataModule.createNewContact(addressBookUri, newContact, groups)
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
}
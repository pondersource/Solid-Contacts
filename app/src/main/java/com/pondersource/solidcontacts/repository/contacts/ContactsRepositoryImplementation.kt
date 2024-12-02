package com.pondersource.solidcontacts.repository.contacts

import com.pondersource.shared.data.datamodule.contact.AddressBook
import com.pondersource.shared.data.datamodule.contact.AddressBookList
import com.pondersource.shared.data.datamodule.contact.FullContact
import com.pondersource.shared.data.datamodule.contact.FullGroup
import com.pondersource.solidandroidclient.sdk.SolidContactsDataModule

class ContactsRepositoryImplementation (
    private val contactDataModule: SolidContactsDataModule,
): ContactsRepository {
    override fun getAddressBooks(): AddressBookList? {
        return contactDataModule.getAddressBooks()
    }

    override fun getAddressBook(addressBookUri: String): AddressBook? {
        return contactDataModule.getAddressBook(addressBookUri)
    }

    override fun getContact(contactUri: String): FullContact? {
        return contactDataModule.getContact(contactUri)
    }

    override fun getGroup(groupUri: String): FullGroup? {
        return contactDataModule.getGroup(groupUri)
    }
}
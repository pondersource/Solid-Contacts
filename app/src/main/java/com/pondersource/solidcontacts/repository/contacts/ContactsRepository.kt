package com.pondersource.solidcontacts.repository.contacts

import com.pondersource.shared.data.datamodule.contact.AddressBook
import com.pondersource.shared.data.datamodule.contact.AddressBookList
import com.pondersource.shared.data.datamodule.contact.FullContact
import com.pondersource.shared.data.datamodule.contact.FullGroup

interface ContactsRepository {

    fun getAddressBooks(): AddressBookList?

    fun getAddressBook(addressBookUri: String): AddressBook?

    fun getContact(contactUri: String): FullContact?

    fun getGroup(groupUri: String): FullGroup?

}
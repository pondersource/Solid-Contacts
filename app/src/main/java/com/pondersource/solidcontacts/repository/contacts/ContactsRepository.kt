package com.pondersource.solidcontacts.repository.contacts

import com.pondersource.shared.data.datamodule.contact.AddressBook
import com.pondersource.shared.data.datamodule.contact.AddressBookList
import com.pondersource.shared.data.datamodule.contact.FullContact
import com.pondersource.shared.data.datamodule.contact.FullGroup
import kotlinx.coroutines.flow.Flow

interface ContactsRepository {

    fun contactsServiceConnectionState(): Flow<Boolean>

    suspend fun getAddressBooks(): AddressBookList?

    suspend fun createNewAddressBook(name: String, isPrivate: Boolean = true): AddressBook?

    suspend fun getAddressBook(addressBookUri: String): AddressBook?

    suspend fun deleteAddressBook(addressBookUri: String): AddressBook?

    suspend fun getContact(contactUri: String): FullContact?

    suspend fun createContact(
        addressBookUri: String,
        name: String,
        email: String,
        phone: String,
        groups: List<String>
    ): FullContact?

    suspend fun deleteContact(
        addressBookUri: String,
        contactUri: String,
    ): FullContact?

    suspend fun createGroup(
        addressBookUri: String,
        title: String,
        contacts: List<String>
    ): FullGroup?

    suspend fun getGroup(groupUri: String): FullGroup?

    suspend fun deleteGroup(
        addressBookUri: String,
        groupUri: String,
    ): FullGroup?
}
package com.pondersource.solidcontacts.repository.contacts

import com.erfangholami.androidsolidservices.shared.model.contacts.AddressBook
import com.erfangholami.androidsolidservices.shared.model.contacts.AddressBookList
import com.erfangholami.androidsolidservices.shared.model.contacts.FullGroup
import com.erfangholami.androidsolidservices.shared.model.contacts.SolidContact
import kotlinx.coroutines.flow.Flow

interface ContactsRepository {

    fun contactsServiceConnectionState(): Flow<Boolean>

    suspend fun getAddressBooks(): AddressBookList?

    suspend fun createNewAddressBook(name: String, isPrivate: Boolean = true): AddressBook?

    suspend fun getAddressBook(addressBookUri: String): AddressBook?

    suspend fun deleteAddressBook(addressBookUri: String): AddressBook?

    suspend fun getContact(contactUri: String): SolidContact?

    suspend fun createContact(
        addressBookUri: String,
        name: String,
        email: String,
        phone: String,
        groups: List<String>
    ): SolidContact?

    suspend fun deleteContact(
        addressBookUri: String,
        contactUri: String,
    ): SolidContact?

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

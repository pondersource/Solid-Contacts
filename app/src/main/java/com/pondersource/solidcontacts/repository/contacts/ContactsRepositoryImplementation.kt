package com.pondersource.solidcontacts.repository.contacts

import android.util.Log
import com.pondersource.shared.domain.datamodule.contact.AddressBook
import com.pondersource.shared.domain.datamodule.contact.AddressBookList
import com.pondersource.shared.domain.datamodule.contact.FullContact
import com.pondersource.shared.domain.datamodule.contact.FullGroup
import com.pondersource.shared.domain.datamodule.contact.NewContact
import com.pondersource.solidandroidclient.sdk.SolidContactsDataModule
import com.pondersource.solidcontacts.repository.user.UserRepository
import kotlinx.coroutines.flow.Flow

class ContactsRepositoryImplementation(
    private val contactDataModule: SolidContactsDataModule,
    val userRepository: UserRepository,
) : ContactsRepository {

    override fun contactsServiceConnectionState(): Flow<Boolean> {
        return contactDataModule.contactsDataModuleServiceConnectionState()
    }

    override suspend fun getAddressBooks(): AddressBookList? {
        try {
            return contactDataModule.getAddressBooks(userRepository.getGrantedWebId())
        } catch (e: Exception) {
            Log.d("TAG", "service is not connected")
            return null
        }
    }

    override suspend fun createNewAddressBook(name: String, isPrivate: Boolean): AddressBook? {
        return contactDataModule.createAddressBook(
            userRepository.getGrantedWebId(),
            title = name,
            isPrivate = isPrivate
        )
    }

    override suspend fun getAddressBook(addressBookUri: String): AddressBook? {
        return contactDataModule.getAddressBook(
            webId = userRepository.getGrantedWebId(),
            addressBookUri
        )
    }

    override suspend fun deleteAddressBook(addressBookUri: String): AddressBook? {
        return contactDataModule.deleteAddressBook(
            webId = userRepository.getGrantedWebId(),
            addressBookUri)
    }

    override suspend fun getContact(contactUri: String): FullContact? {
        return contactDataModule.getContact(userRepository.getGrantedWebId(), contactUri)
    }

    override suspend fun createContact(
        addressBookUri: String,
        name: String,
        email: String,
        phone: String,
        groups: List<String>
    ): FullContact? {
        val newContact = NewContact(name, email, phone)
        return contactDataModule.createNewContact(userRepository.getGrantedWebId(), addressBookUri, newContact, groups)
    }

    override suspend fun deleteContact(addressBookUri: String, contactUri: String): FullContact? {
        return contactDataModule.deleteContact(userRepository.getGrantedWebId(), addressBookUri, contactUri)
    }

    override suspend fun createGroup(
        addressBookUri: String,
        title: String,
        contacts: List<String>
    ): FullGroup? {
        return contactDataModule.createNewGroup(
            userRepository.getGrantedWebId(),
            addressBookUri,
            title,
            contacts
        )
    }

    override suspend fun getGroup(groupUri: String): FullGroup? {
        return contactDataModule.getGroup(
            userRepository.getGrantedWebId(),
            groupUri
        )
    }

    override suspend fun deleteGroup(addressBookUri: String, groupUri: String): FullGroup? {
        return contactDataModule.deleteGroup(userRepository.getGrantedWebId(), addressBookUri, groupUri)
    }
}
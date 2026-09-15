package com.pondersource.solidcontacts.repository.contacts

import com.erfangholami.androidsolidservices.client.sdk.SolidContactsDataModule
import com.erfangholami.androidsolidservices.shared.model.contacts.AddressBook
import com.erfangholami.androidsolidservices.shared.model.contacts.AddressBookList
import com.erfangholami.androidsolidservices.shared.model.contacts.EmailType
import com.erfangholami.androidsolidservices.shared.model.contacts.FullGroup
import com.erfangholami.androidsolidservices.shared.model.contacts.PhoneType
import com.erfangholami.androidsolidservices.shared.model.contacts.SolidContact
import com.erfangholami.androidsolidservices.shared.model.contacts.contactData
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
        return runCatching {
            contactDataModule.books.list(userRepository.getGrantedWebId())
        }.getOrNull()
    }

    override suspend fun createNewAddressBook(name: String, isPrivate: Boolean): AddressBook? {
        return contactDataModule.books.create(
            webId = userRepository.getGrantedWebId(),
            title = name,
            isPrivate = isPrivate,
        )
    }

    override suspend fun getAddressBook(addressBookUri: String): AddressBook? {
        return contactDataModule.books.get(
            webId = userRepository.getGrantedWebId(),
            addressBookUri = addressBookUri,
        )
    }

    override suspend fun deleteAddressBook(addressBookUri: String): AddressBook? {
        return contactDataModule.books.delete(
            webId = userRepository.getGrantedWebId(),
            addressBookUri = addressBookUri,
        )
    }

    override suspend fun getContact(contactUri: String): SolidContact? {
        return contactDataModule.contacts.get(userRepository.getGrantedWebId(), contactUri)
    }

    override suspend fun createContact(
        addressBookUri: String,
        name: String,
        email: String,
        phone: String,
        groups: List<String>
    ): SolidContact? {
        val data = contactData {
            fullName = name
            email(email, EmailType.HOME)
            phone(phone, PhoneType.CELL)
        }
        return contactDataModule.contacts.create(
            webId = userRepository.getGrantedWebId(),
            addressBookUri = addressBookUri,
            data = data,
            groupUris = groups,
        )
    }

    override suspend fun deleteContact(addressBookUri: String, contactUri: String): SolidContact? {
        return contactDataModule.contacts.delete(
            userRepository.getGrantedWebId(),
            addressBookUri,
            contactUri,
        )
    }

    override suspend fun createGroup(
        addressBookUri: String,
        title: String,
        contacts: List<String>
    ): FullGroup? {
        return contactDataModule.groups.create(
            webId = userRepository.getGrantedWebId(),
            addressBookUri = addressBookUri,
            title = title,
            contactUris = contacts,
        )
    }

    override suspend fun getGroup(groupUri: String): FullGroup? {
        return contactDataModule.groups.get(userRepository.getGrantedWebId(), groupUri)
    }

    override suspend fun deleteGroup(addressBookUri: String, groupUri: String): FullGroup? {
        return contactDataModule.groups.delete(
            userRepository.getGrantedWebId(),
            addressBookUri,
            groupUri,
        )
    }
}

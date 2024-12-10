package com.pondersource.solidcontacts.ui.addressbook

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pondersource.shared.data.datamodule.contact.AddressBook
import com.pondersource.solidcontacts.repository.contacts.ContactsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddressBooksViewModel @Inject constructor(
    val contactsRepository: ContactsRepository,
): ViewModel() {

    val addressBooksLoadingState = mutableStateOf(false)
    val privateAddressBooks : MutableState<List<AddressBook>?> = mutableStateOf(null)
    val publicAddressBooks : MutableState<List<AddressBook>?> = mutableStateOf(null)

    val newAddressBookTitle = mutableStateOf("")
    val newAddressBookPrivate = mutableStateOf(true)



    fun loadData() {
        viewModelScope.launch {
            contactsRepository.contactsServiceConnectionState().collect {
                if (it) {
                    getAddressBooks()
                }
            }
        }
    }

    fun createNewAddressBook() {

        viewModelScope.launch(Dispatchers.IO) {
            addressBooksLoadingState.value = true
            contactsRepository.createNewAddressBook(newAddressBookTitle.value, newAddressBookPrivate.value)
            getAddressBooks()
        }
    }


    private suspend fun getAddressBooks() {
        addressBooksLoadingState.value = true
        val addressBooks = contactsRepository.getAddressBooks()
        val localPrivateAddressBookList = arrayListOf<AddressBook>()
        val localPublicAddressBookList = arrayListOf<AddressBook>()
        addressBooks?.privateAddressBookUris?.forEach {
            localPrivateAddressBookList.add(contactsRepository.getAddressBook(it)!!)
        }
        addressBooks?.publicAddressBookUris?.forEach {
            localPublicAddressBookList.add(contactsRepository.getAddressBook(it)!!)
        }
        privateAddressBooks.value = localPrivateAddressBookList
        publicAddressBooks.value = localPublicAddressBookList
        addressBooksLoadingState.value = false
    }
}
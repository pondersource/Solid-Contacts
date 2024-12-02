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


    init {
        viewModelScope.launch(Dispatchers.IO) {
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
        this.privateAddressBooks.value = localPrivateAddressBookList
        this.publicAddressBooks.value = localPublicAddressBookList
        addressBooksLoadingState.value = false
    }
}
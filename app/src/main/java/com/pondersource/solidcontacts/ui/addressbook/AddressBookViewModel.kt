package com.pondersource.solidcontacts.ui.addressbook

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.pondersource.shared.data.datamodule.contact.AddressBook
import com.pondersource.solidcontacts.repository.contacts.ContactsRepository
import com.pondersource.solidcontacts.ui.nav.AddressBookRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddressBookViewModel @Inject constructor(
    val contactsRepository: ContactsRepository,
    val savedStateHandle: SavedStateHandle,
): ViewModel() {

    val addressBookRoute = savedStateHandle.toRoute<AddressBookRoute>()

    val loadingAddressBookDetails = mutableStateOf(true)
    val addressBookDetails: MutableState<AddressBook?> = mutableStateOf(null)

    fun loadData() {
        viewModelScope.launch {
            loadingAddressBookDetails.value = true
            addressBookDetails.value = contactsRepository.getAddressBook(addressBookRoute.addressBookUri)
            loadingAddressBookDetails.value = false
        }
    }
}
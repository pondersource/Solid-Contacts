package com.pondersource.solidcontacts.ui.addressbook

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.pondersource.shared.data.datamodule.contact.FullContact
import com.pondersource.solidcontacts.repository.contacts.ContactsRepository
import com.pondersource.solidcontacts.ui.nav.ContactRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val contactsRepository: ContactsRepository
): ViewModel() {

    val contactRoute = savedStateHandle.toRoute<ContactRoute>()

    val loadingContactDetails = mutableStateOf(true)
    val contactDetails: MutableState<FullContact?> = mutableStateOf(null)

    val deleteLoading = mutableStateOf(false)
    val deleteContactResult = mutableStateOf(false)

    fun loadData() {
        viewModelScope.launch {
            loadingContactDetails.value = true
            contactDetails.value = contactsRepository.getContact(contactRoute.contactUri)
            loadingContactDetails.value = false
        }
    }

    fun deleteContact() {
        viewModelScope.launch {
            deleteLoading.value = true
            val result = contactsRepository.deleteContact(contactRoute.addressBookUri, contactDetails.value!!.uri)
            if (result != null) {
                deleteContactResult.value = true
            }
            deleteLoading.value = true
        }
    }

}
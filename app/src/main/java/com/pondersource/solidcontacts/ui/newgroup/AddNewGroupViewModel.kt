package com.pondersource.solidcontacts.ui.newgroup

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.pondersource.shared.data.datamodule.contact.Contact
import com.pondersource.shared.data.datamodule.contact.FullGroup
import com.pondersource.solidcontacts.repository.contacts.ContactsRepository
import com.pondersource.solidcontacts.ui.nav.AddGroupRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddNewGroupViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val contactsRepository: ContactsRepository,
): ViewModel() {

    val addGroupRoute = savedStateHandle.toRoute<AddGroupRoute>()

    val title = mutableStateOf("")

    val includedContacts = mutableStateListOf<Contact>()
    val notIncludedContacts = mutableStateListOf<Contact>()

    val loading = mutableStateOf(false)
    val addGroupResult = mutableStateOf<FullGroup?>(null)
    val errorMessages = mutableStateOf<String?>(null)

    fun loadData() {
        viewModelScope.launch {
            loading.value = true
            val addressBook  = contactsRepository.getAddressBook(addGroupRoute.addressBookUri)
            notIncludedContacts.clear()
            includedContacts.clear()
            notIncludedContacts.addAll(addressBook?.contacts ?: arrayListOf())
            loading.value = false
        }
    }

    fun includeContact(contact: Contact) {
        notIncludedContacts.remove(contact)
        includedContacts.add(contact)
    }

    fun excludeContact(contact: Contact) {
        includedContacts.remove(contact)
        notIncludedContacts.add(contact)
    }

    fun addNewGroup() {
        viewModelScope.launch {
            if(title.value.isEmpty()) {
                errorMessages.value = "Title cannot be empty."
            } else {
                loading.value = true
                val result = contactsRepository.createGroup(
                    addGroupRoute.addressBookUri,
                    title.value,
                    includedContacts.map { it.uri },
                )
                addGroupResult.value = result
                loading.value = false
                if (result == null) {
                    errorMessages.value = "Something went wrong."
                }
            }
        }
    }

}
package com.pondersource.solidcontacts.ui.newcontact

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.pondersource.shared.data.datamodule.contact.FullContact
import com.pondersource.shared.data.datamodule.contact.Group
import com.pondersource.solidcontacts.repository.contacts.ContactsRepository
import com.pondersource.solidcontacts.ui.nav.AddContactRoute
import com.pondersource.solidcontacts.util.isEmailValid
import com.pondersource.solidcontacts.util.isPhoneNumberValid
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddNewContactViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val contactsRepository: ContactsRepository,
): ViewModel() {

    val addContactRoute = savedStateHandle.toRoute<AddContactRoute>()

    val fullName = mutableStateOf("")
    val phoneNumber = mutableStateOf("")
    val email = mutableStateOf("")

    val phoneNumberError = mutableStateOf(false)
    val emailError = mutableStateOf(false)

    val includedGroups = mutableStateListOf<Group>()
    val notIncludedGroups = mutableStateListOf<Group>()

    val loading = mutableStateOf(false)
    val addContactResult = mutableStateOf<FullContact?>(null)
    val errorMessages = mutableStateOf<String?>(null)

    fun loadData() {
        viewModelScope.launch {
            loading.value = true
            val addressBook  = contactsRepository.getAddressBook(addContactRoute.addressBookUri)
            notIncludedGroups.clear()
            includedGroups.clear()
            notIncludedGroups.addAll(addressBook?.groups ?: arrayListOf())
            loading.value = false
        }
    }

    fun includeGroup(group: Group) {
        notIncludedGroups.remove(group)
        includedGroups.add(group)
    }

    fun excludeGroup(group: Group) {
        includedGroups.remove(group)
        notIncludedGroups.add(group)
    }

    fun addNewContact() {
        viewModelScope.launch {
            if(fullName.value.isEmpty()) {
                errorMessages.value = "Name can not be empty."
            } else if (phoneNumberError.value) {
                errorMessages.value = "Phone number format is wrong."
            } else if (emailError.value) {
                errorMessages.value = "Email format is wrong."
            } else {
                loading.value = true
                val result = contactsRepository.createContact(
                    addContactRoute.addressBookUri,
                    fullName.value,
                    phoneNumber.value,
                    email.value,
                    includedGroups.map { it.uri },
                )
                addContactResult.value = result
                if (result == null) {
                    errorMessages.value = "Something went wrong."
                }
                loading.value = false
            }
        }
    }

    fun setEmail(email: String) {
        this.email.value = email
        if (this.email.value.isNotEmpty()) {
            emailError.value = !isEmailValid(this.email.value)
        } else {
            emailError.value = false
        }
    }
    fun setPhoneNumber(number: String) {
        this.phoneNumber.value = number
        if (this.phoneNumber.value.isNotEmpty()) {
            phoneNumberError.value = !isPhoneNumberValid(this.phoneNumber.value)
        } else {
            phoneNumberError.value = false
        }
    }

}
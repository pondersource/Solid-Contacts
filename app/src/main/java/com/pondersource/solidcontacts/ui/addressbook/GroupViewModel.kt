package com.pondersource.solidcontacts.ui.addressbook

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.pondersource.shared.data.datamodule.contact.FullContact
import com.pondersource.shared.data.datamodule.contact.FullGroup
import com.pondersource.solidcontacts.repository.contacts.ContactsRepository
import com.pondersource.solidcontacts.ui.nav.GroupRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val contactsRepository: ContactsRepository
): ViewModel() {

    val groupRoute = savedStateHandle.toRoute<GroupRoute>()

    val loadingGroupDetails = mutableStateOf(true)
    val groupDetails: MutableState<FullGroup?> = mutableStateOf(null)

    init {
        viewModelScope.launch {
            loadingGroupDetails.value = true
            groupDetails.value = contactsRepository.getGroup(groupRoute.groupUri)
            loadingGroupDetails.value = false
        }
    }

}
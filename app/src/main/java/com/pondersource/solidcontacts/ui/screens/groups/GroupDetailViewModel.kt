package com.pondersource.solidcontacts.ui.screens.groups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.pondersource.solidcontacts.data.repository.ContactsRepository
import com.pondersource.solidcontacts.domain.model.ContactGroup
import com.pondersource.solidcontacts.domain.model.ContactSummary
import com.pondersource.solidcontacts.ui.navigation.GroupDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupDetailUiState(
    val loading: Boolean = true,
    val group: ContactGroup? = null,
    val members: List<ContactSummary> = emptyList(),
    val deleted: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contactsRepository: ContactsRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<GroupDetailRoute>()
    val groupId: String = route.groupId

    private val deleted = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val state: StateFlow<GroupDetailUiState> = combine(
        contactsRepository.observeGroup(groupId),
        contactsRepository.observeGroupMembers(groupId),
        deleted,
        message,
    ) { group, members, isDeleted, msg ->
        GroupDetailUiState(
            loading = false,
            group = group,
            members = members,
            deleted = isDeleted,
            message = msg,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GroupDetailUiState(),
    )

    fun removeMember(contactId: String) {
        viewModelScope.launch {
            contactsRepository.removeFromGroup(groupId, contactId)
            message.value = "Removed from the group"
        }
    }

    fun delete() {
        viewModelScope.launch {
            contactsRepository.deleteGroup(groupId)
            deleted.value = true
        }
    }

    fun consumeMessage() {
        message.value = null
    }
}

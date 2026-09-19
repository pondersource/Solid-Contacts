package com.pondersource.solidcontacts.ui.screens.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pondersource.solidcontacts.data.repository.ContactsRepository
import com.pondersource.solidcontacts.domain.model.AddressBookSummary
import com.pondersource.solidcontacts.domain.model.ContactGroup
import com.pondersource.solidcontacts.sync.NetworkMonitor
import com.pondersource.solidcontacts.ui.components.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupsUiState(
    val loading: Boolean = true,
    val groups: List<ContactGroup> = emptyList(),
    val books: List<AddressBookSummary> = emptyList(),
    val syncStatus: SyncStatus = SyncStatus.InSync,
    val message: String? = null,
) {
    val canCreate: Boolean get() = books.isNotEmpty()
}

@HiltViewModel
class GroupsViewModel @Inject constructor(
    private val contactsRepository: ContactsRepository,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val message = MutableStateFlow<String?>(null)

    val state: StateFlow<GroupsUiState> = combine(
        contactsRepository.observeGroups(),
        contactsRepository.observeBooks(),
        combine(networkMonitor.isOnline, contactsRepository.observePendingWrites()) { online, pending ->
            when {
                !online -> SyncStatus.Offline
                pending > 0 -> SyncStatus.Pending(pending)
                else -> SyncStatus.InSync
            }
        },
        message,
    ) { groups, books, sync, msg ->
        GroupsUiState(
            loading = false,
            groups = groups,
            books = books,
            syncStatus = sync,
            message = msg,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GroupsUiState(),
    )

    fun createGroup(bookId: String, name: String) {
        viewModelScope.launch {
            contactsRepository.createGroup(bookId, name)
            message.value = "Group created"
        }
    }

    fun retrySync() {
        viewModelScope.launch { contactsRepository.retryFailedWrites() }
    }

    fun consumeMessage() {
        message.value = null
    }
}

package com.pondersource.solidcontacts.ui.screens.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pondersource.solidcontacts.data.repository.ContactsRepository
import com.pondersource.solidcontacts.domain.model.AddressBookSummary
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

data class BooksUiState(
    val loading: Boolean = true,
    val privateBooks: List<AddressBookSummary> = emptyList(),
    val publicBooks: List<AddressBookSummary> = emptyList(),
    val syncStatus: SyncStatus = SyncStatus.InSync,
    val refreshing: Boolean = false,
    val message: String? = null,
) {
    val isEmpty: Boolean get() = privateBooks.isEmpty() && publicBooks.isEmpty()
}

@HiltViewModel
class BooksViewModel @Inject constructor(
    private val contactsRepository: ContactsRepository,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val refreshing = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val state: StateFlow<BooksUiState> = combine(
        contactsRepository.observeBooks(),
        combine(networkMonitor.isOnline, contactsRepository.observePendingWrites()) { online, pending ->
            when {
                !online -> SyncStatus.Offline
                pending > 0 -> SyncStatus.Pending(pending)
                else -> SyncStatus.InSync
            }
        },
        refreshing,
        message,
    ) { books, sync, isRefreshing, msg ->
        BooksUiState(
            loading = false,
            privateBooks = books.filter { it.isPrivate },
            publicBooks = books.filterNot { it.isPrivate },
            syncStatus = sync,
            refreshing = isRefreshing,
            message = msg,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BooksUiState(),
    )

    fun createBook(title: String, isPrivate: Boolean) {
        viewModelScope.launch {
            contactsRepository.createBook(title, isPrivate)
            message.value = "Address book created"
        }
    }

    fun refresh() {
        viewModelScope.launch {
            refreshing.value = true
            contactsRepository.refresh()
            refreshing.value = false
        }
    }

    fun retrySync() {
        viewModelScope.launch { contactsRepository.retryFailedWrites() }
    }

    fun consumeMessage() {
        message.value = null
    }
}

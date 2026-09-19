package com.pondersource.solidcontacts.ui.screens.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pondersource.solidcontacts.data.local.prefs.SortOrder
import com.pondersource.solidcontacts.data.repository.AccountRepository
import com.pondersource.solidcontacts.data.repository.ContactsRepository
import com.pondersource.solidcontacts.domain.model.ContactSummary
import com.pondersource.solidcontacts.sync.NetworkMonitor
import com.pondersource.solidcontacts.ui.components.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A run of contacts that file under the same letter. */
data class ContactSection(
    val letter: String,
    val contacts: List<ContactSummary>,
)

data class ContactsUiState(
    val loading: Boolean = true,
    val query: String = "",
    val sections: List<ContactSection> = emptyList(),
    val favorites: List<ContactSummary> = emptyList(),
    val totalCount: Int = 0,
    val syncStatus: SyncStatus = SyncStatus.InSync,
    val refreshing: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val message: String? = null,
) {
    val isSearching: Boolean get() = query.isNotBlank()
    val isEmpty: Boolean get() = sections.isEmpty() && favorites.isEmpty()
    val selectionMode: Boolean get() = selectedIds.isNotEmpty()
    val activeLetters: Set<String> get() = sections.map { it.letter }.toSet()
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val contactsRepository: ContactsRepository,
    private val accountRepository: AccountRepository,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val refreshing = MutableStateFlow(false)
    private val selectedIds = MutableStateFlow<Set<String>>(emptySet())
    private val message = MutableStateFlow<String?>(null)

    private val matches = query
        .debounce { if (it.isEmpty()) 0L else 180L }
        .distinctUntilChanged()
        .flatMapLatest { contactsRepository.searchContacts(it) }

    private val syncStatus = combine(
        networkMonitor.isOnline,
        contactsRepository.observePendingWrites(),
    ) { online, pending ->
        when {
            !online -> SyncStatus.Offline
            pending > 0 -> SyncStatus.Pending(pending)
            else -> SyncStatus.InSync
        }
    }

    val state: StateFlow<ContactsUiState> = combine(
        matches,
        contactsRepository.observeFavorites(),
        accountRepository.settings.map { it.sortOrder }.distinctUntilChanged(),
        syncStatus,
        combine(query, refreshing, selectedIds, message) { q, r, s, m -> Screen(q, r, s, m) },
    ) { contacts, favorites, sortOrder, sync, screen ->
        ContactsUiState(
            loading = false,
            query = screen.query,
            sections = sectionsOf(contacts, sortOrder),
            favorites = if (screen.query.isBlank()) favorites.sortedFor(sortOrder) else emptyList(),
            totalCount = contacts.size,
            syncStatus = sync,
            refreshing = screen.refreshing,
            selectedIds = screen.selectedIds,
            message = screen.message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ContactsUiState(),
    )

    private data class Screen(
        val query: String,
        val refreshing: Boolean,
        val selectedIds: Set<String>,
        val message: String?,
    )

    val selection: StateFlow<Set<String>> = selectedIds.asStateFlow()

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun clearQuery() {
        query.value = ""
    }

    fun refresh() {
        viewModelScope.launch {
            refreshing.value = true
            val result = contactsRepository.refresh()
            refreshing.value = false
            result.onFailure { message.value = "Could not reach your pod. Showing what is saved here." }
        }
    }

    fun retrySync() {
        viewModelScope.launch { contactsRepository.retryFailedWrites() }
    }

    fun toggleSelection(contactId: String) {
        selectedIds.update { current ->
            if (contactId in current) current - contactId else current + contactId
        }
    }

    fun clearSelection() {
        selectedIds.value = emptySet()
    }

    fun deleteSelected() {
        val ids = selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            contactsRepository.deleteContacts(ids)
            selectedIds.value = emptySet()
            message.value = if (ids.size == 1) "Contact deleted" else "${ids.size} contacts deleted"
        }
    }

    fun setFavorite(contactId: String, favorite: Boolean) {
        viewModelScope.launch { contactsRepository.setFavorite(contactId, favorite) }
    }

    fun consumeMessage() {
        message.value = null
    }

    private fun sectionsOf(
        contacts: List<ContactSummary>,
        order: SortOrder,
    ): List<ContactSection> = contacts
        .sortedFor(order)
        .groupBy { it.letterFor(order) }
        .map { (letter, rows) -> ContactSection(letter, rows) }
        // Digits and symbols file under "#", which belongs after the letters.
        .sortedWith(compareBy({ it.letter == "#" }, { it.letter }))

    private fun List<ContactSummary>.sortedFor(order: SortOrder): List<ContactSummary> =
        when (order) {
            SortOrder.FIRST_NAME -> sortedBy { it.displayName.lowercase() }
            SortOrder.LAST_NAME -> sortedBy { it.sortKey }
        }

    private fun ContactSummary.letterFor(order: SortOrder): String {
        val source = if (order == SortOrder.FIRST_NAME) displayName else sortKey
        val first = source.firstOrNull { it.isLetter() } ?: return "#"
        return first.uppercaseChar().toString()
    }
}

package com.pondersource.solidcontacts.ui.screens.picker

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.pondersource.solidcontacts.data.repository.ContactsRepository
import com.pondersource.solidcontacts.domain.model.ContactSummary
import com.pondersource.solidcontacts.ui.navigation.ContactPickerRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PickerUiState(
    val loading: Boolean = true,
    val query: String = "",
    val contacts: List<ContactSummary> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val done: Boolean = false,
) {
    val hasSelection: Boolean get() = selectedIds.isNotEmpty()
}

/** Picks contacts to add to a group. */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class ContactPickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contactsRepository: ContactsRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<ContactPickerRoute>()

    private val query = MutableStateFlow("")
    private val selectedIds = MutableStateFlow<Set<String>>(emptySet())
    private val done = MutableStateFlow(false)

    private val candidates = query
        .debounce { if (it.isEmpty()) 0L else 180L }
        .distinctUntilChanged()
        .flatMapLatest { needle ->
            val source = when {
                route.bookId != null -> contactsRepository.observeContactsInBook(route.bookId)
                else -> contactsRepository.observeAllContacts()
            }
            combine(source, flowOf(needle)) { contacts, text ->
                if (text.isBlank()) {
                    contacts
                } else {
                    contacts.filter { it.matches(text) }
                }
            }
        }

    private val existingMembers = route.groupId
        ?.let { contactsRepository.observeGroupMembers(it) }
        ?: flowOf(emptyList())

    val state: StateFlow<PickerUiState> = combine(
        candidates,
        existingMembers,
        query,
        selectedIds,
        done,
    ) { contacts, members, text, selected, isDone ->
        val memberIds = members.map { it.id }.toSet()
        PickerUiState(
            loading = false,
            query = text,
            contacts = if (route.excludeGroupMembers) {
                contacts.filterNot { it.id in memberIds }
            } else {
                contacts
            },
            selectedIds = selected,
            done = isDone,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PickerUiState(),
    )

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun clearQuery() {
        query.value = ""
    }

    fun toggle(contactId: String) {
        selectedIds.update { current ->
            if (contactId in current) current - contactId else current + contactId
        }
    }

    fun confirm() {
        val groupId = route.groupId ?: run {
            done.value = true
            return
        }
        val ids = selectedIds.value.toList()
        viewModelScope.launch {
            ids.forEach { contactsRepository.addToGroup(groupId, it) }
            done.value = true
        }
    }

    private fun ContactSummary.matches(text: String): Boolean {
        val needle = text.trim().lowercase()
        return displayName.lowercase().contains(needle) ||
            organization?.lowercase()?.contains(needle) == true ||
            primaryEmail?.lowercase()?.contains(needle) == true ||
            primaryPhone?.contains(needle) == true
    }
}

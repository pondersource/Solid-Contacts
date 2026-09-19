package com.pondersource.solidcontacts.ui.screens.books

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.pondersource.solidcontacts.data.repository.ContactsRepository
import com.pondersource.solidcontacts.domain.model.AddressBookSummary
import com.pondersource.solidcontacts.domain.model.ContactGroup
import com.pondersource.solidcontacts.domain.model.ContactSummary
import com.pondersource.solidcontacts.ui.navigation.BookDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookDetailUiState(
    val loading: Boolean = true,
    val book: AddressBookSummary? = null,
    val contacts: List<ContactSummary> = emptyList(),
    val groups: List<ContactGroup> = emptyList(),
    val deleted: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class BookDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contactsRepository: ContactsRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<BookDetailRoute>()
    val bookId: String = route.bookId

    private val deleted = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val state: StateFlow<BookDetailUiState> = combine(
        contactsRepository.observeBook(bookId),
        contactsRepository.observeContactsInBook(bookId),
        contactsRepository.observeGroupsInBook(bookId),
        deleted,
        message,
    ) { book, contacts, groups, isDeleted, msg ->
        BookDetailUiState(
            loading = false,
            book = book,
            contacts = contacts,
            groups = groups,
            deleted = isDeleted,
            message = msg,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BookDetailUiState(),
    )

    fun rename(title: String) {
        viewModelScope.launch {
            contactsRepository.renameBook(bookId, title)
            message.value = "Renamed"
        }
    }

    fun delete() {
        viewModelScope.launch {
            contactsRepository.deleteBook(bookId)
            deleted.value = true
        }
    }

    fun createGroup(name: String) {
        viewModelScope.launch {
            contactsRepository.createGroup(bookId, name)
            message.value = "Group created"
        }
    }

    fun consumeMessage() {
        message.value = null
    }
}

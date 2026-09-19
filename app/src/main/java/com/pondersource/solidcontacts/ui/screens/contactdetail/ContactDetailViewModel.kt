package com.pondersource.solidcontacts.ui.screens.contactdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.pondersource.solidcontacts.data.repository.ContactsRepository
import com.pondersource.solidcontacts.data.vcard.VCardWriter
import com.pondersource.solidcontacts.domain.model.AddressBookSummary
import com.pondersource.solidcontacts.domain.model.ContactDetail
import com.pondersource.solidcontacts.domain.model.ContactGroup
import com.pondersource.solidcontacts.ui.navigation.ContactDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContactDetailUiState(
    val loading: Boolean = true,
    val contact: ContactDetail? = null,
    val photo: ByteArray? = null,
    val book: AddressBookSummary? = null,
    val books: List<AddressBookSummary> = emptyList(),
    val groups: List<ContactGroup> = emptyList(),
    val deleted: Boolean = false,
    val message: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ContactDetailUiState) return false
        return loading == other.loading &&
            contact == other.contact &&
            (photo?.contentEquals(other.photo) ?: (other.photo == null)) &&
            book == other.book &&
            books == other.books &&
            groups == other.groups &&
            deleted == other.deleted &&
            message == other.message
    }

    override fun hashCode(): Int {
        var result = loading.hashCode()
        result = 31 * result + (contact?.hashCode() ?: 0)
        result = 31 * result + (photo?.contentHashCode() ?: 0)
        result = 31 * result + (book?.hashCode() ?: 0)
        result = 31 * result + books.hashCode()
        result = 31 * result + groups.hashCode()
        result = 31 * result + deleted.hashCode()
        result = 31 * result + (message?.hashCode() ?: 0)
        return result
    }
}

@HiltViewModel
class ContactDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contactsRepository: ContactsRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<ContactDetailRoute>()
    val contactId: String = route.contactId

    private val photo = MutableStateFlow<ByteArray?>(null)
    private val deleted = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val state: StateFlow<ContactDetailUiState> = combine(
        contactsRepository.observeContact(contactId),
        contactsRepository.observeBooks(),
        contactsRepository.observeGroups(),
        contactsRepository.observeGroupIdsOf(contactId),
        combine(photo, deleted, message) { p, d, m -> Triple(p, d, m) },
    ) { contact, books, allGroups, groupIds, (photoBytes, isDeleted, msg) ->
        ContactDetailUiState(
            loading = false,
            contact = contact,
            photo = photoBytes,
            book = books.firstOrNull { it.id == contact?.bookId },
            books = books,
            groups = allGroups.filter { it.id in groupIds },
            deleted = isDeleted,
            message = msg,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ContactDetailUiState(),
    )

    init {
        viewModelScope.launch { photo.value = contactsRepository.contactPhoto(contactId) }
    }

    fun toggleFavorite() {
        val contact = state.value.contact ?: return
        viewModelScope.launch {
            contactsRepository.setFavorite(contactId, !contact.isFavorite)
        }
    }

    fun delete() {
        viewModelScope.launch {
            contactsRepository.deleteContact(contactId)
            deleted.value = true
        }
    }

    fun moveTo(bookId: String) {
        viewModelScope.launch {
            contactsRepository.moveContact(contactId, bookId)
            message.value = "Moved to another address book"
        }
    }

    fun removeFromGroup(groupId: String) {
        viewModelScope.launch { contactsRepository.removeFromGroup(groupId, contactId) }
    }

    fun setPhoto(bytes: ByteArray, contentType: String) {
        viewModelScope.launch {
            contactsRepository.setPhoto(contactId, bytes, contentType)
            photo.value = bytes
        }
    }

    fun removePhoto() {
        viewModelScope.launch {
            contactsRepository.removePhoto(contactId)
            photo.value = null
        }
    }

    /** The contact as a `.vcf`, for sharing it with any app that reads vCards. */
    fun asVCard(): String? = state.value.contact?.let { VCardWriter.writeOne(it) }

    fun consumeMessage() {
        message.value = null
    }
}

package com.pondersource.solidcontacts.ui.screens.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.pondersource.solidcontacts.data.repository.ContactsRepository
import com.pondersource.solidcontacts.domain.model.AddressBookSummary
import com.pondersource.solidcontacts.domain.model.ContactDetail
import com.pondersource.solidcontacts.domain.model.EmailAddress
import com.pondersource.solidcontacts.domain.model.EmailKind
import com.pondersource.solidcontacts.domain.model.ImHandle
import com.pondersource.solidcontacts.domain.model.PhoneKind
import com.pondersource.solidcontacts.domain.model.PhoneNumber
import com.pondersource.solidcontacts.domain.model.PostalAddress
import com.pondersource.solidcontacts.domain.model.StructuredName
import com.pondersource.solidcontacts.domain.model.WebLink
import com.pondersource.solidcontacts.ui.navigation.ContactEditorRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditorUiState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val draft: ContactDetail = ContactDetail(id = ""),
    val books: List<AddressBookSummary> = emptyList(),
    val bookId: String = "",
    val photo: ByteArray? = null,
    val photoChanged: Boolean = false,
    val showAllFields: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
) {
    val canSave: Boolean get() = !saving && !draft.isEmpty() && bookId.isNotEmpty()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EditorUiState) return false
        return loading == other.loading &&
            isNew == other.isNew &&
            draft == other.draft &&
            books == other.books &&
            bookId == other.bookId &&
            (photo?.contentEquals(other.photo) ?: (other.photo == null)) &&
            photoChanged == other.photoChanged &&
            showAllFields == other.showAllFields &&
            saving == other.saving &&
            saved == other.saved &&
            error == other.error
    }

    override fun hashCode(): Int {
        var result = loading.hashCode()
        result = 31 * result + isNew.hashCode()
        result = 31 * result + draft.hashCode()
        result = 31 * result + books.hashCode()
        result = 31 * result + bookId.hashCode()
        result = 31 * result + (photo?.contentHashCode() ?: 0)
        result = 31 * result + photoChanged.hashCode()
        result = 31 * result + showAllFields.hashCode()
        result = 31 * result + saving.hashCode()
        result = 31 * result + saved.hashCode()
        result = 31 * result + (error?.hashCode() ?: 0)
        return result
    }
}

/**
 * Edits one contact.
 *
 * The whole vCard is editable, not just a name and a number, because anything this screen cannot
 * express would be silently dropped the next time the contact is saved.
 */
@HiltViewModel
class ContactEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contactsRepository: ContactsRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<ContactEditorRoute>()

    private val _state = MutableStateFlow(EditorUiState(isNew = route.contactId == null))
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    private var photoContentType: String = "image/jpeg"

    init {
        viewModelScope.launch {
            val books = contactsRepository.observeBooks().first()
            val existing = route.contactId?.let { contactsRepository.getContact(it) }
            val photo = route.contactId?.let { contactsRepository.contactPhoto(it) }
            val bookId = existing?.bookId
                ?: route.bookId
                ?: books.firstOrNull()?.id
                ?: contactsRepository.ensureDefaultBook()

            _state.update {
                it.copy(
                    loading = false,
                    isNew = existing == null,
                    draft = existing ?: ContactDetail(id = "", bookId = bookId),
                    books = books.ifEmpty { contactsRepository.observeBooks().first() },
                    bookId = bookId,
                    photo = photo,
                    // An existing contact opens with every field it already uses on show.
                    showAllFields = existing?.usesAdvancedFields() == true,
                )
            }
        }
    }

    private fun edit(block: (ContactDetail) -> ContactDetail) {
        _state.update { it.copy(draft = block(it.draft)) }
    }

    fun setFullName(value: String) = edit { it.copy(fullName = value) }

    fun setName(name: StructuredName) = edit { it.copy(name = name) }

    fun setNickname(value: String) = edit { it.copy(nickname = value) }

    fun setOrganization(value: String) = edit { it.copy(organization = value) }

    fun setOrganizationUnit(value: String) = edit { it.copy(organizationUnit = value) }

    fun setJobTitle(value: String) = edit { it.copy(jobTitle = value) }

    fun setRole(value: String) = edit { it.copy(role = value) }

    fun setNote(value: String) = edit { it.copy(note = value) }

    fun setBirthday(value: String) = edit { it.copy(birthday = value) }

    fun setAnniversary(value: String) = edit { it.copy(anniversary = value) }

    fun setCategories(value: String) = edit { contact ->
        contact.copy(
            categories = value.split(',').map { it.trim() }.filter { it.isNotBlank() },
        )
    }

    fun setLanguages(value: String) = edit { contact ->
        contact.copy(
            languages = value.split(',').map { it.trim() }.filter { it.isNotBlank() },
        )
    }

    fun addPhone() = edit { it.copy(phones = it.phones + PhoneNumber("", PhoneKind.CELL)) }

    fun setPhone(index: Int, phone: PhoneNumber) = edit { contact ->
        contact.copy(phones = contact.phones.replaceAt(index, phone))
    }

    fun removePhone(index: Int) = edit { it.copy(phones = it.phones.removeAt(index)) }

    fun addEmail() = edit { it.copy(emails = it.emails + EmailAddress("", EmailKind.HOME)) }

    fun setEmail(index: Int, email: EmailAddress) = edit { contact ->
        contact.copy(emails = contact.emails.replaceAt(index, email))
    }

    fun removeEmail(index: Int) = edit { it.copy(emails = it.emails.removeAt(index)) }

    fun addAddress() = edit { it.copy(addresses = it.addresses + PostalAddress()) }

    fun setAddress(index: Int, address: PostalAddress) = edit { contact ->
        contact.copy(addresses = contact.addresses.replaceAt(index, address))
    }

    fun removeAddress(index: Int) = edit { it.copy(addresses = it.addresses.removeAt(index)) }

    fun addLink() = edit { it.copy(links = it.links + WebLink("")) }

    fun setLink(index: Int, link: WebLink) = edit { contact ->
        contact.copy(links = contact.links.replaceAt(index, link))
    }

    fun removeLink(index: Int) = edit { it.copy(links = it.links.removeAt(index)) }

    fun addIm() = edit { it.copy(impps = it.impps + ImHandle("")) }

    fun setIm(index: Int, handle: ImHandle) = edit { contact ->
        contact.copy(impps = contact.impps.replaceAt(index, handle))
    }

    fun removeIm(index: Int) = edit { it.copy(impps = it.impps.removeAt(index)) }

    fun setBook(bookId: String) {
        _state.update { it.copy(bookId = bookId) }
    }

    fun toggleAllFields() {
        _state.update { it.copy(showAllFields = !it.showAllFields) }
    }

    fun setPhoto(bytes: ByteArray, contentType: String) {
        photoContentType = contentType
        _state.update { it.copy(photo = bytes, photoChanged = true) }
    }

    fun removePhoto() {
        _state.update { it.copy(photo = null, photoChanged = true) }
    }

    fun save() {
        val current = _state.value
        if (!current.canSave) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            runCatching {
                val cleaned = current.draft.cleaned()
                val contactId = if (current.isNew) {
                    contactsRepository.createContact(current.bookId, cleaned)
                } else {
                    contactsRepository.updateContact(cleaned)
                    cleaned.id
                }
                if (current.photoChanged) {
                    val photo = current.photo
                    if (photo != null) {
                        contactsRepository.setPhoto(contactId, photo, photoContentType)
                    } else {
                        contactsRepository.removePhoto(contactId)
                    }
                }
                if (!current.isNew && current.bookId != current.draft.bookId) {
                    contactsRepository.moveContact(contactId, current.bookId)
                }
            }.fold(
                onSuccess = { _state.update { it.copy(saving = false, saved = true) } },
                onFailure = { error ->
                    _state.update {
                        it.copy(saving = false, error = error.message ?: "Could not save.")
                    }
                },
            )
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    /** Drops the empty rows the editor leaves behind when a user adds a field and thinks better. */
    private fun ContactDetail.cleaned(): ContactDetail = copy(
        fullName = fullName.trim(),
        phones = phones.filter { it.number.isNotBlank() },
        emails = emails.filter { it.address.isNotBlank() },
        impps = impps.filter { it.handle.isNotBlank() },
        addresses = addresses.filterNot { it.isEmpty() },
        links = links.filter { it.url.isNotBlank() },
        nickname = nickname?.trim()?.takeIf { it.isNotBlank() },
        organization = organization?.trim()?.takeIf { it.isNotBlank() },
        organizationUnit = organizationUnit?.trim()?.takeIf { it.isNotBlank() },
        jobTitle = jobTitle?.trim()?.takeIf { it.isNotBlank() },
        role = role?.trim()?.takeIf { it.isNotBlank() },
        note = note?.trim()?.takeIf { it.isNotBlank() },
        birthday = birthday?.trim()?.takeIf { it.isNotBlank() },
        anniversary = anniversary?.trim()?.takeIf { it.isNotBlank() },
    )

    private fun ContactDetail.usesAdvancedFields(): Boolean =
        addresses.isNotEmpty() || links.isNotEmpty() || impps.isNotEmpty() ||
            !nickname.isNullOrBlank() || !birthday.isNullOrBlank() ||
            !anniversary.isNullOrBlank() || !role.isNullOrBlank() ||
            categories.any { !it.equals("Favorites", ignoreCase = true) } ||
            languages.isNotEmpty() || !name.isEmpty()

    private fun <T> List<T>.replaceAt(index: Int, value: T): List<T> =
        if (index !in indices) this else toMutableList().also { it[index] = value }

    private fun <T> List<T>.removeAt(index: Int): List<T> =
        if (index !in indices) this else toMutableList().also { it.removeAt(index) }
}

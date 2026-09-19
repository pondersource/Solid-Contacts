package com.pondersource.solidcontacts.ui.screens.importexport

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pondersource.solidcontacts.data.device.DeviceContactsSource
import com.pondersource.solidcontacts.data.repository.ContactsRepository
import com.pondersource.solidcontacts.data.repository.DuplicateFinder
import com.pondersource.solidcontacts.data.vcard.VCardReader
import com.pondersource.solidcontacts.data.vcard.VCardWriter
import com.pondersource.solidcontacts.domain.model.AddressBookSummary
import com.pondersource.solidcontacts.domain.model.ContactDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** What the screen is doing, so the buttons can say so and stay disabled while it runs. */
sealed interface TransferState {
    data object Idle : TransferState
    data class Working(val label: String) : TransferState
    data class Done(val message: String) : TransferState
    data class Failed(val message: String) : TransferState
}

data class ImportExportUiState(
    val books: List<AddressBookSummary> = emptyList(),
    val targetBookId: String = "",
    val contactCount: Int = 0,
    val canReadDevice: Boolean = false,
    val canWriteDevice: Boolean = false,
    val transfer: TransferState = TransferState.Idle,
    val skipDuplicates: Boolean = true,
) {
    val busy: Boolean get() = transfer is TransferState.Working
}

@HiltViewModel
class ImportExportViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val contactsRepository: ContactsRepository,
    private val deviceContacts: DeviceContactsSource,
    private val io: CoroutineDispatcher,
) : ViewModel() {

    private val targetBookId = MutableStateFlow("")
    private val transfer = MutableStateFlow<TransferState>(TransferState.Idle)
    private val skipDuplicates = MutableStateFlow(true)
    private val permissions = MutableStateFlow(deviceContacts.canRead() to deviceContacts.canWrite())

    val state: StateFlow<ImportExportUiState> = combine(
        contactsRepository.observeBooks(),
        contactsRepository.observeContactCount(),
        targetBookId,
        transfer,
        combine(skipDuplicates, permissions) { skip, perms -> skip to perms },
    ) { books, count, bookId, state, (skip, perms) ->
        ImportExportUiState(
            books = books,
            targetBookId = bookId.ifEmpty { books.firstOrNull()?.id.orEmpty() },
            contactCount = count,
            canReadDevice = perms.first,
            canWriteDevice = perms.second,
            transfer = state,
            skipDuplicates = skip,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ImportExportUiState(),
    )

    fun setTargetBook(bookId: String) {
        targetBookId.value = bookId
    }

    fun setSkipDuplicates(enabled: Boolean) {
        skipDuplicates.value = enabled
    }

    fun refreshPermissions() {
        permissions.value = deviceContacts.canRead() to deviceContacts.canWrite()
    }

    /** Reads a `.vcf` the user picked and saves what it holds into the chosen address book. */
    fun importVCard(uri: Uri) {
        viewModelScope.launch {
            transfer.value = TransferState.Working("Reading the file…")
            runCatching {
                val text = withContext(io) {
                    context.contentResolver.openInputStream(uri)?.use {
                        it.readBytes().toString(Charsets.UTF_8)
                    }
                } ?: error("The file could not be opened.")
                val parsed = VCardReader.read(text)
                saveImported(parsed)
            }.fold(
                onSuccess = { saved ->
                    transfer.value = TransferState.Done(
                        if (saved == 0) {
                            "Nothing new to import."
                        } else {
                            "Imported $saved contact${if (saved == 1) "" else "s"}."
                        },
                    )
                },
                onFailure = {
                    transfer.value = TransferState.Failed(it.message ?: "The import failed.")
                },
            )
        }
    }

    /** Writes every contact as vCard text for the caller to hand to the system share sheet. */
    fun exportVCard(onReady: (String) -> Unit) {
        viewModelScope.launch {
            transfer.value = TransferState.Working("Preparing the export…")
            runCatching {
                val contacts = allContacts()
                VCardWriter.write(contacts) to contacts.size
            }.fold(
                onSuccess = { (text, count) ->
                    transfer.value = TransferState.Done(
                        "Exported $count contact${if (count == 1) "" else "s"}.",
                    )
                    onReady(text)
                },
                onFailure = {
                    transfer.value = TransferState.Failed(it.message ?: "The export failed.")
                },
            )
        }
    }

    /** Brings the phone's own contacts into the pod. */
    fun importFromDevice() {
        viewModelScope.launch {
            transfer.value = TransferState.Working("Reading this phone's contacts…")
            runCatching {
                val deviceList = deviceContacts.readAll()
                saveImported(deviceList)
            }.fold(
                onSuccess = { saved ->
                    transfer.value = TransferState.Done(
                        if (saved == 0) {
                            "Nothing new to import."
                        } else {
                            "Imported $saved contact${if (saved == 1) "" else "s"} from this phone."
                        },
                    )
                },
                onFailure = {
                    transfer.value = TransferState.Failed(it.message ?: "The import failed.")
                },
            )
        }
    }

    /** Puts the pod's contacts into the phone's store, so the dialer can see them. */
    fun exportToDevice() {
        viewModelScope.launch {
            transfer.value = TransferState.Working("Writing to this phone…")
            runCatching { deviceContacts.writeAll(allContacts()) }.fold(
                onSuccess = { written ->
                    transfer.value = TransferState.Done(
                        "Wrote $written contact${if (written == 1) "" else "s"} to this phone.",
                    )
                },
                onFailure = {
                    transfer.value = TransferState.Failed(it.message ?: "The export failed.")
                },
            )
        }
    }

    fun consumeResult() {
        transfer.value = TransferState.Idle
    }

    /**
     * Saves [incoming] into the target book.
     *
     * With duplicate skipping on, anything that already matches a contact on the pod is left
     * out, which is what makes a second import of the same file harmless.
     */
    private suspend fun saveImported(incoming: List<ContactDetail>): Int {
        if (incoming.isEmpty()) return 0
        val bookId = state.value.targetBookId.ifEmpty { contactsRepository.ensureDefaultBook() }

        val toSave = if (skipDuplicates.value) {
            val known = allContacts().flatMap { DuplicateFinder.keysOf(it) }.toMutableSet()
            incoming.filter { candidate ->
                val keys = DuplicateFinder.keysOf(candidate)
                // Also check the keys of the ones already accepted, so a file that repeats the
                // same person imports them once.
                if (keys.any { it in known }) false else { known += keys; true }
            }
        } else {
            incoming
        }
        return contactsRepository.importContacts(bookId, toSave)
    }

    private suspend fun allContacts(): List<ContactDetail> =
        contactsRepository.observeAllContacts().first()
            .mapNotNull { contactsRepository.getContact(it.id) }
}

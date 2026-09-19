package com.pondersource.solidcontacts.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pondersource.solidcontacts.data.local.prefs.SortOrder
import com.pondersource.solidcontacts.data.local.prefs.ThemeMode
import com.pondersource.solidcontacts.data.local.prefs.UserSettings
import com.pondersource.solidcontacts.data.repository.AccountRepository
import com.pondersource.solidcontacts.data.repository.ContactsRepository
import com.pondersource.solidcontacts.sync.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val contactCount: Int = 0,
    val pendingWrites: Int = 0,
    val online: Boolean = true,
    val syncing: Boolean = false,
    val signedOut: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val contactsRepository: ContactsRepository,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val syncing = MutableStateFlow(false)
    private val signedOut = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val state: StateFlow<SettingsUiState> = combine(
        accountRepository.settings,
        contactsRepository.observeContactCount(),
        contactsRepository.observePendingWrites(),
        networkMonitor.isOnline,
        combine(syncing, signedOut, message) { s, out, m -> Triple(s, out, m) },
    ) { settings, count, pending, online, (isSyncing, isSignedOut, msg) ->
        SettingsUiState(
            settings = settings,
            contactCount = count,
            pendingWrites = pending,
            online = online,
            syncing = isSyncing,
            signedOut = isSignedOut,
            message = msg,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun syncNow() {
        viewModelScope.launch {
            syncing.value = true
            contactsRepository.drainOutbox()
            val result = contactsRepository.refresh()
            syncing.value = false
            message.value = result.fold(
                onSuccess = { "Up to date with your pod" },
                onFailure = { "Could not reach your pod. Everything here is still saved." },
            )
        }
    }

    fun setSortOrder(order: SortOrder) {
        viewModelScope.launch { accountRepository.setSortOrder(order) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { accountRepository.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { accountRepository.setDynamicColor(enabled) }
    }

    fun setSyncOnlyOnWifi(enabled: Boolean) {
        viewModelScope.launch { accountRepository.setSyncOnlyOnWifi(enabled) }
    }

    fun disconnect() {
        viewModelScope.launch {
            val result = accountRepository.disconnect()
            result.onFailure { message.value = "Disconnected here, but your pod was not reachable." }
            signedOut.value = true
        }
    }

    fun consumeMessage() {
        message.value = null
    }
}

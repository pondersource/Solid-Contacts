package com.pondersource.solidcontacts.ui.screens.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erfangholami.androidsolidservices.client.sdk.Solid
import com.erfangholami.androidsolidservices.shared.model.datamodule.DataModuleId
import com.erfangholami.androidsolidservices.shared.model.grant.AccessLevel
import com.erfangholami.androidsolidservices.shared.model.grant.AccessRequest
import com.erfangholami.androidsolidservices.shared.model.grant.RequestedTarget
import com.pondersource.solidcontacts.data.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val hostInstalled: Boolean = true,
    val signedIn: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val accountRepository: AccountRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState(hostInstalled = Solid.isHostInstalled(context)))
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    /**
     * What this app asks the user for: the contacts module, at the level that lets it write.
     *
     * It deliberately does not ask for the whole pod, and does not ask for Full access, which is
     * what sharing and notifications would need. A contacts app has no business holding either.
     */
    val accessRequest: AccessRequest = AccessRequest(
        level = AccessLevel.EDIT,
        targets = listOf(RequestedTarget.Module(DataModuleId.CONTACTS)),
        reason = "Solid Contacts keeps your address books, contacts and groups in your pod.",
    )

    /** Re-checks for the host app, for when the user comes back from installing it. */
    fun refreshHostState() {
        _state.update { it.copy(hostInstalled = Solid.isHostInstalled(context)) }
    }

    fun onAuthorized(webId: String) {
        if (webId.isEmpty()) {
            _state.update { it.copy(error = "Solid Share did not return an account.") }
            return
        }
        viewModelScope.launch {
            accountRepository.onAuthorized(webId)
            _state.update { it.copy(signedIn = true) }
        }
    }

    fun onFailed(message: String?) {
        _state.update { it.copy(error = message?.takeIf { m -> m.isNotBlank() } ?: "Sign-in failed.") }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

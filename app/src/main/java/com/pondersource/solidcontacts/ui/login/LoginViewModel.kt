package com.pondersource.solidcontacts.ui.login

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.erfangholami.androidsolidservices.shared.model.datamodule.DataModuleId
import com.erfangholami.androidsolidservices.shared.model.grant.AccessLevel
import com.erfangholami.androidsolidservices.shared.model.grant.AccessRequest
import com.erfangholami.androidsolidservices.shared.model.grant.RequestedTarget
import com.pondersource.solidcontacts.repository.user.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    val userRepository: UserRepository,
) : ViewModel() {

    val loginResult = mutableStateOf(false)
    val loginError = mutableStateOf("")

    val accessRequest = AccessRequest(
        level = AccessLevel.EDIT,
        targets = listOf(RequestedTarget.Module(DataModuleId.CONTACTS)),
        reason = "Solid Contacts keeps your address books, contacts and groups in your pod.",
    )

    fun onAuthorized(webId: String) {
        if (webId.isEmpty()) {
            loginResult.value = false
            loginError.value = "Connect to Solid failed."
            return
        }
        userRepository.setGrantedWebId(webId)
        loginResult.value = true
    }

    fun onFailed(message: String) {
        loginResult.value = false
        loginError.value = message.ifEmpty { "Unknown error." }
    }
}

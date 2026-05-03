package com.pondersource.solidcontacts.ui.setting

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.pondersource.solidandroidclient.sdk.SolidSignInClient
import com.pondersource.solidcontacts.repository.user.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val solidSignInClient: SolidSignInClient,
    val userRepository: UserRepository,
) : ViewModel() {

    val disconnectionLoadingState = mutableStateOf(false)
    val disconnectionResult = mutableStateOf(false)
    val disconnectionError = mutableStateOf("")

    fun disconnectFromSolid() {
        disconnectionLoadingState.value = true
        solidSignInClient.disconnectFromSolid(userRepository.getGrantedWebId()) { result ->
            if (result) {
                disconnectionLoadingState.value = false
                disconnectionResult.value = true
                userRepository.setGrantedWebId("")
            } else {
                disconnectionLoadingState.value = false
                disconnectionResult.value = false
                disconnectionError.value = "Disconnect from Solid failed."

            }
        }
    }
}
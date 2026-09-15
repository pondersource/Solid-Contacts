package com.pondersource.solidcontacts.ui.setting

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erfangholami.androidsolidservices.client.sdk.SolidSignInClient
import com.pondersource.solidcontacts.repository.user.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
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
        viewModelScope.launch {
            disconnectionLoadingState.value = true
            val result = runCatching {
                solidSignInClient.disconnectFromSolid(userRepository.getGrantedWebId())
            }.getOrDefault(false)
            disconnectionLoadingState.value = false
            if (result) {
                disconnectionResult.value = true
                userRepository.setGrantedWebId("")
            } else {
                disconnectionResult.value = false
                disconnectionError.value = "Disconnect from Solid failed."
            }
        }
    }
}

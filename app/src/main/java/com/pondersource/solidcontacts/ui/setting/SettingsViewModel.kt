package com.pondersource.solidcontacts.ui.setting

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.pondersource.solidandroidclient.sdk.SolidSignInClient
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val solidSignInClient: SolidSignInClient,
): ViewModel() {

    val disconnectionLoadingState = mutableStateOf(false)
    val disconnectionResult = mutableStateOf(false)
    val disconnectionError = mutableStateOf("")

    fun disconnectFromSolid() {
        disconnectionLoadingState.value = true
        solidSignInClient.disconnectFromSolid { result ->
            if (result) {
                disconnectionLoadingState.value = false
                disconnectionResult.value = true
            } else {
                disconnectionLoadingState.value = false
                disconnectionResult.value = false
                disconnectionError.value = "Disconnect from Solid failed."

            }
        }
    }
}
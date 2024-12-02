package com.pondersource.solidcontacts.ui.startup

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pondersource.solidandroidclient.sdk.SolidSignInClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StartupViewModel @Inject constructor(
    val solidSignInClient: SolidSignInClient,
): ViewModel() {

    val signInState : MutableState<Boolean?> = mutableStateOf(null)

    init {
        viewModelScope.launch {
            delay(200L)
            signInState.value = hasLoggedIn()
        }
    }

    private fun hasLoggedIn(): Boolean {
        return try {
            solidSignInClient.getAccount() != null
        } catch (e: Exception) {
            false
        }
        return false
    }

}
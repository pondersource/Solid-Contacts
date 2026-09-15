package com.pondersource.solidcontacts.ui.startup

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erfangholami.androidsolidservices.client.sdk.SolidSignInClient
import com.pondersource.solidcontacts.repository.user.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StartupViewModel @Inject constructor(
    val solidSignInClient: SolidSignInClient,
    val userRepository: UserRepository,
) : ViewModel() {

    val signInState: MutableState<Boolean?> = mutableStateOf(null)

    init {
        viewModelScope.launch {
            solidSignInClient.authServiceConnectionState().collect {
                if (it) {
                    //Has connected
                    signInState.value = hasLoggedIn()
                } else {
                    //not connected yet
                }
            }
        }
    }

    private suspend fun hasLoggedIn(): Boolean {
        return try {
            val grantedWebId = userRepository.getGrantedWebId()
            if (grantedWebId.isEmpty()) {
                false
            } else {
                solidSignInClient.getAccount(grantedWebId) != null
            }
        } catch (e: Exception) {
            false
        }
    }

}

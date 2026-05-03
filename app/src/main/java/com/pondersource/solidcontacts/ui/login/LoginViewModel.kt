package com.pondersource.solidcontacts.ui.login

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.pondersource.solidandroidclient.sdk.SolidException.SolidAppNotFoundException
import com.pondersource.solidandroidclient.sdk.SolidException.SolidNotLoggedInException
import com.pondersource.solidandroidclient.sdk.SolidException.SolidServiceConnectionException
import com.pondersource.solidandroidclient.sdk.SolidSignInClient
import com.pondersource.solidcontacts.repository.user.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    val solidSignInClient: SolidSignInClient,
    val userRepository: UserRepository,
) : ViewModel() {

    val loginResult = mutableStateOf(false)
    val loginError = mutableStateOf("")

    fun requestLogin() {
        try {
            solidSignInClient.requestLogin { webid, exception ->
                if (exception == null) {
                    if (!webid.isNullOrEmpty()) {
                        userRepository.setGrantedWebId(webid)
                        loginResult.value = true
                    } else {
                        loginResult.value = false
                        loginError.value = "Connect to Solid failed."
                    }
                } else {
                    loginResult.value = false
                    loginError.value = exception.message!!
                }
            }
        } catch (e: Exception) {
            when (e) {
                is SolidAppNotFoundException, is SolidNotLoggedInException, is SolidServiceConnectionException -> {
                    loginError.value = e.message!!
                }

                else -> {
                    loginError.value = "Unknown error."
                }
            }
        }
    }
}
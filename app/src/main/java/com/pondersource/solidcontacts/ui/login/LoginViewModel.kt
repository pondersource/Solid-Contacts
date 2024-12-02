package com.pondersource.solidcontacts.ui.login

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.pondersource.solidandroidclient.sdk.SolidException.SolidAppNotFoundException
import com.pondersource.solidandroidclient.sdk.SolidException.SolidNotLoggedInException
import com.pondersource.solidandroidclient.sdk.SolidException.SolidServiceConnectionException
import com.pondersource.solidandroidclient.sdk.SolidSignInClient
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    val solidSignInClient: SolidSignInClient,
): ViewModel() {

    val loginResult = mutableStateOf(false)
    val loginError = mutableStateOf("")

    fun requestLogin() {
        try {
            solidSignInClient.requestLogin { granted, exception ->
                if (exception == null) {
                    if (granted == true) {
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
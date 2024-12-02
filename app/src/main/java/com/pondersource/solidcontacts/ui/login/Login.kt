package com.pondersource.solidcontacts.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.pondersource.solidandroidclient.sdk.ui.SignInButton
import com.pondersource.solidcontacts.ui.nav.MainPage

@Composable
fun Login(
    navController: NavController,
    viewModel: LoginViewModel
) {

    val snackBarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.loginResult.value) {
        if(viewModel.loginResult.value == true) {
            navController.navigate(MainPage) {
                popUpTo(MainPage) {
                    inclusive = true
                }
            }
        }
    }

    LaunchedEffect(viewModel.loginError.value) {
        if(viewModel.loginError.value.isNotEmpty()) {
            snackBarHostState.showSnackbar(viewModel.loginError.value)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        snackbarHost = {
            SnackbarHost(hostState = snackBarHostState)
        }
    ) { paddings ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddings),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AndroidView(
                { context -> SignInButton(context).apply {
                        setOnClickListener{
                            viewModel.requestLogin()
                        }
                    }
                }
            )
        }
    }
}
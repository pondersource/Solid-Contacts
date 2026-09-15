package com.pondersource.solidcontacts.ui.login

import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.erfangholami.androidsolidservices.client.sdk.AuthorizeWithSolid
import com.erfangholami.androidsolidservices.client.sdk.Solid
import com.erfangholami.androidsolidservices.client.sdk.SolidSignInResult
import com.erfangholami.androidsolidservices.client.ui.SignInButton
import com.pondersource.solidcontacts.ui.nav.MainPage

@Composable
fun Login(
    navController: NavController,
    viewModel: LoginViewModel
) {

    val context = LocalContext.current
    val snackBarHostState = remember { SnackbarHostState() }

    val authorize = rememberLauncherForActivityResult(
        AuthorizeWithSolid(viewModel.accessRequest)
    ) { result ->
        when (result) {
            is SolidSignInResult.Authorized -> viewModel.onAuthorized(result.webId)
            is SolidSignInResult.Failed -> viewModel.onFailed(result.exception.message.orEmpty())
            SolidSignInResult.Dismissed -> Unit
        }
    }

    LaunchedEffect(viewModel.loginResult.value) {
        if (viewModel.loginResult.value == true) {
            navController.navigate(MainPage) {
                popUpTo(MainPage) {
                    inclusive = true
                }
            }
        }
    }

    LaunchedEffect(viewModel.loginError.value) {
        if (viewModel.loginError.value.isNotEmpty()) {
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
            SignInButton(onClick = {
                if (Solid.isHostInstalled(context)) {
                    authorize.launch(Unit)
                } else {
                    context.startActivity(Solid.hostInstallIntent(context))
                }
            })
        }
    }
}

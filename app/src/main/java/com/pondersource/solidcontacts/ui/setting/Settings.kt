package com.pondersource.solidcontacts.ui.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pondersource.solidcontacts.ui.element.LoadingItem
import com.pondersource.solidcontacts.ui.nav.Login

@Composable
fun Settings(
    navController: NavController,
    viewModel: SettingsViewModel,
) {

    val snackBarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.disconnectionResult.value) {
        if(viewModel.disconnectionResult.value == true) {
            navController.navigate(Login) {
                popUpTo(Login) {
                    inclusive = true
                }
            }
        }
    }

    LaunchedEffect(viewModel.disconnectionError.value) {
        if(viewModel.disconnectionError.value.isNotEmpty()) {
            snackBarHostState.showSnackbar(viewModel.disconnectionError.value)
        }
    }

    Scaffold (
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
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            if (viewModel.disconnectionLoadingState.value) {
                LoadingItem("Disconnecting...")
            } else {
                Button(
                    onClick = {
                        viewModel.disconnectFromSolid()
                    }
                ) {
                    Text(
                        "Disconnect from Solid"
                    )
                }
            }
        }
    }

}
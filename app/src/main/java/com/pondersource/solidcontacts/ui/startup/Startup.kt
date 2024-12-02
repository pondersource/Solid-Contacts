package com.pondersource.solidcontacts.ui.startup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pondersource.solidcontacts.ui.element.LoadingItem
import com.pondersource.solidcontacts.ui.nav.Login
import com.pondersource.solidcontacts.ui.nav.MainPage

@Composable
fun Startup(
    navController: NavController,
    viewModel: StartupViewModel
) {

    LaunchedEffect(viewModel.signInState.value) {
        if (viewModel.signInState.value != null) {
            if (viewModel.signInState.value!!) {
                navController.navigate(MainPage) {
                    popUpTo(MainPage) {
                        inclusive = true
                    }
                }
            } else {
                navController.navigate(Login) {
                    popUpTo(Login) {
                        inclusive = true
                    }
                }
            }
        }
    }

    Scaffold { paddings ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddings),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LoadingItem("Loading...")
        }
    }
}
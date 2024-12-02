package com.pondersource.solidcontacts.ui.addressbook

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun Group(
    innerNavController: NavController,
    viewModel: GroupViewModel
) {

    BackHandler() {
        innerNavController.popBackStack()
    }
}
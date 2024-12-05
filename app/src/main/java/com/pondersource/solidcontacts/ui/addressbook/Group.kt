package com.pondersource.solidcontacts.ui.addressbook

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.pondersource.solidcontacts.ui.element.LoadingItem
import com.pondersource.solidcontacts.ui.nav.ContactRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Group(
    innerNavController: NavController,
    viewModel: GroupViewModel
) {

    Scaffold (
        modifier = Modifier
            .fillMaxWidth(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Group Info") }
            )
        },
    ){ paddings ->

        if(viewModel.loadingGroupDetails.value) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddings),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LoadingItem("Loading contact details...")
            }
        } else if (viewModel.loadingGroupDetails.value == false && viewModel.groupDetails.value != null){
            Column (
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddings),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(viewModel.groupDetails.value!!.name)
                Text("Members:")
                ContactList(
                    viewModel.groupDetails.value!!.contacts,
                    "You don't have any contact in this group"
                ) {
                    innerNavController.navigate(ContactRoute(it.uri))
                }
            }
        }
    }

    BackHandler() {
        innerNavController.popBackStack()
    }
}
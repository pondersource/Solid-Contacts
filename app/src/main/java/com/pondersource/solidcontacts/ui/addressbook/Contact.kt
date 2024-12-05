package com.pondersource.solidcontacts.ui.addressbook

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.pondersource.solidcontacts.ui.element.LoadingItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Contact(
    innerNavController: NavController,
    viewModel: ContactViewModel
) {

    Scaffold (
        modifier = Modifier
            .fillMaxWidth(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Contact Info") }
            )
        },
    ){ paddings ->

        if(viewModel.loadingContactDetails.value) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddings),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LoadingItem("Loading contact details...")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddings),
                horizontalAlignment = Alignment.Start,
            ) {
                item {
                    Text(viewModel.contactDetails.value!!.fullName)
                }
                if (viewModel.contactDetails.value!!.phoneNumbers.isNotEmpty()) {
                    item {
                        Text(if(viewModel.contactDetails.value!!.phoneNumbers.size == 1) "Phone Number" else "Phone Numbers")
                    }
                    items(viewModel.contactDetails.value!!.phoneNumbers) {
                        Text(it.value)
                    }
                }

                if(viewModel.contactDetails.value!!.emailAddresses.isNotEmpty()) {
                    item {
                        Text(if(viewModel.contactDetails.value!!.emailAddresses.size == 1) "Email Address" else "Email Addresses")
                    }
                    items(viewModel.contactDetails.value!!.emailAddresses) {
                        Text(it.value)
                    }
                }
            }
        }
    }

    BackHandler() {
        innerNavController.popBackStack()
    }
}
package com.pondersource.solidcontacts.ui.addressbook

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pondersource.solidcontacts.ui.element.LoadingItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Contact(
    innerNavController: NavController,
    viewModel: ContactViewModel
) {

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    LaunchedEffect(viewModel.deleteContactResult.value) {
        if (viewModel.deleteContactResult.value) {
            innerNavController.popBackStack()
        }
    }


    if(viewModel.loadingContactDetails.value || viewModel.deleteLoading.value) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LoadingItem("Loading contact details...")
        }
    } else {
        Scaffold(
            modifier = Modifier
                .fillMaxWidth(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Contact Info") },
                    navigationIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier
                                .clickable { innerNavController.popBackStack() }
                                .padding(12.dp)
                        )
                    },
                    actions = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier
                                .clickable {
                                    viewModel.deleteContact()
                                }
                                .padding(12.dp),
                            tint = Color.Red
                        )
                    }
                )
            },
        ) { paddings ->

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
                        Text(if (viewModel.contactDetails.value!!.phoneNumbers.size == 1) "Phone Number" else "Phone Numbers")
                    }
                    items(viewModel.contactDetails.value!!.phoneNumbers) {
                        Text(it.value)
                    }
                }

                if (viewModel.contactDetails.value!!.emailAddresses.isNotEmpty()) {
                    item {
                        Text(if (viewModel.contactDetails.value!!.emailAddresses.size == 1) "Email Address" else "Email Addresses")
                    }
                    items(viewModel.contactDetails.value!!.emailAddresses) {
                        Text(it.value)
                    }
                }
            }
        }
    }

    BackHandler() {
        if (!viewModel.deleteLoading.value) {
            innerNavController.popBackStack()
        }
    }
}
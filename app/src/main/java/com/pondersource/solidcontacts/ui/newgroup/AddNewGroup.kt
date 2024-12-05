package com.pondersource.solidcontacts.ui.newgroup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pondersource.solidcontacts.R
import com.pondersource.solidcontacts.ui.element.ContactItem
import com.pondersource.solidcontacts.ui.element.LoadingItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNewGroup(
    navController: NavController,
    viewModel: AddNewGroupViewModel,
) {

    val messageSnackBarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    LaunchedEffect(viewModel.addGroupResult.value) {
        if (viewModel.addGroupResult.value != null) {
            navController.popBackStack()
        }
    }

    LaunchedEffect(viewModel.errorMessages.value) {
        if(!viewModel.errorMessages.value.isNullOrEmpty()) {
            messageSnackBarHostState.showSnackbar(viewModel.errorMessages.value!!)
            viewModel.errorMessages.value = null
        }
    }


    if (!viewModel.loading.value) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = "Create contact")
                    },
                    navigationIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = null,
                            modifier = Modifier.clickable {
                                navController.popBackStack()
                            }.padding(8.dp)
                        )
                    },
                    actions = {
                        Button(
                            onClick = {
                                viewModel.addNewGroup()
                            },
                            modifier = Modifier
                                .padding(8.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(text = "Save")
                        }
                    }
                )
            },
            snackbarHost = {
                SnackbarHost(hostState = messageSnackBarHostState)
            }
        ) { paddings ->
            Column(
                modifier = Modifier
                    .padding(paddings)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                OutlinedTextField(
                    value = viewModel.title.value,
                    onValueChange = {
                        viewModel.title.value = it
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 8.dp),
                    label = {
                        Text("Title")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                )

                if (viewModel.includedContacts.isNotEmpty()) {
                    Text(
                        text = "Included Contacts:",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp, 8.dp),
                        textAlign = TextAlign.Start,
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp, 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(viewModel.includedContacts.size) {
                            ContactItem(
                                viewModel.includedContacts[it],
                                true,
                                true,
                                {viewModel.excludeContact(it)}
                            ) {}
                        }
                    }
                }

                if (viewModel.notIncludedContacts.isNotEmpty()) {
                    Text(
                        text = "You can add these Contacts to the Group:",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp, 8.dp),
                        textAlign = TextAlign.Start,
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp, 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(viewModel.notIncludedContacts.size) {
                            ContactItem(
                                viewModel.notIncludedContacts[it],
                                true,
                                false,
                                {viewModel.includeContact(it)}
                            ) {}
                        }
                    }
                }

            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LoadingItem("Loading...")
        }
    }

    BackHandler() {
        navController.popBackStack()
    }
}
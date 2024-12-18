package com.pondersource.solidcontacts.ui.addressbook

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pondersource.solidcontacts.ui.element.DeleteDialog
import com.pondersource.solidcontacts.ui.element.DeleteElementType
import com.pondersource.solidcontacts.ui.element.LoadingItem
import com.pondersource.solidcontacts.ui.nav.ContactRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Group(
    innerNavController: NavController,
    viewModel: GroupViewModel
) {

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    LaunchedEffect(viewModel.deleteGroupResult.value) {
        if (viewModel.deleteGroupResult.value) {
            innerNavController.popBackStack()
        }
    }

    val showDeleteDialog = remember { mutableStateOf(false) }

    if(viewModel.loadingGroupDetails.value || viewModel.deleteLoading.value) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LoadingItem("Loading contact details...")
        }
    } else if (
        viewModel.loadingGroupDetails.value == false &&
        viewModel.deleteLoading.value == false &&
        viewModel.groupDetails.value != null
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxWidth(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Group Info") },
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
                                    showDeleteDialog.value = true
                                }
                                .padding(12.dp),
                            tint = Color.Red
                        )
                    }
                )
            },
        ) { paddings ->

            Column(
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
                    innerNavController.navigate(ContactRoute(viewModel.groupRoute.addressBookUri, it.uri))
                }
            }
        }

        when {
            showDeleteDialog.value -> {
                DeleteDialog(
                    onDismissRequest = {
                        showDeleteDialog.value = false
                    },
                    onDelete = {
                        viewModel.deleteGroup()
                    },
                    type = DeleteElementType.GROUP
                )
            }
        }
    }

    BackHandler() {
        if(!viewModel.deleteLoading.value) {
            innerNavController.popBackStack()
        }
    }
}
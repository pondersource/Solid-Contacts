package com.pondersource.solidcontacts.ui.screens.picker

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pondersource.solidcontacts.ui.components.ContactRow
import com.pondersource.solidcontacts.ui.components.EmptyState
import com.pondersource.solidcontacts.ui.components.SearchBar

/** Multi-select over contacts, used when adding people to a group. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ContactPickerScreen(
    viewModel: ContactPickerViewModel,
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.done) {
        if (state.done) onClose()
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            if (state.hasSelection) {
                                "${state.selectedIds.size} selected"
                            } else {
                                "Choose contacts"
                            },
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close")
                        }
                    },
                )
                SearchBar(
                    query = state.query,
                    onQueryChange = viewModel::onQueryChange,
                    onClear = viewModel::clearQuery,
                    placeholder = "Search contacts",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        },
        floatingActionButton = {
            if (state.hasSelection) {
                ExtendedFloatingActionButton(
                    onClick = viewModel::confirm,
                    icon = { Icon(Icons.Rounded.Check, contentDescription = null) },
                    text = { Text("Add ${state.selectedIds.size}") },
                )
            }
        },
    ) { padding ->
        if (state.contacts.isEmpty() && !state.loading) {
            EmptyState(
                icon = Icons.Rounded.PersonSearch,
                title = "Nobody to add",
                description = "Every contact in this address book is already in the group.",
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 96.dp),
            ) {
                items(count = state.contacts.size, key = { state.contacts[it].id }) { index ->
                    val contact = state.contacts[index]
                    ContactRow(
                        contact = contact,
                        photo = contact.photoThumb,
                        selected = contact.id in state.selectedIds,
                        selectionMode = true,
                        onClick = { viewModel.toggle(contact.id) },
                        onLongClick = { viewModel.toggle(contact.id) },
                    )
                }
            }
        }
    }
}

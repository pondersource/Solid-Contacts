package com.pondersource.solidcontacts.ui.screens.books

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pondersource.solidcontacts.ui.components.AddressBookRow
import com.pondersource.solidcontacts.ui.components.EmptyState
import com.pondersource.solidcontacts.ui.components.SectionHeader
import com.pondersource.solidcontacts.ui.components.SyncBanner
import com.pondersource.solidcontacts.ui.components.TextInputDialog

/**
 * The address books on the pod.
 *
 * A private book is listed in the pod's private type index and is visible only to the user; a
 * public one is listed publicly, which is how a shared team directory works. The screen keeps
 * that distinction visible, because it decides who can find the contacts inside.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooksScreen(
    viewModel: BooksViewModel,
    onOpenBook: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreate by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                Text(
                    text = "Address books",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 8.dp),
                )
                SyncBanner(status = state.syncStatus, onRetry = viewModel::retrySync)
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreate = true },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("New book") },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.isEmpty && !state.loading) {
                EmptyState(
                    icon = Icons.Rounded.LibraryBooks,
                    title = "No address books",
                    description = "An address book is a container on your pod that holds " +
                        "contacts and groups.",
                    actionLabel = "Create one",
                    onAction = { showCreate = true },
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 96.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (state.privateBooks.isNotEmpty()) {
                        item(key = "private-header") {
                            SectionHeader(
                                title = "Private",
                                trailing = "Only you",
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        items(
                            count = state.privateBooks.size,
                            key = { state.privateBooks[it].id },
                        ) { index ->
                            val book = state.privateBooks[index]
                            AddressBookRow(book = book, onClick = { onOpenBook(book.id) })
                        }
                    }
                    if (state.publicBooks.isNotEmpty()) {
                        item(key = "public-header") {
                            SectionHeader(
                                title = "Public",
                                trailing = "Listed on your pod",
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        items(
                            count = state.publicBooks.size,
                            key = { state.publicBooks[it].id },
                        ) { index ->
                            val book = state.publicBooks[index]
                            AddressBookRow(book = book, onClick = { onOpenBook(book.id) })
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        TextInputDialog(
            title = "New address book",
            label = "Title",
            confirmLabel = "Create",
            checkboxLabel = "Private (only you can find it)",
            checkboxInitial = true,
            onConfirm = viewModel::createBook,
            onDismiss = { showCreate = false },
        )
    }
}

package com.pondersource.solidcontacts.ui.screens.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pondersource.solidcontacts.ui.components.EmptyState
import com.pondersource.solidcontacts.ui.components.GroupRow
import com.pondersource.solidcontacts.ui.components.SectionHeader
import com.pondersource.solidcontacts.ui.components.SyncBanner

/** Every group, across every address book, with the book each one belongs to. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    viewModel: GroupsViewModel,
    onOpenGroup: (String) -> Unit,
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
                    text = "Groups",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 8.dp),
                )
                SyncBanner(status = state.syncStatus, onRetry = viewModel::retrySync)
            }
        },
        floatingActionButton = {
            if (state.canCreate) {
                ExtendedFloatingActionButton(
                    onClick = { showCreate = true },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("New group") },
                )
            }
        },
    ) { padding ->
        if (state.groups.isEmpty() && !state.loading) {
            EmptyState(
                icon = Icons.Rounded.Group,
                title = "No groups yet",
                description = "Groups gather contacts under one name — a team, a family, " +
                    "a project.",
                actionLabel = if (state.canCreate) "Create a group" else null,
                onAction = if (state.canCreate) ({ showCreate = true }) else null,
                modifier = Modifier.padding(padding),
            )
        } else {
            val byBook = state.groups.groupBy { it.bookId }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                byBook.forEach { (bookId, groups) ->
                    val bookTitle = state.books.firstOrNull { it.id == bookId }?.title
                    if (byBook.size > 1 && bookTitle != null) {
                        item(key = "header-$bookId") {
                            SectionHeader(title = bookTitle, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    items(count = groups.size, key = { groups[it].id }) { index ->
                        val group = groups[index]
                        GroupRow(
                            group = group,
                            onClick = { onOpenGroup(group.id) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    if (showCreate) {
        NewGroupDialog(
            books = state.books,
            onConfirm = viewModel::createGroup,
            onDismiss = { showCreate = false },
        )
    }
}

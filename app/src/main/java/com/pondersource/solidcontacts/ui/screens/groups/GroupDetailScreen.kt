package com.pondersource.solidcontacts.ui.screens.groups

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.PersonAddAlt
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pondersource.solidcontacts.ui.components.ConfirmDialog
import com.pondersource.solidcontacts.ui.components.ContactRow
import com.pondersource.solidcontacts.ui.components.EmptyState
import com.pondersource.solidcontacts.ui.components.LoadingState
import com.pondersource.solidcontacts.ui.components.countLabel

/** One group and the people in it. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GroupDetailScreen(
    viewModel: GroupDetailViewModel,
    onBack: () -> Unit,
    onOpenContact: (String) -> Unit,
    onAddMembers: (groupId: String, bookId: String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDelete by remember { mutableStateOf(false) }

    LaunchedEffect(state.deleted) {
        if (state.deleted) onBack()
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.group?.name ?: "Group",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = countLabel(state.members.size, "member", "members"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDelete = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Delete group",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            val group = state.group
            if (group != null) {
                ExtendedFloatingActionButton(
                    onClick = { onAddMembers(group.id, group.bookId) },
                    icon = { Icon(Icons.Rounded.PersonAddAlt, contentDescription = null) },
                    text = { Text("Add members") },
                )
            }
        },
    ) { padding ->
        when {
            state.loading -> LoadingState(modifier = Modifier.padding(padding))

            state.members.isEmpty() -> EmptyState(
                icon = Icons.Rounded.Group,
                title = "No members yet",
                description = "Add contacts from this address book to the group.",
                actionLabel = "Add members",
                onAction = {
                    state.group?.let { onAddMembers(it.id, it.bookId) }
                },
                modifier = Modifier.padding(padding),
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 96.dp),
            ) {
                items(count = state.members.size, key = { state.members[it].id }) { index ->
                    val member = state.members[index]
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        ContactRow(
                            contact = member,
                            photo = member.photoThumb,
                            onClick = { onOpenContact(member.id) },
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { viewModel.removeMember(member.id) }) {
                            Icon(
                                imageVector = Icons.Rounded.PersonRemove,
                                contentDescription = "Remove from group",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDelete) {
        ConfirmDialog(
            title = "Delete group",
            message = "Delete \"${state.group?.name}\"? The contacts in it stay in your pod.",
            confirmLabel = "Delete",
            destructive = true,
            icon = Icons.Rounded.Delete,
            onConfirm = viewModel::delete,
            onDismiss = { showDelete = false },
        )
    }
}

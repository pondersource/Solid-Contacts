package com.pondersource.solidcontacts.ui.screens.books

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PersonAddAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pondersource.solidcontacts.ui.components.ConfirmDialog
import com.pondersource.solidcontacts.ui.components.ContactRow
import com.pondersource.solidcontacts.ui.components.EmptyState
import com.pondersource.solidcontacts.ui.components.GroupRow
import com.pondersource.solidcontacts.ui.components.LoadingState
import com.pondersource.solidcontacts.ui.components.TextInputDialog
import com.pondersource.solidcontacts.ui.components.countLabel
import kotlinx.coroutines.launch

/** One address book: the contacts it holds and the groups defined in it. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BookDetailScreen(
    viewModel: BookDetailViewModel,
    onBack: () -> Unit,
    onOpenContact: (String) -> Unit,
    onOpenGroup: (String) -> Unit,
    onCreateContact: (bookId: String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val pagerState = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showNewGroup by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

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
                            text = state.book?.title ?: "Address book",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        state.book?.let { book ->
                            Text(
                                text = if (book.isPrivate) "Private" else "Public",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                leadingIcon = { Icon(Icons.Rounded.DriveFileRenameOutline, null) },
                                onClick = {
                                    showMenu = false
                                    showRename = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("New group") },
                                leadingIcon = { Icon(Icons.Rounded.GroupAdd, null) },
                                onClick = {
                                    showMenu = false
                                    showNewGroup = true
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text("Delete book", color = MaterialTheme.colorScheme.error)
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        null,
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    showDelete = true
                                },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (pagerState.currentPage == 0) {
                        onCreateContact(viewModel.bookId)
                    } else {
                        showNewGroup = true
                    }
                },
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add")
            }
        },
    ) { padding ->
        if (state.loading) {
            LoadingState(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TabRow(selectedTabIndex = pagerState.currentPage) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text(countLabel(state.contacts.size, "contact", "contacts")) },
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text(countLabel(state.groups.size, "group", "groups")) },
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                when (page) {
                    0 -> if (state.contacts.isEmpty()) {
                        EmptyState(
                            icon = Icons.Rounded.PersonAddAlt,
                            title = "No contacts here",
                            description = "Contacts you add to this book appear here.",
                            actionLabel = "Add a contact",
                            onAction = { onCreateContact(viewModel.bookId) },
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 8.dp,
                                end = 8.dp,
                                top = 8.dp,
                                bottom = 96.dp,
                            ),
                        ) {
                            items(
                                count = state.contacts.size,
                                key = { state.contacts[it].id },
                            ) { index ->
                                val contact = state.contacts[index]
                                ContactRow(
                                    contact = contact,
                                    photo = contact.photoThumb,
                                    onClick = { onOpenContact(contact.id) },
                                )
                            }
                        }
                    }

                    1 -> if (state.groups.isEmpty()) {
                        EmptyState(
                            icon = Icons.Rounded.Group,
                            title = "No groups here",
                            description = "A group gathers contacts from this book under one name.",
                            actionLabel = "Create a group",
                            onAction = { showNewGroup = true },
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 8.dp,
                                end = 8.dp,
                                top = 8.dp,
                                bottom = 96.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(
                                count = state.groups.size,
                                key = { state.groups[it].id },
                            ) { index ->
                                val group = state.groups[index]
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
        }
    }

    if (showRename) {
        TextInputDialog(
            title = "Rename address book",
            label = "Title",
            initialValue = state.book?.title.orEmpty(),
            onConfirm = { value, _ -> viewModel.rename(value) },
            onDismiss = { showRename = false },
        )
    }

    if (showNewGroup) {
        TextInputDialog(
            title = "New group",
            label = "Group name",
            confirmLabel = "Create",
            onConfirm = { value, _ -> viewModel.createGroup(value) },
            onDismiss = { showNewGroup = false },
        )
    }

    if (showDelete) {
        ConfirmDialog(
            title = "Delete address book",
            message = "Delete \"${state.book?.title}\" and everything in it? " +
                "This removes ${countLabel(state.contacts.size, "contact", "contacts")} " +
                "from your pod.",
            confirmLabel = "Delete",
            destructive = true,
            icon = Icons.Rounded.Delete,
            onConfirm = viewModel::delete,
            onDismiss = { showDelete = false },
        )
    }
}

package com.pondersource.solidcontacts.ui.screens.contacts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PersonAddAlt
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pondersource.solidcontacts.ui.components.ConfirmDialog
import com.pondersource.solidcontacts.ui.components.ContactRow
import com.pondersource.solidcontacts.ui.components.EmptyState
import com.pondersource.solidcontacts.ui.components.LetterHeader
import com.pondersource.solidcontacts.ui.components.SearchBar
import com.pondersource.solidcontacts.ui.components.SectionHeader
import com.pondersource.solidcontacts.ui.components.SyncBanner
import com.pondersource.solidcontacts.ui.components.AlphabetIndex
import kotlinx.coroutines.launch

private val INDEX_LETTERS = ('A'..'Z').map(Char::toString) + "#"

/**
 * Wraps an empty state so the pull-to-refresh gesture still reaches the container.
 *
 * A pull-to-refresh box feels the drag through nested scroll, so a fixed child swallows it and
 * the one screen where a refresh matters most becomes the one screen that cannot ask for one.
 * A single-item lazy list scrolls by nature, and `fillParentMaxSize` keeps the content centred
 * in the viewport exactly as before.
 */
@Composable
private fun PullableEmpty(content: @Composable () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Box(modifier = Modifier.fillParentMaxSize()) { content() }
        }
    }
}

/**
 * Every contact the user has, from every address book.
 *
 * The list is the app's home: alphabetical with sticky letters, favourites pinned on top, a
 * letter rail for jumping, search over every field, and long-press for bulk actions.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ContactsScreen(
    viewModel: ContactsViewModel,
    onOpenContact: (String) -> Unit,
    onCreateContact: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    // The letter rail needs to know where each letter starts in the flattened list.
    val letterOffsets = remember(state.sections, state.favorites) {
        buildMap {
            var index = if (state.favorites.isEmpty()) 0 else state.favorites.size + 1
            state.sections.forEach { section ->
                put(section.letter, index)
                index += section.contacts.size + 1
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (state.selectionMode) {
                TopAppBar(
                    title = { Text("${state.selectedIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = viewModel::clearSelection) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Delete selected",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                )
            } else {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("Contacts", style = MaterialTheme.typography.headlineMedium)
                            if (state.totalCount > 0) {
                                Text(
                                    text = com.pondersource.solidcontacts.ui.components.countLabel(
                                        state.totalCount,
                                        "contact",
                                        "contacts",
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    SearchBar(
                        query = state.query,
                        onQueryChange = viewModel::onQueryChange,
                        onClear = viewModel::clearQuery,
                        placeholder = "Search name, number, company…",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    SyncBanner(status = state.syncStatus, onRetry = viewModel::retrySync)
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(visible = !state.selectionMode) {
                ExtendedFloatingActionButton(
                    onClick = onCreateContact,
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("New contact") },
                )
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.loading -> Unit

                // PullToRefreshBox needs a scrolling child to feel the drag, so the empty
                // states scroll too. Otherwise the one screen where a refresh matters most
                // is the one screen that cannot ask for it.
                state.isEmpty && state.isSearching -> PullableEmpty {
                    EmptyState(
                        icon = Icons.Rounded.Search,
                        title = "No matches",
                        description = "Nothing here matches \"${state.query}\".",
                    )
                }

                state.isEmpty -> PullableEmpty {
                    EmptyState(
                        icon = Icons.Rounded.PersonAddAlt,
                        title = "No contacts yet",
                        description = "Pull down to check your pod, add someone, or bring your " +
                            "existing contacts in from Settings → Import and export.",
                        actionLabel = "Add a contact",
                        onAction = onCreateContact,
                    )
                }

                else -> Box(Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 8.dp,
                            end = 32.dp,
                            top = 4.dp,
                            bottom = 96.dp,
                        ),
                    ) {
                        if (state.favorites.isNotEmpty()) {
                            item(key = "favorites-header") {
                                SectionHeader(title = "Favourites")
                            }
                            items(
                                count = state.favorites.size,
                                key = { "fav-${state.favorites[it].id}" },
                            ) { index ->
                                val contact = state.favorites[index]
                                ContactRow(
                                    contact = contact,
                                    photo = contact.photoThumb,
                                    selected = contact.id in state.selectedIds,
                                    selectionMode = state.selectionMode,
                                    onClick = {
                                        if (state.selectionMode) {
                                            viewModel.toggleSelection(contact.id)
                                        } else {
                                            onOpenContact(contact.id)
                                        }
                                    },
                                    onLongClick = { viewModel.toggleSelection(contact.id) },
                                )
                            }
                        }

                        state.sections.forEach { section ->
                            stickyHeader(key = "header-${section.letter}") {
                                LetterHeader(section.letter)
                            }
                            items(
                                count = section.contacts.size,
                                key = { section.contacts[it].id },
                            ) { index ->
                                val contact = section.contacts[index]
                                ContactRow(
                                    contact = contact,
                                    photo = contact.photoThumb,
                                    selected = contact.id in state.selectedIds,
                                    selectionMode = state.selectionMode,
                                    onClick = {
                                        if (state.selectionMode) {
                                            viewModel.toggleSelection(contact.id)
                                        } else {
                                            onOpenContact(contact.id)
                                        }
                                    },
                                    onLongClick = { viewModel.toggleSelection(contact.id) },
                                )
                            }
                        }

                        item(key = "footer") { Spacer(Modifier.height(8.dp)) }
                    }

                    if (!state.isSearching && state.sections.size > 2) {
                        AlphabetIndex(
                            letters = INDEX_LETTERS,
                            activeLetters = state.activeLetters,
                            onLetterSelected = { letter ->
                                letterOffsets[letter]?.let { offset ->
                                    scope.launch { listState.animateScrollToItem(offset) }
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 4.dp, top = 8.dp, bottom = 88.dp),
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = "Delete contacts",
            message = "Delete ${state.selectedIds.size} " +
                if (state.selectedIds.size == 1) "contact?" else "contacts?",
            confirmLabel = "Delete",
            destructive = true,
            icon = Icons.Rounded.Delete,
            onConfirm = viewModel::deleteSelected,
            onDismiss = { showDeleteDialog = false },
        )
    }
}

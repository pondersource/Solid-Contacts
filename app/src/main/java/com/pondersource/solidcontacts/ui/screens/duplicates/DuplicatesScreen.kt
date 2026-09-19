package com.pondersource.solidcontacts.ui.screens.duplicates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Merge
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pondersource.solidcontacts.domain.model.ContactDetail
import com.pondersource.solidcontacts.domain.model.DuplicateCluster
import com.pondersource.solidcontacts.ui.components.ContactAvatar
import com.pondersource.solidcontacts.ui.components.EmptyState
import com.pondersource.solidcontacts.ui.components.LoadingState

/**
 * The same person, saved twice.
 *
 * Each card shows what the app believes is one person and what each copy carries, so the user
 * can see what a merge would keep before doing it. A merge keeps every value from every copy.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicatesScreen(
    viewModel: DuplicatesViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

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
                title = { Text("Duplicates") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading -> LoadingState(
                message = "Looking for the same person twice…",
                modifier = Modifier.padding(padding),
            )

            state.visible.isEmpty() -> EmptyState(
                icon = Icons.Rounded.CheckCircle,
                title = "No duplicates",
                description = "Every contact looks like a different person.",
                modifier = Modifier.padding(padding),
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    count = state.visible.size,
                    key = { state.visible[it].signature },
                ) { index ->
                    val cluster = state.visible[index]
                    DuplicateCard(
                        cluster = cluster,
                        merging = state.merging == cluster.signature,
                        onMerge = { viewModel.merge(cluster, cluster.members.first().id) },
                        onKeepSeparate = { viewModel.keepSeparate(cluster) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DuplicateCard(
    cluster: DuplicateCluster,
    merging: Boolean,
    onMerge: () -> Unit,
    onKeepSeparate: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${cluster.members.size} copies of the same person",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Merging keeps every phone, email and address from all of them.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            cluster.members.forEachIndexed { index, member ->
                DuplicateMember(member = member, isSurvivor = index == 0)
            }

            if (merging) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onKeepSeparate) { Text("Keep separate") }
                    Button(onClick = onMerge) {
                        Icon(
                            imageVector = Icons.Rounded.Merge,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 6.dp),
                        )
                        Text("Merge")
                    }
                }
            }
        }
    }
}

@Composable
private fun DuplicateMember(member: ContactDetail, isSurvivor: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ContactAvatar(name = member.displayName(), size = 40.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = member.displayName(),
                style = MaterialTheme.typography.bodyLarge,
            )
            val details = listOfNotNull(
                member.phones.firstOrNull()?.number,
                member.emails.firstOrNull()?.address,
                member.organization,
            ).joinToString(" · ")
            if (details.isNotBlank()) {
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (isSurvivor) {
            Text(
                text = "Kept",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

package com.pondersource.solidcontacts.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.ImportExport
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pondersource.solidcontacts.data.local.prefs.SortOrder
import com.pondersource.solidcontacts.data.local.prefs.ThemeMode
import com.pondersource.solidcontacts.ui.components.ConfirmDialog
import com.pondersource.solidcontacts.ui.components.countLabel
import java.text.DateFormat
import java.util.Date

/** The account, how the app syncs, how it looks, and the way out. */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenDuplicates: () -> Unit,
    onOpenImportExport: () -> Unit,
    onSignedOut: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDisconnect by remember { mutableStateOf(false) }

    LaunchedEffect(state.signedOut) {
        if (state.signedOut) onSignedOut()
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 8.dp),
            )

            // --- Account ---------------------------------------------------------------
            SettingsSection("Your pod") {
                SettingsRow(
                    icon = Icons.Rounded.Badge,
                    title = "WebID",
                    subtitle = state.settings.webId.ifEmpty { "Not connected" },
                )
                SettingsRow(
                    icon = Icons.Rounded.Cloud,
                    title = "Stored here",
                    subtitle = countLabel(state.contactCount, "contact", "contacts") +
                        if (state.pendingWrites > 0) {
                            " · ${state.pendingWrites} waiting to sync"
                        } else {
                            ""
                        },
                )
            }

            // --- Sync ------------------------------------------------------------------
            SettingsSection("Sync") {
                SettingsRow(
                    icon = Icons.Rounded.Sync,
                    title = if (state.syncing) "Syncing…" else "Sync now",
                    subtitle = state.settings.lastSyncAt
                        .takeIf { it > 0 }
                        ?.let { "Last sync ${formatTime(it)}" }
                        ?: "Never synced yet",
                    onClick = if (state.syncing) null else viewModel::syncNow,
                )
                SettingsToggle(
                    icon = Icons.Rounded.Wifi,
                    title = "Background sync on Wi-Fi only",
                    subtitle = "Your changes still send straight away on any network.",
                    checked = state.settings.syncOnlyOnWifi,
                    onCheckedChange = viewModel::setSyncOnlyOnWifi,
                )
                if (!state.online) {
                    SettingsRow(
                        icon = Icons.Rounded.Cloud,
                        title = "Offline",
                        subtitle = "Everything works. Changes sync when you are back online.",
                    )
                }
            }

            // --- Contacts --------------------------------------------------------------
            SettingsSection("Contacts") {
                SettingsRow(
                    icon = Icons.Rounded.SortByAlpha,
                    title = "Sort by",
                    subtitle = when (state.settings.sortOrder) {
                        SortOrder.FIRST_NAME -> "First name"
                        SortOrder.LAST_NAME -> "Last name"
                    },
                    trailing = {
                        SingleChoiceSegmentedButtonRow {
                            SegmentedButton(
                                selected = state.settings.sortOrder == SortOrder.FIRST_NAME,
                                onClick = { viewModel.setSortOrder(SortOrder.FIRST_NAME) },
                                shape = SegmentedButtonDefaults.itemShape(0, 2),
                            ) { Text("First") }
                            SegmentedButton(
                                selected = state.settings.sortOrder == SortOrder.LAST_NAME,
                                onClick = { viewModel.setSortOrder(SortOrder.LAST_NAME) },
                                shape = SegmentedButtonDefaults.itemShape(1, 2),
                            ) { Text("Last") }
                        }
                    },
                )
                SettingsRow(
                    icon = Icons.Rounded.ContentCopy,
                    title = "Find duplicates",
                    subtitle = "Spot the same person saved twice, and merge them.",
                    onClick = onOpenDuplicates,
                )
                SettingsRow(
                    icon = Icons.Rounded.ImportExport,
                    title = "Import and export",
                    subtitle = "vCard files, and the contacts on this phone.",
                    onClick = onOpenImportExport,
                )
            }

            // --- Appearance ------------------------------------------------------------
            SettingsSection("Appearance") {
                SettingsRow(
                    icon = Icons.Rounded.DarkMode,
                    title = "Theme",
                    subtitle = when (state.settings.themeMode) {
                        ThemeMode.SYSTEM -> "Follow the system"
                        ThemeMode.LIGHT -> "Light"
                        ThemeMode.DARK -> "Dark"
                    },
                    trailing = {
                        SingleChoiceSegmentedButtonRow {
                            ThemeMode.entries.forEachIndexed { index, mode ->
                                SegmentedButton(
                                    selected = state.settings.themeMode == mode,
                                    onClick = { viewModel.setThemeMode(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index,
                                        ThemeMode.entries.size,
                                    ),
                                ) {
                                    Text(
                                        when (mode) {
                                            ThemeMode.SYSTEM -> "Auto"
                                            ThemeMode.LIGHT -> "Light"
                                            ThemeMode.DARK -> "Dark"
                                        },
                                    )
                                }
                            }
                        }
                    },
                )
                SettingsToggle(
                    icon = Icons.Rounded.Palette,
                    title = "Use my wallpaper colours",
                    subtitle = "Takes the palette from your home screen.",
                    checked = state.settings.dynamicColor,
                    onCheckedChange = viewModel::setDynamicColor,
                )
            }

            // --- About -----------------------------------------------------------------
            SettingsSection("About") {
                SettingsRow(
                    icon = Icons.Rounded.Link,
                    title = "Solid Contacts",
                    subtitle = "Your contacts live on your own pod, in the standard vCard " +
                        "vocabulary, readable by any Solid app.",
                )
                SettingsRow(
                    icon = Icons.Rounded.Logout,
                    title = "Disconnect from Solid",
                    subtitle = "Gives the grant back and removes the local copy.",
                    destructive = true,
                    onClick = { showDisconnect = true },
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showDisconnect) {
        ConfirmDialog(
            title = "Disconnect",
            message = "This removes the copy on this device and gives the grant back to Solid " +
                "Share. Your contacts stay on your pod." +
                if (state.pendingWrites > 0) {
                    " ${state.pendingWrites} change(s) have not reached your pod yet and will be lost."
                } else {
                    ""
                },
            confirmLabel = "Disconnect",
            destructive = true,
            icon = Icons.Rounded.Logout,
            onConfirm = viewModel::disconnect,
            onDismiss = { showDisconnect = false },
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 6.dp),
        )
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) { content() }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    destructive: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val tint = if (destructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (destructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (trailing != null) {
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.padding(start = 38.dp)) { trailing() }
        }
    }
}

@Composable
private fun SettingsToggle(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun formatTime(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(millis))

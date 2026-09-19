package com.pondersource.solidcontacts.ui.screens.importexport

import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContactPhone
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pondersource.solidcontacts.ui.components.countLabel

/**
 * Moving contacts in and out.
 *
 * Four directions: a `.vcf` file either way, and the phone's own contact store either way. The
 * pod is always the source of truth; nothing here changes that.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportExportScreen(
    viewModel: ImportExportViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val pickFile = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri -> if (uri != null) viewModel.importVCard(uri) }

    val requestRead = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.refreshPermissions()
        if (granted) viewModel.importFromDevice()
    }

    val requestWrite = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.refreshPermissions()
        if (granted) viewModel.exportToDevice()
    }

    LaunchedEffect(state.transfer) {
        when (val transfer = state.transfer) {
            is TransferState.Done -> {
                snackbarHostState.showSnackbar(transfer.message)
                viewModel.consumeResult()
            }

            is TransferState.Failed -> {
                snackbarHostState.showSnackbar(transfer.message)
                viewModel.consumeResult()
            }

            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Import and export") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            if (state.busy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                (state.transfer as? TransferState.Working)?.let { working ->
                    Text(
                        text = working.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
            }

            SectionTitle("Where imports go")
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    BookPicker(
                        books = state.books,
                        selectedId = state.targetBookId,
                        onSelect = viewModel::setTargetBook,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setSkipDuplicates(!state.skipDuplicates) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Skip people I already have",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = "Matches on WebID, email, phone number or exact name.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = state.skipDuplicates,
                            onCheckedChange = viewModel::setSkipDuplicates,
                        )
                    }
                }
            }

            SectionTitle("vCard files")
            ActionCard(
                icon = Icons.Rounded.FileDownload,
                title = "Import a .vcf file",
                description = "Reads vCard 2.1, 3.0 and 4.0 — what other contact apps export.",
                enabled = !state.busy,
                onClick = { pickFile.launch("*/*") },
            )
            ActionCard(
                icon = Icons.Rounded.FileUpload,
                title = "Export all as a vCard",
                description = "Shares ${countLabel(state.contactCount, "contact", "contacts")} " +
                    "as vCard text you can save or send anywhere.",
                enabled = !state.busy && state.contactCount > 0,
                onClick = {
                    viewModel.exportVCard { card ->
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/vcard"
                            putExtra(Intent.EXTRA_TEXT, card)
                            putExtra(Intent.EXTRA_TITLE, "contacts.vcf")
                        }
                        runCatching {
                            context.startActivity(Intent.createChooser(intent, "Export contacts"))
                        }
                    }
                },
            )

            SectionTitle("This phone")
            ActionCard(
                icon = Icons.Rounded.ContactPhone,
                title = "Import from this phone",
                description = "Copies the contacts already on this device into your pod.",
                enabled = !state.busy,
                onClick = {
                    if (state.canReadDevice) {
                        viewModel.importFromDevice()
                    } else {
                        requestRead.launch(Manifest.permission.READ_CONTACTS)
                    }
                },
            )
            ActionCard(
                icon = Icons.Rounded.PhoneAndroid,
                title = "Export to this phone",
                description = "Writes your pod's contacts into the phone's own address book, so " +
                    "the dialer and messaging apps can see them.",
                enabled = !state.busy && state.contactCount > 0,
                onClick = {
                    if (state.canWriteDevice) {
                        viewModel.exportToDevice()
                    } else {
                        requestWrite.launch(Manifest.permission.WRITE_CONTACTS)
                    }
                },
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 6.dp),
    )
}

@Composable
private fun ActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookPicker(
    books: List<com.pondersource.solidcontacts.domain.model.AddressBookSummary>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = books.firstOrNull { it.id == selectedId }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.title ?: "Default address book",
            onValueChange = { },
            readOnly = true,
            label = { Text("Address book") },
            leadingIcon = {
                Icon(
                    Icons.Rounded.LibraryBooks,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            books.forEach { book ->
                DropdownMenuItem(
                    text = { Text(book.title) },
                    onClick = {
                        onSelect(book.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

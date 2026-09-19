package com.pondersource.solidcontacts.ui.screens.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Note
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pondersource.solidcontacts.domain.model.AddressKind
import com.pondersource.solidcontacts.domain.model.EmailKind
import com.pondersource.solidcontacts.domain.model.ImKind
import com.pondersource.solidcontacts.domain.model.LinkKind
import com.pondersource.solidcontacts.domain.model.PhoneKind
import com.pondersource.solidcontacts.ui.components.ContactAvatar
import com.pondersource.solidcontacts.ui.components.LoadingState

/**
 * Creates or edits a contact.
 *
 * The common fields are on show; everything else the vCard standard carries is one tap away
 * behind "More fields", so a quick add stays quick without the app quietly dropping data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactEditorScreen(
    viewModel: ContactEditorViewModel,
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            val bytes = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }.getOrNull()
            if (bytes != null) {
                viewModel.setPhoto(bytes, context.contentResolver.getType(uri) ?: "image/jpeg")
            }
        }
    }

    LaunchedEffect(state.saved) {
        if (state.saved) onClose()
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "New contact" else "Edit contact") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    Button(
                        onClick = viewModel::save,
                        enabled = state.canSave,
                        modifier = Modifier.padding(end = 12.dp),
                    ) {
                        Text("Save")
                    }
                },
            )
        },
    ) { padding ->
        if (state.loading) {
            LoadingState(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState()),
        ) {
            // --- Photo -----------------------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box {
                        ContactAvatar(
                            name = state.draft.displayName(),
                            photo = state.photo,
                            size = 96.dp,
                        )
                        Surface(
                            onClick = { pickPhoto.launch("image/*") },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .size(32.dp)
                                .align(Alignment.BottomEnd),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.PhotoCamera,
                                    contentDescription = "Change photo",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }
                    if (state.photo != null) {
                        androidx.compose.material3.TextButton(onClick = viewModel::removePhoto) {
                            Text("Remove photo")
                        }
                    }
                }
            }

            // --- Name ------------------------------------------------------------------
            EditorSection(title = "Name") {
                EditorField(
                    value = state.draft.fullName,
                    onValueChange = viewModel::setFullName,
                    label = "Full name",
                    leadingIcon = Icons.Rounded.Person,
                )
                if (state.showAllFields) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        EditorField(
                            value = state.draft.name.given.orEmpty(),
                            onValueChange = {
                                viewModel.setName(state.draft.name.copy(given = it))
                            },
                            label = "First",
                            modifier = Modifier.weight(1f),
                        )
                        EditorField(
                            value = state.draft.name.family.orEmpty(),
                            onValueChange = {
                                viewModel.setName(state.draft.name.copy(family = it))
                            },
                            label = "Last",
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        EditorField(
                            value = state.draft.name.prefix.orEmpty(),
                            onValueChange = {
                                viewModel.setName(state.draft.name.copy(prefix = it))
                            },
                            label = "Prefix",
                            modifier = Modifier.weight(1f),
                        )
                        EditorField(
                            value = state.draft.name.middle.orEmpty(),
                            onValueChange = {
                                viewModel.setName(state.draft.name.copy(middle = it))
                            },
                            label = "Middle",
                            modifier = Modifier.weight(1f),
                        )
                        EditorField(
                            value = state.draft.name.suffix.orEmpty(),
                            onValueChange = {
                                viewModel.setName(state.draft.name.copy(suffix = it))
                            },
                            label = "Suffix",
                            modifier = Modifier.weight(1f),
                        )
                    }
                    EditorField(
                        value = state.draft.nickname.orEmpty(),
                        onValueChange = viewModel::setNickname,
                        label = "Nickname",
                    )
                }
            }

            // --- Phone -----------------------------------------------------------------
            EditorSection(title = "Phone") {
                state.draft.phones.forEachIndexed { index, phone ->
                    TypedValueRow(
                        value = phone.number,
                        onValueChange = { viewModel.setPhone(index, phone.copy(number = it)) },
                        label = "Number",
                        kind = phone.kind,
                        kinds = PhoneKind.entries,
                        kindLabel = { it.prettyName() },
                        onKindChange = { viewModel.setPhone(index, phone.copy(kind = it)) },
                        onRemove = { viewModel.removePhone(index) },
                        keyboardType = KeyboardType.Phone,
                    )
                }
                AddFieldButton("Add phone", viewModel::addPhone)
            }

            // --- Email -----------------------------------------------------------------
            EditorSection(title = "Email") {
                state.draft.emails.forEachIndexed { index, email ->
                    TypedValueRow(
                        value = email.address,
                        onValueChange = { viewModel.setEmail(index, email.copy(address = it)) },
                        label = "Address",
                        kind = email.kind,
                        kinds = EmailKind.entries,
                        kindLabel = { it.prettyName() },
                        onKindChange = { viewModel.setEmail(index, email.copy(kind = it)) },
                        onRemove = { viewModel.removeEmail(index) },
                        keyboardType = KeyboardType.Email,
                    )
                }
                AddFieldButton("Add email", viewModel::addEmail)
            }

            // --- Work ------------------------------------------------------------------
            EditorSection(title = "Work") {
                EditorField(
                    value = state.draft.organization.orEmpty(),
                    onValueChange = viewModel::setOrganization,
                    label = "Company",
                    leadingIcon = Icons.Rounded.Business,
                )
                EditorField(
                    value = state.draft.jobTitle.orEmpty(),
                    onValueChange = viewModel::setJobTitle,
                    label = "Job title",
                )
                if (state.showAllFields) {
                    EditorField(
                        value = state.draft.organizationUnit.orEmpty(),
                        onValueChange = viewModel::setOrganizationUnit,
                        label = "Department",
                    )
                    EditorField(
                        value = state.draft.role.orEmpty(),
                        onValueChange = viewModel::setRole,
                        label = "Role",
                    )
                }
            }

            if (state.showAllFields) {
                // --- Addresses ---------------------------------------------------------
                EditorSection(title = "Address") {
                    state.draft.addresses.forEachIndexed { index, address ->
                        AddressEditor(
                            address = address,
                            onChange = { viewModel.setAddress(index, it) },
                            onRemove = { viewModel.removeAddress(index) },
                        )
                    }
                    AddFieldButton("Add address", viewModel::addAddress)
                }

                // --- Links -------------------------------------------------------------
                EditorSection(title = "Websites and WebID") {
                    state.draft.links.forEachIndexed { index, link ->
                        TypedValueRow(
                            value = link.url,
                            onValueChange = { viewModel.setLink(index, link.copy(url = it)) },
                            label = "URL",
                            kind = link.kind,
                            kinds = LinkKind.entries,
                            kindLabel = { it.prettyName() },
                            onKindChange = { viewModel.setLink(index, link.copy(kind = it)) },
                            onRemove = { viewModel.removeLink(index) },
                            keyboardType = KeyboardType.Uri,
                        )
                    }
                    AddFieldButton("Add link", viewModel::addLink)
                }

                // --- Instant messaging -------------------------------------------------
                EditorSection(title = "Instant messaging") {
                    state.draft.impps.forEachIndexed { index, im ->
                        TypedValueRow(
                            value = im.handle,
                            onValueChange = { viewModel.setIm(index, im.copy(handle = it)) },
                            label = "Handle",
                            kind = im.kind,
                            kinds = ImKind.entries,
                            kindLabel = { it.prettyName() },
                            onKindChange = { viewModel.setIm(index, im.copy(kind = it)) },
                            onRemove = { viewModel.removeIm(index) },
                        )
                    }
                    AddFieldButton("Add handle", viewModel::addIm)
                }

                // --- Dates and extras --------------------------------------------------
                EditorSection(title = "Dates") {
                    EditorField(
                        value = state.draft.birthday.orEmpty(),
                        onValueChange = viewModel::setBirthday,
                        label = "Birthday",
                        leadingIcon = Icons.Rounded.Cake,
                        supportingText = "YYYY-MM-DD",
                    )
                    EditorField(
                        value = state.draft.anniversary.orEmpty(),
                        onValueChange = viewModel::setAnniversary,
                        label = "Anniversary",
                        leadingIcon = Icons.Rounded.Favorite,
                        supportingText = "YYYY-MM-DD",
                    )
                }

                EditorSection(title = "More") {
                    EditorField(
                        value = state.draft.categories
                            .filterNot { it.equals("Favorites", ignoreCase = true) }
                            .joinToString(", "),
                        onValueChange = viewModel::setCategories,
                        label = "Labels",
                        supportingText = "Separate with commas",
                    )
                    EditorField(
                        value = state.draft.languages.joinToString(", "),
                        onValueChange = viewModel::setLanguages,
                        label = "Languages",
                        leadingIcon = Icons.Rounded.Translate,
                        supportingText = "Language tags such as en, nl, fa-IR",
                    )
                    EditorField(
                        value = state.draft.note.orEmpty(),
                        onValueChange = viewModel::setNote,
                        label = "Note",
                        leadingIcon = Icons.AutoMirrored.Rounded.Note,
                        singleLine = false,
                    )
                }
            }

            // --- Address book ------------------------------------------------------------
            if (state.books.size > 1) {
                EditorSection(title = "Address book") {
                    BookPicker(
                        books = state.books,
                        selectedId = state.bookId,
                        onSelect = viewModel::setBook,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                FilterChip(
                    selected = state.showAllFields,
                    onClick = viewModel::toggleAllFields,
                    label = {
                        Text(if (state.showAllFields) "Fewer fields" else "More fields")
                    },
                )
            }
            Spacer(Modifier.height(48.dp))
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
            value = selected?.title.orEmpty(),
            onValueChange = { },
            readOnly = true,
            label = { Text("Save in") },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddressEditor(
    address: com.pondersource.solidcontacts.domain.model.PostalAddress,
    onChange: (com.pondersource.solidcontacts.domain.model.PostalAddress) -> Unit,
    onRemove: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.weight(1f),
                ) {
                    OutlinedTextField(
                        value = address.kind.prettyName(),
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Type") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        AddressKind.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.prettyName()) },
                                onClick = {
                                    onChange(address.copy(kind = option))
                                    expanded = false
                                },
                            )
                        }
                    }
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Remove address",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            EditorField(
                value = address.street.orEmpty(),
                onValueChange = { onChange(address.copy(street = it)) },
                label = "Street",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                EditorField(
                    value = address.postalCode.orEmpty(),
                    onValueChange = { onChange(address.copy(postalCode = it)) },
                    label = "Postcode",
                    modifier = Modifier.weight(1f),
                )
                EditorField(
                    value = address.locality.orEmpty(),
                    onValueChange = { onChange(address.copy(locality = it)) },
                    label = "City",
                    modifier = Modifier.weight(1.4f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                EditorField(
                    value = address.region.orEmpty(),
                    onValueChange = { onChange(address.copy(region = it)) },
                    label = "Region",
                    modifier = Modifier.weight(1f),
                )
                EditorField(
                    value = address.country.orEmpty(),
                    onValueChange = { onChange(address.copy(country = it)) },
                    label = "Country",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// --- Labels for the enum values the pickers show -----------------------------------------

private fun PhoneKind.prettyName(): String = when (this) {
    PhoneKind.CELL -> "Mobile"
    PhoneKind.TEXT_PHONE -> "Text phone"
    else -> name.lowercase().replaceFirstChar(Char::uppercase)
}

private fun EmailKind.prettyName(): String = name.lowercase().replaceFirstChar(Char::uppercase)

private fun AddressKind.prettyName(): String = name.lowercase().replaceFirstChar(Char::uppercase)

private fun ImKind.prettyName(): String = name.lowercase().replaceFirstChar(Char::uppercase)

private fun LinkKind.prettyName(): String = when (this) {
    LinkKind.WEB_ID -> "WebID"
    LinkKind.PUBLIC_ID -> "Public ID"
    LinkKind.HOMEPAGE -> "Website"
    LinkKind.HOME -> "Personal"
    LinkKind.WORK -> "Work"
}

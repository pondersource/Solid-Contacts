package com.pondersource.solidcontacts.ui.screens.contactdetail

import android.content.Intent
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.automirrored.rounded.Note
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pondersource.solidcontacts.domain.model.ContactDetail
import com.pondersource.solidcontacts.domain.model.LinkKind
import com.pondersource.solidcontacts.ui.components.ConfirmDialog
import com.pondersource.solidcontacts.ui.components.ContactAvatar
import com.pondersource.solidcontacts.ui.components.LoadingState
import com.pondersource.solidcontacts.ui.theme.StatusColors

/**
 * Everything about one contact.
 *
 * The top is who they are and the four things a user most often wants to do with them; below
 * that, every field the pod holds, grouped and each one actionable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    viewModel: ContactDetailViewModel,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onOpenGroup: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showDelete by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showMoveSheet by remember { mutableStateOf(false) }

    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            val bytes = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }.getOrNull()
            val type = context.contentResolver.getType(uri) ?: "image/jpeg"
            if (bytes != null) viewModel.setPhoto(bytes, type)
        }
    }

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
            CenterAlignedTopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val contact = state.contact
                    IconButton(onClick = viewModel::toggleFavorite, enabled = contact != null) {
                        Icon(
                            imageVector = if (contact?.isFavorite == true) {
                                Icons.Rounded.Star
                            } else {
                                Icons.Rounded.StarBorder
                            },
                            contentDescription = "Favourite",
                            tint = if (contact?.isFavorite == true) {
                                StatusColors.favorite
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    IconButton(
                        onClick = { contact?.let { onEdit(it.id) } },
                        enabled = contact != null,
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Share as vCard") },
                                leadingIcon = { Icon(Icons.Rounded.Share, null) },
                                onClick = {
                                    showMenu = false
                                    viewModel.asVCard()?.let { card ->
                                        context.shareVCard(card, contact?.displayName().orEmpty())
                                    }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Change photo") },
                                leadingIcon = { Icon(Icons.Rounded.PhotoCamera, null) },
                                onClick = {
                                    showMenu = false
                                    pickPhoto.launch("image/*")
                                },
                            )
                            if (state.photo != null) {
                                DropdownMenuItem(
                                    text = { Text("Remove photo") },
                                    leadingIcon = { Icon(Icons.Rounded.PersonRemove, null) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.removePhoto()
                                    },
                                )
                            }
                            if (state.books.size > 1) {
                                DropdownMenuItem(
                                    text = { Text("Move to address book") },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.LibraryBooks, null)
                                    },
                                    onClick = {
                                        showMenu = false
                                        showMoveSheet = true
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    Text("Delete", color = MaterialTheme.colorScheme.error)
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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        val contact = state.contact
        when {
            state.loading -> LoadingState(modifier = Modifier.padding(padding))
            contact == null -> LoadingState(
                message = "This contact is no longer here.",
                modifier = Modifier.padding(padding),
            )

            else -> ContactDetailBody(
                contact = contact,
                photo = state.photo,
                bookTitle = state.book?.title,
                groups = state.groups,
                onOpenGroup = onOpenGroup,
                onRemoveFromGroup = viewModel::removeFromGroup,
                onEditPhoto = { pickPhoto.launch("image/*") },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        }
    }

    if (showDelete && state.contact != null) {
        ConfirmDialog(
            title = "Delete contact",
            message = "Delete ${state.contact?.displayName()} from your pod?",
            confirmLabel = "Delete",
            destructive = true,
            icon = Icons.Rounded.Delete,
            onConfirm = viewModel::delete,
            onDismiss = { showDelete = false },
        )
    }

    if (showMoveSheet) {
        MoveToBookSheet(
            books = state.books.filter { it.id != state.contact?.bookId },
            onPick = { bookId ->
                showMoveSheet = false
                viewModel.moveTo(bookId)
            },
            onDismiss = { showMoveSheet = false },
        )
    }
}

@Composable
private fun ContactDetailBody(
    contact: ContactDetail,
    photo: ByteArray?,
    bookTitle: String?,
    groups: List<com.pondersource.solidcontacts.domain.model.ContactGroup>,
    onOpenGroup: (String) -> Unit,
    onRemoveFromGroup: (String) -> Unit,
    onEditPhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ContactAvatar(
            name = contact.displayName(),
            photo = photo,
            size = 120.dp,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = contact.displayName().ifBlank { "Unnamed" },
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        val subtitle = listOfNotNull(
            contact.jobTitle?.takeIf { it.isNotBlank() },
            contact.organization?.takeIf { it.isNotBlank() },
        ).joinToString(" · ")
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (contact.nickname?.isNotBlank() == true) {
            Text(
                text = "\"${contact.nickname}\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(24.dp))

        val phone = contact.phones.firstOrNull()?.number
        val email = contact.emails.firstOrNull()?.address
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            QuickAction(
                icon = Icons.Rounded.Call,
                label = "Call",
                enabled = phone != null,
                onClick = { phone?.let { context.dial(it) } },
            )
            QuickAction(
                icon = Icons.AutoMirrored.Rounded.Chat,
                label = "Message",
                enabled = phone != null,
                onClick = { phone?.let { context.message(it) } },
            )
            QuickAction(
                icon = Icons.Rounded.Email,
                label = "Email",
                enabled = email != null,
                onClick = { email?.let { context.mail(it) } },
            )
            QuickAction(
                icon = Icons.Rounded.PhotoCamera,
                label = "Photo",
                enabled = true,
                onClick = onEditPhoto,
            )
        }

        if (contact.phones.isNotEmpty()) {
            DetailSection(title = "Phone") {
                contact.phones.forEach { phoneNumber ->
                    DetailRow(
                        icon = Icons.Rounded.Phone,
                        label = phoneNumber.kind.name.lowercase().replace('_', ' '),
                        value = phoneNumber.number,
                        onClick = { context.dial(phoneNumber.number) },
                        trailingIcon = Icons.AutoMirrored.Rounded.Chat,
                        trailingDescription = "Send a message",
                        onTrailingClick = { context.message(phoneNumber.number) },
                    )
                }
            }
        }

        if (contact.emails.isNotEmpty()) {
            DetailSection(title = "Email") {
                contact.emails.forEach { emailAddress ->
                    DetailRow(
                        icon = Icons.Rounded.Email,
                        label = emailAddress.kind.name.lowercase(),
                        value = emailAddress.address,
                        onClick = { context.mail(emailAddress.address) },
                    )
                }
            }
        }

        if (contact.addresses.isNotEmpty()) {
            DetailSection(title = "Address") {
                contact.addresses.forEach { address ->
                    DetailRow(
                        icon = Icons.Rounded.LocationOn,
                        label = address.kind.name.lowercase(),
                        value = address.formatted(),
                        onClick = { context.map(address.formatted()) },
                    )
                }
            }
        }

        if (contact.links.isNotEmpty() || contact.impps.isNotEmpty()) {
            DetailSection(title = "Online") {
                contact.links.forEach { link ->
                    DetailRow(
                        icon = if (link.kind == LinkKind.WEB_ID) {
                            Icons.Rounded.Badge
                        } else {
                            Icons.Rounded.Link
                        },
                        label = when (link.kind) {
                            LinkKind.WEB_ID -> "WebID"
                            LinkKind.PUBLIC_ID -> "public ID"
                            LinkKind.HOMEPAGE -> "website"
                            LinkKind.HOME -> "personal site"
                            LinkKind.WORK -> "work site"
                        },
                        value = link.url,
                        onClick = { context.open(link.url) },
                    )
                }
                contact.impps.forEach { im ->
                    DetailRow(
                        icon = Icons.Rounded.AlternateEmail,
                        label = "instant message · ${im.kind.name.lowercase()}",
                        value = im.handle,
                    )
                }
            }
        }

        val hasWork = !contact.organization.isNullOrBlank() ||
            !contact.organizationUnit.isNullOrBlank() ||
            !contact.role.isNullOrBlank()
        if (hasWork) {
            DetailSection(title = "Work") {
                contact.organization?.takeIf { it.isNotBlank() }?.let {
                    DetailRow(icon = Icons.Rounded.Work, label = "company", value = it)
                }
                contact.organizationUnit?.takeIf { it.isNotBlank() }?.let {
                    DetailRow(icon = Icons.Rounded.Work, label = "department", value = it)
                }
                contact.role?.takeIf { it.isNotBlank() }?.let {
                    DetailRow(icon = Icons.Rounded.Badge, label = "role", value = it)
                }
            }
        }

        val hasDates = !contact.birthday.isNullOrBlank() || !contact.anniversary.isNullOrBlank()
        if (hasDates) {
            DetailSection(title = "Dates") {
                contact.birthday?.takeIf { it.isNotBlank() }?.let {
                    DetailRow(icon = Icons.Rounded.Cake, label = "birthday", value = it)
                }
                contact.anniversary?.takeIf { it.isNotBlank() }?.let {
                    DetailRow(icon = Icons.Rounded.Favorite, label = "anniversary", value = it)
                }
            }
        }

        if (groups.isNotEmpty()) {
            DetailSection(title = "Groups") {
                groups.forEach { group ->
                    DetailRow(
                        icon = Icons.Rounded.Group,
                        label = "group",
                        value = group.name,
                        onClick = { onOpenGroup(group.id) },
                        trailingIcon = Icons.Rounded.PersonRemove,
                        trailingDescription = "Remove from this group",
                        onTrailingClick = { onRemoveFromGroup(group.id) },
                    )
                }
            }
        }

        val extras = contact.categories.filterNot { it.equals("Favorites", ignoreCase = true) }
        if (extras.isNotEmpty() || contact.languages.isNotEmpty() ||
            !contact.note.isNullOrBlank()
        ) {
            DetailSection(title = "More") {
                contact.note?.takeIf { it.isNotBlank() }?.let {
                    DetailRow(
                        icon = Icons.AutoMirrored.Rounded.Note,
                        label = "note",
                        value = it,
                    )
                }
                if (extras.isNotEmpty()) {
                    DetailRow(
                        icon = Icons.AutoMirrored.Rounded.Label,
                        label = "labels",
                        value = extras.joinToString(", "),
                    )
                }
                contact.languages.takeIf { it.isNotEmpty() }?.let { languages ->
                    DetailRow(
                        icon = Icons.Rounded.Translate,
                        label = "languages",
                        value = languages.joinToString(", "),
                    )
                }
                contact.geos.forEach { geo ->
                    DetailRow(
                        icon = Icons.Rounded.Language,
                        label = "position",
                        value = geo,
                        onClick = { context.open(geo) },
                    )
                }
            }
        }

        if (bookTitle != null) {
            Spacer(Modifier.height(20.dp))
            AssistChip(
                onClick = { },
                label = { Text(bookTitle) },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.LibraryBooks,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}

/** Offers the other address books this contact could live in. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoveToBookSheet(
    books: List<com.pondersource.solidcontacts.domain.model.AddressBookSummary>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = "Move to",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 24.dp, bottom = 8.dp),
        )
        books.forEach { book ->
            DetailRow(
                icon = Icons.Rounded.LibraryBooks,
                label = com.pondersource.solidcontacts.ui.components.countLabel(
                    book.contactCount,
                    "contact",
                    "contacts",
                ),
                value = book.title,
                onClick = { onPick(book.id) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

// --- The intents a contact row fires -----------------------------------------------------

private fun android.content.Context.dial(number: String) =
    safeStart(Intent(Intent.ACTION_DIAL, "tel:$number".toUri()))

private fun android.content.Context.message(number: String) =
    safeStart(Intent(Intent.ACTION_SENDTO, "smsto:$number".toUri()))

private fun android.content.Context.mail(address: String) =
    safeStart(Intent(Intent.ACTION_SENDTO, "mailto:$address".toUri()))

private fun android.content.Context.map(query: String) =
    safeStart(Intent(Intent.ACTION_VIEW, "geo:0,0?q=${android.net.Uri.encode(query)}".toUri()))

private fun android.content.Context.open(url: String) {
    val normalized = if (url.contains("://") || url.startsWith("geo:")) url else "https://$url"
    safeStart(Intent(Intent.ACTION_VIEW, normalized.toUri()))
}

private fun android.content.Context.shareVCard(card: String, name: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/vcard"
        putExtra(Intent.EXTRA_TEXT, card)
        putExtra(Intent.EXTRA_TITLE, name)
    }
    safeStart(Intent.createChooser(intent, "Share $name"))
}

/** No app handles every scheme; a missing handler should do nothing, not crash the screen. */
private fun android.content.Context.safeStart(intent: Intent) {
    runCatching { startActivity(intent) }
}

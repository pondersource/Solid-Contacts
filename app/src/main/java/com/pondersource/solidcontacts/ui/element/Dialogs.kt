package com.pondersource.solidcontacts.ui.element

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

@Composable
fun DeleteDialog(
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit,
    type: DeleteElementType
) {

    val title = remember {
        when(type) {
            DeleteElementType.ADDRESS_BOOK -> "Delete Address Book"
            DeleteElementType.CONTACT -> "Delete Contact"
            DeleteElementType.GROUP -> "Delete Group"
        }
    }

    val message = remember {
        when(type) {
            DeleteElementType.ADDRESS_BOOK -> "Are you sure you want to delete this address book?"
            DeleteElementType.CONTACT -> "Are you sure you want to delete this contact?"
            DeleteElementType.GROUP -> "Are you sure you want to delete this group?"
        }
    }

    AlertDialog(
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton({
                onDismissRequest()
                onDelete()
            }) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton({
                onDismissRequest()
            }) {
                Text("Dismiss")
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = Color.Red
            )
        },
        title = {
            Text(title)
        },
        text = {
            Text(message)
        }
    )
}

enum class DeleteElementType {
    ADDRESS_BOOK,
    CONTACT,
    GROUP,
}
package com.pondersource.solidcontacts.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** What the app is doing about the pod right now, told in one line or not at all. */
sealed interface SyncStatus {
    /** Everything the user did is on the pod. Nothing is shown. */
    data object InSync : SyncStatus

    /** No network. Writes are kept and sent later. */
    data object Offline : SyncStatus

    /** Writes are queued and waiting for a turn. */
    data class Pending(val count: Int) : SyncStatus
}

/**
 * The one-line status strip above a list.
 *
 * It appears only when there is something to say, and it never blocks the content behind it:
 * the app is fully usable offline, so this reports rather than warns.
 */
@Composable
fun SyncBanner(
    status: SyncStatus,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    AnimatedVisibility(
        visible = status != SyncStatus.InSync,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier,
    ) {
        val (container, content, icon, message) = when (status) {
            SyncStatus.Offline -> BannerLook(
                MaterialTheme.colorScheme.surfaceContainerHighest,
                MaterialTheme.colorScheme.onSurfaceVariant,
                Icons.Rounded.CloudOff,
                "Offline. Your changes are saved here and sync later.",
            )

            is SyncStatus.Pending -> BannerLook(
                MaterialTheme.colorScheme.tertiaryContainer,
                MaterialTheme.colorScheme.onTertiaryContainer,
                Icons.Rounded.CloudSync,
                if (status.count == 1) {
                    "1 change waiting to reach your pod"
                } else {
                    "${status.count} changes waiting to reach your pod"
                },
            )

            SyncStatus.InSync -> BannerLook(
                Color.Transparent,
                Color.Transparent,
                Icons.Rounded.CloudSync,
                "",
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(container)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = content,
                modifier = Modifier.weight(1f),
            )
            if (onRetry != null && status is SyncStatus.Pending) {
                TextButton(onClick = onRetry) {
                    Text("Retry", style = MaterialTheme.typography.labelMedium, color = content)
                }
            }
        }
    }
}

private data class BannerLook(
    val container: Color,
    val content: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val message: String,
)

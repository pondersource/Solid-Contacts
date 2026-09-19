package com.pondersource.solidcontacts.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pondersource.solidcontacts.ui.theme.AvatarGradients
import kotlin.math.absoluteValue

/**
 * A contact's face: their photo when we hold one, otherwise their initials on a gradient chosen
 * from the name, so the same person always looks the same.
 */
@Composable
fun ContactAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    photo: ByteArray? = null,
) {
    val initials = remember(name) { initialsOf(name) }
    val gradient = remember(name) { gradientFor(name) }

    val bitmap = rememberPhotoBitmap(photo)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(gradient.first, gradient.second))),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.size(size),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                text = initials,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = (size.value * 0.38f).sp,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

/** Decodes photo bytes off the composition, and forgets them when the bytes change. */
@Composable
fun rememberPhotoBitmap(photo: ByteArray?): ImageBitmap? {
    var bitmap by remember(photo) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(photo) {
        bitmap = photo
            ?.let { bytes -> runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }.getOrNull() }
            ?.asImageBitmap()
    }
    return bitmap
}

/** One or two letters: the first letter of the first and the last word of the name. */
fun initialsOf(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        words.isEmpty() -> "?"
        words.size == 1 -> words.first().take(1).uppercase()
        else -> (words.first().take(1) + words.last().take(1)).uppercase()
    }
}

/** A stable colour per name, so a contact does not change colour between screens. */
fun gradientFor(name: String): Pair<Color, Color> =
    AvatarGradients[(name.hashCode().absoluteValue) % AvatarGradients.size]

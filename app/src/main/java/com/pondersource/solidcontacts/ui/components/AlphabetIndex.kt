package com.pondersource.solidcontacts.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The letter rail down the right edge of a long contact list.
 *
 * Touching or dragging it jumps to that letter, which is how a phone book with hundreds of
 * entries stays navigable without scrolling. Letters with no contacts are dimmed rather than
 * hidden, so the rail keeps a stable shape under the finger.
 */
@Composable
fun AlphabetIndex(
    letters: List<String>,
    activeLetters: Set<String>,
    onLetterSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (letters.isEmpty()) return

    val haptics = LocalHapticFeedback.current
    var railHeight by remember { mutableFloatStateOf(0f) }
    var lastLetter by remember { mutableStateOf<String?>(null) }

    fun letterAt(y: Float): String? {
        if (railHeight <= 0f) return null
        val index = ((y / railHeight) * letters.size).toInt().coerceIn(0, letters.lastIndex)
        return letters[index]
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(28.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f))
            .padding(vertical = 8.dp)
            .pointerInput(letters) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        letterAt(offset.y)?.let { letter ->
                            if (letter != lastLetter) {
                                lastLetter = letter
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onLetterSelected(letter)
                            }
                        }
                    },
                    onDragEnd = { lastLetter = null },
                    onDragCancel = { lastLetter = null },
                ) { change, _ ->
                    letterAt(change.position.y)?.let { letter ->
                        if (letter != lastLetter) {
                            lastLetter = letter
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onLetterSelected(letter)
                        }
                    }
                }
            }
            .pointerInput(letters) {
                detectTapGestures { offset -> letterAt(offset.y)?.let(onLetterSelected) }
            }
            .onSizeChanged { railHeight = it.height.toFloat() },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEach { letter ->
            val active = letter in activeLetters
            Text(
                text = letter,
                fontSize = 10.sp,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                color = if (active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                },
            )
        }
    }
}

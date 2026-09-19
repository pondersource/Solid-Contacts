package com.pondersource.solidcontacts.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * The app's palette: a deep indigo-violet for Solid itself, a calm teal for the things the user
 * does, and a warm amber for the things that need noticing.
 *
 * The tones follow the Material 3 tonal ladder, so the light and the dark scheme are the same
 * colours read from opposite ends.
 */

// Indigo-violet - the app's own colour.
private val Violet10 = Color(0xFF21005D)
private val Violet20 = Color(0xFF381E72)
private val Violet30 = Color(0xFF4F378B)
private val Violet40 = Color(0xFF6750A4)
private val Violet80 = Color(0xFFD0BCFF)
private val Violet90 = Color(0xFFEADDFF)

// Teal - actions and accents.
private val Teal10 = Color(0xFF00201A)
private val Teal20 = Color(0xFF00382F)
private val Teal30 = Color(0xFF005046)
private val Teal40 = Color(0xFF00695C)
private val Teal80 = Color(0xFF6FDBC7)
private val Teal90 = Color(0xFF8FF8E2)

// Amber - highlights, favourites, sync attention.
private val Amber10 = Color(0xFF2A1800)
private val Amber20 = Color(0xFF452B00)
private val Amber30 = Color(0xFF633F00)
private val Amber40 = Color(0xFF855400)
private val Amber80 = Color(0xFFFFB951)
private val Amber90 = Color(0xFFFFDDB3)

private val Red10 = Color(0xFF410002)
private val Red20 = Color(0xFF690005)
private val Red30 = Color(0xFF93000A)
private val Red40 = Color(0xFFBA1A1A)
private val Red80 = Color(0xFFFFB4AB)
private val Red90 = Color(0xFFFFDAD6)

private val Neutral6 = Color(0xFF131316)
private val Neutral10 = Color(0xFF1C1B1F)
private val Neutral12 = Color(0xFF201F23)
private val Neutral17 = Color(0xFF2B2930)
private val Neutral20 = Color(0xFF322F35)
private val Neutral22 = Color(0xFF36343B)
private val Neutral24 = Color(0xFF3B383E)
private val Neutral30 = Color(0xFF49454F)
private val Neutral50 = Color(0xFF79747E)
private val Neutral60 = Color(0xFF938F99)
private val Neutral80 = Color(0xFFCAC4D0)
private val Neutral87 = Color(0xFFDED8E1)
private val Neutral90 = Color(0xFFE6E0E9)
private val Neutral92 = Color(0xFFECE6F0)
private val Neutral94 = Color(0xFFF3EDF7)
private val Neutral95 = Color(0xFFF5EFF7)
private val Neutral96 = Color(0xFFF7F2FA)
private val Neutral98 = Color(0xFFFEF7FF)
private val Neutral100 = Color(0xFFFFFFFF)

val LightColors = lightColorScheme(
    primary = Violet40,
    onPrimary = Neutral100,
    primaryContainer = Violet90,
    onPrimaryContainer = Violet10,
    inversePrimary = Violet80,
    secondary = Teal40,
    onSecondary = Neutral100,
    secondaryContainer = Teal90,
    onSecondaryContainer = Teal10,
    tertiary = Amber40,
    onTertiary = Neutral100,
    tertiaryContainer = Amber90,
    onTertiaryContainer = Amber10,
    error = Red40,
    onError = Neutral100,
    errorContainer = Red90,
    onErrorContainer = Red10,
    background = Neutral98,
    onBackground = Neutral10,
    surface = Neutral98,
    onSurface = Neutral10,
    surfaceVariant = Neutral90,
    onSurfaceVariant = Neutral30,
    surfaceTint = Violet40,
    inverseSurface = Neutral20,
    inverseOnSurface = Neutral95,
    outline = Neutral50,
    outlineVariant = Neutral80,
    scrim = Color.Black,
    surfaceBright = Neutral98,
    surfaceDim = Neutral87,
    surfaceContainerLowest = Neutral100,
    surfaceContainerLow = Neutral96,
    surfaceContainer = Neutral94,
    surfaceContainerHigh = Neutral92,
    surfaceContainerHighest = Neutral90,
)

val DarkColors = darkColorScheme(
    primary = Violet80,
    onPrimary = Violet20,
    primaryContainer = Violet30,
    onPrimaryContainer = Violet90,
    inversePrimary = Violet40,
    secondary = Teal80,
    onSecondary = Teal20,
    secondaryContainer = Teal30,
    onSecondaryContainer = Teal90,
    tertiary = Amber80,
    onTertiary = Amber20,
    tertiaryContainer = Amber30,
    onTertiaryContainer = Amber90,
    error = Red80,
    onError = Red20,
    errorContainer = Red30,
    onErrorContainer = Red90,
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = Neutral30,
    onSurfaceVariant = Neutral80,
    surfaceTint = Violet80,
    inverseSurface = Neutral90,
    inverseOnSurface = Neutral20,
    outline = Neutral60,
    outlineVariant = Neutral30,
    scrim = Color.Black,
    surfaceBright = Neutral24,
    surfaceDim = Neutral6,
    surfaceContainerLowest = Neutral6,
    surfaceContainerLow = Neutral10,
    surfaceContainer = Neutral12,
    surfaceContainerHigh = Neutral17,
    surfaceContainerHighest = Neutral22,
)

/**
 * The six gradients an avatar picks from when a contact has no photo.
 *
 * The choice comes from the name, so the same person keeps the same colour on every screen and
 * a list of contacts reads as a set of stable, distinguishable faces.
 */
val AvatarGradients: List<Pair<Color, Color>> = listOf(
    Color(0xFF7C4DFF) to Color(0xFF536DFE),
    Color(0xFF00BFA5) to Color(0xFF1DE9B6),
    Color(0xFFFF6E40) to Color(0xFFFF9E80),
    Color(0xFF00B0FF) to Color(0xFF40C4FF),
    Color(0xFFEC407A) to Color(0xFFF48FB1),
    Color(0xFFFFB300) to Color(0xFFFFD54F),
)

/** Colours that carry meaning and are the same in both schemes. */
object StatusColors {
    val favorite = Color(0xFFFFC107)
    val online = Color(0xFF2E7D32)
    val pending = Color(0xFFF57C00)
}

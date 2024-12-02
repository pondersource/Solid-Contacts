package com.pondersource.solidcontacts.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun AppTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColors: Boolean = false,
    content: @Composable () -> Unit
) {

    val lightColorScheme = lightColorScheme()
    val darkColorScheme = darkColorScheme()

    val colorScheme  = when {
        dynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDarkTheme) {
                dynamicDarkColorScheme(LocalContext.current)
            }
            else {
                dynamicLightColorScheme(LocalContext.current)
            }
        }
        isDarkTheme -> {
            darkColorScheme
        }
        else -> {
            lightColorScheme
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkTheme
        }
    }

    CompositionLocalProvider {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
package com.pondersource.solidcontacts.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.pondersource.solidcontacts.data.local.prefs.UserPreferences
import com.pondersource.solidcontacts.data.local.prefs.UserSettings
import com.pondersource.solidcontacts.ui.navigation.AppNavHost
import com.pondersource.solidcontacts.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var preferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by preferences.settings.collectAsStateWithLifecycle(UserSettings())
            AppTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColor,
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(navController = rememberNavController())
                }
            }
        }
    }
}

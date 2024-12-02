package com.pondersource.solidcontacts.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.pondersource.solidcontacts.ui.nav.AppNavHost
import com.pondersource.solidcontacts.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity: ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val navController = rememberNavController()
            AppTheme {
                AppNavHost(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding(),
                    navController = navController,
                )
            }
        }
    }
}
package com.pondersource.solidcontacts.ui.screens.login

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.erfangholami.androidsolidservices.client.sdk.AuthorizeWithSolid
import com.erfangholami.androidsolidservices.client.sdk.Solid
import com.erfangholami.androidsolidservices.client.sdk.SolidSignInResult
import com.pondersource.solidcontacts.ui.theme.AvatarGradients

/**
 * The sign-in screen.
 *
 * The app never sees a password or a token: the host app, Solid Share, owns the login and shows
 * the consent screen. All this screen does is say what the app will ask for, and launch it.
 */
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onSignedIn: () -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val authorize = rememberLauncherForActivityResult(
        AuthorizeWithSolid(viewModel.accessRequest),
    ) { result ->
        when (result) {
            is SolidSignInResult.Authorized -> viewModel.onAuthorized(result.webId)
            is SolidSignInResult.Failed -> viewModel.onFailed(result.exception.message)
            SolidSignInResult.Dismissed -> Unit
        }
    }

    LaunchedEffect(state.signedIn) {
        if (state.signedIn) onSignedIn()
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.height(48.dp))
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(AvatarGradients[0].first, AvatarGradients[0].second),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Contacts,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(52.dp),
                )
            }

            Spacer(Modifier.height(28.dp))
            Text(
                text = "Solid Contacts",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Every contact lives in your own pod. You decide who reaches it, and you " +
                    "can take it with you.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(36.dp))
            Feature(
                icon = Icons.Rounded.Lock,
                title = "Yours alone",
                description = "Contacts are stored on your pod, not on anyone's server.",
            )
            Spacer(Modifier.height(16.dp))
            Feature(
                icon = Icons.Rounded.CloudOff,
                title = "Works offline",
                description = "Read and edit with no network. Changes sync when you are back.",
            )
            Spacer(Modifier.height(16.dp))
            Feature(
                icon = Icons.Rounded.Sync,
                title = "One sign-in",
                description = "Solid Share holds your login, so this app never sees a password.",
            )

            Spacer(Modifier.height(40.dp))

            if (state.hostInstalled) {
                Button(
                    onClick = { authorize.launch(Unit) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text("Connect your Solid pod", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Text(
                    text = "Solid Contacts reaches your pod through Solid Share. Install it once, " +
                        "sign in there, and come back.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { context.startActivity(Solid.hostInstallIntent(context)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text("Install Solid Share")
                }
            }
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun Feature(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

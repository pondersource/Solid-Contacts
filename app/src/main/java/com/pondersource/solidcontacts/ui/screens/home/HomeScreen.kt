package com.pondersource.solidcontacts.ui.screens.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pondersource.solidcontacts.ui.navigation.HomeRoute
import com.pondersource.solidcontacts.ui.screens.books.BooksScreen
import com.pondersource.solidcontacts.ui.screens.books.BooksViewModel
import com.pondersource.solidcontacts.ui.screens.contacts.ContactsScreen
import com.pondersource.solidcontacts.ui.screens.contacts.ContactsViewModel
import com.pondersource.solidcontacts.ui.screens.groups.GroupsScreen
import com.pondersource.solidcontacts.ui.screens.groups.GroupsViewModel
import com.pondersource.solidcontacts.ui.screens.settings.SettingsScreen
import com.pondersource.solidcontacts.ui.screens.settings.SettingsViewModel

private data class Tab(
    val label: String,
    val icon: ImageVector,
    val route: Any,
)

/**
 * The tabbed shell.
 *
 * Four places: everyone, the books they live in, the groups they belong to, and the settings.
 * Each tab keeps its own back stack, so switching away and back lands where the user left off.
 */
@Composable
fun HomeScreen(
    onOpenContact: (String) -> Unit,
    onCreateContact: (bookId: String?) -> Unit,
    onOpenBook: (String) -> Unit,
    onOpenGroup: (String) -> Unit,
    onOpenDuplicates: () -> Unit,
    onOpenImportExport: () -> Unit,
    onSignedOut: () -> Unit,
) {
    val navController = rememberNavController()
    val tabs = remember {
        listOf(
            Tab("Contacts", Icons.Rounded.Contacts, HomeRoute.Contacts),
            Tab("Books", Icons.Rounded.LibraryBooks, HomeRoute.Books),
            Tab("Groups", Icons.Rounded.Group, HomeRoute.Groups),
            Tab("Settings", Icons.Rounded.Settings, HomeRoute.Settings),
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val destination = backStackEntry?.destination
                tabs.forEach { tab ->
                    val selected = destination?.hierarchy?.any {
                        it.hasRoute(tab.route::class)
                    } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute.Contacts,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            composable<HomeRoute.Contacts> {
                ContactsScreen(
                    viewModel = hiltViewModel<ContactsViewModel>(),
                    onOpenContact = onOpenContact,
                    onCreateContact = { onCreateContact(null) },
                )
            }
            composable<HomeRoute.Books> {
                BooksScreen(
                    viewModel = hiltViewModel<BooksViewModel>(),
                    onOpenBook = onOpenBook,
                )
            }
            composable<HomeRoute.Groups> {
                GroupsScreen(
                    viewModel = hiltViewModel<GroupsViewModel>(),
                    onOpenGroup = onOpenGroup,
                )
            }
            composable<HomeRoute.Settings> {
                SettingsScreen(
                    viewModel = hiltViewModel<SettingsViewModel>(),
                    onOpenDuplicates = onOpenDuplicates,
                    onOpenImportExport = onOpenImportExport,
                    onSignedOut = onSignedOut,
                )
            }
        }
    }
}

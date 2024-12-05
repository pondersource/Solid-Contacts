package com.pondersource.solidcontacts.ui.main

import com.pondersource.solidcontacts.R
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pondersource.solidcontacts.ui.addressbook.AddressBook
import com.pondersource.solidcontacts.ui.addressbook.AddressBookViewModel
import com.pondersource.solidcontacts.ui.addressbook.AddressBooks
import com.pondersource.solidcontacts.ui.addressbook.AddressBooksViewModel
import com.pondersource.solidcontacts.ui.addressbook.Contact
import com.pondersource.solidcontacts.ui.addressbook.ContactViewModel
import com.pondersource.solidcontacts.ui.addressbook.Group
import com.pondersource.solidcontacts.ui.addressbook.GroupViewModel
import com.pondersource.solidcontacts.ui.nav.AddressBookRoute
import com.pondersource.solidcontacts.ui.nav.AddressBooksRoute
import com.pondersource.solidcontacts.ui.nav.ContactRoute
import com.pondersource.solidcontacts.ui.nav.GroupRoute
import com.pondersource.solidcontacts.ui.nav.MainPage
import com.pondersource.solidcontacts.ui.nav.MainPage.MainNavBottomItem
import com.pondersource.solidcontacts.ui.setting.Settings
import com.pondersource.solidcontacts.ui.setting.SettingsViewModel

@Composable
fun MainPage(
    navController: NavHostController,
    viewModel: MainPageViewModel
) {
    val nestedNavController = rememberNavController()
    val addressBookInnerNavController = rememberNavController()

    val bottomItems = remember {
        listOf (
            MainNavBottomItem<MainPage.Home>(R.string.home, R.drawable.ic_home, MainPage.Home),
            MainNavBottomItem<MainPage.Setting>(R.string.setting, R.drawable.ic_setting, MainPage.Setting),
        )
    }

    val changeTab : (T: Any) -> Unit = { tabRoute ->
        nestedNavController.navigate(tabRoute) {

            popUpTo(nestedNavController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold (
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by nestedNavController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                bottomItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(painterResource(screen.icon), contentDescription = null) },
                        label = { Text(stringResource(screen.title)) },
                        selected = currentDestination?.hierarchy?.any { it.hasRoute(screen.route::class) } == true,
                        onClick = {
                            changeTab(screen.route)
                        }
                    )
                }
            }
        }
    ){ paddingValues ->
        NavHost(
            navController = nestedNavController,
            startDestination = MainPage.Home,
            modifier = Modifier
                .systemBarsPadding()
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            /*AddressBookGraph(
                navController,
                addressBookInnerNavController,
            )*/
            composable<MainPage.Home> {
                NavHost(
                    navController = addressBookInnerNavController,
                    startDestination = AddressBooksRoute,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    composable<AddressBooksRoute> { AddressBooks(navController, addressBookInnerNavController, hiltViewModel<AddressBooksViewModel>()) }
                    composable<AddressBookRoute> { AddressBook(navController, addressBookInnerNavController, hiltViewModel<AddressBookViewModel>()) }
                    composable<ContactRoute> { Contact(addressBookInnerNavController, hiltViewModel<ContactViewModel>()) }
                    composable<GroupRoute> { Group(addressBookInnerNavController, hiltViewModel<GroupViewModel>()) }
                }
            }
            composable<MainPage.Setting> {
                Settings(navController, hiltViewModel<SettingsViewModel>())
            }
        }
    }
}
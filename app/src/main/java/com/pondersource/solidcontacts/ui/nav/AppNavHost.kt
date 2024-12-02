package com.pondersource.solidcontacts.ui.nav

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.pondersource.solidcontacts.ui.addressbook.AddressBook
import com.pondersource.solidcontacts.ui.addressbook.AddressBookViewModel
import com.pondersource.solidcontacts.ui.addressbook.AddressBooks
import com.pondersource.solidcontacts.ui.addressbook.AddressBooksViewModel
import com.pondersource.solidcontacts.ui.addressbook.Contact
import com.pondersource.solidcontacts.ui.addressbook.ContactViewModel
import com.pondersource.solidcontacts.ui.addressbook.Group
import com.pondersource.solidcontacts.ui.addressbook.GroupViewModel
import com.pondersource.solidcontacts.ui.login.Login
import com.pondersource.solidcontacts.ui.login.LoginViewModel
import com.pondersource.solidcontacts.ui.main.MainPage
import com.pondersource.solidcontacts.ui.main.MainPageViewModel
import com.pondersource.solidcontacts.ui.startup.Startup
import com.pondersource.solidcontacts.ui.startup.StartupViewModel
import kotlinx.serialization.Serializable

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
) {
    val context = LocalContext.current
    NavHost (
        modifier = modifier,
        navController = navController,
        startDestination = Startup
    ) {
        composable<Startup> { Startup(navController, hiltViewModel<StartupViewModel>()) }
        composable<Login> { Login(navController, hiltViewModel<LoginViewModel>()) }
        composable<MainPage> { MainPage(navController, hiltViewModel<MainPageViewModel>()) }
    }
}

fun NavGraphBuilder.AddressBookGraph(
    navController: NavController,
    innerNavController: NavController,
) {
    navigation<MainPage.Home>(startDestination = AddressBooksRoute) {
        composable<AddressBooksRoute> { AddressBooks(navController, innerNavController, hiltViewModel<AddressBooksViewModel>()) }
        composable<AddressBookRoute> { AddressBook(innerNavController, hiltViewModel<AddressBookViewModel>()) }
        composable<ContactRoute> { Contact(innerNavController, hiltViewModel<ContactViewModel>()) }
        composable<GroupRoute> { Group(innerNavController, hiltViewModel<GroupViewModel>()) }
    }
}

@Serializable
object Startup

@Serializable
object Login

@Serializable
object MainPage {

    @Serializable
    data class MainNavBottomItem<T: Any>(
        @StringRes val title: Int,
        @DrawableRes val icon: Int,
        val route: T,
    )

    @Serializable
    object Home

    @Serializable
    object Setting
}

@Serializable
object AddressBooksRoute

@Serializable
data class AddressBookRoute(
    val addressBookUri: String,
    val name: String
)

@Serializable
data class ContactRoute(
    val contactUri: String
)

@Serializable
data class GroupRoute (
    val groupUri: String
)
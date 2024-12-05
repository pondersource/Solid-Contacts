package com.pondersource.solidcontacts.ui.nav

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.pondersource.solidcontacts.ui.login.Login
import com.pondersource.solidcontacts.ui.login.LoginViewModel
import com.pondersource.solidcontacts.ui.main.MainPage
import com.pondersource.solidcontacts.ui.main.MainPageViewModel
import com.pondersource.solidcontacts.ui.newcontact.AddNewContact
import com.pondersource.solidcontacts.ui.newcontact.AddNewContactViewModel
import com.pondersource.solidcontacts.ui.newgroup.AddNewGroup
import com.pondersource.solidcontacts.ui.newgroup.AddNewGroupViewModel
import com.pondersource.solidcontacts.ui.startup.Startup
import com.pondersource.solidcontacts.ui.startup.StartupViewModel
import kotlinx.serialization.Serializable

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
) {
    NavHost (
        modifier = modifier,
        navController = navController,
        startDestination = Startup
    ) {
        composable<Startup> { Startup(navController, hiltViewModel<StartupViewModel>()) }
        composable<Login> { Login(navController, hiltViewModel<LoginViewModel>()) }
        composable<MainPage> { MainPage(navController, hiltViewModel<MainPageViewModel>()) }
        composable<AddContactRoute> { AddNewContact(navController, hiltViewModel<AddNewContactViewModel>()) }
        composable<AddGroupRoute> { AddNewGroup(navController, hiltViewModel<AddNewGroupViewModel>()) }
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
data class ContactRoute (
    val addressBookUri: String,
    val contactUri: String
)

@Serializable
data class GroupRoute (
    val addressBookUri: String,
    val groupUri: String
)

@Serializable
data class AddContactRoute(
    val addressBookUri: String
)

@Serializable
data class AddGroupRoute(
    val addressBookUri: String
)
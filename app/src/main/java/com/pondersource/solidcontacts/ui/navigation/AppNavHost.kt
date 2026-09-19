package com.pondersource.solidcontacts.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.pondersource.solidcontacts.ui.screens.books.BookDetailScreen
import com.pondersource.solidcontacts.ui.screens.books.BookDetailViewModel
import com.pondersource.solidcontacts.ui.screens.contactdetail.ContactDetailScreen
import com.pondersource.solidcontacts.ui.screens.contactdetail.ContactDetailViewModel
import com.pondersource.solidcontacts.ui.screens.duplicates.DuplicatesScreen
import com.pondersource.solidcontacts.ui.screens.duplicates.DuplicatesViewModel
import com.pondersource.solidcontacts.ui.screens.editor.ContactEditorScreen
import com.pondersource.solidcontacts.ui.screens.editor.ContactEditorViewModel
import com.pondersource.solidcontacts.ui.screens.groups.GroupDetailScreen
import com.pondersource.solidcontacts.ui.screens.groups.GroupDetailViewModel
import com.pondersource.solidcontacts.ui.screens.home.HomeScreen
import com.pondersource.solidcontacts.ui.screens.importexport.ImportExportScreen
import com.pondersource.solidcontacts.ui.screens.importexport.ImportExportViewModel
import com.pondersource.solidcontacts.ui.screens.login.LoginScreen
import com.pondersource.solidcontacts.ui.screens.login.LoginViewModel
import com.pondersource.solidcontacts.ui.screens.picker.ContactPickerScreen
import com.pondersource.solidcontacts.ui.screens.picker.ContactPickerViewModel
import com.pondersource.solidcontacts.ui.screens.startup.StartupScreen
import com.pondersource.solidcontacts.ui.screens.startup.StartupViewModel

private const val TRANSITION_MILLIS = 260

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = StartupRoute,
        modifier = modifier,
        // A push slides in from the trailing edge and a pop slides back, so the stack has a
        // direction the user can feel.
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                tween(TRANSITION_MILLIS),
            ) + fadeIn(tween(TRANSITION_MILLIS))
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                tween(TRANSITION_MILLIS),
            ) + fadeOut(tween(TRANSITION_MILLIS))
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                tween(TRANSITION_MILLIS),
            ) + fadeIn(tween(TRANSITION_MILLIS))
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                tween(TRANSITION_MILLIS),
            ) + fadeOut(tween(TRANSITION_MILLIS))
        },
    ) {
        composable<StartupRoute>(
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
        ) {
            StartupScreen(
                viewModel = hiltViewModel<StartupViewModel>(),
                onSignedIn = { navController.replaceWith(HomeRoute) },
                onSignedOut = { navController.replaceWith(LoginRoute) },
            )
        }

        composable<LoginRoute>(
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
        ) {
            LoginScreen(
                viewModel = hiltViewModel<LoginViewModel>(),
                onSignedIn = { navController.replaceWith(HomeRoute) },
            )
        }

        composable<HomeRoute>(
            enterTransition = { fadeIn() },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    tween(TRANSITION_MILLIS),
                ) + fadeIn(tween(TRANSITION_MILLIS))
            },
        ) {
            HomeScreen(
                onOpenContact = { navController.navigate(ContactDetailRoute(it)) },
                onCreateContact = { bookId ->
                    navController.navigate(ContactEditorRoute(bookId = bookId))
                },
                onOpenBook = { navController.navigate(BookDetailRoute(it)) },
                onOpenGroup = { navController.navigate(GroupDetailRoute(it)) },
                onOpenDuplicates = { navController.navigate(DuplicatesRoute) },
                onOpenImportExport = { navController.navigate(ImportExportRoute) },
                onSignedOut = { navController.replaceWith(LoginRoute) },
            )
        }

        composable<ContactDetailRoute> {
            ContactDetailScreen(
                viewModel = hiltViewModel<ContactDetailViewModel>(),
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(ContactEditorRoute(contactId = it)) },
                onOpenGroup = { navController.navigate(GroupDetailRoute(it)) },
            )
        }

        composable<ContactEditorRoute> {
            ContactEditorScreen(
                viewModel = hiltViewModel<ContactEditorViewModel>(),
                onClose = { navController.popBackStack() },
            )
        }

        composable<BookDetailRoute> {
            BookDetailScreen(
                viewModel = hiltViewModel<BookDetailViewModel>(),
                onBack = { navController.popBackStack() },
                onOpenContact = { navController.navigate(ContactDetailRoute(it)) },
                onOpenGroup = { navController.navigate(GroupDetailRoute(it)) },
                onCreateContact = { bookId ->
                    navController.navigate(ContactEditorRoute(bookId = bookId))
                },
            )
        }

        composable<GroupDetailRoute> {
            GroupDetailScreen(
                viewModel = hiltViewModel<GroupDetailViewModel>(),
                onBack = { navController.popBackStack() },
                onOpenContact = { navController.navigate(ContactDetailRoute(it)) },
                onAddMembers = { groupId, bookId ->
                    navController.navigate(
                        ContactPickerRoute(
                            bookId = bookId,
                            groupId = groupId,
                            excludeGroupMembers = true,
                        ),
                    )
                },
            )
        }

        composable<ContactPickerRoute> {
            ContactPickerScreen(
                viewModel = hiltViewModel<ContactPickerViewModel>(),
                onClose = { navController.popBackStack() },
            )
        }

        composable<DuplicatesRoute> {
            DuplicatesScreen(
                viewModel = hiltViewModel<DuplicatesViewModel>(),
                onBack = { navController.popBackStack() },
            )
        }

        composable<ImportExportRoute> {
            ImportExportScreen(
                viewModel = hiltViewModel<ImportExportViewModel>(),
                onBack = { navController.popBackStack() },
            )
        }
    }
}

/** Replaces the whole stack, for the moves between signed-out and signed-in. */
private fun NavHostController.replaceWith(route: Any) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

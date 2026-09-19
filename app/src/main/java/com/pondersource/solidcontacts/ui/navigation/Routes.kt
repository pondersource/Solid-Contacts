package com.pondersource.solidcontacts.ui.navigation

import kotlinx.serialization.Serializable

/** Every destination in the app, as a type the navigator checks at compile time. */

@Serializable
object StartupRoute

@Serializable
object LoginRoute

/** The tabbed shell that holds the four main screens. */
@Serializable
object HomeRoute {

    @Serializable
    object Contacts

    @Serializable
    object Books

    @Serializable
    object Groups

    @Serializable
    object Settings
}

@Serializable
data class ContactDetailRoute(val contactId: String)

/** Opens the editor on an existing contact, or on a blank one when [contactId] is null. */
@Serializable
data class ContactEditorRoute(
    val contactId: String? = null,
    val bookId: String? = null,
)

@Serializable
data class BookDetailRoute(val bookId: String)

@Serializable
data class GroupDetailRoute(val groupId: String)

/** Picks contacts, for building a group or adding members to one. */
@Serializable
data class ContactPickerRoute(
    val bookId: String? = null,
    val groupId: String? = null,
    val excludeGroupMembers: Boolean = false,
)

@Serializable
object DuplicatesRoute

@Serializable
object ImportExportRoute

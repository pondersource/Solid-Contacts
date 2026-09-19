package com.pondersource.solidcontacts.data.outbox

import kotlinx.serialization.Serializable

/**
 * The writes this app can replay against a pod.
 *
 * Every op names its subject by *local* id, never by pod URI, because a row created offline has
 * no URI until its own create op runs. The drain resolves the URI at the moment it sends.
 */
enum class OpType {
    CREATE_BOOK,
    RENAME_BOOK,
    DELETE_BOOK,
    CREATE_CONTACT,
    UPDATE_CONTACT,
    DELETE_CONTACT,
    MOVE_CONTACT,
    SET_PHOTO,
    REMOVE_PHOTO,
    CREATE_GROUP,
    DELETE_GROUP,
    ADD_MEMBER,
    REMOVE_MEMBER,
}

/**
 * One op's arguments.
 *
 * A single shape for every op keeps the queue readable and the codec trivial; each op reads only
 * the fields it needs.
 */
@Serializable
data class OutboxPayload(
    val bookId: String? = null,
    val contactId: String? = null,
    val groupId: String? = null,
    val targetBookId: String? = null,
    val title: String? = null,
    /** Set on a delete, where the row may be gone locally by the time the op runs. */
    val uri: String? = null,
    val contentType: String? = null,
) {
    /** What this op is about, used to collapse repeats of the same op on the same row. */
    fun subject(): String = listOfNotNull(bookId, contactId, groupId, targetBookId).joinToString("/")
}

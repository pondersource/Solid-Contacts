package com.pondersource.solidcontacts.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pondersource.solidcontacts.domain.model.SyncState

/**
 * Local identity is the primary key everywhere, and the pod URI is a nullable column beside it.
 *
 * A row created offline has no URI yet; the outbox fills it in when the create lands on the pod.
 * Because nothing references a row by URI, a contact created offline can be edited, put in a
 * group and deleted before it has ever reached the pod.
 */
@Entity(
    tableName = "address_book",
    indices = [Index(value = ["webId"]), Index(value = ["uri"])],
)
data class AddressBookEntity(
    @PrimaryKey val id: String,
    val webId: String,
    val uri: String?,
    val title: String,
    val isPrivate: Boolean,
    val syncState: SyncState = SyncState.SYNCED,
    val updatedAt: Long,
)

@Entity(
    tableName = "contact",
    indices = [
        Index(value = ["webId", "bookId"]),
        Index(value = ["webId", "sortKey"]),
        Index(value = ["uri"]),
    ],
)
data class ContactEntity(
    @PrimaryKey val id: String,
    val webId: String,
    val bookId: String,
    val uri: String?,
    val displayName: String,
    val sortKey: String,
    /** Lower-cased haystack of every searchable field, so search is one LIKE over one column. */
    val searchText: String,
    val organization: String?,
    val primaryPhone: String?,
    val primaryEmail: String?,
    /** The whole [com.pondersource.solidcontacts.domain.model.ContactDetail] as JSON. */
    val detailJson: String,
    val photoUri: String?,
    /**
     * A small copy of the photo, a few kilobytes, so a list of hundreds of rows stays cheap to
     * draw. The full-size image is never stored here: SQLite hands a row through a 2 MB cursor
     * window, and a large photo in the row makes the contact unreadable. It lives in
     * [com.pondersource.solidcontacts.data.local.PhotoStore] instead.
     */
    val photoThumb: ByteArray?,
    val isFavorite: Boolean = false,
    val syncState: SyncState = SyncState.SYNCED,
    val updatedAt: Long,
) {
    // A ByteArray field makes the generated equals() reference-compare, so spell both out.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ContactEntity) return false
        return id == other.id &&
            webId == other.webId &&
            bookId == other.bookId &&
            uri == other.uri &&
            displayName == other.displayName &&
            sortKey == other.sortKey &&
            searchText == other.searchText &&
            organization == other.organization &&
            primaryPhone == other.primaryPhone &&
            primaryEmail == other.primaryEmail &&
            detailJson == other.detailJson &&
            photoUri == other.photoUri &&
            (photoThumb?.contentEquals(other.photoThumb) ?: (other.photoThumb == null)) &&
            isFavorite == other.isFavorite &&
            syncState == other.syncState &&
            updatedAt == other.updatedAt
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + webId.hashCode()
        result = 31 * result + bookId.hashCode()
        result = 31 * result + (uri?.hashCode() ?: 0)
        result = 31 * result + displayName.hashCode()
        result = 31 * result + sortKey.hashCode()
        result = 31 * result + searchText.hashCode()
        result = 31 * result + (organization?.hashCode() ?: 0)
        result = 31 * result + (primaryPhone?.hashCode() ?: 0)
        result = 31 * result + (primaryEmail?.hashCode() ?: 0)
        result = 31 * result + detailJson.hashCode()
        result = 31 * result + (photoUri?.hashCode() ?: 0)
        result = 31 * result + (photoThumb?.contentHashCode() ?: 0)
        result = 31 * result + isFavorite.hashCode()
        result = 31 * result + syncState.hashCode()
        result = 31 * result + updatedAt.hashCode()
        return result
    }
}

@Entity(
    tableName = "contact_group",
    indices = [Index(value = ["webId", "bookId"]), Index(value = ["uri"])],
)
data class GroupEntity(
    @PrimaryKey val id: String,
    val webId: String,
    val bookId: String,
    val uri: String?,
    val name: String,
    val syncState: SyncState = SyncState.SYNCED,
    val updatedAt: Long,
)

/** Membership by local id on both sides, so it survives a contact that has no pod URI yet. */
@Entity(
    tableName = "group_member",
    primaryKeys = ["groupId", "contactId"],
    indices = [Index(value = ["contactId"]), Index(value = ["webId"])],
)
data class GroupMemberEntity(
    val groupId: String,
    val contactId: String,
    val webId: String,
)

/** One queued write, replayed against the pod when the device is next online. */
@Entity(
    tableName = "outbox_op",
    indices = [Index(value = ["webId", "status", "nextRetryAt"])],
)
data class OutboxOpEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val webId: String,
    val type: String,
    val payload: String,
    val status: OpStatus,
    val attempts: Int = 0,
    val nextRetryAt: Long = 0,
    val lastError: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

enum class OpStatus { PENDING, RUNNING, FAILED }

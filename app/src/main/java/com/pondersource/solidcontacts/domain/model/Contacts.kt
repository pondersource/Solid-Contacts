package com.pondersource.solidcontacts.domain.model

import kotlinx.serialization.Serializable

/**
 * How a row stands relative to the pod.
 *
 * Every write lands in the local database first and gets one of the `PENDING_*` states. A
 * background worker drains the outbox and puts the row back to [SYNCED] once the pod agrees.
 */
enum class SyncState {
    SYNCED,
    PENDING_CREATE,
    PENDING_UPDATE,
    PENDING_DELETE,
    FAILED,
}

/**
 * An address book as the list screen shows it.
 *
 * [id] is the local identity and never changes. [uri] is the pod URI, which is `null` until the
 * outbox creates the book on the pod.
 */
data class AddressBookSummary(
    val id: String,
    val uri: String?,
    val title: String,
    val isPrivate: Boolean,
    val contactCount: Int = 0,
    val groupCount: Int = 0,
    val syncState: SyncState = SyncState.SYNCED,
)

/** A contact as a list row shows it: just enough to draw the row and sort it. */
data class ContactSummary(
    val id: String,
    val uri: String?,
    val bookId: String,
    val displayName: String,
    val sortKey: String,
    val organization: String? = null,
    val primaryPhone: String? = null,
    val primaryEmail: String? = null,
    val isFavorite: Boolean = false,
    val photoThumb: ByteArray? = null,
    val syncState: SyncState = SyncState.SYNCED,
) {
    val hasPhoto: Boolean get() = photoThumb != null

    /** The first letter this contact files under, or "#" when it files under none. */
    fun indexLetter(): String {
        val first = sortKey.firstOrNull { it.isLetter() } ?: return "#"
        return first.uppercaseChar().toString()
    }

    // A ByteArray makes the generated equals() compare by reference, which would redraw every
    // row on every database emission. Compare the bytes instead.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ContactSummary) return false
        return id == other.id &&
            uri == other.uri &&
            bookId == other.bookId &&
            displayName == other.displayName &&
            sortKey == other.sortKey &&
            organization == other.organization &&
            primaryPhone == other.primaryPhone &&
            primaryEmail == other.primaryEmail &&
            isFavorite == other.isFavorite &&
            (photoThumb?.contentEquals(other.photoThumb) ?: (other.photoThumb == null)) &&
            syncState == other.syncState
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (uri?.hashCode() ?: 0)
        result = 31 * result + bookId.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + sortKey.hashCode()
        result = 31 * result + (organization?.hashCode() ?: 0)
        result = 31 * result + (primaryPhone?.hashCode() ?: 0)
        result = 31 * result + (primaryEmail?.hashCode() ?: 0)
        result = 31 * result + isFavorite.hashCode()
        result = 31 * result + (photoThumb?.contentHashCode() ?: 0)
        result = 31 * result + syncState.hashCode()
        return result
    }
}

/** A group inside an address book. */
data class ContactGroup(
    val id: String,
    val uri: String?,
    val bookId: String,
    val name: String,
    val memberCount: Int = 0,
    val syncState: SyncState = SyncState.SYNCED,
)

@Serializable
enum class PhoneKind { CELL, HOME, WORK, FAX, PAGER, VOICE, TEXT, VIDEO, TEXT_PHONE, OTHER }

@Serializable
enum class EmailKind { HOME, WORK, OTHER }

@Serializable
enum class AddressKind { HOME, WORK, OTHER }

@Serializable
enum class ImKind { HOME, WORK, OTHER }

@Serializable
enum class LinkKind { HOME, WORK, HOMEPAGE, WEB_ID, PUBLIC_ID }

@Serializable
enum class GenderKind { MALE, FEMALE, OTHER, NONE, UNKNOWN }

@Serializable
data class PhoneNumber(val number: String, val kind: PhoneKind = PhoneKind.CELL)

@Serializable
data class EmailAddress(val address: String, val kind: EmailKind = EmailKind.HOME)

@Serializable
data class ImHandle(val handle: String, val kind: ImKind = ImKind.OTHER)

@Serializable
data class WebLink(val url: String, val kind: LinkKind = LinkKind.HOMEPAGE)

@Serializable
data class PostalAddress(
    val street: String? = null,
    val locality: String? = null,
    val region: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
    val poBox: String? = null,
    val kind: AddressKind = AddressKind.HOME,
) {
    fun isEmpty(): Boolean =
        listOf(street, locality, region, postalCode, country, poBox).all { it.isNullOrBlank() }

    /** The address on one line, in the order a postal label reads. */
    fun formatted(): String = listOfNotNull(
        poBox?.takeIf { it.isNotBlank() },
        street?.takeIf { it.isNotBlank() },
        locality?.takeIf { it.isNotBlank() },
        region?.takeIf { it.isNotBlank() },
        postalCode?.takeIf { it.isNotBlank() },
        country?.takeIf { it.isNotBlank() },
    ).joinToString(", ")
}

/** The structured name parts, kept apart so the editor can show one field for each. */
@Serializable
data class StructuredName(
    val given: String? = null,
    val family: String? = null,
    val middle: String? = null,
    val prefix: String? = null,
    val suffix: String? = null,
) {
    fun isEmpty(): Boolean = listOf(given, family, middle, prefix, suffix).all { it.isNullOrBlank() }

    fun formatted(): String =
        listOfNotNull(prefix, given, middle, family, suffix)
            .filter { it.isNotBlank() }
            .joinToString(" ")
}

/**
 * Everything the app knows about one contact.
 *
 * This is the full vCard 4.0 surface the contacts data module carries, so nothing a pod holds is
 * dropped on a read-edit-write round trip.
 */
@Serializable
data class ContactDetail(
    val id: String,
    val uri: String? = null,
    val bookId: String = "",
    val fullName: String = "",
    val name: StructuredName = StructuredName(),
    val nickname: String? = null,
    val phones: List<PhoneNumber> = emptyList(),
    val emails: List<EmailAddress> = emptyList(),
    val impps: List<ImHandle> = emptyList(),
    val addresses: List<PostalAddress> = emptyList(),
    val links: List<WebLink> = emptyList(),
    val birthday: String? = null,
    val anniversary: String? = null,
    val organization: String? = null,
    val organizationUnit: String? = null,
    val jobTitle: String? = null,
    val role: String? = null,
    val note: String? = null,
    val categories: List<String> = emptyList(),
    val gender: GenderKind? = null,
    val geos: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val uid: String? = null,
    val photoUri: String? = null,
    val modified: Long? = null,
    val isFavorite: Boolean = false,
    val syncState: SyncState = SyncState.SYNCED,
) {
    /** The WebID this contact published, when they have one. */
    val webId: String? get() = links.firstOrNull { it.kind == LinkKind.WEB_ID }?.url

    /** The name to show, falling back through the name parts to any identifier we hold. */
    fun displayName(): String {
        fullName.takeIf { it.isNotBlank() }?.let { return it.trim() }
        name.formatted().takeIf { it.isNotBlank() }?.let { return it }
        nickname?.takeIf { it.isNotBlank() }?.let { return it.trim() }
        emails.firstOrNull { it.address.isNotBlank() }?.let { return it.address.trim() }
        phones.firstOrNull { it.number.isNotBlank() }?.let { return it.number.trim() }
        return ""
    }

    /** Sorts by family name when we have one, so lists read the way a phone book does. */
    fun sortKey(): String {
        val family = name.family?.takeIf { it.isNotBlank() }
        return if (family != null) {
            listOfNotNull(family, name.given?.takeIf { it.isNotBlank() }).joinToString(" ")
        } else {
            displayName()
        }.lowercase()
    }

    fun isEmpty(): Boolean = displayName().isBlank() &&
        phones.isEmpty() && emails.isEmpty() && addresses.isEmpty()
}

/** A group with its members resolved, for the group detail screen. */
data class GroupDetail(
    val group: ContactGroup,
    val members: List<ContactSummary>,
)

/** An address book with its contacts and groups resolved. */
data class AddressBookDetail(
    val book: AddressBookSummary,
    val contacts: List<ContactSummary>,
    val groups: List<ContactGroup>,
)

/** Two or more contacts the duplicate finder believes are the same person. */
data class DuplicateCluster(
    val signature: String,
    val members: List<ContactDetail>,
)

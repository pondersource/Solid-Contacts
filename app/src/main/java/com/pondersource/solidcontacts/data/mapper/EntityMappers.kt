package com.pondersource.solidcontacts.data.mapper

import com.pondersource.solidcontacts.data.local.db.AddressBookEntity
import com.pondersource.solidcontacts.data.local.db.AddressBookWithCounts
import com.pondersource.solidcontacts.data.local.db.ContactEntity
import com.pondersource.solidcontacts.data.local.db.GroupEntity
import com.pondersource.solidcontacts.domain.model.AddressBookSummary
import com.pondersource.solidcontacts.domain.model.ContactDetail
import com.pondersource.solidcontacts.domain.model.ContactGroup
import com.pondersource.solidcontacts.domain.model.ContactSummary
import com.pondersource.solidcontacts.domain.model.SyncState
import kotlinx.serialization.json.Json

/** One JSON codec for the whole cache; tolerant of fields a newer build added. */
internal val cacheJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun AddressBookWithCounts.toDomain(): AddressBookSummary = AddressBookSummary(
    id = id,
    uri = uri,
    title = title,
    isPrivate = isPrivate,
    contactCount = contactCount,
    groupCount = groupCount,
    syncState = syncState,
)

fun AddressBookEntity.toDomain(contactCount: Int = 0, groupCount: Int = 0): AddressBookSummary =
    AddressBookSummary(
        id = id,
        uri = uri,
        title = title,
        isPrivate = isPrivate,
        contactCount = contactCount,
        groupCount = groupCount,
        syncState = syncState,
    )

fun GroupEntity.toDomain(memberCount: Int = 0): ContactGroup = ContactGroup(
    id = id,
    uri = uri,
    bookId = bookId,
    name = name,
    memberCount = memberCount,
    syncState = syncState,
)

fun ContactEntity.toSummary(): ContactSummary = ContactSummary(
    id = id,
    uri = uri,
    bookId = bookId,
    displayName = displayName,
    sortKey = sortKey,
    organization = organization,
    primaryPhone = primaryPhone,
    primaryEmail = primaryEmail,
    isFavorite = isFavorite,
    photoThumb = photoThumb,
    syncState = syncState,
)

/** The full contact, rehydrated from the JSON column with the row's live columns applied on top. */
fun ContactEntity.toDetail(): ContactDetail =
    cacheJson.decodeFromString(ContactDetail.serializer(), detailJson).copy(
        id = id,
        uri = uri,
        bookId = bookId,
        photoUri = photoUri,
        isFavorite = isFavorite,
        syncState = syncState,
    )

/**
 * Flattens a contact into a row.
 *
 * [searchText] gathers every field a user might type, lower-cased, so the list screen can search
 * with a single `LIKE` and still match an email, a company or a note.
 */
fun ContactDetail.toEntity(
    webId: String,
    now: Long,
    photoThumb: ByteArray? = null,
    syncState: SyncState = this.syncState,
): ContactEntity {
    val haystack = buildList {
        add(displayName())
        add(name.formatted())
        nickname?.let(::add)
        addAll(phones.map { it.number })
        addAll(phones.map { it.number.filter(Char::isDigit) })
        addAll(emails.map { it.address })
        addAll(impps.map { it.handle })
        addAll(addresses.map { it.formatted() })
        addAll(links.map { it.url })
        organization?.let(::add)
        organizationUnit?.let(::add)
        jobTitle?.let(::add)
        role?.let(::add)
        note?.let(::add)
        addAll(categories)
    }.filter { it.isNotBlank() }.joinToString(" ").lowercase()

    return ContactEntity(
        id = id,
        webId = webId,
        bookId = bookId,
        uri = uri,
        displayName = displayName(),
        sortKey = sortKey(),
        searchText = haystack,
        organization = organization,
        primaryPhone = phones.firstOrNull()?.number,
        primaryEmail = emails.firstOrNull()?.address,
        detailJson = cacheJson.encodeToString(ContactDetail.serializer(), this),
        photoUri = photoUri,
        photoThumb = photoThumb,
        isFavorite = isFavorite,
        syncState = syncState,
        updatedAt = now,
    )
}

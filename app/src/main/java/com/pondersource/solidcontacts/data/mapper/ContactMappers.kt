package com.pondersource.solidcontacts.data.mapper

import com.erfangholami.androidsolidservices.shared.model.contacts.AddressEntry
import com.erfangholami.androidsolidservices.shared.model.contacts.AddressType
import com.erfangholami.androidsolidservices.shared.model.contacts.ContactData
import com.erfangholami.androidsolidservices.shared.model.contacts.EmailEntry
import com.erfangholami.androidsolidservices.shared.model.contacts.EmailType
import com.erfangholami.androidsolidservices.shared.model.contacts.Gender
import com.erfangholami.androidsolidservices.shared.model.contacts.ImEntry
import com.erfangholami.androidsolidservices.shared.model.contacts.ImType
import com.erfangholami.androidsolidservices.shared.model.contacts.Name
import com.erfangholami.androidsolidservices.shared.model.contacts.PhoneEntry
import com.erfangholami.androidsolidservices.shared.model.contacts.PhoneType
import com.erfangholami.androidsolidservices.shared.model.contacts.SolidContact
import com.erfangholami.androidsolidservices.shared.model.contacts.URLType
import com.erfangholami.androidsolidservices.shared.model.contacts.UrlEntry
import com.pondersource.solidcontacts.domain.model.AddressKind
import com.pondersource.solidcontacts.domain.model.ContactDetail
import com.pondersource.solidcontacts.domain.model.EmailAddress
import com.pondersource.solidcontacts.domain.model.EmailKind
import com.pondersource.solidcontacts.domain.model.GenderKind
import com.pondersource.solidcontacts.domain.model.ImHandle
import com.pondersource.solidcontacts.domain.model.ImKind
import com.pondersource.solidcontacts.domain.model.LinkKind
import com.pondersource.solidcontacts.domain.model.PhoneKind
import com.pondersource.solidcontacts.domain.model.PhoneNumber
import com.pondersource.solidcontacts.domain.model.PostalAddress
import com.pondersource.solidcontacts.domain.model.StructuredName
import com.pondersource.solidcontacts.domain.model.SyncState
import com.pondersource.solidcontacts.domain.model.WebLink

/**
 * Translates between the contacts data module's [ContactData] and the app's [ContactDetail].
 *
 * The two carry the same vCard 4.0 fields, so every mapping here is total: a contact read from
 * the pod and written straight back loses nothing.
 */

fun PhoneType.toDomain(): PhoneKind = when (this) {
    PhoneType.CELL -> PhoneKind.CELL
    PhoneType.HOME -> PhoneKind.HOME
    PhoneType.WORK -> PhoneKind.WORK
    PhoneType.FAX -> PhoneKind.FAX
    PhoneType.PAGER -> PhoneKind.PAGER
    PhoneType.VOICE -> PhoneKind.VOICE
    PhoneType.TEXT -> PhoneKind.TEXT
    PhoneType.VIDEO -> PhoneKind.VIDEO
    PhoneType.TEXT_PHONE -> PhoneKind.TEXT_PHONE
    PhoneType.OTHER -> PhoneKind.OTHER
}

fun PhoneKind.toLib(): PhoneType = when (this) {
    PhoneKind.CELL -> PhoneType.CELL
    PhoneKind.HOME -> PhoneType.HOME
    PhoneKind.WORK -> PhoneType.WORK
    PhoneKind.FAX -> PhoneType.FAX
    PhoneKind.PAGER -> PhoneType.PAGER
    PhoneKind.VOICE -> PhoneType.VOICE
    PhoneKind.TEXT -> PhoneType.TEXT
    PhoneKind.VIDEO -> PhoneType.VIDEO
    PhoneKind.TEXT_PHONE -> PhoneType.TEXT_PHONE
    PhoneKind.OTHER -> PhoneType.OTHER
}

fun EmailType.toDomain(): EmailKind = when (this) {
    EmailType.HOME -> EmailKind.HOME
    EmailType.WORK -> EmailKind.WORK
    EmailType.OTHER -> EmailKind.OTHER
}

fun EmailKind.toLib(): EmailType = when (this) {
    EmailKind.HOME -> EmailType.HOME
    EmailKind.WORK -> EmailType.WORK
    EmailKind.OTHER -> EmailType.OTHER
}

fun AddressType.toDomain(): AddressKind = when (this) {
    AddressType.HOME -> AddressKind.HOME
    AddressType.WORK -> AddressKind.WORK
    AddressType.OTHER -> AddressKind.OTHER
}

fun AddressKind.toLib(): AddressType = when (this) {
    AddressKind.HOME -> AddressType.HOME
    AddressKind.WORK -> AddressType.WORK
    AddressKind.OTHER -> AddressType.OTHER
}

fun ImType.toDomain(): ImKind = when (this) {
    ImType.HOME -> ImKind.HOME
    ImType.WORK -> ImKind.WORK
    ImType.OTHER -> ImKind.OTHER
}

fun ImKind.toLib(): ImType = when (this) {
    ImKind.HOME -> ImType.HOME
    ImKind.WORK -> ImType.WORK
    ImKind.OTHER -> ImType.OTHER
}

fun URLType.toDomain(): LinkKind = when (this) {
    URLType.Home -> LinkKind.HOME
    URLType.Work -> LinkKind.WORK
    URLType.Homepage -> LinkKind.HOMEPAGE
    URLType.WebId -> LinkKind.WEB_ID
    URLType.PublicId -> LinkKind.PUBLIC_ID
}

fun LinkKind.toLib(): URLType = when (this) {
    LinkKind.HOME -> URLType.Home
    LinkKind.WORK -> URLType.Work
    LinkKind.HOMEPAGE -> URLType.Homepage
    LinkKind.WEB_ID -> URLType.WebId
    LinkKind.PUBLIC_ID -> URLType.PublicId
}

fun Gender.toDomain(): GenderKind = when (this) {
    Gender.MALE -> GenderKind.MALE
    Gender.FEMALE -> GenderKind.FEMALE
    Gender.OTHER -> GenderKind.OTHER
    Gender.NONE -> GenderKind.NONE
    Gender.UNKNOWN -> GenderKind.UNKNOWN
}

fun GenderKind.toLib(): Gender = when (this) {
    GenderKind.MALE -> Gender.MALE
    GenderKind.FEMALE -> Gender.FEMALE
    GenderKind.OTHER -> Gender.OTHER
    GenderKind.NONE -> Gender.NONE
    GenderKind.UNKNOWN -> Gender.UNKNOWN
}

fun Name.toDomain(): StructuredName = StructuredName(
    given = givenName,
    family = familyName,
    middle = additionalName,
    prefix = honorificPrefix,
    suffix = honorificSuffix,
)

fun StructuredName.toLib(): Name? = if (isEmpty()) {
    null
} else {
    Name(
        familyName = family?.takeIf { it.isNotBlank() },
        givenName = given?.takeIf { it.isNotBlank() },
        additionalName = middle?.takeIf { it.isNotBlank() },
        honorificPrefix = prefix?.takeIf { it.isNotBlank() },
        honorificSuffix = suffix?.takeIf { it.isNotBlank() },
    )
}

fun AddressEntry.toDomain(): PostalAddress = PostalAddress(
    street = street,
    locality = locality,
    region = region,
    postalCode = postalCode,
    country = countryName,
    poBox = poBox,
    kind = type.toDomain(),
)

fun PostalAddress.toLib(): AddressEntry = AddressEntry(
    street = street?.takeIf { it.isNotBlank() },
    locality = locality?.takeIf { it.isNotBlank() },
    region = region?.takeIf { it.isNotBlank() },
    postalCode = postalCode?.takeIf { it.isNotBlank() },
    countryName = country?.takeIf { it.isNotBlank() },
    poBox = poBox?.takeIf { it.isNotBlank() },
    type = kind.toLib(),
)

/** Folds a contact the pod returned into the app's model, keeping the local [id] and [bookId]. */
fun SolidContact.toDomain(id: String, bookId: String): ContactDetail = ContactDetail(
    id = id,
    uri = uri,
    bookId = bookId,
    fullName = data.fullName ?: data.effectiveFullName(),
    name = data.name?.toDomain() ?: StructuredName(),
    nickname = data.nickname,
    phones = data.phones.map { PhoneNumber(it.number, it.type.toDomain()) },
    emails = data.emails.map { EmailAddress(it.address, it.type.toDomain()) },
    impps = data.impps.map { ImHandle(it.handle, it.type.toDomain()) },
    addresses = data.addresses.map { it.toDomain() },
    links = data.urls.map { WebLink(it.value, it.type.toDomain()) },
    birthday = data.birthday,
    anniversary = data.anniversary,
    organization = data.organizationName,
    organizationUnit = data.organizationUnit,
    jobTitle = data.title,
    role = data.role,
    note = data.note,
    categories = data.categories,
    gender = data.gender?.toDomain(),
    geos = data.geos,
    languages = data.languages,
    uid = data.uid,
    photoUri = photoUri,
    modified = modified,
    syncState = SyncState.SYNCED,
)

/** The write model the data module takes. Blank values are dropped so the pod stays tidy. */
fun ContactDetail.toContactData(): ContactData = ContactData(
    fullName = displayName().takeIf { it.isNotBlank() },
    name = name.toLib(),
    nickname = nickname?.takeIf { it.isNotBlank() },
    phones = phones.filter { it.number.isNotBlank() }
        .map { PhoneEntry(it.number.trim(), it.kind.toLib()) },
    emails = emails.filter { it.address.isNotBlank() }
        .map { EmailEntry(it.address.trim(), it.kind.toLib()) },
    impps = impps.filter { it.handle.isNotBlank() }
        .map { ImEntry(it.handle.trim(), it.kind.toLib()) },
    addresses = addresses.filterNot { it.isEmpty() }.map { it.toLib() },
    birthday = birthday?.takeIf { it.isNotBlank() },
    anniversary = anniversary?.takeIf { it.isNotBlank() },
    organizationName = organization?.takeIf { it.isNotBlank() },
    organizationUnit = organizationUnit?.takeIf { it.isNotBlank() },
    role = role?.takeIf { it.isNotBlank() },
    title = jobTitle?.takeIf { it.isNotBlank() },
    note = note?.takeIf { it.isNotBlank() },
    categories = categories.filter { it.isNotBlank() }.distinct(),
    gender = gender?.toLib(),
    geos = geos.filter { it.isNotBlank() },
    languages = languages.filter { it.isNotBlank() },
    urls = links.filter { it.url.isNotBlank() }
        .map { UrlEntry(it.kind.toLib(), it.url.trim()) },
    uid = uid?.takeIf { it.isNotBlank() },
)

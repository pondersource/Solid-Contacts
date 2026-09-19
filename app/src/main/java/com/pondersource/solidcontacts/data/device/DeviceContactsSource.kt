package com.pondersource.solidcontacts.data.device

import android.Manifest
import android.content.ContentProviderOperation
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.pondersource.solidcontacts.domain.model.AddressKind
import com.pondersource.solidcontacts.domain.model.ContactDetail
import com.pondersource.solidcontacts.domain.model.EmailAddress
import com.pondersource.solidcontacts.domain.model.EmailKind
import com.pondersource.solidcontacts.domain.model.ImHandle
import com.pondersource.solidcontacts.domain.model.ImKind
import com.pondersource.solidcontacts.domain.model.LinkKind
import com.pondersource.solidcontacts.domain.model.PhoneKind
import com.pondersource.solidcontacts.domain.model.PhoneNumber
import com.pondersource.solidcontacts.domain.model.PostalAddress
import com.pondersource.solidcontacts.domain.model.StructuredName
import com.pondersource.solidcontacts.domain.model.WebLink
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The phone's own contact store.
 *
 * Reading it lets the user bring an existing address book into their pod; writing it back makes
 * pod contacts reachable from the dialer and the messaging apps. Both directions need a runtime
 * permission, which the caller asks for; every method here returns empty or `false` rather than
 * throwing when the permission is missing.
 */
@Singleton
class DeviceContactsSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val io: CoroutineDispatcher,
) {

    fun canRead(): Boolean = hasPermission(Manifest.permission.READ_CONTACTS)

    fun canWrite(): Boolean = hasPermission(Manifest.permission.WRITE_CONTACTS)

    /** Reads every contact on the device, with the fields this app can store in a pod. */
    suspend fun readAll(): List<ContactDetail> = withContext(io) {
        if (!canRead()) return@withContext emptyList()

        val builders = LinkedHashMap<Long, DeviceContactBuilder>()
        val resolver = context.contentResolver

        val projection = arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Data.DATA1,
            ContactsContract.Data.DATA2,
            ContactsContract.Data.DATA3,
            ContactsContract.Data.DATA4,
            ContactsContract.Data.DATA5,
            ContactsContract.Data.DATA6,
            ContactsContract.Data.DATA7,
            ContactsContract.Data.DATA8,
            ContactsContract.Data.DATA9,
            ContactsContract.Data.DATA10,
        )

        runCatching {
            resolver.query(
                ContactsContract.Data.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.Data.CONTACT_ID} ASC",
            )
        }.getOrNull()?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID)
            val mimeIndex = cursor.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE)

            fun column(name: String): String? {
                val index = cursor.getColumnIndex(name)
                return if (index >= 0 && !cursor.isNull(index)) cursor.getString(index) else null
            }

            while (cursor.moveToNext()) {
                val contactId = cursor.getLong(idIndex)
                val builder = builders.getOrPut(contactId) { DeviceContactBuilder() }
                when (cursor.getString(mimeIndex)) {
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE -> {
                        builder.fullName = column(ContactsContract.Data.DATA1)
                        builder.name = StructuredName(
                            given = column(ContactsContract.Data.DATA2),
                            family = column(ContactsContract.Data.DATA3),
                            prefix = column(ContactsContract.Data.DATA4),
                            middle = column(ContactsContract.Data.DATA5),
                            suffix = column(ContactsContract.Data.DATA6),
                        )
                    }

                    ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE ->
                        builder.nickname = column(ContactsContract.Data.DATA1)

                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE ->
                        column(ContactsContract.Data.DATA1)?.let { number ->
                            builder.phones += PhoneNumber(
                                number,
                                phoneKind(column(ContactsContract.Data.DATA2)?.toIntOrNull()),
                            )
                        }

                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE ->
                        column(ContactsContract.Data.DATA1)?.let { address ->
                            builder.emails += EmailAddress(
                                address,
                                emailKind(column(ContactsContract.Data.DATA2)?.toIntOrNull()),
                            )
                        }

                    ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE -> {
                        val address = PostalAddress(
                            street = column(ContactsContract.Data.DATA4),
                            poBox = column(ContactsContract.Data.DATA5),
                            locality = column(ContactsContract.Data.DATA7),
                            region = column(ContactsContract.Data.DATA8),
                            postalCode = column(ContactsContract.Data.DATA9),
                            country = column(ContactsContract.Data.DATA10),
                            kind = addressKind(column(ContactsContract.Data.DATA2)?.toIntOrNull()),
                        )
                        if (!address.isEmpty()) builder.addresses += address
                    }

                    ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE -> {
                        builder.organization = column(ContactsContract.Data.DATA1)
                        builder.jobTitle = column(ContactsContract.Data.DATA4)
                        builder.organizationUnit = column(ContactsContract.Data.DATA5)
                    }

                    ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE ->
                        builder.note = column(ContactsContract.Data.DATA1)

                    ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE ->
                        column(ContactsContract.Data.DATA1)?.let {
                            builder.links += WebLink(it, LinkKind.HOMEPAGE)
                        }

                    ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE ->
                        column(ContactsContract.Data.DATA1)?.let {
                            builder.impps += ImHandle(it, ImKind.OTHER)
                        }

                    ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE -> {
                        val type = column(ContactsContract.Data.DATA2)?.toIntOrNull()
                        val date = column(ContactsContract.Data.DATA1)
                        when (type) {
                            ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY ->
                                builder.birthday = date

                            ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY ->
                                builder.anniversary = date
                        }
                    }
                }
            }
        }

        builders.values.mapNotNull { it.build() }
    }

    /**
     * Writes [contacts] into the device store under this app's own account, so the entries stay
     * separable from the user's other accounts and a later export replaces rather than duplicates.
     */
    suspend fun writeAll(contacts: List<ContactDetail>): Int = withContext(io) {
        if (!canWrite()) return@withContext 0
        var written = 0
        // One batch per contact: a single failure then costs one contact, not the whole export.
        for (contact in contacts) {
            val ops = arrayListOf<ContentProviderOperation>()
            ops += ContentProviderOperation
                .newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build()

            fun data(mimeType: String) = ContentProviderOperation
                .newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, mimeType)

            ops += data(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.Data.DATA1, contact.displayName())
                .withValue(ContactsContract.Data.DATA2, contact.name.given)
                .withValue(ContactsContract.Data.DATA3, contact.name.family)
                .withValue(ContactsContract.Data.DATA4, contact.name.prefix)
                .withValue(ContactsContract.Data.DATA5, contact.name.middle)
                .withValue(ContactsContract.Data.DATA6, contact.name.suffix)
                .build()

            contact.phones.forEach { phone ->
                ops += data(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.Data.DATA1, phone.number)
                    .withValue(ContactsContract.Data.DATA2, phone.kind.androidType())
                    .build()
            }
            contact.emails.forEach { email ->
                ops += data(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.Data.DATA1, email.address)
                    .withValue(ContactsContract.Data.DATA2, email.kind.androidType())
                    .build()
            }
            contact.addresses.forEach { address ->
                ops += data(ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.Data.DATA1, address.formatted())
                    .withValue(ContactsContract.Data.DATA2, address.kind.androidType())
                    .withValue(ContactsContract.Data.DATA4, address.street)
                    .withValue(ContactsContract.Data.DATA5, address.poBox)
                    .withValue(ContactsContract.Data.DATA7, address.locality)
                    .withValue(ContactsContract.Data.DATA8, address.region)
                    .withValue(ContactsContract.Data.DATA9, address.postalCode)
                    .withValue(ContactsContract.Data.DATA10, address.country)
                    .build()
            }
            if (!contact.organization.isNullOrBlank() || !contact.jobTitle.isNullOrBlank()) {
                ops += data(ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.Data.DATA1, contact.organization)
                    .withValue(ContactsContract.Data.DATA4, contact.jobTitle)
                    .withValue(ContactsContract.Data.DATA5, contact.organizationUnit)
                    .build()
            }
            contact.note?.takeIf { it.isNotBlank() }?.let { note ->
                ops += data(ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.Data.DATA1, note)
                    .build()
            }
            contact.links.forEach { link ->
                ops += data(ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.Data.DATA1, link.url)
                    .build()
            }
            contact.birthday?.takeIf { it.isNotBlank() }?.let { date ->
                ops += data(ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.Data.DATA1, date)
                    .withValue(
                        ContactsContract.Data.DATA2,
                        ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY,
                    )
                    .build()
            }

            val applied = runCatching {
                context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            }.isSuccess
            if (applied) written++
        }
        written
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun phoneKind(type: Int?): PhoneKind = when (type) {
        ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> PhoneKind.HOME
        ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> PhoneKind.WORK
        ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK,
        ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME,
        -> PhoneKind.FAX

        ContactsContract.CommonDataKinds.Phone.TYPE_PAGER -> PhoneKind.PAGER
        else -> PhoneKind.CELL
    }

    private fun PhoneKind.androidType(): Int = when (this) {
        PhoneKind.HOME -> ContactsContract.CommonDataKinds.Phone.TYPE_HOME
        PhoneKind.WORK -> ContactsContract.CommonDataKinds.Phone.TYPE_WORK
        PhoneKind.FAX -> ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK
        PhoneKind.PAGER -> ContactsContract.CommonDataKinds.Phone.TYPE_PAGER
        else -> ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
    }

    private fun emailKind(type: Int?): EmailKind = when (type) {
        ContactsContract.CommonDataKinds.Email.TYPE_WORK -> EmailKind.WORK
        ContactsContract.CommonDataKinds.Email.TYPE_HOME -> EmailKind.HOME
        else -> EmailKind.OTHER
    }

    private fun EmailKind.androidType(): Int = when (this) {
        EmailKind.WORK -> ContactsContract.CommonDataKinds.Email.TYPE_WORK
        EmailKind.HOME -> ContactsContract.CommonDataKinds.Email.TYPE_HOME
        EmailKind.OTHER -> ContactsContract.CommonDataKinds.Email.TYPE_OTHER
    }

    private fun addressKind(type: Int?): AddressKind = when (type) {
        ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK -> AddressKind.WORK
        ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME -> AddressKind.HOME
        else -> AddressKind.OTHER
    }

    private fun AddressKind.androidType(): Int = when (this) {
        AddressKind.WORK -> ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK
        AddressKind.HOME -> ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME
        AddressKind.OTHER -> ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER
    }

    private class DeviceContactBuilder {
        var fullName: String? = null
        var name = StructuredName()
        var nickname: String? = null
        val phones = mutableListOf<PhoneNumber>()
        val emails = mutableListOf<EmailAddress>()
        val impps = mutableListOf<ImHandle>()
        val addresses = mutableListOf<PostalAddress>()
        val links = mutableListOf<WebLink>()
        var organization: String? = null
        var organizationUnit: String? = null
        var jobTitle: String? = null
        var note: String? = null
        var birthday: String? = null
        var anniversary: String? = null

        fun build(): ContactDetail? {
            val contact = ContactDetail(
                id = "",
                fullName = fullName.orEmpty(),
                name = name,
                nickname = nickname,
                phones = phones.distinctBy { it.number.filter(Char::isDigit) },
                emails = emails.distinctBy { it.address.lowercase() },
                impps = impps.distinctBy { it.handle.lowercase() },
                addresses = addresses.distinctBy { it.formatted() },
                links = links.distinctBy { it.url.lowercase() },
                organization = organization,
                organizationUnit = organizationUnit,
                jobTitle = jobTitle,
                note = note,
                birthday = birthday,
                anniversary = anniversary,
            )
            return if (contact.isEmpty()) null else contact
        }
    }
}

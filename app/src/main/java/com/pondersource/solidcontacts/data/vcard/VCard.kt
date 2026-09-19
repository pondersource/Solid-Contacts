package com.pondersource.solidcontacts.data.vcard

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
import com.pondersource.solidcontacts.domain.model.WebLink
import java.util.UUID

/**
 * Writes contacts as vCard 4.0 text.
 *
 * Every field the app models has a vCard equivalent, so a contact exported and imported again is
 * the same contact. Lines are folded at 75 octets and values escaped, as RFC 6350 requires.
 */
object VCardWriter {

    fun write(contacts: List<ContactDetail>): String =
        contacts.joinToString("") { writeOne(it) }

    fun writeOne(contact: ContactDetail): String = buildString {
        line("BEGIN:VCARD")
        line("VERSION:4.0")
        line("FN", contact.displayName())

        with(contact.name) {
            if (!isEmpty()) {
                line(
                    "N",
                    listOf(family, given, middle, prefix, suffix)
                        .joinToString(";") { escape(it.orEmpty()) },
                    escaped = true,
                )
            }
        }

        contact.nickname?.takeIf { it.isNotBlank() }?.let { line("NICKNAME", it) }

        contact.phones.forEach { phone ->
            line("TEL;TYPE=${phone.kind.vcardType()}", phone.number)
        }
        contact.emails.forEach { email ->
            line("EMAIL;TYPE=${email.kind.vcardType()}", email.address)
        }
        contact.impps.forEach { im ->
            line("IMPP;TYPE=${im.kind.vcardType()}", im.handle)
        }
        contact.addresses.forEach { address ->
            val value = listOf(
                address.poBox,
                "",
                address.street,
                address.locality,
                address.region,
                address.postalCode,
                address.country,
            ).joinToString(";") { escape(it.orEmpty()) }
            line("ADR;TYPE=${address.kind.vcardType()}", value, escaped = true)
        }
        contact.links.forEach { link ->
            when (link.kind) {
                LinkKind.WEB_ID -> line("URL;TYPE=webid", link.url)
                LinkKind.PUBLIC_ID -> line("URL;TYPE=publicid", link.url)
                LinkKind.HOME -> line("URL;TYPE=home", link.url)
                LinkKind.WORK -> line("URL;TYPE=work", link.url)
                LinkKind.HOMEPAGE -> line("URL", link.url)
            }
        }

        contact.birthday?.takeIf { it.isNotBlank() }?.let { line("BDAY", it) }
        contact.anniversary?.takeIf { it.isNotBlank() }?.let { line("ANNIVERSARY", it) }

        val org = listOfNotNull(
            contact.organization?.takeIf { it.isNotBlank() },
            contact.organizationUnit?.takeIf { it.isNotBlank() },
        )
        if (org.isNotEmpty()) line("ORG", org.joinToString(";") { escape(it) }, escaped = true)

        contact.jobTitle?.takeIf { it.isNotBlank() }?.let { line("TITLE", it) }
        contact.role?.takeIf { it.isNotBlank() }?.let { line("ROLE", it) }
        contact.note?.takeIf { it.isNotBlank() }?.let { line("NOTE", it) }
        contact.categories.takeIf { it.isNotEmpty() }
            ?.let { line("CATEGORIES", it.joinToString(",") { c -> escape(c) }, escaped = true) }
        contact.gender?.let { line("GENDER", it.vcardValue()) }
        contact.geos.forEach { line("GEO", if (it.startsWith("geo:")) it else "geo:$it") }
        contact.languages.forEach { line("LANG", it) }
        line("UID", contact.uid?.takeIf { it.isNotBlank() } ?: "urn:uuid:${UUID.randomUUID()}")
        line("END:VCARD")
    }

    private fun StringBuilder.line(raw: String) {
        append(fold(raw)).append("\r\n")
    }

    private fun StringBuilder.line(name: String, value: String, escaped: Boolean = false) {
        if (value.isBlank()) return
        val body = if (escaped) value else escape(value)
        append(fold("$name:$body")).append("\r\n")
    }

    private fun escape(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\n", "\\n")
        .replace(",", "\\,")
        .replace(";", "\\;")

    /** RFC 6350 folds long lines at 75 octets, continuing them with a leading space. */
    private fun fold(line: String): String {
        if (line.length <= 75) return line
        val out = StringBuilder()
        var index = 0
        while (index < line.length) {
            val end = minOf(index + if (index == 0) 75 else 74, line.length)
            if (index > 0) out.append("\r\n ")
            out.append(line, index, end)
            index = end
        }
        return out.toString()
    }

    private fun PhoneKind.vcardType(): String = when (this) {
        PhoneKind.CELL -> "cell"
        PhoneKind.HOME -> "home"
        PhoneKind.WORK -> "work"
        PhoneKind.FAX -> "fax"
        PhoneKind.PAGER -> "pager"
        PhoneKind.VOICE -> "voice"
        PhoneKind.TEXT -> "text"
        PhoneKind.VIDEO -> "video"
        PhoneKind.TEXT_PHONE -> "textphone"
        PhoneKind.OTHER -> "other"
    }

    private fun EmailKind.vcardType(): String = name.lowercase()

    private fun AddressKind.vcardType(): String = name.lowercase()

    private fun ImKind.vcardType(): String = name.lowercase()

    private fun GenderKind.vcardValue(): String = when (this) {
        GenderKind.MALE -> "M"
        GenderKind.FEMALE -> "F"
        GenderKind.OTHER -> "O"
        GenderKind.NONE -> "N"
        GenderKind.UNKNOWN -> "U"
    }
}

/**
 * Reads vCard 2.1, 3.0 and 4.0 text into contacts.
 *
 * Phones exported by other apps arrive in every shape imaginable, so the parser is forgiving: an
 * unknown property is skipped rather than failing the import, and a card with no `FN` still
 * yields a contact when it carries a name, an email or a number.
 */
object VCardReader {

    fun read(text: String): List<ContactDetail> {
        val cards = mutableListOf<ContactDetail>()
        var current: MutableCard? = null

        for (line in unfold(text)) {
            val trimmed = line.trim()
            when {
                trimmed.equals("BEGIN:VCARD", ignoreCase = true) -> current = MutableCard()
                trimmed.equals("END:VCARD", ignoreCase = true) -> {
                    current?.build()?.let { cards += it }
                    current = null
                }

                else -> current?.consume(trimmed)
            }
        }
        return cards
    }

    /** Joins continuation lines, which start with a space or a tab, back onto their property. */
    private fun unfold(text: String): List<String> {
        val out = mutableListOf<String>()
        for (raw in text.replace("\r\n", "\n").replace('\r', '\n').split('\n')) {
            if ((raw.startsWith(" ") || raw.startsWith("\t")) && out.isNotEmpty()) {
                out[out.lastIndex] = out.last() + raw.substring(1)
            } else {
                out += raw
            }
        }
        return out
    }

    private class MutableCard {
        var fullName: String? = null
        var name = StructuredName()
        var nickname: String? = null
        val phones = mutableListOf<PhoneNumber>()
        val emails = mutableListOf<EmailAddress>()
        val impps = mutableListOf<ImHandle>()
        val addresses = mutableListOf<PostalAddress>()
        val links = mutableListOf<WebLink>()
        var birthday: String? = null
        var anniversary: String? = null
        var organization: String? = null
        var organizationUnit: String? = null
        var jobTitle: String? = null
        var role: String? = null
        var note: String? = null
        val categories = mutableListOf<String>()
        var gender: GenderKind? = null
        val geos = mutableListOf<String>()
        val languages = mutableListOf<String>()
        var uid: String? = null

        fun consume(line: String) {
            val colon = line.indexOf(':')
            if (colon <= 0) return
            val head = line.substring(0, colon)
            val value = line.substring(colon + 1)
            if (value.isBlank()) return

            val parts = head.split(';')
            val property = parts.first().substringAfter('.').uppercase()
            val params = parts.drop(1)
            // A base64 photo or an encoded property is not something this reader handles.
            if (params.any { it.contains("ENCODING=B", ignoreCase = true) }) return
            val types = params
                .filter { it.contains('=') }
                .filter { it.substringBefore('=').equals("TYPE", ignoreCase = true) }
                .flatMap { it.substringAfter('=').split(',') }
                .map { it.trim().trim('"').lowercase() }
                .ifEmpty { params.map { it.trim().lowercase() } }

            when (property) {
                "FN" -> fullName = unescape(value)
                "N" -> {
                    val f = splitValue(value)
                    name = StructuredName(
                        family = f.getOrNull(0)?.takeIf { it.isNotBlank() },
                        given = f.getOrNull(1)?.takeIf { it.isNotBlank() },
                        middle = f.getOrNull(2)?.takeIf { it.isNotBlank() },
                        prefix = f.getOrNull(3)?.takeIf { it.isNotBlank() },
                        suffix = f.getOrNull(4)?.takeIf { it.isNotBlank() },
                    )
                }

                "NICKNAME" -> nickname = unescape(value)
                "TEL" -> phones += PhoneNumber(unescape(value), phoneKind(types))
                "EMAIL" -> emails += EmailAddress(unescape(value), emailKind(types))
                "IMPP", "X-JABBER", "X-SKYPE" -> impps += ImHandle(unescape(value), imKind(types))
                "ADR" -> {
                    val f = splitValue(value)
                    val address = PostalAddress(
                        poBox = f.getOrNull(0)?.takeIf { it.isNotBlank() },
                        street = f.getOrNull(2)?.takeIf { it.isNotBlank() },
                        locality = f.getOrNull(3)?.takeIf { it.isNotBlank() },
                        region = f.getOrNull(4)?.takeIf { it.isNotBlank() },
                        postalCode = f.getOrNull(5)?.takeIf { it.isNotBlank() },
                        country = f.getOrNull(6)?.takeIf { it.isNotBlank() },
                        kind = addressKind(types),
                    )
                    if (!address.isEmpty()) addresses += address
                }

                "URL" -> links += WebLink(unescape(value), linkKind(types))
                "BDAY" -> birthday = unescape(value)
                "ANNIVERSARY" -> anniversary = unescape(value)
                "ORG" -> {
                    val f = splitValue(value)
                    organization = f.getOrNull(0)?.takeIf { it.isNotBlank() }
                    organizationUnit = f.getOrNull(1)?.takeIf { it.isNotBlank() }
                }

                "TITLE" -> jobTitle = unescape(value)
                "ROLE" -> role = unescape(value)
                "NOTE" -> note = unescape(value)
                "CATEGORIES" -> categories += splitList(value).filter { it.isNotBlank() }
                "GENDER" -> gender = genderOf(value)
                "GEO" -> geos += unescape(value)
                "LANG" -> languages += unescape(value)
                "UID" -> uid = unescape(value)
            }
        }

        fun build(): ContactDetail? {
            val contact = ContactDetail(
                id = "",
                fullName = fullName.orEmpty(),
                name = name,
                nickname = nickname,
                phones = phones.toList(),
                emails = emails.toList(),
                impps = impps.toList(),
                addresses = addresses.toList(),
                links = links.toList(),
                birthday = birthday,
                anniversary = anniversary,
                organization = organization,
                organizationUnit = organizationUnit,
                jobTitle = jobTitle,
                role = role,
                note = note,
                categories = categories.distinct(),
                gender = gender,
                geos = geos.toList(),
                languages = languages.toList(),
                uid = uid,
            )
            return if (contact.isEmpty()) null else contact
        }

        private fun phoneKind(types: List<String>): PhoneKind = when {
            types.any { it.contains("cell") || it.contains("mobile") } -> PhoneKind.CELL
            types.any { it.contains("fax") } -> PhoneKind.FAX
            types.any { it.contains("pager") } -> PhoneKind.PAGER
            types.any { it.contains("video") } -> PhoneKind.VIDEO
            types.any { it.contains("textphone") } -> PhoneKind.TEXT_PHONE
            types.any { it.contains("text") } -> PhoneKind.TEXT
            types.any { it.contains("work") } -> PhoneKind.WORK
            types.any { it.contains("home") } -> PhoneKind.HOME
            types.any { it.contains("voice") } -> PhoneKind.VOICE
            else -> PhoneKind.CELL
        }

        private fun emailKind(types: List<String>): EmailKind = when {
            types.any { it.contains("work") } -> EmailKind.WORK
            types.any { it.contains("home") } -> EmailKind.HOME
            else -> EmailKind.OTHER
        }

        private fun addressKind(types: List<String>): AddressKind = when {
            types.any { it.contains("work") } -> AddressKind.WORK
            types.any { it.contains("home") } -> AddressKind.HOME
            else -> AddressKind.OTHER
        }

        private fun imKind(types: List<String>): ImKind = when {
            types.any { it.contains("work") } -> ImKind.WORK
            types.any { it.contains("home") } -> ImKind.HOME
            else -> ImKind.OTHER
        }

        private fun linkKind(types: List<String>): LinkKind = when {
            types.any { it.contains("webid") } -> LinkKind.WEB_ID
            types.any { it.contains("publicid") } -> LinkKind.PUBLIC_ID
            types.any { it.contains("work") } -> LinkKind.WORK
            types.any { it.contains("home") } -> LinkKind.HOME
            else -> LinkKind.HOMEPAGE
        }

        private fun genderOf(value: String): GenderKind? =
            when (value.substringBefore(';').trim().uppercase()) {
                "M" -> GenderKind.MALE
                "F" -> GenderKind.FEMALE
                "O" -> GenderKind.OTHER
                "N" -> GenderKind.NONE
                "U" -> GenderKind.UNKNOWN
                else -> null
            }
    }

    /** Splits on unescaped `;`, the separator for structured values such as `N` and `ADR`. */
    private fun splitValue(value: String): List<String> = splitOn(value, ';').map(::unescape)

    /** Splits on unescaped `,`, the separator inside `CATEGORIES`. */
    private fun splitList(value: String): List<String> = splitOn(value, ',').map(::unescape)

    private fun splitOn(value: String, separator: Char): List<String> {
        val out = mutableListOf<String>()
        val current = StringBuilder()
        var escaped = false
        for (char in value) {
            when {
                escaped -> {
                    current.append('\\').append(char)
                    escaped = false
                }

                char == '\\' -> escaped = true
                char == separator -> {
                    out += current.toString()
                    current.clear()
                }

                else -> current.append(char)
            }
        }
        if (escaped) current.append('\\')
        out += current.toString()
        return out
    }

    private fun unescape(value: String): String {
        val out = StringBuilder(value.length)
        var index = 0
        while (index < value.length) {
            val char = value[index]
            if (char == '\\' && index + 1 < value.length) {
                when (val next = value[index + 1]) {
                    'n', 'N' -> out.append('\n')
                    '\\' -> out.append('\\')
                    ',' -> out.append(',')
                    ';' -> out.append(';')
                    else -> out.append(next)
                }
                index += 2
            } else {
                out.append(char)
                index++
            }
        }
        return out.toString().trim()
    }
}

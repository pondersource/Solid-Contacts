package com.pondersource.solidcontacts.data.repository

import com.pondersource.solidcontacts.domain.model.ContactDetail
import com.pondersource.solidcontacts.domain.model.DuplicateCluster
import com.pondersource.solidcontacts.domain.model.StructuredName

/**
 * Finds contacts that look like the same person, and folds them into one.
 *
 * Two contacts are the same person when they share a WebID, an email address, a phone number
 * (compared on digits only, so `+31 6 1234` and `0031612 34` match) or an exact name. A match on
 * any one of those is enough, and matches chain: A matches B on a phone number and B matches C on
 * an email, so all three are one cluster.
 */
object DuplicateFinder {

    fun cluster(contacts: List<ContactDetail>): List<DuplicateCluster> {
        if (contacts.size < 2) return emptyList()

        val parent = HashMap<String, String>()
        contacts.forEach { parent[it.id] = it.id }

        fun find(id: String): String {
            var root = id
            while (parent[root] != root) root = parent[root] ?: root
            var walk = id
            while (parent[walk] != root) {
                val next = parent[walk] ?: break
                parent[walk] = root
                walk = next
            }
            return root
        }

        fun union(a: String, b: String) {
            val rootA = find(a)
            val rootB = find(b)
            if (rootA != rootB) parent[rootB] = rootA
        }

        val byKey = HashMap<String, String>()
        for (contact in contacts) {
            for (key in keysOf(contact)) {
                val seen = byKey[key]
                if (seen == null) byKey[key] = contact.id else union(seen, contact.id)
            }
        }

        return contacts
            .groupBy { find(it.id) }
            .values
            .filter { it.size > 1 }
            .map { members ->
                val ordered = members.sortedByDescending { it.richness() }
                DuplicateCluster(
                    signature = ordered.map { it.id }.sorted().joinToString("|"),
                    members = ordered,
                )
            }
            .sortedByDescending { it.members.size }
    }

    /**
     * Folds [losers] into [survivor]: the survivor's own values win, and anything it lacks is
     * taken from the others. Nothing is dropped, so a merge never loses a phone number.
     */
    fun merge(survivor: ContactDetail, losers: List<ContactDetail>): ContactDetail {
        val all = listOf(survivor) + losers
        return survivor.copy(
            fullName = survivor.displayName().ifBlank {
                all.firstNotNullOfOrNull { it.displayName().takeIf(String::isNotBlank) }.orEmpty()
            },
            name = mergeName(all.map { it.name }),
            nickname = survivor.nickname.orFirstOf(all) { it.nickname },
            phones = all.flatMap { it.phones }.distinctBy { it.number.digits() },
            emails = all.flatMap { it.emails }.distinctBy { it.address.lowercase() },
            impps = all.flatMap { it.impps }.distinctBy { it.handle.lowercase() },
            addresses = all.flatMap { it.addresses }.distinctBy { it.formatted().lowercase() },
            links = all.flatMap { it.links }.distinctBy { it.url.lowercase() },
            birthday = survivor.birthday.orFirstOf(all) { it.birthday },
            anniversary = survivor.anniversary.orFirstOf(all) { it.anniversary },
            organization = survivor.organization.orFirstOf(all) { it.organization },
            organizationUnit = survivor.organizationUnit.orFirstOf(all) { it.organizationUnit },
            jobTitle = survivor.jobTitle.orFirstOf(all) { it.jobTitle },
            role = survivor.role.orFirstOf(all) { it.role },
            note = all.mapNotNull { it.note?.takeIf(String::isNotBlank) }
                .distinct()
                .joinToString("\n\n")
                .takeIf { it.isNotBlank() },
            categories = all.flatMap { it.categories }.distinctBy { it.lowercase() },
            gender = survivor.gender ?: all.firstNotNullOfOrNull { it.gender },
            geos = all.flatMap { it.geos }.distinct(),
            languages = all.flatMap { it.languages }.distinctBy { it.lowercase() },
            uid = survivor.uid.orFirstOf(all) { it.uid },
            isFavorite = all.any { it.isFavorite },
        )
    }

    /**
     * The values that identify this person: WebID, emails, phone digits and the exact name.
     *
     * Two contacts sharing any one of these are treated as the same person, which is also what
     * an import uses to leave out someone the pod already holds.
     */
    fun keysOf(contact: ContactDetail): List<String> = buildList {
        contact.webId?.takeIf { it.isNotBlank() }?.let { add("w:${it.lowercase()}") }
        contact.emails.forEach { email ->
            email.address.takeIf { it.isNotBlank() }?.let { add("e:${it.lowercase().trim()}") }
        }
        contact.phones.forEach { phone ->
            // Compare the last nine digits: enough to be specific, short enough that a country
            // code written two different ways still matches.
            phone.number.digits().takeIf { it.length >= 6 }?.let { add("p:${it.takeLast(9)}") }
        }
        contact.displayName().takeIf { it.isNotBlank() }?.let { add("n:${it.lowercase().trim()}") }
    }

    private fun mergeName(names: List<StructuredName>): StructuredName = StructuredName(
        given = names.firstNotNullOfOrNull { it.given?.takeIf(String::isNotBlank) },
        family = names.firstNotNullOfOrNull { it.family?.takeIf(String::isNotBlank) },
        middle = names.firstNotNullOfOrNull { it.middle?.takeIf(String::isNotBlank) },
        prefix = names.firstNotNullOfOrNull { it.prefix?.takeIf(String::isNotBlank) },
        suffix = names.firstNotNullOfOrNull { it.suffix?.takeIf(String::isNotBlank) },
    )

    /** How much this contact carries, so the fullest one is offered as the survivor. */
    private fun ContactDetail.richness(): Int =
        phones.size + emails.size + addresses.size + impps.size + links.size +
            listOfNotNull(organization, jobTitle, note, birthday, photoUri).size +
            (if (!name.isEmpty()) 2 else 0)

    private fun String.digits(): String = filter(Char::isDigit)

    private inline fun String?.orFirstOf(
        all: List<ContactDetail>,
        pick: (ContactDetail) -> String?,
    ): String? = this?.takeIf { it.isNotBlank() }
        ?: all.firstNotNullOfOrNull { pick(it)?.takeIf(String::isNotBlank) }
}

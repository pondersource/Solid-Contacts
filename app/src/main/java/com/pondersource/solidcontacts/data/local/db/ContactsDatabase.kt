package com.pondersource.solidcontacts.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.pondersource.solidcontacts.domain.model.SyncState

class DbConverters {

    @TypeConverter
    fun syncStateToName(value: SyncState): String = value.name

    @TypeConverter
    fun nameToSyncState(value: String): SyncState =
        runCatching { SyncState.valueOf(value) }.getOrDefault(SyncState.SYNCED)

    @TypeConverter
    fun opStatusToName(value: OpStatus): String = value.name

    @TypeConverter
    fun nameToOpStatus(value: String): OpStatus =
        runCatching { OpStatus.valueOf(value) }.getOrDefault(OpStatus.PENDING)
}

/**
 * The offline copy of the user's contacts.
 *
 * Every screen reads from here and never from the pod directly, so the app opens with data even
 * with no network and never blocks on a round trip.
 */
@Database(
    entities = [
        AddressBookEntity::class,
        ContactEntity::class,
        GroupEntity::class,
        GroupMemberEntity::class,
        OutboxOpEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(DbConverters::class)
abstract class ContactsDatabase : RoomDatabase() {
    abstract fun addressBookDao(): AddressBookDao
    abstract fun contactDao(): ContactDao
    abstract fun groupDao(): GroupDao
    abstract fun groupMemberDao(): GroupMemberDao
    abstract fun outboxDao(): OutboxDao

    companion object {
        const val NAME: String = "solid-contacts.db"
    }
}

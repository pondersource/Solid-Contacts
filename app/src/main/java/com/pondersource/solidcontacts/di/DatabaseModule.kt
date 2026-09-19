package com.pondersource.solidcontacts.di

import android.content.Context
import androidx.room.Room
import com.pondersource.solidcontacts.data.local.db.AddressBookDao
import com.pondersource.solidcontacts.data.local.db.ContactDao
import com.pondersource.solidcontacts.data.local.db.ContactsDatabase
import com.pondersource.solidcontacts.data.local.db.GroupDao
import com.pondersource.solidcontacts.data.local.db.GroupMemberDao
import com.pondersource.solidcontacts.data.local.db.OutboxDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ContactsDatabase =
        Room.databaseBuilder(context, ContactsDatabase::class.java, ContactsDatabase.NAME)
            // The cache can always be rebuilt from the pod, so an upgrade may start over rather
            // than carry a migration for data the pod already holds. Queued writes are the one
            // thing that would be lost, which is why the app drains before it updates.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideAddressBookDao(database: ContactsDatabase): AddressBookDao = database.addressBookDao()

    @Provides
    fun provideContactDao(database: ContactsDatabase): ContactDao = database.contactDao()

    @Provides
    fun provideGroupDao(database: ContactsDatabase): GroupDao = database.groupDao()

    @Provides
    fun provideGroupMemberDao(database: ContactsDatabase): GroupMemberDao = database.groupMemberDao()

    @Provides
    fun provideOutboxDao(database: ContactsDatabase): OutboxDao = database.outboxDao()
}

package com.pondersource.solidcontacts.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.pondersource.solidandroidclient.sdk.SolidContactsDataModule
import com.pondersource.solidcontacts.repository.contacts.ContactsRepository
import com.pondersource.solidcontacts.repository.contacts.ContactsRepositoryImplementation
import com.pondersource.solidcontacts.repository.user.UserRepository
import com.pondersource.solidcontacts.repository.user.UserRepositoryImplementation
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class RepositoryModule {

    companion object {
        private const val PREFERENCES_NAME = "com.pondersource.solidcontacts.preferences"
    }

    private val Context.preferencesDataStore: DataStore<Preferences> by preferencesDataStore(
        PREFERENCES_NAME
    )

    @Provides
    @Singleton
    fun providePreferencesDatasource(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.preferencesDataStore


    @Provides
    @Singleton
    fun providesContactsRepository(
        contactsDataModule: SolidContactsDataModule,
        userRepository: UserRepository,
    ): ContactsRepository {
        return ContactsRepositoryImplementation(contactsDataModule, userRepository)
    }

    @Provides
    @Singleton
    fun providesUserRepository(
        dataStore: DataStore<Preferences>
    ): UserRepository {
        return UserRepositoryImplementation(dataStore)
    }
}
package com.pondersource.solidcontacts.di

import com.pondersource.solidandroidclient.sdk.SolidContactsDataModule
import com.pondersource.solidcontacts.repository.contacts.ContactsRepository
import com.pondersource.solidcontacts.repository.contacts.ContactsRepositoryImplementation
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class RepositoryModule {

    @Provides
    @Singleton
    fun providesContactsRepository(
        contactsDataModule: SolidContactsDataModule
    ): ContactsRepository {
        return ContactsRepositoryImplementation(contactsDataModule)
    }
}
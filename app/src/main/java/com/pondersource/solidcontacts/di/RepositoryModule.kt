package com.pondersource.solidcontacts.di

import com.pondersource.solidcontacts.data.repository.AccountRepository
import com.pondersource.solidcontacts.data.repository.AccountRepositoryImpl
import com.pondersource.solidcontacts.data.repository.ContactsRepository
import com.pondersource.solidcontacts.data.repository.ContactsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindContactsRepository(impl: ContactsRepositoryImpl): ContactsRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository
}

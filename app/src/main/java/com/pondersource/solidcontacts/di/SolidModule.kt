package com.pondersource.solidcontacts.di

import android.content.Context
import com.erfangholami.androidsolidservices.client.sdk.Solid
import com.erfangholami.androidsolidservices.client.sdk.SolidContactsDataModule
import com.erfangholami.androidsolidservices.client.sdk.SolidSignInClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The Android Solid Services clients.
 *
 * Each one is a process-wide singleton that binds to the host app on first use, so they are
 * created once here and injected everywhere else.
 */
@Module
@InstallIn(SingletonComponent::class)
object SolidModule {

    @Provides
    @Singleton
    fun provideSignInClient(@ApplicationContext context: Context): SolidSignInClient =
        Solid.getSignInClient(context)

    @Provides
    @Singleton
    fun provideContactsDataModule(@ApplicationContext context: Context): SolidContactsDataModule =
        Solid.getContactsDataModule(context)
}

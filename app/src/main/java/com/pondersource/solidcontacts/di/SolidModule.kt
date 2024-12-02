package com.pondersource.solidcontacts.di

import android.content.Context
import com.pondersource.solidandroidclient.sdk.Solid
import com.pondersource.solidandroidclient.sdk.SolidContactsDataModule
import com.pondersource.solidandroidclient.sdk.SolidResourceClient
import com.pondersource.solidandroidclient.sdk.SolidSignInClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class SolidModule {

    @Provides
    @Singleton
    fun providesSolidSignInClient(
        @ApplicationContext context: Context
    ): SolidSignInClient {
        return Solid.getSignInClient(context)
    }

    @Provides
    @Singleton
    fun providesSolidResourceClient(
        @ApplicationContext context: Context
    ): SolidResourceClient {
        return Solid.getResourceClient(context)
    }

    @Provides
    @Singleton
    fun providesSolidContactModule(
        @ApplicationContext context: Context
    ): SolidContactsDataModule {
        return Solid.getContactsDataModule(context)
    }
}
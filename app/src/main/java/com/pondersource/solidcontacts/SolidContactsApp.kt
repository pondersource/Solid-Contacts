package com.pondersource.solidcontacts

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.pondersource.solidcontacts.data.local.prefs.UserPreferences
import com.pondersource.solidcontacts.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SolidContactsApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var scheduler: SyncScheduler

    @Inject lateinit var preferences: UserPreferences

    @Inject lateinit var applicationScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // A launch is a good moment both to catch the pod up on whatever was written offline
        // and to pull what changed elsewhere. Without the pull, a cache that starts empty -
        // a fresh install, or a schema upgrade that rebuilt it - stays empty until the user
        // happens to find the refresh.
        applicationScope.launch {
            val settings = preferences.current()
            if (settings.isSignedIn) {
                scheduler.schedulePeriodicSync(settings.syncOnlyOnWifi)
                scheduler.requestDrain()
                scheduler.requestRefresh()
            }
        }
    }
}

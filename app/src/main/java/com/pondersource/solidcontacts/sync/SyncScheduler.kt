package com.pondersource.solidcontacts.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decides when the app talks to the pod.
 *
 * Nothing in the UI calls this to get its data; it only asks the system to catch the pod up in
 * the background. A drain runs as soon as there is a network, a refresh runs every six hours and
 * whenever the user pulls to refresh.
 */
@Singleton
class SyncScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    /** Asks for the queued writes to be sent as soon as the device has a network. */
    fun requestDrain() {
        val request = OneTimeWorkRequestBuilder<OutboxWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(TAG_DRAIN)
            .build()
        workManager.enqueueUniqueWork(WORK_DRAIN, ExistingWorkPolicy.REPLACE, request)
    }

    /** Asks for a pull from the pod right now. */
    fun requestRefresh() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .addTag(TAG_SYNC)
            .build()
        workManager.enqueueUniqueWork(WORK_SYNC_NOW, ExistingWorkPolicy.KEEP, request)
    }

    /** Keeps the cache fresh in the background. Call once, when an account is connected. */
    fun schedulePeriodicSync(wifiOnly: Boolean = false) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(
                        if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED,
                    )
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .addTag(TAG_SYNC)
            .build()
        workManager.enqueueUniquePeriodicWork(
            WORK_SYNC_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    /** Stops all background work, for when the user disconnects the account. */
    fun cancelAll() {
        workManager.cancelUniqueWork(WORK_SYNC_PERIODIC)
        workManager.cancelUniqueWork(WORK_SYNC_NOW)
        workManager.cancelUniqueWork(WORK_DRAIN)
    }

    /** `true` while a refresh or a drain is running, for the progress line in the UI. */
    fun observeSyncing(): Flow<Boolean> =
        workManager.getWorkInfosByTagFlow(TAG_SYNC).map { infos ->
            infos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
        }

    companion object {
        const val WORK_SYNC_PERIODIC = "solid-contacts-sync-periodic"
        const val WORK_SYNC_NOW = "solid-contacts-sync-now"
        const val WORK_DRAIN = "solid-contacts-outbox-drain"
        const val TAG_SYNC = "solid-contacts-sync"
        const val TAG_DRAIN = "solid-contacts-drain"
    }
}

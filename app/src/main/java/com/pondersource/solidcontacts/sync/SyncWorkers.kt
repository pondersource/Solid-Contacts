package com.pondersource.solidcontacts.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pondersource.solidcontacts.data.repository.ContactsRepository
import com.pondersource.solidcontacts.domain.error.toAppError
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Sends the writes the user made while the pod was out of reach.
 *
 * Retries only while the failure could still clear on its own — a lost connection, a host app
 * that is not answering. A refusal the pod will keep repeating is left for the user to see in
 * the sync status rather than retried forever.
 */
@HiltWorker
class OutboxWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: ContactsRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val outcome = repository.drainOutbox()
        val drained = outcome.getOrElse { error ->
            return if (error.toAppError().isTransient) Result.retry() else Result.failure()
        }
        return if (drained.allClear) Result.success() else Result.retry()
    }
}

/** Pulls the pod's contacts into the local database so the app opens with fresh data. */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: ContactsRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        repository.drainOutbox()
        return repository.refresh().fold(
            onSuccess = { Result.success() },
            onFailure = { error ->
                if (error.toAppError().isRetryable) Result.retry() else Result.failure()
            },
        )
    }
}

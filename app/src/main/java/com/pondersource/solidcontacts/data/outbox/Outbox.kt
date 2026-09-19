package com.pondersource.solidcontacts.data.outbox

import com.pondersource.solidcontacts.data.local.db.OpStatus
import com.pondersource.solidcontacts.data.local.db.OutboxDao
import com.pondersource.solidcontacts.data.local.db.OutboxOpEntity
import com.pondersource.solidcontacts.domain.error.toAppError
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** The result of one drain pass, so the caller knows whether to ask for another. */
data class DrainResult(
    val succeeded: Int,
    val failed: Int,
    val remaining: Int,
) {
    val allClear: Boolean get() = failed == 0 && remaining == 0
}

/**
 * The queue of writes waiting for the pod.
 *
 * Writes go into the local database and into this queue in the same breath, so the UI never
 * waits for the network. A worker drains the queue whenever the device is online; an op that
 * fails backs off and is tried again rather than being lost.
 */
@Singleton
class Outbox @Inject constructor(
    private val dao: OutboxDao,
) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /**
     * Queues [type] for [payload], unless the same op is already queued for the same row.
     *
     * The collapse matters for an editor that saves on every keystroke: the drain re-reads the
     * row when it sends, so one queued update carries the latest text either way.
     */
    suspend fun enqueue(webId: String, type: OpType, payload: OutboxPayload) {
        if (type.isCollapsible() && isAlreadyQueued(webId, type, payload)) return
        val now = System.currentTimeMillis()
        dao.insert(
            OutboxOpEntity(
                webId = webId,
                type = type.name,
                payload = json.encodeToString(OutboxPayload.serializer(), payload),
                status = OpStatus.PENDING,
                attempts = 0,
                nextRetryAt = 0,
                lastError = null,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    /**
     * Runs every op that is due, oldest first, handing each to [execute].
     *
     * Order matters: a contact's create is queued before its update, so by the time the update
     * runs the create has already given the row its pod URI. An op that throws backs off and
     * stays queued; the ops behind it still get their turn this pass.
     */
    suspend fun drain(
        webId: String,
        execute: suspend (OpType, OutboxPayload) -> Unit,
    ): DrainResult {
        val now = System.currentTimeMillis()
        var succeeded = 0
        var failed = 0
        for (op in dao.due(webId, now)) {
            val type = runCatching { OpType.valueOf(op.type) }.getOrNull()
            if (type == null) {
                // An op this build no longer knows. Dropping it is better than blocking the queue.
                dao.deleteById(op.id)
                continue
            }
            val payload = runCatching {
                json.decodeFromString(OutboxPayload.serializer(), op.payload)
            }.getOrNull()
            if (payload == null) {
                dao.deleteById(op.id)
                continue
            }
            val outcome = runCatching { execute(type, payload) }
            if (outcome.isSuccess) {
                dao.deleteById(op.id)
                succeeded++
            } else {
                failed++
                val cause = outcome.exceptionOrNull()
                val attempts = op.attempts + 1
                val error = cause?.toAppError()
                // A permanent refusal is worth surfacing, but keep it queued: a grant the user
                // restores later makes the very same op succeed.
                dao.update(
                    op.copy(
                        status = OpStatus.FAILED,
                        attempts = attempts,
                        nextRetryAt = System.currentTimeMillis() + backoffMillis(attempts),
                        lastError = error?.let { it::class.simpleName } ?: cause?.message,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            }
        }
        val remaining = dao.allFor(webId).count { it.status != OpStatus.RUNNING }
        return DrainResult(succeeded, failed, remaining)
    }

    /** How many writes are still waiting, for the "not synced yet" badge. */
    fun observePendingCount(webId: String): Flow<Int> = dao.observePendingCount(webId)

    suspend fun pendingCount(webId: String): Int = dao.allFor(webId).size

    suspend fun webIdsWithWork(): List<String> = dao.webIdsWithWork()

    /** Clears the queue, e.g. when the user disconnects the account. */
    suspend fun clear(webId: String) = dao.deleteAllFor(webId)

    /** Puts every failed op back in line right away, for a manual "retry now". */
    suspend fun retryAllNow(webId: String) {
        val now = System.currentTimeMillis()
        dao.allFor(webId)
            .filter { it.status == OpStatus.FAILED }
            .forEach { dao.update(it.copy(status = OpStatus.PENDING, nextRetryAt = 0, updatedAt = now)) }
    }

    private suspend fun isAlreadyQueued(
        webId: String,
        type: OpType,
        payload: OutboxPayload,
    ): Boolean {
        val subject = payload.subject()
        return dao.allFor(webId).any { queued ->
            queued.type == type.name &&
                runCatching {
                    json.decodeFromString(OutboxPayload.serializer(), queued.payload).subject()
                }.getOrNull() == subject
        }
    }

    /** Ops whose effect is "make the pod match the local row" can safely collapse into one. */
    private fun OpType.isCollapsible(): Boolean = when (this) {
        OpType.UPDATE_CONTACT,
        OpType.SET_PHOTO,
        OpType.REMOVE_PHOTO,
        OpType.RENAME_BOOK,
        -> true

        else -> false
    }

    /** 30s, doubling to an hour: long enough to ride out a flaky link, short enough to feel live. */
    private fun backoffMillis(attempts: Int): Long =
        minOf(30_000L * (1L shl minOf(attempts, 7)), 3_600_000L)
}

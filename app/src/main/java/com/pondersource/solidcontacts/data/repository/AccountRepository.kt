package com.pondersource.solidcontacts.data.repository

import com.erfangholami.androidsolidservices.client.sdk.SolidSignInClient
import com.pondersource.solidcontacts.data.local.prefs.SortOrder
import com.pondersource.solidcontacts.data.local.prefs.ThemeMode
import com.pondersource.solidcontacts.data.local.prefs.UserPreferences
import com.pondersource.solidcontacts.data.local.prefs.UserSettings
import com.pondersource.solidcontacts.sync.SyncScheduler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Whether the app currently holds a usable grant. */
sealed interface AccountState {
    data object Checking : AccountState
    data object SignedOut : AccountState
    data class SignedIn(val webId: String) : AccountState
}

/**
 * The signed-in Solid account.
 *
 * The host app owns the login and the tokens; this app only remembers which WebID the user let
 * it act as, and asks the host whether that grant still stands.
 */
interface AccountRepository {

    val settings: Flow<UserSettings>

    /** Emits `true` once the host app's auth service is bound. */
    fun connectionState(): Flow<Boolean>

    /** Asks the host whether the stored grant is still good. */
    suspend fun currentState(): AccountState

    /** Records the WebID the user granted and starts background sync for it. */
    suspend fun onAuthorized(webId: String)

    /** Gives the grant back and forgets everything cached for the account. */
    suspend fun disconnect(): Result<Unit>

    suspend fun setSortOrder(order: SortOrder)

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setDynamicColor(enabled: Boolean)

    suspend fun setSyncOnlyOnWifi(enabled: Boolean)

    suspend fun setDefaultBookId(bookId: String)
}

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val signInClient: SolidSignInClient,
    private val prefs: UserPreferences,
    private val contactsRepository: ContactsRepository,
    private val scheduler: SyncScheduler,
    private val io: CoroutineDispatcher,
) : AccountRepository {

    override val settings: Flow<UserSettings> = prefs.settings

    override fun connectionState(): Flow<Boolean> = signInClient.authServiceConnectionState()

    override suspend fun currentState(): AccountState = withContext(io) {
        val webId = prefs.webId()
        if (webId.isEmpty()) return@withContext AccountState.SignedOut

        // The sign-in client fails fast when the service is not bound yet, so wait for it.
        runCatching { signInClient.authServiceConnectionState().first { it } }

        val account = runCatching { signInClient.getAccount(webId) }.getOrNull()
        if (account == null) AccountState.SignedOut else AccountState.SignedIn(webId)
    }

    override suspend fun onAuthorized(webId: String) = withContext(io) {
        prefs.setWebId(webId)
        scheduler.schedulePeriodicSync(prefs.current().syncOnlyOnWifi)
        scheduler.requestRefresh()
    }

    override suspend fun disconnect(): Result<Unit> = withContext(io) {
        runCatching {
            val webId = prefs.webId()
            if (webId.isNotEmpty()) {
                // Drop the local copy first: whatever the host says next, the data is off the
                // device, which is what the user asked for.
                contactsRepository.clearLocalData()
                scheduler.cancelAll()
                val released = signInClient.disconnectFromSolid(webId)
                prefs.clearAccount()
                check(released) { "The Solid host app did not release the grant." }
            }
        }
    }

    override suspend fun setSortOrder(order: SortOrder) = prefs.setSortOrder(order)

    override suspend fun setThemeMode(mode: ThemeMode) = prefs.setThemeMode(mode)

    override suspend fun setDynamicColor(enabled: Boolean) = prefs.setDynamicColor(enabled)

    override suspend fun setSyncOnlyOnWifi(enabled: Boolean) = withContext(io) {
        prefs.setSyncOnlyOnWifi(enabled)
        if (prefs.webId().isNotEmpty()) scheduler.schedulePeriodicSync(enabled)
    }

    override suspend fun setDefaultBookId(bookId: String) = prefs.setDefaultBookId(bookId)
}

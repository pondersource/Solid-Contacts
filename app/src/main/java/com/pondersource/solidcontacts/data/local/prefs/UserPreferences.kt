package com.pondersource.solidcontacts.data.local.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** How a contact list orders and labels its rows. */
enum class SortOrder { FIRST_NAME, LAST_NAME }

/** Which theme the app paints itself in. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Everything the app remembers between launches. */
data class UserSettings(
    val webId: String = "",
    val sortOrder: SortOrder = SortOrder.FIRST_NAME,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val defaultBookId: String = "",
    val lastSyncAt: Long = 0,
    val syncOnlyOnWifi: Boolean = false,
) {
    val isSignedIn: Boolean get() = webId.isNotEmpty()
}

/**
 * The app's settings, backed by DataStore.
 *
 * Everything is a [Flow]; nothing here blocks a thread. The old implementation read the WebID
 * with `runBlocking` on whatever thread asked, which stalled the first frame of every screen.
 */
@Singleton
class UserPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    private object Keys {
        val WEB_ID = stringPreferencesKey("granted_webid")
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val DEFAULT_BOOK = stringPreferencesKey("default_book_id")
        val LAST_SYNC = longPreferencesKey("last_sync_at")
        val WIFI_ONLY = booleanPreferencesKey("sync_wifi_only")
    }

    val settings: Flow<UserSettings> = dataStore.data.map { prefs ->
        UserSettings(
            webId = prefs[Keys.WEB_ID].orEmpty(),
            sortOrder = prefs[Keys.SORT_ORDER]
                ?.let { runCatching { SortOrder.valueOf(it) }.getOrNull() }
                ?: SortOrder.FIRST_NAME,
            themeMode = prefs[Keys.THEME_MODE]
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,
            defaultBookId = prefs[Keys.DEFAULT_BOOK].orEmpty(),
            lastSyncAt = prefs[Keys.LAST_SYNC] ?: 0L,
            syncOnlyOnWifi = prefs[Keys.WIFI_ONLY] ?: false,
        )
    }

    val webIdFlow: Flow<String> = settings.map { it.webId }

    suspend fun webId(): String = settings.first().webId

    suspend fun current(): UserSettings = settings.first()

    suspend fun setWebId(webId: String) = edit { it[Keys.WEB_ID] = webId }

    suspend fun setSortOrder(order: SortOrder) = edit { it[Keys.SORT_ORDER] = order.name }

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.THEME_MODE] = mode.name }

    suspend fun setDynamicColor(enabled: Boolean) = edit { it[Keys.DYNAMIC_COLOR] = enabled }

    suspend fun setDefaultBookId(bookId: String) = edit { it[Keys.DEFAULT_BOOK] = bookId }

    suspend fun setLastSyncAt(millis: Long) = edit { it[Keys.LAST_SYNC] = millis }

    suspend fun setSyncOnlyOnWifi(enabled: Boolean) = edit { it[Keys.WIFI_ONLY] = enabled }

    /** Forgets the account, keeping device-level preferences such as the theme. */
    suspend fun clearAccount() = edit {
        it.remove(Keys.WEB_ID)
        it.remove(Keys.DEFAULT_BOOK)
        it.remove(Keys.LAST_SYNC)
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        dataStore.edit(block)
    }
}

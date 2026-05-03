package com.pondersource.solidcontacts.repository.user

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pondersource.solidcontacts.repository.user.UserRepositoryImplementation.PreferencesKeys.GRANTED_WEBID
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class UserRepositoryImplementation @Inject constructor(
    val dataStore: DataStore<Preferences>
): UserRepository {

    private object PreferencesKeys {
        val GRANTED_WEBID = stringPreferencesKey("granted_webid")
    }

    override fun getGrantedWebId(): String {
        return runBlocking {
            withContext(Dispatchers.IO) {
                val getting = dataStore.data.map {
                    it[GRANTED_WEBID] ?: ""
                }.first()
                Log.d("UserRepo", "Getting webid: $getting")
                getting
            }
        }
    }

    override fun setGrantedWebId(webId: String) {
        runBlocking {
            withContext(Dispatchers.IO) {
                dataStore.edit {
                    it[GRANTED_WEBID] = webId
                    Log.d("UserRepo", "Setting webid: $webId")
                }
            }
        }
    }
}
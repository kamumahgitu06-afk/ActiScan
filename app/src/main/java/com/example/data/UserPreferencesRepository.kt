package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "actiscan_preferences"
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val DISCLAIMER_ACCEPTED = booleanPreferencesKey("disclaimer_accepted")
        val SHOW_ROOT_STATUS = booleanPreferencesKey("show_root_status")
    }

    val disclaimerAccepted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.DISCLAIMER_ACCEPTED] ?: false
    }

    val showRootStatus: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SHOW_ROOT_STATUS] ?: true
    }

    suspend fun setDisclaimerAccepted(accepted: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DISCLAIMER_ACCEPTED] = accepted
        }
    }

    suspend fun setShowRootStatus(show: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SHOW_ROOT_STATUS] = show
        }
    }
}

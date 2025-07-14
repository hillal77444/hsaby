package com.hillal.acc.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {
    companion object {
        private val SESSION_NAME = stringPreferencesKey("session_name")
        private val SESSION_EXPIRY = stringPreferencesKey("session_expiry")
    }

    suspend fun saveSessionInfo(sessionName: String, sessionExpiry: String?) {
        context.dataStore.edit { preferences ->
            preferences[SESSION_NAME] = sessionName
            preferences[SESSION_EXPIRY] = sessionExpiry ?: ""
        }
    }

    val sessionName: Flow<String?>
        get() = context.dataStore.data.map { it[SESSION_NAME] }

    val sessionExpiry: Flow<String?>
        get() = context.dataStore.data.map { it[SESSION_EXPIRY] }
} 
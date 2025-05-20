package com.hsaby.accounting.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = Constants.PREF_NAME)

class PreferencesManager(private val context: Context) {
    
    val token: Flow<String?>
        get() = context.dataStore.data.map { preferences ->
            preferences[stringPreferencesKey(Constants.PREF_TOKEN)]
        }
    
    val userId: Flow<String?>
        get() = context.dataStore.data.map { preferences ->
            preferences[stringPreferencesKey(Constants.PREF_USER_ID)]
        }
    
    val username: Flow<String?>
        get() = context.dataStore.data.map { preferences ->
            preferences[stringPreferencesKey(Constants.PREF_USERNAME)]
        }
    
    val rememberMe: Flow<Boolean>
        get() = context.dataStore.data.map { preferences ->
            preferences[booleanPreferencesKey(Constants.PREF_REMEMBER_ME)] ?: false
        }
    
    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey(Constants.PREF_TOKEN)] = token
        }
    }
    
    suspend fun saveUserId(userId: String) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey(Constants.PREF_USER_ID)] = userId
        }
    }
    
    suspend fun saveUsername(username: String) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey(Constants.PREF_USERNAME)] = username
        }
    }
    
    suspend fun saveRememberMe(rememberMe: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[booleanPreferencesKey(Constants.PREF_REMEMBER_ME)] = rememberMe
        }
    }
    
    suspend fun clearPreferences() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
} 
package com.hillal.acc.util

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(private val context: Context) {
    companion object {
        private const val PREF_NAME = "user_preferences"
        private const val KEY_SESSION_NAME = "session_name"
        private const val KEY_SESSION_EXPIRY = "session_expiry"
    }

    private val preferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveSessionInfo(sessionName: String, sessionExpiry: String?) {
        preferences.edit()
            .putString(KEY_SESSION_NAME, sessionName)
            .putString(KEY_SESSION_EXPIRY, sessionExpiry)
            .apply()
    }

    fun getSessionName(): String? = preferences.getString(KEY_SESSION_NAME, null)
    fun getSessionExpiry(): String? = preferences.getString(KEY_SESSION_EXPIRY, null)
} 
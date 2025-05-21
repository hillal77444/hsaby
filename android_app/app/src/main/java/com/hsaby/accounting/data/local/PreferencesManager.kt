package com.hsaby.accounting.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var isFirstLaunch: Boolean
        get() = sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()

    var isLoggedIn: Boolean
        get() = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = sharedPreferences.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    var userId: String?
        get() = sharedPreferences.getString(KEY_USER_ID, null)
        set(value) = sharedPreferences.edit().putString(KEY_USER_ID, value).apply()

    var userToken: String?
        get() = sharedPreferences.getString(KEY_USER_TOKEN, null)
        set(value) = sharedPreferences.edit().putString(KEY_USER_TOKEN, value).apply()

    var userPhone: String?
        get() = sharedPreferences.getString(KEY_USER_PHONE, null)
        set(value) = sharedPreferences.edit().putString(KEY_USER_PHONE, value).apply()

    var userPassword: String?
        get() = sharedPreferences.getString(KEY_USER_PASSWORD, null)
        set(value) = sharedPreferences.edit().putString(KEY_USER_PASSWORD, value).apply()

    fun saveLoginCredentials(phone: String, password: String, userId: String, token: String) {
        sharedPreferences.edit().apply {
            putString(KEY_USER_PHONE, phone)
            putString(KEY_USER_PASSWORD, password)
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_TOKEN, token)
            putBoolean(KEY_IS_LOGGED_IN, true)
            putBoolean(KEY_FIRST_LAUNCH, false)
            apply()
        }
    }

    fun clearLoginCredentials() {
        sharedPreferences.edit().apply {
            remove(KEY_USER_PHONE)
            remove(KEY_USER_PASSWORD)
            remove(KEY_USER_ID)
            remove(KEY_USER_TOKEN)
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }
    }

    fun saveToken(token: String) {
        sharedPreferences.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? {
        return sharedPreferences.getString(KEY_TOKEN, null)
    }

    fun saveRefreshToken(token: String) {
        sharedPreferences.edit().putString(KEY_REFRESH_TOKEN, token).apply()
    }

    fun getRefreshToken(): String? {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    }

    fun saveUsername(username: String) {
        sharedPreferences.edit().putString(KEY_USERNAME, username).apply()
    }

    fun getUsername(): String? {
        return sharedPreferences.getString(KEY_USERNAME, null)
    }

    fun clearAuthData() {
        sharedPreferences.edit().clear().apply()
    }

    companion object {
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_TOKEN = "user_token"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_USER_PASSWORD = "user_password"
        private const val KEY_TOKEN = "token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USERNAME = "username"
    }
} 
package com.hsaby.accounting.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
        PREF_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    val isLoggedIn: Flow<Boolean>
        get() = getToken().map { it != null }

    fun getToken(): Flow<String?> = flow {
        emit(sharedPreferences.getString(KEY_TOKEN, null))
    }

    fun getRefreshToken(): Flow<String?> = flow {
        emit(sharedPreferences.getString(KEY_REFRESH_TOKEN, null))
    }

    fun getUserId(): Flow<String?> = flow {
        emit(sharedPreferences.getString(KEY_USER_ID, null))
    }

    fun getUsername(): Flow<String?> = flow {
        emit(sharedPreferences.getString(KEY_USERNAME, null))
    }

    private fun getString(key: String): Flow<String?> {
        return kotlinx.coroutines.flow.Flow { emit(sharedPreferences.getString(key, null)) }
    }

    suspend fun saveLoginCredentials(
        phone: String,
        password: String,
        userId: String,
        token: String
    ) {
        sharedPreferences.edit().apply {
            putString(KEY_PHONE, phone)
            putString(KEY_PASSWORD, password)
            putString(KEY_USER_ID, userId)
            putString(KEY_TOKEN, token)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    suspend fun saveToken(token: String) {
        sharedPreferences.edit().putString(KEY_TOKEN, token).apply()
    }

    suspend fun saveRefreshToken(refreshToken: String) {
        sharedPreferences.edit().putString(KEY_REFRESH_TOKEN, refreshToken).apply()
    }

    suspend fun saveUserId(userId: String) {
        sharedPreferences.edit().putString(KEY_USER_ID, userId).apply()
    }

    suspend fun clearLoginCredentials() {
        sharedPreferences.edit().apply {
            remove(KEY_PHONE)
            remove(KEY_PASSWORD)
            remove(KEY_USER_ID)
            remove(KEY_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }
    }

    companion object {
        private const val PREF_NAME = "accounting_prefs"
        private const val KEY_TOKEN = "token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_PHONE = "phone"
        private const val KEY_PASSWORD = "password"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }
} 
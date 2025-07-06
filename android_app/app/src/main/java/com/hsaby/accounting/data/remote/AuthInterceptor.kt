package com.hsaby.accounting.data.remote

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hsaby.accounting.util.Constants
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

private val Context.dataStore by preferencesDataStore(name = Constants.PREF_NAME)

class AuthInterceptor(private val context: Context) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking {
            context.dataStore.data.first()[stringPreferencesKey(Constants.PREF_TOKEN)]
        }
        
        val request = chain.request().newBuilder().apply {
            token?.let {
                addHeader("Authorization", "Bearer $it")
            }
        }.build()
        
        return chain.proceed(request)
    }
} 
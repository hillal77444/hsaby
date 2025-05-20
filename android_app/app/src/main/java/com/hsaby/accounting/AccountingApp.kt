package com.hsaby.accounting

import android.app.Application
import androidx.room.Room
import androidx.work.*
import com.hsaby.accounting.data.local.AppDatabase
import com.hsaby.accounting.data.remote.ApiService
import com.hsaby.accounting.data.repository.AccountRepository
import com.hsaby.accounting.data.repository.TransactionRepository
import com.hsaby.accounting.data.repository.UserRepository
import com.hsaby.accounting.sync.SyncManager
import com.hsaby.accounting.sync.SyncWorker
import com.hsaby.accounting.util.Constants
import com.hsaby.accounting.util.PreferencesManager
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class AccountingApp : Application() {
    
    @Inject
    lateinit var syncManager: SyncManager

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var database: AppDatabase
    
    @Inject
    lateinit var apiService: ApiService
    
    @Inject
    lateinit var userRepository: UserRepository
    
    @Inject
    lateinit var accountRepository: AccountRepository
    
    @Inject
    lateinit var transactionRepository: TransactionRepository
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        setupPeriodicSync()
    }
    
    private fun setupPeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "sync_work",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
    
    companion object {
        lateinit var instance: AccountingApp
            private set
    }
} 
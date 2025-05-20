package com.hsaby.accounting

import android.app.Application
import androidx.room.Room
import com.hsaby.accounting.data.local.AppDatabase
import com.hsaby.accounting.data.remote.ApiService
import com.hsaby.accounting.data.repository.AccountRepository
import com.hsaby.accounting.data.repository.TransactionRepository
import com.hsaby.accounting.data.repository.UserRepository
import com.hsaby.accounting.util.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AccountingApp : Application() {
    
    lateinit var database: AppDatabase
    lateinit var apiService: ApiService
    
    lateinit var userRepository: UserRepository
    lateinit var accountRepository: AccountRepository
    lateinit var transactionRepository: TransactionRepository
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Room Database
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "accounting_db"
        ).build()
        
        // Initialize Retrofit
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        apiService = retrofit.create(ApiService::class.java)
        
        // Initialize Repositories
        userRepository = UserRepository(database.userDao(), apiService)
        accountRepository = AccountRepository(database.accountDao(), apiService)
        transactionRepository = TransactionRepository(database.transactionDao(), apiService)
    }
    
    companion object {
        lateinit var instance: AccountingApp
            private set
    }
} 
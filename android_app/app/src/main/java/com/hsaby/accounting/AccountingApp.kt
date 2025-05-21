package com.hsaby.accounting

import android.app.Application
import com.hsaby.accounting.data.remote.ApiService
import com.hsaby.accounting.util.PreferencesManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AccountingApp : Application() {
    
    @Inject
    lateinit var preferencesManager: PreferencesManager
    
    @Inject
    lateinit var apiService: ApiService
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    
    companion object {
        lateinit var instance: AccountingApp
            private set
    }
} 
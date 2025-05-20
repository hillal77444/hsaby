package com.hsaby.accounting.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hsaby.accounting.data.local.AppDatabase
import com.hsaby.accounting.data.remote.ApiService
import com.hsaby.accounting.util.PreferencesManager

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val syncManager: SyncManager by lazy {
        val database = AppDatabase.getInstance(context)
        val apiService = ApiService.create()
        val preferencesManager = PreferencesManager(context)
        
        SyncManager(context, apiService, database, preferencesManager)
    }

    override suspend fun doWork(): Result {
        return try {
            // مزامنة البيانات الجديدة فقط
            val syncResult = syncManager.syncNewData()
            
            // تنظيف البيانات القديمة
            syncManager.cleanupOldData()
            
            if (syncResult) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
} 
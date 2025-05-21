package com.hsaby.accounting.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hsaby.accounting.data.remote.Result as ApiResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SyncWorker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workerParams: WorkerParameters,
    private val syncManager: SyncManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = try {
        when (val result = syncManager.syncNow()) {
            is ApiResult.Success -> Result.success()
            is ApiResult.Error -> Result.retry()
            else -> Result.retry()
        }
    } catch (e: Exception) {
        Result.retry()
    }
} 
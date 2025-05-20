package com.hsaby.accounting.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hsaby.accounting.data.remote.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SyncWorker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workerParams: WorkerParameters,
    private val syncManager: SyncManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): androidx.work.Result = try {
        when (val result = syncManager.syncNow()) {
            is Result.Success -> androidx.work.Result.success()
            is Result.Error -> androidx.work.Result.retry()
            else -> androidx.work.Result.retry()
        }
    } catch (e: Exception) {
        androidx.work.Result.retry()
    }
} 
package com.hsaby.accounting.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.*
import com.hsaby.accounting.data.local.AppDatabase
import com.hsaby.accounting.data.model.*
import com.hsaby.accounting.data.remote.ApiService
import com.hsaby.accounting.data.remote.Result
import com.hsaby.accounting.data.repository.AccountRepository
import com.hsaby.accounting.data.repository.TransactionRepository
import com.hsaby.accounting.util.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ApiService,
    private val database: AppDatabase,
    private val preferencesManager: PreferencesManager,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) {
    companion object {
        private const val SYNC_WORK_NAME = "sync_work"
        private const val SYNC_INTERVAL_HOURS = 1L
        private const val BATCH_SIZE = 50
        private const val MIN_SYNC_INTERVAL = 5 * 60 * 1000L // 5 دقائق
        private const val MAX_RETRY_COUNT = 3 // عدد محاولات إعادة المزامنة
    }

    private val isSyncing = AtomicBoolean(false)
    private var lastSyncAttempt = 0L
    private val localIdCounter = AtomicLong(-1)
    private var retryCount = 0

    // حالة المزامنة
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    // إحصائيات المزامنة
    private val _syncStats = MutableStateFlow(SyncStats())
    val syncStats: StateFlow<SyncStats> = _syncStats

    private val workManager = WorkManager.getInstance(context)

    // توليد معرف محلي مؤقت
    private fun generateLocalId(): Long {
        return localIdCounter.getAndDecrement()
    }

    // التحقق من حالة الاتصال بالإنترنت
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // بدء المزامنة التلقائية
    fun startAutoSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            SYNC_INTERVAL_HOURS,
            TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag("auto_sync")
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
    }

    // إيقاف المزامنة التلقائية
    fun stopAutoSync() {
        WorkManager.getInstance(context)
            .cancelUniqueWork(SYNC_WORK_NAME)
    }

    // مزامنة يدوية
    suspend fun manualSync() = withContext(Dispatchers.IO) {
        if (!canSync()) return@withContext false
        
        try {
            isSyncing.set(true)
            lastSyncAttempt = System.currentTimeMillis()
            _syncState.value = SyncState.Syncing
            _syncStats.value = SyncStats()
            
            val userId = preferencesManager.getUserId() ?: return@withContext false
            
            // مزامنة الحسابات
            syncAccounts(userId)
            
            // مزامنة المعاملات
            syncTransactions(userId)
            
            // تحديث وقت آخر مزامنة
            preferencesManager.setLastSyncTime(System.currentTimeMillis())
            
            // إعادة تعيين عداد المحاولات
            retryCount = 0
            
            _syncState.value = SyncState.Success(_syncStats.value)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            _syncState.value = SyncState.Error(e.message ?: "Unknown error")
            
            // إعادة المحاولة في حالة الفشل
            if (retryCount < MAX_RETRY_COUNT) {
                retryCount++
                return@withContext manualSync()
            }
            
            false
        } finally {
            isSyncing.set(false)
        }
    }

    // التحقق من إمكانية المزامنة
    private fun canSync(): Boolean {
        if (isSyncing.get()) {
            _syncState.value = SyncState.Error("Sync already in progress")
            return false
        }
        
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSyncAttempt < MIN_SYNC_INTERVAL) {
            _syncState.value = SyncState.Error("Please wait before syncing again")
            return false
        }
        
        if (!isNetworkAvailable()) {
            _syncState.value = SyncState.Error("No internet connection")
            return false
        }
        
        return true
    }

    // مزامنة الحسابات
    private suspend fun syncAccounts(userId: String) = withContext(Dispatchers.IO) {
        val lastSyncTime = preferencesManager.getLastSyncTime()
        var currentPage = 1
        var hasMorePages = true
        
        try {
            while (hasMorePages) {
                _syncState.value = SyncState.SyncingAccounts(currentPage)
                
                // جلب الحسابات المحدثة من الخادم
                val response = apiService.getAccounts(userId, lastSyncTime, currentPage, BATCH_SIZE)
                if (!response.isSuccessful) {
                    throw Exception("Failed to fetch accounts: ${response.code()}")
                }
                
                val paginatedResponse = response.body() ?: throw Exception("Empty response")
                
                // التحقق من تعارض البيانات
                paginatedResponse.data.forEach { serverAccount ->
                    val localAccount = database.accountDao().getAccountByServerId(serverAccount.serverId)
                    if (localAccount != null && localAccount.lastModified > serverAccount.lastModified) {
                        // إذا كانت البيانات المحلية أحدث، قم برفعها إلى الخادم
                        apiService.updateAccount(serverAccount.serverId, localAccount)
                    } else {
                        // تحديث البيانات المحلية بالبيانات من الخادم
                        database.accountDao().updateServerId(serverAccount.id, serverAccount.serverId)
                        database.accountDao().updateAccount(serverAccount)
                    }
                }
                
                // تحديث الإحصائيات
                _syncStats.value = _syncStats.value.copy(
                    accountsSynced = _syncStats.value.accountsSynced + paginatedResponse.data.size
                )
                
                // التحقق من وجود صفحات إضافية
                hasMorePages = currentPage < paginatedResponse.totalPages
                currentPage++
            }
            
            // جلب الحسابات المحلية غير المتزامنة
            val localAccounts = database.accountDao().getUnsyncedAccounts()
            
            // رفع الحسابات المحلية إلى الخادم بشكل مجمع
            localAccounts.chunked(BATCH_SIZE).forEach { batch ->
                try {
                    // تعيين server_id مؤقت لكل حساب جديد
                    batch.forEach { account ->
                        if (account.serverId == null) {
                            account.serverId = generateLocalId()
                        }
                    }
                    
                    val response = apiService.updateAccounts(batch)
                    if (response.isSuccessful) {
                        response.body()?.forEach { account ->
                            // تحديث server_id في قاعدة البيانات المحلية بالقيمة الفعلية من الخادم
                            database.accountDao().updateServerId(account.id, account.serverId)
                            _syncStats.value = _syncStats.value.copy(
                                accountsUploaded = _syncStats.value.accountsUploaded + 1
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    _syncStats.value = _syncStats.value.copy(
                        accountsFailed = _syncStats.value.accountsFailed + batch.size
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    // مزامنة المعاملات
    private suspend fun syncTransactions(userId: String) = withContext(Dispatchers.IO) {
        val lastSyncTime = preferencesManager.getLastSyncTime()
        var currentPage = 1
        var hasMorePages = true
        
        try {
            while (hasMorePages) {
                _syncState.value = SyncState.SyncingTransactions(currentPage)
                
                // جلب المعاملات المحدثة من الخادم
                val response = apiService.getTransactions(userId, lastSyncTime, currentPage, BATCH_SIZE)
                if (!response.isSuccessful) {
                    throw Exception("Failed to fetch transactions: ${response.code()}")
                }
                
                val paginatedResponse = response.body() ?: throw Exception("Empty response")
                
                // التحقق من تعارض البيانات
                paginatedResponse.data.forEach { serverTransaction ->
                    val localTransaction = database.transactionDao().getTransactionByServerId(serverTransaction.serverId)
                    if (localTransaction != null && localTransaction.lastModified > serverTransaction.lastModified) {
                        // إذا كانت البيانات المحلية أحدث، قم برفعها إلى الخادم
                        apiService.updateTransaction(serverTransaction.serverId, localTransaction)
                    } else {
                        // تحديث البيانات المحلية بالبيانات من الخادم
                        database.transactionDao().updateServerId(serverTransaction.id, serverTransaction.serverId)
                        database.transactionDao().updateTransaction(serverTransaction)
                    }
                }
                
                // تحديث الإحصائيات
                _syncStats.value = _syncStats.value.copy(
                    transactionsSynced = _syncStats.value.transactionsSynced + paginatedResponse.data.size
                )
                
                // التحقق من وجود صفحات إضافية
                hasMorePages = currentPage < paginatedResponse.totalPages
                currentPage++
            }
            
            // جلب المعاملات المحلية غير المتزامنة
            val localTransactions = database.transactionDao().getUnsyncedTransactions()
            
            // رفع المعاملات المحلية إلى الخادم بشكل مجمع
            localTransactions.chunked(BATCH_SIZE).forEach { batch ->
                try {
                    // تعيين server_id مؤقت لكل معاملة جديدة
                    batch.forEach { transaction ->
                        if (transaction.serverId == null) {
                            transaction.serverId = generateLocalId()
                        }
                    }
                    
                    val response = apiService.updateTransactions(batch)
                    if (response.isSuccessful) {
                        response.body()?.forEach { transaction ->
                            // تحديث server_id في قاعدة البيانات المحلية بالقيمة الفعلية من الخادم
                            database.transactionDao().updateServerId(transaction.id, transaction.serverId)
                            _syncStats.value = _syncStats.value.copy(
                                transactionsUploaded = _syncStats.value.transactionsUploaded + 1
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    _syncStats.value = _syncStats.value.copy(
                        transactionsFailed = _syncStats.value.transactionsFailed + batch.size
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    // مزامنة البيانات الجديدة فقط
    suspend fun syncNewData() = withContext(Dispatchers.IO) {
        if (!canSync()) return@withContext false
        
        try {
            isSyncing.set(true)
            lastSyncAttempt = System.currentTimeMillis()
            _syncState.value = SyncState.Syncing
            _syncStats.value = SyncStats()
            
            val userId = preferencesManager.getUserId() ?: return@withContext false
            val lastSyncTime = preferencesManager.getLastSyncTime()
            
            // جلب البيانات الجديدة فقط
            val response = apiService.getNewData(userId, lastSyncTime)
            if (!response.isSuccessful) {
                throw Exception("Failed to fetch new data: ${response.code()}")
            }
            
            val newData = response.body() ?: throw Exception("Empty response")
            
            // التحقق من تعارض البيانات وتحديثها
            newData.accounts.forEach { account ->
                val localAccount = database.accountDao().getAccountByServerId(account.serverId)
                if (localAccount != null && localAccount.lastModified > account.lastModified) {
                    apiService.updateAccount(account.serverId, localAccount)
                } else {
                    database.accountDao().updateServerId(account.id, account.serverId)
                    database.accountDao().updateAccount(account)
                }
            }
            
            newData.transactions.forEach { transaction ->
                val localTransaction = database.transactionDao().getTransactionByServerId(transaction.serverId)
                if (localTransaction != null && localTransaction.lastModified > transaction.lastModified) {
                    apiService.updateTransaction(transaction.serverId, localTransaction)
                } else {
                    database.transactionDao().updateServerId(transaction.id, transaction.serverId)
                    database.transactionDao().updateTransaction(transaction)
                }
            }
            
            // تحديث الإحصائيات
            _syncStats.value = _syncStats.value.copy(
                accountsSynced = newData.accounts.size,
                transactionsSynced = newData.transactions.size
            )
            
            // تحديث وقت آخر مزامنة
            preferencesManager.setLastSyncTime(System.currentTimeMillis())
            
            _syncState.value = SyncState.Success(_syncStats.value)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            _syncState.value = SyncState.Error(e.message ?: "Unknown error")
            false
        } finally {
            isSyncing.set(false)
        }
    }

    fun startPeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "periodic_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    fun stopPeriodicSync() {
        workManager.cancelUniqueWork("periodic_sync")
    }

    suspend fun syncNow() = withContext(Dispatchers.IO) {
        try {
            val userId = accountRepository.getUserId() ?: return@withContext Result.Error("لم يتم تسجيل الدخول")
            val lastSyncTime = accountRepository.getLastSyncTime()

            // Sync from server
            val syncResponse = apiService.sync(SyncRequest(lastSyncTime))
            if (!syncResponse.isSuccessful) {
                return@withContext Result.Error("فشل المزامنة: ${syncResponse.errorBody()?.string()}")
            }

            val response = syncResponse.body() ?: return@withContext Result.Error("لا توجد بيانات للمزامنة")

            // Update accounts
            response.accounts.forEach { account ->
                val existingAccount = accountRepository.getAccountByServerId(account.serverId)
                if (existingAccount != null) {
                    accountRepository.updateAccount(account)
                } else {
                    accountRepository.insertAccount(account)
                }
            }

            // Update transactions
            response.transactions.forEach { transaction ->
                val existingTransaction = transactionRepository.getTransactionByServerId(transaction.serverId)
                if (existingTransaction != null) {
                    transactionRepository.updateTransaction(transaction)
                } else {
                    transactionRepository.insertTransaction(transaction)
                }
            }

            // Sync local changes to server
            val unsyncedAccounts = accountRepository.getUnsyncedAccounts()
            val unsyncedTransactions = transactionRepository.getUnsyncedTransactions()

            if (unsyncedAccounts.isNotEmpty() || unsyncedTransactions.isNotEmpty()) {
                val changesResponse = apiService.syncChanges(
                    SyncChangesRequest(unsyncedAccounts, unsyncedTransactions)
                )

                if (changesResponse.isSuccessful) {
                    // Update server IDs for synced items
                    unsyncedAccounts.forEach { account ->
                        accountRepository.updateServerId(account.id, account.serverId)
                    }
                    unsyncedTransactions.forEach { transaction ->
                        transactionRepository.updateServerId(transaction.id, transaction.serverId)
                    }
                }
            }

            // Update last sync time
            accountRepository.setLastSyncTime(System.currentTimeMillis())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "حدث خطأ أثناء المزامنة")
        }
    }
}

// حالة المزامنة
sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class SyncingAccounts(val page: Int) : SyncState()
    data class SyncingTransactions(val page: Int) : SyncState()
    data class Success(val stats: SyncStats) : SyncState()
    data class Error(val message: String) : SyncState()
}

// إحصائيات المزامنة
data class SyncStats(
    val accountsSynced: Int = 0,
    val accountsUploaded: Int = 0,
    val accountsFailed: Int = 0,
    val transactionsSynced: Int = 0,
    val transactionsUploaded: Int = 0,
    val transactionsFailed: Int = 0
) 
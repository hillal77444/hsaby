package com.hsaby.accounting.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.*
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ApiService,
    private val preferencesManager: PreferencesManager,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) {
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    private val _syncStats = MutableStateFlow(SyncStats())
    val syncStats: StateFlow<SyncStats> = _syncStats

    private val workManager = WorkManager.getInstance(context)

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
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
        if (!isNetworkAvailable()) {
            return@withContext Result.Error("لا يوجد اتصال بالإنترنت")
        }

        try {
            _syncState.value = SyncState.Syncing
            _syncStats.value = SyncStats()

            val userId = preferencesManager.getUserId() ?: return@withContext Result.Error("لم يتم تسجيل الدخول")
            val lastSyncTime = preferencesManager.getLastSyncTime() ?: 0L

            // Sync from server
            val syncResponse = apiService.sync(SyncRequest(lastSyncTime))
            if (!syncResponse.isSuccessful) {
                return@withContext Result.Error("فشل المزامنة: ${syncResponse.errorBody()?.string()}")
            }

            val response = syncResponse.body() ?: return@withContext Result.Error("لا توجد بيانات للمزامنة")

            // Update accounts
            response.accounts.forEach { account ->
                val existingAccount = accountRepository.getAccountByServerId(account.serverId ?: 0L)
                if (existingAccount != null) {
                    accountRepository.updateAccount(existingAccount.copy(
                        accountName = account.name,
                        balance = account.balance,
                        currency = account.currency,
                        phoneNumber = account.phoneNumber,
                        notes = account.notes,
                        isDebtor = account.isDebtor,
                        whatsappEnabled = account.whatsappEnabled,
                        lastSync = System.currentTimeMillis()
                    ))
                } else {
                    accountRepository.insertAccount(com.hsaby.accounting.data.local.entity.AccountEntity(
                        id = account.id,
                        serverId = account.serverId,
                        accountName = account.name,
                        balance = account.balance,
                        currency = account.currency,
                        phoneNumber = account.phoneNumber,
                        notes = account.notes,
                        isDebtor = account.isDebtor,
                        whatsappEnabled = account.whatsappEnabled,
                        userId = userId,
                        lastSync = System.currentTimeMillis()
                    ))
                }
                _syncStats.value = _syncStats.value.copy(
                    accountsSynced = _syncStats.value.accountsSynced + 1
                )
            }

            // Update transactions
            response.transactions.forEach { transaction ->
                val existingTransaction = transactionRepository.getTransactionById(transaction.id)
                if (existingTransaction != null) {
                    transactionRepository.updateTransaction(existingTransaction.copy(
                        amount = transaction.amount,
                        type = transaction.type,
                        description = transaction.description,
                        date = transaction.date,
                        currency = transaction.currency,
                        notes = transaction.notes,
                        whatsappEnabled = transaction.whatsappEnabled,
                        isSynced = true,
                        lastSync = System.currentTimeMillis()
                    ))
                } else {
                    transactionRepository.insertTransaction(com.hsaby.accounting.data.local.entity.TransactionEntity(
                        id = transaction.id,
                        serverId = transaction.serverId,
                        accountId = transaction.accountId,
                        amount = transaction.amount,
                        type = transaction.type,
                        description = transaction.description,
                        date = transaction.date,
                        currency = transaction.currency,
                        notes = transaction.notes,
                        whatsappEnabled = transaction.whatsappEnabled,
                        userId = userId,
                        isSynced = true,
                        lastSync = System.currentTimeMillis()
                    ))
                }
                _syncStats.value = _syncStats.value.copy(
                    transactionsSynced = _syncStats.value.transactionsSynced + 1
                )
            }

            // Sync local changes to server
            val unsyncedAccounts = accountRepository.getUnsyncedAccounts().first()
            val unsyncedTransactions = transactionRepository.getUnsyncedTransactions(userId).first()

            if (unsyncedAccounts.isNotEmpty() || unsyncedTransactions.isNotEmpty()) {
                val changesResponse = apiService.syncChanges(
                    SyncChangesRequest(unsyncedAccounts, unsyncedTransactions)
                )

                if (changesResponse.isSuccessful) {
                    // Update server IDs for synced items
                    unsyncedAccounts.forEach { account ->
                        accountRepository.updateServerId(account.id, account.serverId)
                        _syncStats.value = _syncStats.value.copy(
                            accountsUploaded = _syncStats.value.accountsUploaded + 1
                        )
                    }
                    unsyncedTransactions.forEach { transaction ->
                        transactionRepository.updateServerId(transaction.id, transaction.serverId)
                        _syncStats.value = _syncStats.value.copy(
                            transactionsUploaded = _syncStats.value.transactionsUploaded + 1
                        )
                    }
                }
            }

            // Update last sync time
            preferencesManager.setLastSyncTime(System.currentTimeMillis())
            _syncState.value = SyncState.Success(_syncStats.value)
            Result.Success(Unit)
        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.message ?: "حدث خطأ أثناء المزامنة")
            Result.Error(e.message ?: "حدث خطأ أثناء المزامنة")
        }
    }
}

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Success(val stats: SyncStats) : SyncState()
    data class Error(val message: String) : SyncState()
}

data class SyncStats(
    val accountsSynced: Int = 0,
    val accountsUploaded: Int = 0,
    val accountsFailed: Int = 0,
    val transactionsSynced: Int = 0,
    val transactionsUploaded: Int = 0,
    val transactionsFailed: Int = 0
) 
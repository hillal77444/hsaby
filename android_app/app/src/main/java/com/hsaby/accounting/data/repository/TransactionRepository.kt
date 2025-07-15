package com.hsaby.accounting.data.repository

import com.hsaby.accounting.data.local.dao.TransactionDao
import com.hsaby.accounting.data.local.entity.TransactionEntity
import com.hsaby.accounting.data.remote.ApiService
import com.hsaby.accounting.data.remote.model.Transaction
import com.hsaby.accounting.util.PreferencesManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val apiService: ApiService,
    private val preferencesManager: PreferencesManager
) {
    fun getTransactionsByUserId(userId: String): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsByUserId(userId)
    }

    fun getTransactionsByAccountId(accountId: String): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsByAccountId(accountId)
    }

    suspend fun getTransactionById(transactionId: String): TransactionEntity? {
        return transactionDao.getTransactionById(transactionId)
    }

    suspend fun insertTransaction(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun insertTransactions(transactions: List<TransactionEntity>) {
        transactionDao.insertTransactions(transactions)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteAllTransactions(userId: String) {
        transactionDao.deleteAllTransactions(userId)
    }

    suspend fun syncTransactions(userId: String) {
        val lastSyncTime = preferencesManager.getLastSyncTime() ?: 0L

        try {
            // Get transactions from server
            val serverTransactions = apiService.getTransactions(userId, lastSyncTime)

            // Update local database
            for (serverTransaction in serverTransactions) {
                val localTransaction = getTransactionById(serverTransaction.id)
                if (localTransaction == null) {
                    // Insert new transaction
                    insertTransaction(TransactionEntity(
                        id = serverTransaction.id,
                        serverId = serverTransaction.serverId,
                        accountId = serverTransaction.accountId,
                        amount = serverTransaction.amount,
                        type = serverTransaction.type,
                        description = serverTransaction.description,
                        date = serverTransaction.date,
                        currency = serverTransaction.currency,
                        notes = serverTransaction.notes,
                        whatsappEnabled = serverTransaction.whatsappEnabled,
                        userId = userId,
                        isSynced = true,
                        lastSync = System.currentTimeMillis()
                    ))
                } else {
                    // Update existing transaction
                    updateTransaction(localTransaction.copy(
                        amount = serverTransaction.amount,
                        type = serverTransaction.type,
                        description = serverTransaction.description,
                        date = serverTransaction.date,
                        currency = serverTransaction.currency,
                        notes = serverTransaction.notes,
                        whatsappEnabled = serverTransaction.whatsappEnabled,
                        isSynced = true,
                        lastSync = System.currentTimeMillis()
                    ))
                }
            }

            // Update last sync time
            preferencesManager.setLastSyncTime(System.currentTimeMillis())
        } catch (e: Exception) {
            // Handle error appropriately
            e.printStackTrace()
            throw e
        }
    }

    fun getBalanceUntilTransaction(accountId: String, transactionId: String, currency: String): Double? {
        return transactionDao.getBalanceUntilTransaction(accountId, transactionId, currency)
    }
} 
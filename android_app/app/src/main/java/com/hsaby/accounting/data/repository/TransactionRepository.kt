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
    fun getAllTransactions(): Flow<List<TransactionEntity>> {
        return transactionDao.getAllTransactions()
    }

    fun getTransactionById(id: Long): Flow<TransactionEntity?> {
        return transactionDao.getTransactionById(id)
    }

    fun getTransactionByServerId(serverId: String): Flow<TransactionEntity?> {
        return transactionDao.getTransactionByServerId(serverId)
    }

    fun getTransactionsByAccountId(accountId: Long): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsByAccountId(accountId)
    }

    suspend fun insertTransaction(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun updateServerId(localId: Long, serverId: String) {
        transactionDao.updateServerId(localId, serverId)
    }

    suspend fun syncTransactions() {
        val userId = preferencesManager.getUserId()
        val lastSyncTime = preferencesManager.getLastSyncTime()

        // Get transactions from server
        val serverTransactions = apiService.getTransactions(userId, lastSyncTime)

        // Update local database
        serverTransactions.forEach { serverTransaction ->
            val localTransaction = transactionDao.getTransactionByServerIdSync(serverTransaction.id)
            if (localTransaction == null) {
                // Insert new transaction
                transactionDao.insertTransaction(TransactionEntity(
                    id = 0,
                    serverId = serverTransaction.id,
                    accountId = serverTransaction.accountId,
                    amount = serverTransaction.amount,
                    type = serverTransaction.type,
                    description = serverTransaction.description,
                    date = serverTransaction.date,
                    lastModified = serverTransaction.lastModified,
                    userId = userId
                ))
            } else {
                // Update existing transaction
                transactionDao.updateTransaction(localTransaction.copy(
                    amount = serverTransaction.amount,
                    type = serverTransaction.type,
                    description = serverTransaction.description,
                    date = serverTransaction.date,
                    lastModified = serverTransaction.lastModified
                ))
            }
        }

        // Update last sync time
        preferencesManager.setLastSyncTime(System.currentTimeMillis())
    }
} 
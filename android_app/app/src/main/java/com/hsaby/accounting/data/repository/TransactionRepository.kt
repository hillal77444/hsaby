package com.hsaby.accounting.data.repository

import com.hsaby.accounting.data.local.dao.TransactionDao
import com.hsaby.accounting.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) {
    fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAllTransactions()

    fun getTransactionsByAccountId(accountId: String): Flow<List<Transaction>> =
        transactionDao.getTransactionsByAccountId(accountId)

    fun getTransactionById(id: String): Flow<Transaction?> = transactionDao.getTransactionById(id)

    fun getTransactionByServerId(serverId: Long?): Transaction? =
        transactionDao.getTransactionByServerId(serverId)

    fun getUnsyncedTransactions(): List<Transaction> = transactionDao.getUnsyncedTransactions()

    suspend fun insertTransaction(transaction: Transaction) =
        transactionDao.insertTransaction(transaction)

    suspend fun updateTransaction(transaction: Transaction) =
        transactionDao.updateTransaction(transaction)

    suspend fun deleteTransaction(transaction: Transaction) =
        transactionDao.deleteTransaction(transaction)

    suspend fun updateServerId(transactionId: String, serverId: Long?) =
        transactionDao.updateServerId(transactionId, serverId)

    suspend fun deleteOldTransactions(beforeTime: Long) =
        transactionDao.deleteOldTransactions(beforeTime)
} 
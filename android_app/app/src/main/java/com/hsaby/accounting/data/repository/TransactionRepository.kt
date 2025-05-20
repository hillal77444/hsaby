package com.hsaby.accounting.data.repository

import com.hsaby.accounting.data.local.dao.TransactionDao
import com.hsaby.accounting.data.local.entity.TransactionEntity
import com.hsaby.accounting.data.model.Transaction
import com.hsaby.accounting.data.remote.ApiService
import com.hsaby.accounting.data.remote.Result
import com.hsaby.accounting.util.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val apiService: ApiService,
    private val preferencesManager: PreferencesManager
) {
    fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions().map { entities ->
            entities.map { it.toModel() }
        }
    }

    suspend fun getTransactionById(id: Long): Transaction? {
        return transactionDao.getTransactionById(id)?.toModel()
    }

    suspend fun getTransactionByServerId(serverId: String): Transaction? {
        return transactionDao.getTransactionByServerId(serverId)?.toModel()
    }

    suspend fun getUnsyncedTransactions(userId: String): List<Transaction> {
        return transactionDao.getUnsyncedTransactions().map { it.toModel() }
    }

    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction.toEntity())
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction.toEntity())
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction.toEntity())
    }

    suspend fun updateServerId(id: Long, serverId: String) {
        transactionDao.updateServerId(id, serverId)
    }

    private fun TransactionEntity.toModel(): Transaction {
        return Transaction(
            id = id,
            serverId = serverId,
            accountId = accountId,
            amount = amount,
            type = type,
            description = description,
            date = date,
            lastModified = lastModified,
            currency = currency,
            notes = notes,
            whatsappEnabled = whatsappEnabled,
            userId = userId,
            isSynced = isSynced,
            lastSync = lastSync
        )
    }

    private fun Transaction.toEntity(): TransactionEntity {
        return TransactionEntity(
            id = id ?: 0,
            serverId = serverId,
            accountId = accountId,
            amount = amount,
            type = type,
            description = description,
            date = date,
            lastModified = lastModified,
            currency = currency,
            notes = notes,
            whatsappEnabled = whatsappEnabled,
            userId = userId,
            isSynced = isSynced,
            lastSync = lastSync
        )
    }
} 
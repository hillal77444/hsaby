package com.hsaby.accounting.data.repository

import com.hsaby.accounting.data.local.dao.AccountDao
import com.hsaby.accounting.data.local.dao.TransactionDao
import com.hsaby.accounting.data.local.dao.UserDao
import com.hsaby.accounting.data.local.entity.AccountEntity
import com.hsaby.accounting.data.local.entity.TransactionEntity
import com.hsaby.accounting.data.local.entity.UserEntity
import com.hsaby.accounting.data.model.*
import com.hsaby.accounting.data.remote.ApiService
import com.hsaby.accounting.util.PreferencesManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val apiService: ApiService,
    private val preferencesManager: PreferencesManager
) {
    suspend fun register(username: String, phone: String, password: String): Result<RegisterResponse> {
        return try {
            val response = apiService.register(RegisterRequest(username, phone, password))
            if (response.isSuccessful) {
                response.body()?.let {
                    userDao.insertUser(
                        UserEntity(
                            id = it.userId,
                            username = it.username,
                            phone = phone,
                            passwordHash = password,
                            lastSync = System.currentTimeMillis()
                        )
                    )
                    preferencesManager.saveToken(it.token)
                    preferencesManager.saveRefreshToken(it.refreshToken)
                    preferencesManager.saveUserId(it.userId)
                    Result.success(it)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Registration failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun login(phone: String, password: String): Result<LoginResponse> {
        return try {
            val response = apiService.login(LoginRequest(phone, password))
            if (response.isSuccessful) {
                response.body()?.let {
                    userDao.insertUser(
                        UserEntity(
                            id = it.userId,
                            username = it.username,
                            phone = phone,
                            passwordHash = password,
                            lastSync = System.currentTimeMillis()
                        )
                    )
                    preferencesManager.saveToken(it.token)
                    preferencesManager.saveRefreshToken(it.refreshToken)
                    preferencesManager.saveUserId(it.userId)
                    Result.success(it)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Login failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserById(userId: String): UserEntity? {
        return userDao.getUserById(userId)
    }

    fun getCurrentUserId(): String? {
        return preferencesManager.getUserId()
    }

    fun getCurrentUsername(): String? {
        return preferencesManager.getUsername()
    }

    fun clearUserData() {
        preferencesManager.clearAuthData()
    }
}

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val apiService: ApiService
) {
    fun getTransactionsByUserId(userId: String): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsByUserId(userId)
    }
    
    fun getTransactionsByAccountId(accountId: String): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsByAccountId(accountId)
    }
    
    suspend fun syncTransactions(userId: String): Result<Unit> {
        return try {
            val response = apiService.getTransactions()
            if (response.isSuccessful) {
                response.body()?.let { transactions ->
                    val transactionEntities = transactions.map { transaction ->
                        TransactionEntity(
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
                        )
                    }
                    transactionDao.insertTransactions(transactionEntities)
                    Result.success(Unit)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Failed to fetch transactions: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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
    
    suspend fun getUnsyncedTransactions(userId: String): List<TransactionEntity> {
        return transactionDao.getUnsyncedTransactions(userId)
    }
} 
package com.hsaby.accounting.data.repository

import com.hsaby.accounting.data.local.dao.AccountDao
import com.hsaby.accounting.data.local.dao.TransactionDao
import com.hsaby.accounting.data.local.dao.UserDao
import com.hsaby.accounting.data.local.entity.AccountEntity
import com.hsaby.accounting.data.local.entity.TransactionEntity
import com.hsaby.accounting.data.local.entity.UserEntity
import com.hsaby.accounting.data.model.*
import com.hsaby.accounting.data.remote.ApiService
import kotlinx.coroutines.flow.Flow
import java.util.*

class UserRepository(
    private val userDao: UserDao,
    private val apiService: ApiService
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
}

class AccountRepository(
    private val accountDao: AccountDao,
    private val apiService: ApiService
) {
    fun getAccountsByUserId(userId: String): Flow<List<AccountEntity>> {
        return accountDao.getAccountsByUserId(userId)
    }
    
    suspend fun syncAccounts(userId: String): Result<Unit> {
        return try {
            val response = apiService.getAccounts()
            if (response.isSuccessful) {
                response.body()?.let { accounts ->
                    val accountEntities = accounts.map { account ->
                        AccountEntity(
                            id = account.id,
                            serverId = account.serverId,
                            accountName = account.accountName,
                            balance = account.balance,
                            currency = account.currency,
                            phoneNumber = account.phoneNumber,
                            notes = account.notes,
                            isDebtor = account.isDebtor,
                            whatsappEnabled = account.whatsappEnabled,
                            userId = userId,
                            lastSync = System.currentTimeMillis()
                        )
                    }
                    accountDao.insertAccounts(accountEntities)
                    Result.success(Unit)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Failed to fetch accounts: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun insertAccount(account: AccountEntity) {
        accountDao.insertAccount(account)
    }
    
    suspend fun updateAccount(account: AccountEntity) {
        accountDao.updateAccount(account)
    }
    
    suspend fun deleteAccount(account: AccountEntity) {
        accountDao.deleteAccount(account)
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
package com.hsaby.accounting.data.repository

import com.hsaby.accounting.data.local.dao.AccountDao
import com.hsaby.accounting.data.model.Account
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val accountDao: AccountDao
) {
    fun getAllAccounts(): Flow<List<Account>> = accountDao.getAllAccounts()

    fun getAccountById(id: String): Flow<Account?> = accountDao.getAccountById(id)

    fun getAccountByServerId(serverId: Long?): Account? = accountDao.getAccountByServerId(serverId)

    fun getUnsyncedAccounts(): List<Account> = accountDao.getUnsyncedAccounts()

    suspend fun insertAccount(account: Account) = accountDao.insertAccount(account)

    suspend fun updateAccount(account: Account) = accountDao.updateAccount(account)

    suspend fun deleteAccount(account: Account) = accountDao.deleteAccount(account)

    suspend fun updateServerId(accountId: String, serverId: Long?) = 
        accountDao.updateServerId(accountId, serverId)

    fun getUserId(): String? = accountDao.getUserId()

    fun getLastSyncTime(): Long = accountDao.getLastSyncTime()

    suspend fun setLastSyncTime(time: Long) = accountDao.setLastSyncTime(time)
} 
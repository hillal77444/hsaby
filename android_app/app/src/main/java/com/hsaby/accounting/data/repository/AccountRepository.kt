package com.hsaby.accounting.data.repository

import com.hsaby.accounting.data.local.dao.AccountDao
import com.hsaby.accounting.data.local.entity.AccountEntity
import com.hsaby.accounting.data.model.Account
import com.hsaby.accounting.data.remote.ApiService
import com.hsaby.accounting.data.remote.Result
import com.hsaby.accounting.util.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val apiService: ApiService,
    private val preferencesManager: PreferencesManager
) {
    fun getAllAccounts(): Flow<List<Account>> {
        return accountDao.getAllAccounts().map { entities ->
            entities.map { it.toModel() }
        }
    }

    suspend fun getAccountById(id: Long): Account? {
        return accountDao.getAccountById(id)?.toModel()
    }

    suspend fun getAccountByServerId(serverId: String): Account? {
        return accountDao.getAccountByServerId(serverId)?.toModel()
    }

    suspend fun getUnsyncedAccounts(): List<Account> {
        return accountDao.getUnsyncedAccounts().map { it.toModel() }
    }

    suspend fun insertAccount(account: Account) {
        accountDao.insertAccount(account.toEntity())
    }

    suspend fun updateAccount(account: Account) {
        accountDao.updateAccount(account.toEntity())
    }

    suspend fun deleteAccount(account: Account) {
        accountDao.deleteAccount(account.toEntity())
    }

    suspend fun updateServerId(id: Long, serverId: String) {
        accountDao.updateServerId(id, serverId)
    }

    fun getUserId(): String? {
        return preferencesManager.getUserId()
    }

    fun getLastSyncTime(): Long {
        return preferencesManager.getLastSyncTime()
    }

    fun setLastSyncTime(time: Long) {
        preferencesManager.setLastSyncTime(time)
    }

    private fun AccountEntity.toModel(): Account {
        return Account(
            id = id,
            serverId = serverId,
            name = name,
            balance = balance,
            isActive = isActive,
            lastModified = lastModified
        )
    }

    private fun Account.toEntity(): AccountEntity {
        return AccountEntity(
            id = id,
            serverId = serverId,
            name = name,
            balance = balance,
            isActive = isActive,
            lastModified = lastModified
        )
    }
} 
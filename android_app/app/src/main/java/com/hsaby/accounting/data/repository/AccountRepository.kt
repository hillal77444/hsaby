package com.hsaby.accounting.data.repository

import com.hsaby.accounting.data.local.dao.AccountDao
import com.hsaby.accounting.data.local.entity.AccountEntity
import com.hsaby.accounting.data.remote.ApiService
import com.hsaby.accounting.data.remote.model.Account
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
    fun getAllAccounts(): Flow<List<AccountEntity>> {
        return accountDao.getAllAccounts()
    }

    suspend fun getAccountById(id: String): AccountEntity? {
        return accountDao.getAccountById(id.toLongOrNull() ?: 0L)
    }

    suspend fun getAccountByServerId(serverId: String): AccountEntity? {
        return accountDao.getAccountByServerId(serverId)
    }

    fun getUnsyncedAccounts(): Flow<List<AccountEntity>> {
        return accountDao.getUnsyncedAccounts()
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

    suspend fun updateServerId(localId: Long, serverId: String) {
        accountDao.updateServerId(localId, serverId)
    }

    suspend fun syncAccounts() {
        val userId = preferencesManager.getUserId() ?: return
        val lastSyncTime = preferencesManager.getLastSyncTime() ?: 0L

        try {
            // Get accounts from server
            val serverAccounts = apiService.getAccounts(userId, lastSyncTime)

            // Update local database
            serverAccounts.forEach { serverAccount ->
                val localAccount = accountDao.getAccountByServerIdSync(serverAccount.id)
                if (localAccount == null) {
                    // Insert new account
                    accountDao.insertAccount(AccountEntity(
                        id = 0,
                        serverId = serverAccount.id,
                        name = serverAccount.name,
                        balance = serverAccount.balance,
                        isActive = serverAccount.isActive,
                        lastModified = serverAccount.lastModified,
                        phoneNumber = serverAccount.phoneNumber,
                        userId = userId
                    ))
                } else {
                    // Update existing account
                    accountDao.updateAccount(localAccount.copy(
                        name = serverAccount.name,
                        balance = serverAccount.balance,
                        isActive = serverAccount.isActive,
                        lastModified = serverAccount.lastModified,
                        phoneNumber = serverAccount.phoneNumber
                    ))
                }
            }

            // Update last sync time
            preferencesManager.setLastSyncTime(System.currentTimeMillis())
        } catch (e: Exception) {
            // Handle error appropriately
            e.printStackTrace()
        }
    }
} 
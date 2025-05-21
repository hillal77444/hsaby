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
    // Flow functions
    fun getAccountsByUserId(userId: String): Flow<List<AccountEntity>> {
        return accountDao.getAccountsByUserId(userId)
    }

    fun getDebtorAccounts(userId: String): Flow<List<AccountEntity>> {
        return accountDao.getDebtorAccounts(userId)
    }

    // Suspend functions
    suspend fun getAccountById(accountId: String): AccountEntity? {
        return accountDao.getAccountById(accountId)
    }

    suspend fun getAccountByServerId(serverId: Long): AccountEntity? {
        return accountDao.getAccountByServerId(serverId)
    }

    suspend fun insertAccount(account: AccountEntity) {
        accountDao.insertAccount(account)
    }

    suspend fun insertAccounts(accounts: List<AccountEntity>) {
        accountDao.insertAccounts(accounts)
    }

    suspend fun updateAccount(account: AccountEntity) {
        accountDao.updateAccount(account)
    }

    suspend fun deleteAccount(account: AccountEntity) {
        accountDao.deleteAccount(account)
    }

    suspend fun deleteAllAccounts(userId: String) {
        accountDao.deleteAllAccounts(userId)
    }

    // Search functions
    fun searchAccounts(query: String, userId: String): Flow<List<AccountEntity>> {
        return accountDao.searchAccounts(query, userId)
    }

    // Statistics functions
    suspend fun getTotalBalance(userId: String): Double {
        return accountDao.getTotalBalance(userId) ?: 0.0
    }

    suspend fun getTotalDebtors(userId: String): Double {
        return accountDao.getTotalDebtors(userId) ?: 0.0
    }

    suspend fun getAccountsCount(userId: String): Int {
        return accountDao.getAccountsCount(userId)
    }

    // Sync functions
    suspend fun syncAccounts(userId: String) {
        val lastSyncTime = preferencesManager.getLastSyncTime() ?: 0L

        try {
            // Get accounts from server
            val serverAccounts = apiService.getAccounts(userId, lastSyncTime)

            // Update local database
            serverAccounts.forEach { serverAccount ->
                val localAccount = getAccountByServerId(serverAccount.serverId ?: 0L)
                if (localAccount == null) {
                    // Insert new account
                    insertAccount(AccountEntity(
                        id = serverAccount.id,
                        serverId = serverAccount.serverId,
                        accountName = serverAccount.name,
                        balance = serverAccount.balance,
                        currency = serverAccount.currency,
                        phoneNumber = serverAccount.phoneNumber,
                        notes = serverAccount.notes,
                        isDebtor = serverAccount.isDebtor,
                        whatsappEnabled = serverAccount.whatsappEnabled,
                        userId = userId,
                        isSynced = true,
                        lastSync = System.currentTimeMillis()
                    ))
                } else {
                    // Update existing account
                    updateAccount(localAccount.copy(
                        accountName = serverAccount.name,
                        balance = serverAccount.balance,
                        currency = serverAccount.currency,
                        phoneNumber = serverAccount.phoneNumber,
                        notes = serverAccount.notes,
                        isDebtor = serverAccount.isDebtor,
                        whatsappEnabled = serverAccount.whatsappEnabled,
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
} 
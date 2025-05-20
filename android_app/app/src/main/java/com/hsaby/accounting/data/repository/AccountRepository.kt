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
    fun getAllAccounts(): Flow<List<AccountEntity>> {
        return accountDao.getAllAccounts()
    }

    fun getAccountsByUserId(userId: String): Flow<List<AccountEntity>> {
        return accountDao.getAccountsByUserId(userId)
    }

    fun getUnsyncedAccounts(): Flow<List<AccountEntity>> {
        return accountDao.getUnsyncedAccounts()
    }

    fun getActiveAccounts(): Flow<List<AccountEntity>> {
        return accountDao.getActiveAccounts()
    }

    fun getDebtorAccounts(): Flow<List<AccountEntity>> {
        return accountDao.getDebtorAccounts()
    }

    // Suspend functions
    suspend fun getAccountById(id: String): AccountEntity? {
        return accountDao.getAccountById(id)
    }

    suspend fun getAccountByServerId(serverId: String): AccountEntity? {
        return accountDao.getAccountByServerId(serverId)
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

    suspend fun deleteAllAccounts(userId: String) {
        accountDao.deleteAllAccounts(userId)
    }

    suspend fun updateServerId(localId: Long, serverId: String) {
        accountDao.updateServerId(localId, serverId)
    }

    suspend fun updateAccountBalance(accountId: Long, newBalance: Double) {
        accountDao.updateAccountBalance(accountId, newBalance)
    }

    suspend fun updateAccountStatus(accountId: Long, isActive: Boolean) {
        accountDao.updateAccountStatus(accountId, isActive)
    }

    // Sync functions
    suspend fun syncAccounts(userId: String? = null) {
        val currentUserId = userId ?: preferencesManager.getUserId() ?: return
        val lastSyncTime = preferencesManager.getLastSyncTime() ?: 0L

        try {
            // Get accounts from server
            val serverAccounts = apiService.getAccounts(currentUserId, lastSyncTime)

            // Update local database
            serverAccounts.forEach { serverAccount ->
                val localAccount = accountDao.getAccountById(serverAccount.id)
                if (localAccount == null) {
                    // Insert new account
                    accountDao.insertAccount(AccountEntity(
                        id = serverAccount.id,
                        serverId = serverAccount.serverId,
                        accountName = serverAccount.name,
                        balance = serverAccount.balance,
                        currency = serverAccount.currency,
                        phoneNumber = serverAccount.phoneNumber,
                        notes = serverAccount.notes,
                        isDebtor = serverAccount.isDebtor,
                        whatsappEnabled = serverAccount.whatsappEnabled,
                        userId = currentUserId,
                        lastSync = System.currentTimeMillis()
                    ))
                } else {
                    // Update existing account
                    accountDao.updateAccount(localAccount.copy(
                        accountName = serverAccount.name,
                        balance = serverAccount.balance,
                        currency = serverAccount.currency,
                        phoneNumber = serverAccount.phoneNumber,
                        notes = serverAccount.notes,
                        isDebtor = serverAccount.isDebtor,
                        whatsappEnabled = serverAccount.whatsappEnabled,
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

    // Search functions
    fun searchAccounts(query: String): Flow<List<AccountEntity>> {
        return accountDao.searchAccounts("%$query%")
    }

    // Statistics functions
    suspend fun getTotalBalance(): Double {
        return accountDao.getTotalBalance() ?: 0.0
    }

    suspend fun getTotalDebtors(): Double {
        return accountDao.getTotalDebtors() ?: 0.0
    }

    suspend fun getActiveAccountsCount(): Int {
        return accountDao.getActiveAccountsCount()
    }
} 
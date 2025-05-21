package com.hsaby.accounting.data.local.dao

import androidx.room.*
import com.hsaby.accounting.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE userId = :userId")
    fun getAccountsByUserId(userId: String): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE isSynced = 0")
    fun getUnsyncedAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE isActive = 1")
    fun getActiveAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE isDebtor = 1")
    fun getDebtorAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE serverId = :serverId")
    suspend fun getAccountByServerId(serverId: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE userId = :userId")
    suspend fun deleteAllAccounts(userId: String)

    @Query("UPDATE accounts SET serverId = :serverId WHERE id = :localId")
    suspend fun updateServerId(localId: Long, serverId: String)

    @Query("UPDATE accounts SET balance = :newBalance WHERE id = :accountId")
    suspend fun updateAccountBalance(accountId: Long, newBalance: Double)

    @Query("UPDATE accounts SET isActive = :isActive WHERE id = :accountId")
    suspend fun updateAccountStatus(accountId: Long, isActive: Boolean)

    @Query("SELECT * FROM accounts WHERE accountName LIKE :query OR phoneNumber LIKE :query")
    fun searchAccounts(query: String): Flow<List<AccountEntity>>

    @Query("SELECT SUM(balance) FROM accounts WHERE isDebtor = 0")
    suspend fun getTotalBalance(): Double?

    @Query("SELECT SUM(balance) FROM accounts WHERE isDebtor = 1")
    suspend fun getTotalDebtors(): Double?

    @Query("SELECT COUNT(*) FROM accounts WHERE isActive = 1")
    suspend fun getActiveAccountsCount(): Int
} 
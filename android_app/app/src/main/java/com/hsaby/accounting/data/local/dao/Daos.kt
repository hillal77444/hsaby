package com.hsaby.accounting.data.local.dao

import androidx.room.*
import com.hsaby.accounting.data.local.entity.AccountEntity
import com.hsaby.accounting.data.local.entity.TransactionEntity
import com.hsaby.accounting.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
    
    @Update
    suspend fun updateUser(user: UserEntity)
    
    @Delete
    suspend fun deleteUser(user: UserEntity)
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE userId = :userId")
    fun getAccountsByUserId(userId: String): Flow<List<AccountEntity>>
    
    @Query("SELECT * FROM accounts WHERE id = :accountId")
    suspend fun getAccountById(accountId: String): AccountEntity?
    
    @Query("SELECT * FROM accounts WHERE serverId = :serverId")
    suspend fun getAccountByServerId(serverId: Long): AccountEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)
    
    @Update
    suspend fun updateAccount(account: AccountEntity)
    
    @Delete
    suspend fun deleteAccount(account: AccountEntity)
    
    @Query("DELETE FROM accounts WHERE userId = :userId")
    suspend fun deleteAllAccounts(userId: String)
    
    @Query("SELECT * FROM accounts WHERE isDebtor = 1 AND userId = :userId")
    fun getDebtorAccounts(userId: String): Flow<List<AccountEntity>>
    
    @Query("SELECT * FROM accounts WHERE accountName LIKE :query AND userId = :userId")
    fun searchAccounts(query: String, userId: String): Flow<List<AccountEntity>>
    
    @Query("SELECT SUM(balance) FROM accounts WHERE userId = :userId")
    suspend fun getTotalBalance(userId: String): Double?
    
    @Query("SELECT SUM(balance) FROM accounts WHERE isDebtor = 1 AND userId = :userId")
    suspend fun getTotalDebtors(userId: String): Double?
    
    @Query("SELECT COUNT(*) FROM accounts WHERE userId = :userId")
    suspend fun getAccountsCount(userId: String): Int
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC")
    fun getTransactionsByUserId(userId: String): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC")
    fun getTransactionsByAccountId(accountId: String): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    suspend fun getTransactionById(transactionId: String): TransactionEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)
    
    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)
    
    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)
    
    @Query("DELETE FROM transactions WHERE userId = :userId")
    suspend fun deleteAllTransactions(userId: String)
    
    @Query("SELECT * FROM transactions WHERE isSynced = 0 AND userId = :userId")
    suspend fun getUnsyncedTransactions(userId: String): List<TransactionEntity>

    @Query("""
        SELECT SUM(CASE WHEN type = 'credit' THEN amount ELSE -amount END)
        FROM transactions
        WHERE accountId = :accountId
          AND currency = :currency
          AND (date < (
                SELECT date FROM transactions WHERE id = :transactionId
              )
              OR (date = (
                    SELECT date FROM transactions WHERE id = :transactionId
                  ) AND id <= :transactionId)
              )
    """)
    fun getBalanceUntilTransaction(accountId: String, transactionId: String, currency: String): Double?
} 
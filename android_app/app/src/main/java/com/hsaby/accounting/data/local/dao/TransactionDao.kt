package com.hsaby.accounting.data.local.dao

import androidx.room.*
import com.hsaby.accounting.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE serverId = :serverId")
    suspend fun getTransactionByServerId(serverId: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE serverId = :serverId")
    fun getTransactionByServerIdSync(serverId: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE accountId = :accountId")
    suspend fun deleteTransactionsByAccountId(accountId: String)

    @Query("UPDATE transactions SET serverId = :serverId WHERE id = :localId")
    suspend fun updateServerId(localId: String, serverId: String)

    @Query("SELECT * FROM transactions WHERE accountId = :accountId")
    fun getTransactionsByAccountId(accountId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId AND type = :type")
    fun getTransactionsByAccountIdAndType(accountId: String, type: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId AND date >= :startDate AND date <= :endDate")
    fun getTransactionsByDateRange(accountId: String, startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(amount) FROM transactions WHERE accountId = :accountId AND type = :type")
    suspend fun getTotalAmountByType(accountId: String, type: String): Double?
} 
package com.accounting.app.database

import androidx.room.*
import com.accounting.app.models.Transaction

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<Transaction>)

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactions(): List<Transaction>

    @Query("SELECT * FROM transactions WHERE account_id = :accountId")
    suspend fun getTransactionsForAccount(accountId: Long): List<Transaction>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :fromDate AND :toDate")
    suspend fun getTransactionsBetweenDates(fromDate: Long, toDate: Long): List<Transaction>

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)
} 
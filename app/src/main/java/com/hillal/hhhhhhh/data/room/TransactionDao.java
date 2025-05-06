package com.hillal.hhhhhhh.data.room;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.hillal.hhhhhhh.data.model.Transaction;

import java.util.List;

@Dao
public interface TransactionDao {
    @Insert
    void insertTransaction(Transaction transaction);

    @Update
    void updateTransaction(Transaction transaction);

    @Delete
    void deleteTransaction(Transaction transaction);

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC")
    LiveData<List<Transaction>> getTransactionsForAccount(long accountId);

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    LiveData<List<Transaction>> getAllTransactions();

    @Query("SELECT SUM(CASE WHEN type = 'debit' THEN amount ELSE -amount END) FROM transactions WHERE accountId = :accountId")
    LiveData<Double> getAccountBalance(long accountId);

    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    LiveData<Transaction> getTransactionById(long transactionId);

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    LiveData<List<Transaction>> getTransactionsByType(String type);

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    LiveData<List<Transaction>> getTransactionsByDateRange(long startDate, long endDate);

    @Query("SELECT * FROM transactions WHERE currency = :currency ORDER BY date DESC")
    LiveData<List<Transaction>> getTransactionsByCurrency(String currency);
} 
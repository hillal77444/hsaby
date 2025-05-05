package com.hillal.hhhhhhh.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.hillal.hhhhhhh.data.entities.Transaction;

import java.util.List;

@Dao
public interface TransactionDao {
    @Insert
    long insert(Transaction transaction);

    @Update
    void update(Transaction transaction);

    @Delete
    void delete(Transaction transaction);

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    LiveData<List<Transaction>> getAllTransactions();

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC")
    LiveData<List<Transaction>> getTransactionsForAccount(long accountId);

    @Query("SELECT SUM(CASE WHEN isDebit = 1 THEN amount ELSE -amount END) FROM transactions WHERE accountId = :accountId")
    LiveData<Double> getAccountBalance(long accountId);

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate")
    LiveData<List<Transaction>> getTransactionsBetweenDates(long startDate, long endDate);
} 
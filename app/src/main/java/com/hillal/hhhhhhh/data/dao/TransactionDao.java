package com.hillal.hhhhhhh.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;

import com.hillal.hhhhhhh.data.model.Transaction;

import java.util.Date;
import java.util.List;

@Dao
public interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC")
    LiveData<List<Transaction>> getTransactionsForAccount(long accountId);

    @Query("SELECT * FROM transactions WHERE accountId = :accountId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    LiveData<List<Transaction>> getTransactionsForAccountInDateRange(long accountId, Date startDate, Date endDate);

    @Query("SELECT SUM(CASE WHEN type = 'credit' THEN amount ELSE 0 END) FROM transactions WHERE accountId = :accountId")
    LiveData<Double> getTotalCredits(long accountId);

    @Query("SELECT SUM(CASE WHEN type = 'debit' THEN amount ELSE 0 END) FROM transactions WHERE accountId = :accountId")
    LiveData<Double> getTotalDebits(long accountId);

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC LIMIT 1")
    LiveData<Transaction> getLastTransaction(long accountId);
} 
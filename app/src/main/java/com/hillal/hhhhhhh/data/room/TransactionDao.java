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
    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC")
    LiveData<List<Transaction>> getTransactionsForAccount(int accountId);

    @Query("SELECT COALESCE(SUM(CASE WHEN isDebit = 1 THEN amount ELSE -amount END), 0) FROM transactions WHERE accountId = :accountId")
    LiveData<Double> getAccountBalance(int accountId);

    @Insert
    void insertTransaction(Transaction transaction);

    @Update
    void updateTransaction(Transaction transaction);

    @Delete
    void deleteTransaction(Transaction transaction);
} 
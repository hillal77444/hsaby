package com.hillal.hhhhhhh;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TransactionDao {
    @Insert
    void insert(Transaction transaction);

    @Update
    void update(Transaction transaction);

    @Delete
    void delete(Transaction transaction);

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC")
    LiveData<List<Transaction>> getTransactionsByAccount(int accountId);

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    LiveData<List<Transaction>> getAllTransactions();

    @Query("SELECT * FROM transactions WHERE isCredit = :isCredit ORDER BY date DESC")
    LiveData<List<Transaction>> getTransactionsByType(boolean isCredit);

    @Query("SELECT SUM(amount) FROM transactions WHERE accountId = :accountId AND isCredit = 1")
    LiveData<Double> getTotalCredits(int accountId);

    @Query("SELECT SUM(amount) FROM transactions WHERE accountId = :accountId AND isCredit = 0")
    LiveData<Double> getTotalDebits(int accountId);
} 
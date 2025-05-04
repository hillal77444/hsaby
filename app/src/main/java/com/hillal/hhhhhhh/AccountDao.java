package com.hillal.hhhhhhh;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface AccountDao {
    @Query("SELECT * FROM accounts")
    LiveData<List<Account>> getAllAccounts();

    @Query("SELECT * FROM accounts WHERE id = :accountId")
    LiveData<Account> getAccountById(int accountId);

    @Query("SELECT SUM(openingBalance) FROM accounts WHERE isCreditor = 1")
    LiveData<Double> getTotalCreditor();

    @Query("SELECT SUM(openingBalance) FROM accounts WHERE isCreditor = 0")
    LiveData<Double> getTotalDebtor();

    @Query("SELECT (SELECT SUM(openingBalance) FROM accounts WHERE isCreditor = 1) - " +
           "(SELECT SUM(openingBalance) FROM accounts WHERE isCreditor = 0)")
    LiveData<Double> getNetBalance();

    @Insert
    void insert(Account account);

    @Update
    void update(Account account);

    @Delete
    void delete(Account account);
} 
package com.hillal.hhhhhhh.data.room;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.hillal.hhhhhhh.data.model.Account;

import java.util.List;

@Dao
public interface AccountDao {
    @Insert
    void insert(Account account);

    @Update
    void update(Account account);

    @Delete
    void delete(Account account);

    @Query("SELECT * FROM accounts ORDER BY updatedAt DESC LIMIT 5")
    List<Account> getRecentAccounts();

    @Query("SELECT * FROM accounts WHERE id = :id")
    LiveData<Account> getAccountById(int id);

    @Query("SELECT * FROM accounts ORDER BY name ASC")
    LiveData<List<Account>> getAllAccounts();

    @Query("SELECT * FROM accounts WHERE isCreditor = 1 ORDER BY balance DESC")
    LiveData<List<Account>> getCreditors();

    @Query("SELECT * FROM accounts WHERE isCreditor = 0 ORDER BY balance DESC")
    LiveData<List<Account>> getDebtors();

    @Query("SELECT SUM(balance) FROM accounts WHERE isCreditor = 1")
    LiveData<Double> getTotalCreditors();

    @Query("SELECT SUM(balance) FROM accounts WHERE isCreditor = 0")
    LiveData<Double> getTotalDebtors();

    @Query("SELECT * FROM accounts WHERE name LIKE :query OR phone LIKE :query")
    LiveData<List<Account>> searchAccounts(String query);
} 
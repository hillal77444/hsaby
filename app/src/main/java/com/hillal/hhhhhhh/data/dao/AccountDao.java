package com.hillal.hhhhhhh.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.hillal.hhhhhhh.data.entities.Account;

import java.util.List;

@Dao
public interface AccountDao {
    @Insert
    long insert(Account account);

    @Update
    void update(Account account);

    @Delete
    void delete(Account account);

    @Query("SELECT * FROM accounts ORDER BY name ASC")
    LiveData<List<Account>> getAllAccounts();

    @Query("SELECT * FROM accounts WHERE id = :accountId")
    LiveData<Account> getAccountById(long accountId);

    @Query("SELECT * FROM accounts WHERE name LIKE :searchQuery")
    LiveData<List<Account>> searchAccounts(String searchQuery);
} 
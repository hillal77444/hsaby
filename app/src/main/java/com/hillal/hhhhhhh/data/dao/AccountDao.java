package com.hillal.hhhhhhh.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.hillal.hhhhhhh.data.model.Account;

import java.util.List;

@Dao
public interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY name ASC")
    LiveData<List<Account>> getAllAccounts();

    @Query("SELECT * FROM accounts WHERE id = :accountId")
    LiveData<Account> getAccountById(long accountId);

    @Insert
    void insert(Account account);

    @Update
    void update(Account account);

    @Query("DELETE FROM accounts WHERE id = :accountId")
    void deleteAccount(long accountId);
} 
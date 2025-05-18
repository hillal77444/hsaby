package com.hillal.hhhhhhh.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.OnConflictStrategy;

import com.hillal.hhhhhhh.data.model.Account;

import java.util.List;

@Dao
public interface AccountDao {
    @Query("SELECT * FROM accounts")
    LiveData<List<Account>> getAllAccounts();

    @Query("SELECT * FROM accounts")
    List<Account> getAllAccountsSync();

    @Query("SELECT * FROM accounts WHERE id = :id")
    LiveData<Account> getAccountById(long id);

    @Query("SELECT * FROM accounts WHERE id = :id")
    Account getAccountByIdSync(long id);

    @Query("SELECT * FROM accounts WHERE account_number = :accountNumber")
    LiveData<Account> getAccountByNumber(String accountNumber);

    @Query("SELECT * FROM accounts WHERE account_number = :accountNumber")
    Account getAccountByNumberSync(String accountNumber);

    @Query("SELECT * FROM accounts WHERE sync_status != :syncStatus")
    List<Account> getModifiedAccounts(long lastSyncTime);

    @Query("SELECT * FROM accounts WHERE server_id = 0")
    List<Account> getNewAccounts();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Account account);

    @Update
    void update(Account account);

    @Delete
    void delete(Account account);

    @Query("DELETE FROM accounts")
    void deleteAll();

    @Query("SELECT * FROM accounts WHERE server_id = :serverId")
    LiveData<Account> getAccountByServerId(long serverId);

    @Query("SELECT * FROM accounts WHERE server_id = :serverId")
    Account getAccountByServerIdSync(long serverId);
} 
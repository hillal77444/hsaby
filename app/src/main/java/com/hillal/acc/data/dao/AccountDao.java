package com.hillal.acc.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.OnConflictStrategy;

import com.hillal.acc.data.model.Account;

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

    @Query("SELECT * FROM accounts WHERE sync_status != :syncStatus AND last_sync_time > :lastSyncTime")
    List<Account> getModifiedAccounts(long lastSyncTime, int syncStatus);

    @Query("SELECT * FROM accounts WHERE server_id = -1")
    List<Account> getNewAccounts();

    @Query("SELECT * FROM accounts WHERE server_id = -1")
    List<Account> getAccountsToMigrate();

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

    @Query("SELECT * FROM accounts WHERE sync_status = :syncStatus")
    List<Account> getPendingSyncAccounts();

    @Query("SELECT * FROM accounts WHERE sync_status != :syncStatus")
    List<Account> getAccountsForSync();
} 
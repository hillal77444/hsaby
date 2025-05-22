package com.hillal.hhhhhhh.data.room;

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
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Account account);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Account> accounts);

    @Update
    void update(Account account);

    @Delete
    void delete(Account account);

    @Query("SELECT * FROM accounts ORDER BY created_at DESC LIMIT 5")
    List<Account> getRecentAccounts();

    @Query("SELECT * FROM accounts WHERE id = :id")
    LiveData<Account> getAccountById(long id);

    @Query("SELECT * FROM accounts")
    LiveData<List<Account>> getAllAccounts();

    @Query("SELECT * FROM accounts")
    List<Account> getAllAccountsSync();

    @Query("SELECT * FROM accounts WHERE is_debtor = 0 ORDER BY balance DESC")
    LiveData<List<Account>> getCreditors();

    @Query("SELECT * FROM accounts WHERE is_debtor = 1 ORDER BY balance DESC")
    LiveData<List<Account>> getDebtors();

    @Query("SELECT SUM(balance) FROM accounts WHERE is_debtor = 0")
    LiveData<Double> getTotalCreditors();

    @Query("SELECT SUM(balance) FROM accounts WHERE is_debtor = 1")
    LiveData<Double> getTotalDebtors();

    @Query("SELECT * FROM accounts WHERE account_name LIKE :query OR phone_number LIKE :query")
    LiveData<List<Account>> searchAccounts(String query);

    @Query("DELETE FROM accounts WHERE id = :accountId")
    void deleteAccount(long accountId);

    @Query("SELECT * FROM accounts WHERE updated_at > :timestamp")
    List<Account> getAccountsModifiedAfter(long timestamp);

    @Query("SELECT * FROM accounts WHERE server_id < 0")
    List<Account> getNewAccounts();

    @Query("SELECT * FROM accounts WHERE updated_at > :timestamp AND server_id > 0")
    List<Account> getModifiedAccounts(long timestamp);

    @Query("SELECT * FROM accounts WHERE account_number = :accountNumber")
    Account getAccountByNumberSync(String accountNumber);

    @Query("SELECT * FROM accounts WHERE server_id = :serverId")
    Account getAccountByServerIdSync(long serverId);

    @Query("SELECT * FROM accounts WHERE phone_number = :phoneNumber")
    Account getAccountByPhoneNumber(String phoneNumber);

    @Query("DELETE FROM accounts")
    void deleteAllAccounts();

    @Query("SELECT * FROM accounts WHERE server_id = :serverId")
    List<Account> getAccountsByServerId(long serverId);
} 
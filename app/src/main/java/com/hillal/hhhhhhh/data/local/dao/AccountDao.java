package com.hillal.hhhhhhh.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.hillal.hhhhhhh.data.local.entity.Account;
import java.util.List;

@Dao
public interface AccountDao {
    @Query("SELECT * FROM accounts")
    List<Account> getAllAccounts();

    @Query("SELECT * FROM accounts WHERE id = :id")
    Account getAccountById(long id);

    @Query("SELECT * FROM accounts WHERE server_id = :serverId")
    Account getAccountByServerId(long serverId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Account account);

    @Update
    void update(Account account);

    @Delete
    void delete(Account account);

    @Query("DELETE FROM accounts")
    void deleteAll();

    @Query("SELECT * FROM accounts WHERE user_id = :userId")
    List<Account> getAccountsByUserId(long userId);
} 
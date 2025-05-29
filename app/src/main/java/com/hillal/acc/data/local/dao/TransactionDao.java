package com.hillal.acc.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.OnConflictStrategy;
import com.hillal.acc.data.local.entity.Transaction;
import java.util.List;

@Dao
public interface TransactionDao {
    @Query("SELECT * FROM transactions")
    List<Transaction> getAllTransactions();

    @Query("SELECT * FROM transactions WHERE id = :id")
    Transaction getTransactionById(long id);

    @Query("SELECT * FROM transactions WHERE server_id = :serverId")
    Transaction getTransactionByServerId(long serverId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Transaction transaction);

    @Update
    void update(Transaction transaction);

    @Delete
    void delete(Transaction transaction);

    @Query("DELETE FROM transactions")
    void deleteAll();

    @Query("SELECT * FROM transactions WHERE account_id = :accountId")
    List<Transaction> getTransactionsByAccountId(long accountId);

    @Query("SELECT * FROM transactions WHERE user_id = :userId")
    List<Transaction> getTransactionsByUserId(long userId);
} 
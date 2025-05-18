package com.hillal.hhhhhhh.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.OnConflictStrategy;

import com.hillal.hhhhhhh.data.model.Transaction;

import java.util.Date;
import java.util.List;

@Dao
public interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY transaction_date DESC")
    LiveData<List<Transaction>> getTransactionsForAccount(long accountId);

    @Query("SELECT * FROM transactions WHERE accountId = :accountId AND transaction_date BETWEEN :startDate AND :endDate ORDER BY transaction_date DESC")
    LiveData<List<Transaction>> getTransactionsForAccountInDateRange(long accountId, Date startDate, Date endDate);

    @Query("SELECT SUM(amount) FROM transactions WHERE accountId = :accountId AND type = 'credit'")
    LiveData<Double> getTotalCredits(long accountId);

    @Query("SELECT SUM(amount) FROM transactions WHERE accountId = :accountId AND type = 'debit'")
    LiveData<Double> getTotalDebits(long accountId);

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY transaction_date DESC LIMIT 1")
    LiveData<Transaction> getLastTransaction(long accountId);

    @Query("SELECT SUM(amount) FROM transactions WHERE accountId = :accountId AND type = 'credit' AND transaction_date BETWEEN :startDate AND :endDate")
    LiveData<Double> getTotalCreditsInDateRange(long accountId, Date startDate, Date endDate);

    @Query("SELECT SUM(amount) FROM transactions WHERE accountId = :accountId AND type = 'debit' AND transaction_date BETWEEN :startDate AND :endDate")
    LiveData<Double> getTotalDebitsInDateRange(long accountId, Date startDate, Date endDate);

    @Query("SELECT * FROM transactions WHERE transaction_date BETWEEN :startDate AND :endDate ORDER BY transaction_date DESC")
    LiveData<List<Transaction>> getTransactionsByDateRange(Date startDate, Date endDate);

    @Query("SELECT * FROM transactions")
    LiveData<List<Transaction>> getAllTransactions();

    @Query("SELECT * FROM transactions")
    List<Transaction> getAllTransactionsSync();

    @Query("SELECT * FROM transactions WHERE id = :id")
    LiveData<Transaction> getTransactionById(long id);

    @Query("SELECT * FROM transactions WHERE id = :id")
    Transaction getTransactionByIdSync(long id);

    @Query("SELECT * FROM transactions WHERE server_id = :serverId")
    LiveData<Transaction> getTransactionByServerId(long serverId);

    @Query("SELECT * FROM transactions WHERE server_id = :serverId")
    Transaction getTransactionByServerIdSync(long serverId);

    @Query("SELECT * FROM transactions WHERE account_id = :accountId")
    LiveData<List<Transaction>> getTransactionsByAccountId(long accountId);

    @Query("SELECT * FROM transactions WHERE sync_status != :syncStatus")
    List<Transaction> getModifiedTransactions(int syncStatus);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Transaction transaction);

    @Update
    void update(Transaction transaction);

    @Delete
    void delete(Transaction transaction);

    @Query("DELETE FROM transactions")
    void deleteAll();

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    void deleteTransaction(long transactionId);
} 
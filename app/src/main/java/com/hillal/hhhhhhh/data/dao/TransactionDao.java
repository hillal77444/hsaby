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
    @Query("SELECT * FROM transactions WHERE account_id = :accountId ORDER BY transaction_date DESC")
    LiveData<List<Transaction>> getTransactionsForAccount(long accountId);

    @Query("SELECT * FROM transactions WHERE account_id = :accountId AND transaction_date BETWEEN :startDate AND :endDate ORDER BY transaction_date DESC")
    LiveData<List<Transaction>> getTransactionsForAccountInDateRange(long accountId, long startDate, long endDate);

    @Query("SELECT SUM(amount) FROM transactions WHERE account_id = :accountId AND type = 'credit'")
    LiveData<Double> getTotalCredits(long accountId);

    @Query("SELECT SUM(amount) FROM transactions WHERE account_id = :accountId AND type = 'debit'")
    LiveData<Double> getTotalDebits(long accountId);

    @Query("SELECT * FROM transactions WHERE account_id = :accountId ORDER BY transaction_date DESC LIMIT 1")
    LiveData<Transaction> getLastTransaction(long accountId);

    @Query("SELECT SUM(amount) FROM transactions WHERE account_id = :accountId AND type = 'credit' AND transaction_date BETWEEN :startDate AND :endDate")
    LiveData<Double> getTotalCreditsInDateRange(long accountId, long startDate, long endDate);

    @Query("SELECT SUM(amount) FROM transactions WHERE account_id = :accountId AND type = 'debit' AND transaction_date BETWEEN :startDate AND :endDate")
    LiveData<Double> getTotalDebitsInDateRange(long accountId, long startDate, long endDate);

    @Query("SELECT * FROM transactions WHERE transaction_date BETWEEN :startDate AND :endDate ORDER BY transaction_date DESC")
    LiveData<List<Transaction>> getTransactionsByDateRange(long startDate, long endDate);

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
    LiveData<List<Transaction>> getTransactionsByAccount(long accountId);

    @Query("SELECT * FROM transactions WHERE account_id = :accountId")
    List<Transaction> getTransactionsByAccountSync(long accountId);

    @Query("SELECT SUM(amount) FROM transactions WHERE account_id = :accountId")
    LiveData<Double> getAccountBalance(long accountId);

    @Query("SELECT SUM(amount) FROM transactions WHERE account_id = :accountId")
    double getAccountBalanceSync(long accountId);

    @Query("SELECT * FROM transactions WHERE sync_status != :syncStatus AND last_sync_time > :lastSyncTime")
    List<Transaction> getModifiedTransactions(long lastSyncTime, int syncStatus);

    @Query("SELECT * FROM transactions WHERE server_id = -1")
    List<Transaction> getNewTransactions();

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY transaction_date DESC")
    LiveData<List<Transaction>> getTransactionsByType(String type);

    @Query("SELECT * FROM transactions WHERE account_id = :accountId AND transaction_date BETWEEN :fromDate AND :toDate ORDER BY transaction_date DESC")
    LiveData<List<Transaction>> getTransactionsByAccountAndDateRange(long accountId, long fromDate, long toDate);

    @Query("SELECT SUM(CASE WHEN type = 'credit' THEN amount ELSE -amount END) FROM transactions WHERE account_id = :accountId AND transaction_date <= :transactionDate AND currency = :currency")
    LiveData<Double> getBalanceUntilDate(long accountId, long transactionDate, String currency);

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

    @Query("SELECT * FROM transactions WHERE sync_status = :syncStatus")
    List<Transaction> getPendingSyncTransactions();

    @Query("SELECT * FROM transactions WHERE sync_status != :syncStatus")
    List<Transaction> getTransactionsForSync();

    @Query("SELECT * FROM transactions WHERE server_id = -1")
    List<Transaction> getTransactionsToMigrate();
} 
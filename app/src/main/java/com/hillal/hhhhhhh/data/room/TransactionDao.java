package com.hillal.hhhhhhh.data.room;

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
    @Insert
    void insert(Transaction transaction);

    @Insert
    void insertAll(List<Transaction> transactions);

    @Update
    void update(Transaction transaction);

    @Delete
    void delete(Transaction transaction);

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

    @Query("SELECT * FROM transactions ORDER BY transaction_date DESC")
    LiveData<List<Transaction>> getAllTransactions();

    @Query("SELECT * FROM transactions")
    List<Transaction> getAllTransactionsSync();

    @Query("SELECT SUM(CASE WHEN type = 'debit' THEN amount ELSE -amount END) FROM transactions WHERE account_id = :accountId")
    LiveData<Double> getAccountBalance(long accountId);

    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    LiveData<Transaction> getTransactionById(long transactionId);

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY transaction_date DESC")
    LiveData<List<Transaction>> getTransactionsByType(String type);

    @Query("SELECT * FROM transactions WHERE transaction_date BETWEEN :fromDate AND :toDate ORDER BY transaction_date DESC")
    LiveData<List<Transaction>> getTransactionsByDateRange(long fromDate, long toDate);

    @Query("SELECT * FROM transactions WHERE account_id = :accountId ORDER BY transaction_date DESC")
    LiveData<List<Transaction>> getTransactionsByAccount(long accountId);

    @Query("SELECT * FROM transactions WHERE account_id = :accountId AND transaction_date BETWEEN :fromDate AND :toDate ORDER BY transaction_date DESC")
    LiveData<List<Transaction>> getTransactionsByAccountAndDateRange(long accountId, long fromDate, long toDate);

    @Query("SELECT SUM(amount) FROM transactions WHERE account_id = :accountId AND type = 'debit'")
    LiveData<Double> getTotalDebit(long accountId);

    @Query("SELECT SUM(amount) FROM transactions WHERE account_id = :accountId AND type = 'credit'")
    LiveData<Double> getTotalCredit(long accountId);

    @Query("SELECT * FROM transactions WHERE server_id < 0")
    List<Transaction> getNewTransactions();

    @Query("SELECT * FROM transactions WHERE server_id = :serverId")
    Transaction getTransactionByServerIdSync(long serverId);

    @Query("DELETE FROM transactions")
    void deleteAllTransactions();

    @Query("SELECT * FROM transactions WHERE account_id = :accountId")
    List<Transaction> getTransactionsByAccountId(long accountId);
} 
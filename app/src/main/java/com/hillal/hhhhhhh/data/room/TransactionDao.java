package com.hillal.hhhhhhh.data.room;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
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

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC")
    LiveData<List<Transaction>> getTransactionsForAccount(long accountId);

    @Query("SELECT * FROM transactions WHERE accountId = :accountId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    LiveData<List<Transaction>> getTransactionsForAccountInDateRange(long accountId, Date startDate, Date endDate);

    @Query("SELECT SUM(amount) FROM transactions WHERE accountId = :accountId AND type = 'credit'")
    LiveData<Double> getTotalCredits(long accountId);

    @Query("SELECT SUM(amount) FROM transactions WHERE accountId = :accountId AND type = 'debit'")
    LiveData<Double> getTotalDebits(long accountId);

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC LIMIT 1")
    LiveData<Transaction> getLastTransaction(long accountId);

    @Query("SELECT SUM(amount) FROM transactions WHERE accountId = :accountId AND type = 'credit' AND date BETWEEN :startDate AND :endDate")
    LiveData<Double> getTotalCreditsInDateRange(long accountId, Date startDate, Date endDate);

    @Query("SELECT SUM(amount) FROM transactions WHERE accountId = :accountId AND type = 'debit' AND date BETWEEN :startDate AND :endDate")
    LiveData<Double> getTotalDebitsInDateRange(long accountId, Date startDate, Date endDate);

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    LiveData<List<Transaction>> getAllTransactions();

    @Query("SELECT SUM(CASE WHEN type = 'debit' THEN amount ELSE -amount END) FROM transactions WHERE accountId = :accountId")
    LiveData<Double> getAccountBalance(long accountId);

    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    LiveData<Transaction> getTransactionById(long transactionId);

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    LiveData<List<Transaction>> getTransactionsByType(String type);

    @Query("SELECT * FROM transactions WHERE date BETWEEN :fromDate AND :toDate ORDER BY date DESC")
    LiveData<List<Transaction>> getTransactionsByDateRange(long fromDate, long toDate);

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC")
    LiveData<List<Transaction>> getTransactionsByAccount(long accountId);

    @Query("SELECT * FROM transactions WHERE accountId = :accountId AND date BETWEEN :fromDate AND :toDate ORDER BY date DESC")
    LiveData<List<Transaction>> getTransactionsByAccountAndDateRange(long accountId, long fromDate, long toDate);

    @Query("SELECT SUM(amount) FROM transactions WHERE accountId = :accountId AND type = 'debit'")
    LiveData<Double> getTotalDebit(long accountId);

    @Query("SELECT SUM(amount) FROM transactions WHERE accountId = :accountId AND type = 'credit'")
    LiveData<Double> getTotalCredit(long accountId);

    @Query("SELECT * FROM transactions WHERE updatedAt > :timestamp AND serverId != 0")
    List<Transaction> getModifiedTransactions(long timestamp);

    @Query("SELECT * FROM transactions WHERE serverId = 0")
    List<Transaction> getNewTransactions();

    @Query("SELECT * FROM transactions WHERE updatedAt > :timestamp AND serverId != 0")
    List<Transaction> getTransactionsModifiedAfter(long timestamp);
} 
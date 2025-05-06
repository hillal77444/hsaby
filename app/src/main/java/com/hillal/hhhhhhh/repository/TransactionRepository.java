package com.hillal.hhhhhhh.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.hillal.hhhhhhh.data.room.TransactionDao;
import com.hillal.hhhhhhh.data.model.Transaction;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransactionRepository {
    private final TransactionDao transactionDao;
    private final ExecutorService executorService;

    public TransactionRepository(TransactionDao transactionDao) {
        this.transactionDao = transactionDao;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<Transaction>> getAllTransactions() {
        return transactionDao.getAllTransactions();
    }

    public LiveData<List<Transaction>> getTransactionsForAccount(long accountId) {
        return transactionDao.getTransactionsForAccount(accountId);
    }

    public LiveData<Double> getAccountBalance(long accountId) {
        return transactionDao.getAccountBalance(accountId);
    }

    public void insert(Transaction transaction) {
        executorService.execute(() -> transactionDao.insert(transaction));
    }

    public void update(Transaction transaction) {
        executorService.execute(() -> transactionDao.update(transaction));
    }

    public void delete(Transaction transaction) {
        executorService.execute(() -> transactionDao.delete(transaction));
    }

    public LiveData<List<Transaction>> getTransactionsByDateRange(long fromDate, long toDate) {
        return transactionDao.getTransactionsByDateRange(fromDate, toDate);
    }
} 
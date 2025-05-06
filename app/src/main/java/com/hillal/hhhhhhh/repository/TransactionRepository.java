package com.hillal.hhhhhhh.repository;

import androidx.lifecycle.LiveData;
import com.hillal.hhhhhhh.data.dao.TransactionDao;
import com.hillal.hhhhhhh.data.entities.Transaction;
import java.util.List;

public class TransactionRepository {
    private final TransactionDao transactionDao;

    public TransactionRepository(TransactionDao transactionDao) {
        this.transactionDao = transactionDao;
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
        new Thread(() -> transactionDao.insert(transaction)).start();
    }

    public void update(Transaction transaction) {
        new Thread(() -> transactionDao.update(transaction)).start();
    }

    public void delete(Transaction transaction) {
        new Thread(() -> transactionDao.delete(transaction)).start();
    }

    public LiveData<List<Transaction>> getTransactionsByDateRange(long accountId, long fromDate, long toDate) {
        return transactionDao.getTransactionsByDateRange(accountId, fromDate, toDate);
    }
} 
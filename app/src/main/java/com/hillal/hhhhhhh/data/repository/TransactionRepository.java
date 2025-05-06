package com.hillal.hhhhhhh.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.hillal.hhhhhhh.data.model.Transaction;
import com.hillal.hhhhhhh.data.room.AppDatabase;
import com.hillal.hhhhhhh.data.room.TransactionDao;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransactionRepository {
    private final TransactionDao transactionDao;
    private final ExecutorService executorService;

    public TransactionRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        transactionDao = db.transactionDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<Transaction>> getAllTransactions() {
        return transactionDao.getAllTransactions();
    }

    public LiveData<List<Transaction>> getTransactionsByAccount(long accountId) {
        return transactionDao.getTransactionsByAccount(accountId);
    }

    public LiveData<List<Transaction>> getTransactionsByType(String type) {
        return transactionDao.getTransactionsByType(type);
    }

    public LiveData<List<Transaction>> getTransactionsByCurrency(String currency) {
        return transactionDao.getTransactionsByCurrency(currency);
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

    public LiveData<Transaction> getById(long id) {
        return transactionDao.getTransactionById(id);
    }

    public LiveData<List<Transaction>> getTransactionsByDateRange(long fromDate, long toDate) {
        return transactionDao.getTransactionsByDateRange(fromDate, toDate);
    }

    public LiveData<List<Transaction>> getTransactionsByAccountAndDateRange(long accountId, long fromDate, long toDate) {
        return transactionDao.getTransactionsByAccountAndDateRange(accountId, fromDate, toDate);
    }

    public LiveData<Double> getTotalDebit(long accountId) {
        return transactionDao.getTotalDebit(accountId);
    }

    public LiveData<Double> getTotalCredit(long accountId) {
        return transactionDao.getTotalCredit(accountId);
    }
}
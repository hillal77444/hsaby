package com.hillal.hhhhhhh.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.hillal.hhhhhhh.data.room.AppDatabase;
import com.hillal.hhhhhhh.data.room.TransactionDao;
import com.hillal.hhhhhhh.data.model.Transaction;
import java.util.Date;
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

    public LiveData<List<Transaction>> getTransactionsForAccount(long accountId) {
        return transactionDao.getTransactionsForAccount(accountId);
    }

    public LiveData<List<Transaction>> getTransactionsForAccountInDateRange(long accountId, Date startDate, Date endDate) {
        return transactionDao.getTransactionsForAccountInDateRange(accountId, startDate, endDate);
    }

    public LiveData<Double> getTotalCredits(long accountId) {
        return transactionDao.getTotalCredits(accountId);
    }

    public LiveData<Double> getTotalDebits(long accountId) {
        return transactionDao.getTotalDebits(accountId);
    }

    public LiveData<Transaction> getLastTransaction(long accountId) {
        return transactionDao.getLastTransaction(accountId);
    }

    public LiveData<Double> getTotalCreditsInDateRange(long accountId, Date startDate, Date endDate) {
        return transactionDao.getTotalCreditsInDateRange(accountId, startDate, endDate);
    }

    public LiveData<Double> getTotalDebitsInDateRange(long accountId, Date startDate, Date endDate) {
        return transactionDao.getTotalDebitsInDateRange(accountId, startDate, endDate);
    }

    public LiveData<List<Transaction>> getAllTransactions() {
        return transactionDao.getAllTransactions();
    }

    public LiveData<Double> getAccountBalance(long accountId) {
        return transactionDao.getAccountBalance(accountId);
    }

    public LiveData<Transaction> getTransactionById(long transactionId) {
        return transactionDao.getTransactionById(transactionId);
    }

    public LiveData<List<Transaction>> getTransactionsByType(String type) {
        return transactionDao.getTransactionsByType(type);
    }

    public LiveData<List<Transaction>> getTransactionsByDateRange(Date startDate, Date endDate) {
        return transactionDao.getTransactionsByDateRange(startDate, endDate);
    }

    public LiveData<List<Transaction>> getTransactionsByAccount(long accountId) {
        return transactionDao.getTransactionsByAccount(accountId);
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

    public List<Transaction> getModifiedTransactions(long timestamp) {
        return transactionDao.getModifiedTransactions(timestamp);
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

    public LiveData<Double> getTotalDebtors() {
        return transactionDao.getTotalDebtors();
    }

    public LiveData<Double> getTotalCreditors() {
        return transactionDao.getTotalCreditors();
    }
}
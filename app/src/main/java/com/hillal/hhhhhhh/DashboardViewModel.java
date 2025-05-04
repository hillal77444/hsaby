package com.hillal.hhhhhhh;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardViewModel extends AndroidViewModel {
    private AppDatabase database;
    private LiveData<List<Account>> accounts;
    private LiveData<Double> totalCreditor;
    private LiveData<Double> totalDebtor;
    private static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(4);

    public DashboardViewModel(Application application) {
        super(application);
        database = AppDatabase.getInstance(application);
        accounts = database.accountDao().getAllAccounts();
        totalCreditor = database.accountDao().getTotalCreditor();
        totalDebtor = database.accountDao().getTotalDebtor();
    }

    public LiveData<List<Account>> getAllAccounts() {
        return accounts;
    }

    public LiveData<Account> getAccountById(int accountId) {
        return database.accountDao().getAccountById(accountId);
    }

    public LiveData<List<Transaction>> getTransactionsByAccount(int accountId) {
        return database.transactionDao().getTransactionsByAccount(accountId);
    }

    public LiveData<Double> getTotalCreditor() {
        return totalCreditor;
    }

    public LiveData<Double> getTotalDebtor() {
        return totalDebtor;
    }

    public void insertAccount(Account account) {
        databaseWriteExecutor.execute(() -> {
            database.accountDao().insert(account);
        });
    }

    public void deleteAccount(Account account) {
        databaseWriteExecutor.execute(() -> {
            database.accountDao().delete(account);
        });
    }

    public void insertTransaction(Transaction transaction) {
        databaseWriteExecutor.execute(() -> {
            database.transactionDao().insert(transaction);
        });
    }

    public void deleteTransaction(Transaction transaction) {
        databaseWriteExecutor.execute(() -> {
            database.transactionDao().delete(transaction);
        });
    }
} 
package com.hillal.hhhhhhh.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.hillal.hhhhhhh.data.room.AppDatabase;
import com.hillal.hhhhhhh.data.entities.Transaction;
import java.util.Date;
import java.util.List;

public class TransactionViewModel extends AndroidViewModel {
    private final AppDatabase database;
    private final TransactionRepository repository;

    public TransactionViewModel(Application application) {
        super(application);
        database = AppDatabase.getInstance(application);
        repository = new TransactionRepository(database.transactionDao());
    }

    public LiveData<List<Transaction>> getAllTransactions() {
        return database.transactionDao().getAllTransactions();
    }

    public LiveData<List<Transaction>> getTransactionsByAccount(long accountId) {
        return database.transactionDao().getTransactionsByAccount(accountId);
    }

    public LiveData<List<Transaction>> getTransactionsByAccountAndDateRange(long accountId, Date fromDate, Date toDate) {
        return database.transactionDao().getTransactionsByAccountAndDateRange(accountId, fromDate, toDate);
    }

    public void insert(Transaction transaction) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            database.transactionDao().insert(transaction);
        });
    }

    public void update(Transaction transaction) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            database.transactionDao().update(transaction);
        });
    }

    public void delete(Transaction transaction) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            database.transactionDao().delete(transaction);
        });
    }

    public LiveData<List<Transaction>> getTransactionsForAccount(long accountId) {
        return database.transactionDao().getTransactionsForAccount(accountId);
    }

    public LiveData<Double> getAccountBalance(long accountId) {
        return database.transactionDao().getAccountBalance(accountId);
    }

    public LiveData<List<Transaction>> getTransactionsByDateRange(long accountId, long fromDate, long toDate) {
        return database.transactionDao().getTransactionsByDateRange(accountId, fromDate, toDate);
    }
} 
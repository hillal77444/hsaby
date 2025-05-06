package com.hillal.hhhhhhh.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.hillal.hhhhhhh.data.room.AppDatabase;
import com.hillal.hhhhhhh.data.model.Transaction;
import com.hillal.hhhhhhh.repository.TransactionRepository;
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
        return repository.getAllTransactions();
    }

    public LiveData<List<Transaction>> getTransactionsForAccount(long accountId) {
        return repository.getTransactionsForAccount(accountId);
    }

    public void insert(Transaction transaction) {
        repository.insert(transaction);
    }

    public void update(Transaction transaction) {
        repository.update(transaction);
    }

    public void delete(Transaction transaction) {
        repository.delete(transaction);
    }

    public LiveData<Double> getAccountBalance(long accountId) {
        return repository.getAccountBalance(accountId);
    }

    public LiveData<List<Transaction>> getTransactionsByDateRange(long fromDate, long toDate) {
        return repository.getTransactionsByDateRange(fromDate, toDate);
    }
} 
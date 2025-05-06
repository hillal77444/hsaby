package com.hillal.hhhhhhh.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.hillal.hhhhhhh.data.model.Transaction;
import com.hillal.hhhhhhh.data.repository.TransactionRepository;
import java.util.List;

public class TransactionViewModel extends AndroidViewModel {
    private final TransactionRepository repository;
    private final LiveData<List<Transaction>> allTransactions;

    public TransactionViewModel(Application application) {
        super(application);
        repository = ((com.hillal.hhhhhhh.App) application).getTransactionRepository();
        allTransactions = repository.getAllTransactions();
    }

    public LiveData<List<Transaction>> getAllTransactions() {
        return allTransactions;
    }

    public LiveData<List<Transaction>> getTransactionsForAccount(long accountId) {
        return repository.getTransactionsForAccount(accountId);
    }

    public LiveData<Double> getAccountBalance(long accountId) {
        return repository.getAccountBalance(accountId);
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

    public LiveData<List<Transaction>> getTransactionsByDateRange(long accountId, long fromDate, long toDate) {
        return repository.getTransactionsByDateRange(accountId, fromDate, toDate);
    }
} 
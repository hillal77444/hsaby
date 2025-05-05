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
        repository = new TransactionRepository(application);
        allTransactions = repository.getAllTransactions();
    }

    public LiveData<List<Transaction>> getAllTransactions() {
        return allTransactions;
    }

    public void insertTransaction(Transaction transaction) {
        repository.insert(transaction);
    }

    public void updateTransaction(Transaction transaction) {
        repository.update(transaction);
    }

    public void deleteTransaction(Transaction transaction) {
        repository.delete(transaction);
    }

    public LiveData<List<Transaction>> getTransactionsByAccountId(long accountId) {
        return repository.getTransactionsByAccountId(accountId);
    }
} 
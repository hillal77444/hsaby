package com.hillal.hhhhhhh.ui.transactions;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.hillal.hhhhhhh.data.model.Transaction;
import com.hillal.hhhhhhh.data.repository.AccountRepository;

import java.util.List;

public class TransactionViewModel extends ViewModel {
    private final AccountRepository accountRepository;

    public TransactionViewModel(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public LiveData<List<Transaction>> getTransactionsForAccount(long accountId) {
        return accountRepository.getTransactionsForAccount(accountId);
    }

    public LiveData<Double> getAccountBalance(long accountId) {
        return accountRepository.getAccountBalance(accountId);
    }

    public void insertTransaction(Transaction transaction) {
        accountRepository.insertTransaction(transaction);
    }

    public void updateTransaction(Transaction transaction) {
        accountRepository.updateTransaction(transaction);
    }

    public void deleteTransaction(Transaction transaction) {
        accountRepository.deleteTransaction(transaction);
    }
} 
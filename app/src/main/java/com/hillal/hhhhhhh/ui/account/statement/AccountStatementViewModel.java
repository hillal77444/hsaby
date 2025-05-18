package com.hillal.hhhhhhh.ui.account.statement;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.model.Transaction;
import com.hillal.hhhhhhh.data.repository.AccountStatementRepository;

import java.util.List;

public class AccountStatementViewModel extends ViewModel {
    private final AccountStatementRepository repository;

    public AccountStatementViewModel(AccountStatementRepository repository) {
        this.repository = repository;
    }

    public double getAccountBalance(long accountId) {
        return repository.getAccountBalance(accountId);
    }

    public LiveData<List<Transaction>> getTransactionsForAccount(long accountId) {
        return repository.getTransactionsForAccount(accountId);
    }

    public LiveData<List<Transaction>> getTransactionsForAccountInDateRange(long accountId, long startDate, long endDate) {
        return repository.getTransactionsForAccountInDateRange(accountId, startDate, endDate);
    }

    public LiveData<Double> getTotalCredits(long accountId) {
        return repository.getTotalCredits(accountId);
    }

    public LiveData<Double> getTotalDebits(long accountId) {
        return repository.getTotalDebits(accountId);
    }

    public LiveData<Account> getAccountById(long accountId) {
        return repository.getAccountById(accountId);
    }
} 
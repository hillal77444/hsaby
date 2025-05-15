package com.hillal.hhhhhhh.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.model.Transaction;
import com.hillal.hhhhhhh.repository.AccountStatementRepository;

import java.util.List;

public class AccountStatementViewModel extends AndroidViewModel {
    private final AccountStatementRepository repository;
    private long currentAccountId = -1;
    private long startDate;
    private long endDate;

    public AccountStatementViewModel(Application application) {
        super(application);
        repository = new AccountStatementRepository(application);
    }

    public void setCurrentAccount(long accountId) {
        this.currentAccountId = accountId;
    }

    public void setDateRange(long startDate, long endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public LiveData<Account> getCurrentAccount() {
        return repository.getAccount(currentAccountId);
    }

    public LiveData<List<Transaction>> getTransactionsForCurrentAccount() {
        if (currentAccountId == -1) {
            return null;
        }
        return repository.getTransactionsForAccountInDateRange(currentAccountId, startDate, endDate);
    }

    public LiveData<Double> getTotalCreditsForCurrentAccount() {
        if (currentAccountId == -1) {
            return null;
        }
        return repository.getTotalCreditsInDateRange(currentAccountId, startDate, endDate);
    }

    public LiveData<Double> getTotalDebitsForCurrentAccount() {
        if (currentAccountId == -1) {
            return null;
        }
        return repository.getTotalDebitsInDateRange(currentAccountId, startDate, endDate);
    }

    public LiveData<List<Transaction>> getTransactionsForAccount(long accountId) {
        return repository.getTransactionsForAccountInDateRange(accountId, startDate, endDate);
    }

    public LiveData<Double> getTotalCreditsForAccount(long accountId) {
        return repository.getTotalCreditsInDateRange(accountId, startDate, endDate);
    }

    public LiveData<Double> getTotalDebitsForAccount(long accountId) {
        return repository.getTotalDebitsInDateRange(accountId, startDate, endDate);
    }

    public LiveData<List<Account>> getAllAccounts() {
        return repository.getAllAccounts();
    }

    public LiveData<List<Transaction>> getTransactionsForAccountInDateRange(long accountId, long startDate, long endDate) {
        return repository.getTransactionsForAccountInDateRange(accountId, startDate, endDate);
    }
} 
package com.hillal.hhhhhhh.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.model.Transaction;
import com.hillal.hhhhhhh.repository.AccountStatementRepository;

import java.util.Date;
import java.util.List;

public class AccountStatementViewModel extends AndroidViewModel {
    private final AccountStatementRepository repository;
    private long currentAccountId;
    private Date startDate;
    private Date endDate;

    public AccountStatementViewModel(Application application) {
        super(application);
        repository = new AccountStatementRepository(application);
    }

    public void setAccountId(long accountId) {
        this.currentAccountId = accountId;
    }

    public void setDateRange(Date startDate, Date endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public LiveData<Account> getAccount() {
        return repository.getAccount(currentAccountId);
    }

    public LiveData<List<Transaction>> getTransactions() {
        if (startDate != null && endDate != null) {
            return repository.getTransactionsForAccountInDateRange(currentAccountId, startDate, endDate);
        }
        return repository.getTransactionsForAccount(currentAccountId);
    }

    public LiveData<Double> getTotalCredits() {
        return repository.getTotalCredits(currentAccountId);
    }

    public LiveData<Double> getTotalDebits() {
        return repository.getTotalDebits(currentAccountId);
    }

    public LiveData<Transaction> getLastTransaction() {
        return repository.getLastTransaction(currentAccountId);
    }

    public LiveData<List<Account>> getAllAccounts() {
        return repository.getAllAccounts();
    }

    public LiveData<List<Transaction>> getTransactionsForAccountInDateRange(long accountId, Date startDate, Date endDate) {
        return repository.getTransactionsForAccountInDateRange(accountId, startDate, endDate);
    }

    public LiveData<Double> getTotalCredits(long accountId, Date startDate, Date endDate) {
        return repository.getTotalCredits(accountId, startDate, endDate);
    }

    public LiveData<Double> getTotalDebits(long accountId, Date startDate, Date endDate) {
        return repository.getTotalDebits(accountId, startDate, endDate);
    }
} 
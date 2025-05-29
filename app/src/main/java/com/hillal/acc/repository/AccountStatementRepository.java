package com.hillal.acc.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.hillal.acc.data.room.AppDatabase;
import com.hillal.acc.data.room.AccountDao;
import com.hillal.acc.data.room.TransactionDao;
import com.hillal.acc.data.model.Account;
import com.hillal.acc.data.model.Transaction;

import java.util.Date;
import java.util.List;

public class AccountStatementRepository {
    private final TransactionDao transactionDao;
    private final AccountDao accountDao;

    public AccountStatementRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        transactionDao = db.transactionDao();
        accountDao = db.accountDao();
    }

    public LiveData<Account> getAccount(long accountId) {
        return accountDao.getAccountById(accountId);
    }

    public LiveData<List<Transaction>> getTransactionsForAccount(long accountId) {
        return transactionDao.getTransactionsForAccount(accountId);
    }

    public LiveData<List<Transaction>> getTransactionsForAccountInDateRange(long accountId, long startDate, long endDate) {
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

    public LiveData<List<Account>> getAllAccounts() {
        return accountDao.getAllAccounts();
    }

    public LiveData<Double> getTotalCreditsInDateRange(long accountId, long startDate, long endDate) {
        return transactionDao.getTotalCreditsInDateRange(accountId, startDate, endDate);
    }

    public LiveData<Double> getTotalDebitsInDateRange(long accountId, long startDate, long endDate) {
        return transactionDao.getTotalDebitsInDateRange(accountId, startDate, endDate);
    }
} 
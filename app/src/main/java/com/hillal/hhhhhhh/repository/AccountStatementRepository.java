package com.hillal.hhhhhhh.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.hillal.hhhhhhh.data.AppDatabase;
import com.hillal.hhhhhhh.data.dao.AccountDao;
import com.hillal.hhhhhhh.data.dao.TransactionDao;
import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.model.Transaction;

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

    public LiveData<List<Transaction>> getTransactionsForAccountInDateRange(long accountId, Date startDate, Date endDate) {
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

    public LiveData<Double> getTotalCredits(long accountId, Date startDate, Date endDate) {
        return transactionDao.getTotalCreditsInDateRange(accountId, startDate, endDate);
    }

    public LiveData<Double> getTotalDebits(long accountId, Date startDate, Date endDate) {
        return transactionDao.getTotalDebitsInDateRange(accountId, startDate, endDate);
    }
} 
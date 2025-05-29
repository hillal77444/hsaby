package com.hillal.acc.data.repository;

import androidx.lifecycle.LiveData;
import com.hillal.acc.data.dao.AccountDao;
import com.hillal.acc.data.dao.TransactionDao;
import com.hillal.acc.data.model.Account;
import com.hillal.acc.data.model.Transaction;

import java.util.List;

public class AccountStatementRepository {
    private final TransactionDao transactionDao;
    private final AccountDao accountDao;

    public AccountStatementRepository(TransactionDao transactionDao, AccountDao accountDao) {
        this.transactionDao = transactionDao;
        this.accountDao = accountDao;
    }

    public double getAccountBalance(long accountId) {
        return transactionDao.getAccountBalanceSync(accountId);
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

    public LiveData<Account> getAccountById(long accountId) {
        return accountDao.getAccountById(accountId);
    }
} 
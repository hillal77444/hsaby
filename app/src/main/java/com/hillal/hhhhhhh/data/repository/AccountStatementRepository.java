package com.hillal.hhhhhhh.data.repository;

import com.hillal.hhhhhhh.data.dao.AccountDao;
import com.hillal.hhhhhhh.data.dao.TransactionDao;
import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.model.Transaction;

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
} 
package com.hillal.hhhhhhh.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.model.Transaction;
import com.hillal.hhhhhhh.data.room.AccountDao;
import com.hillal.hhhhhhh.data.room.AppDatabase;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AccountRepository {
    private final AccountDao accountDao;
    private final ExecutorService executorService;
    private final MutableLiveData<List<Account>> recentAccounts = new MutableLiveData<>();
    private final AppDatabase database;

    public AccountRepository(AccountDao accountDao, AppDatabase database) {
        this.accountDao = accountDao;
        this.executorService = Executors.newSingleThreadExecutor();
        this.database = database;
        loadRecentAccounts();
    }

    private void loadRecentAccounts() {
        executorService.execute(() -> {
            List<Account> accounts = accountDao.getRecentAccounts();
            recentAccounts.postValue(accounts);
        });
    }

    public LiveData<List<Account>> getRecentAccounts() {
        return recentAccounts;
    }

    public void insertAccount(Account account) {
        executorService.execute(() -> accountDao.insert(account));
    }

    public void updateAccount(Account account) {
        executorService.execute(() -> accountDao.update(account));
    }

    public void deleteAccount(Account account) {
        executorService.execute(() -> accountDao.delete(account));
    }

    public LiveData<Account> getAccountById(long id) {
        return accountDao.getAccountById(id);
    }

    public LiveData<List<Account>> getAllAccounts() {
        return accountDao.getAllAccounts();
    }

    public LiveData<List<Account>> searchAccounts(String query) {
        return accountDao.searchAccounts("%" + query + "%");
    }

    public LiveData<List<Transaction>> getTransactionsForAccount(long accountId) {
        return database.transactionDao().getTransactionsForAccount(accountId);
    }

    public LiveData<Double> getAccountBalance(long accountId) {
        return database.transactionDao().getAccountBalance(accountId);
    }

    public void insertTransaction(Transaction transaction) {
        database.transactionDao().insertTransaction(transaction);
    }

    public void updateTransaction(Transaction transaction) {
        database.transactionDao().updateTransaction(transaction);
    }

    public void deleteTransaction(Transaction transaction) {
        database.transactionDao().deleteTransaction(transaction);
    }

    public void backupData() {
        // TODO: Implement backup functionality
    }

    public void restoreData() {
        // TODO: Implement restore functionality
    }

    public void clearAllData() {
        database.clearAllTables();
    }
} 
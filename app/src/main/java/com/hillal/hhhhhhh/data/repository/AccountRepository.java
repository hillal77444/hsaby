package com.hillal.hhhhhhh.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.room.AccountDao;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AccountRepository {
    private final AccountDao accountDao;
    private final ExecutorService executorService;
    private final MutableLiveData<List<Account>> recentAccounts = new MutableLiveData<>();

    public AccountRepository(AccountDao accountDao) {
        this.accountDao = accountDao;
        this.executorService = Executors.newSingleThreadExecutor();
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
} 
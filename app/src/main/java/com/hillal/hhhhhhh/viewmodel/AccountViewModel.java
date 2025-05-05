package com.hillal.hhhhhhh.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.hillal.hhhhhhh.data.AppDatabase;
import com.hillal.hhhhhhh.data.dao.AccountDao;
import com.hillal.hhhhhhh.data.entities.Account;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AccountViewModel extends AndroidViewModel {
    private final AccountDao accountDao;
    private final ExecutorService executorService;

    public AccountViewModel(Application application) {
        super(application);
        accountDao = AppDatabase.getInstance(application).accountDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<Account>> getAllAccounts() {
        return accountDao.getAllAccounts();
    }

    public LiveData<Account> getAccountById(long accountId) {
        return accountDao.getAccountById(accountId);
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

    public LiveData<List<Account>> searchAccounts(String query) {
        return accountDao.searchAccounts("%" + query + "%");
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
} 
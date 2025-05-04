package com.hillal.hhhhhhh;

import android.app.Application;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AccountRepository {
    private final AccountDao accountDao;
    private final ExecutorService databaseWriteExecutor;

    public AccountRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        accountDao = db.accountDao();
        databaseWriteExecutor = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<Account>> getAllAccounts() {
        return accountDao.getAllAccounts();
    }

    public LiveData<Account> getAccountById(int accountId) {
        return accountDao.getAccountById(accountId);
    }

    public LiveData<Double> getTotalCreditor() {
        return accountDao.getTotalCreditor();
    }

    public LiveData<Double> getTotalDebtor() {
        return accountDao.getTotalDebtor();
    }

    public LiveData<Double> getNetBalance() {
        return accountDao.getNetBalance();
    }

    public void insert(Account account) {
        databaseWriteExecutor.execute(() -> accountDao.insert(account));
    }

    public void update(Account account) {
        databaseWriteExecutor.execute(() -> accountDao.update(account));
    }

    public void delete(Account account) {
        databaseWriteExecutor.execute(() -> accountDao.delete(account));
    }
} 
package com.hillal.hhhhhhh.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.model.Transaction;
import com.hillal.hhhhhhh.data.room.AccountDao;
import com.hillal.hhhhhhh.data.room.TransactionDao;
import com.hillal.hhhhhhh.data.room.AppDatabase;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AccountRepository {
    private final AccountDao accountDao;
    private final TransactionDao transactionDao;
    private final ExecutorService executorService;
    private final MutableLiveData<List<Account>> recentAccounts = new MutableLiveData<>();
    private final AppDatabase database;

    public AccountRepository(AccountDao accountDao, AppDatabase database) {
        this.accountDao = accountDao;
        this.transactionDao = database.transactionDao();
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
        return database.transactionDao().getTransactionsByAccount(accountId);
    }

    public LiveData<Double> getAccountBalance(long accountId) {
        return database.transactionDao().getAccountBalance(accountId);
    }

    public LiveData<Double> getAccountBalanceYemeni(long accountId) {
        return database.transactionDao().getAccountBalanceYemeni(accountId);
    }

    public Account getAccountByNumberSync(String accountNumber) {
        return accountDao.getAccountByNumberSync(accountNumber);
    }

    public Account getAccountByPhoneNumberSync(String phoneNumber) {
        return accountDao.getAccountByPhoneNumberSync(phoneNumber);
    }

    private String generateUniqueAccountNumber() {
        String accountNumber;
        do {
            // توليد رقم حساب جديد بتنسيق: ACC-YYYYMMDD-XXXX
            // YYYYMMDD: التاريخ
            // XXXX: رقم تسلسلي من 0001 إلى 9999
            String date = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
            int random = (int)(Math.random() * 9000) + 1000; // رقم بين 1000 و 9999
            accountNumber = "ACC-" + date + "-" + random;
        } while (accountDao.getAccountByNumberSync(accountNumber) != null);
        return accountNumber;
    }

    public void insertTransaction(Transaction transaction) {
        executorService.execute(() -> database.transactionDao().insert(transaction));
    }

    public void updateTransaction(Transaction transaction) {
        executorService.execute(() -> database.transactionDao().update(transaction));
    }

    public void deleteTransaction(Transaction transaction) {
        executorService.execute(() -> database.transactionDao().delete(transaction));
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
package com.hillal.acc.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.hillal.acc.data.model.Account;
import com.hillal.acc.data.model.Transaction;
import com.hillal.acc.data.room.AccountDao;
import com.hillal.acc.data.room.TransactionDao;
import com.hillal.acc.data.room.AppDatabase;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.HashMap;
import java.util.Map;

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

    public Account getAccountByPhoneNumber(String phoneNumber) {
        try {
            return executorService.submit(() -> accountDao.getAccountByPhoneNumber(phoneNumber)).get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String generateUniqueAccountNumber() {
        while (true) {
            try {
                // توليد رقم حساب جديد بتنسيق: ACC-YYYYMMDD-XXXX
                // YYYYMMDD: التاريخ
                // XXXX: رقم تسلسلي من 0001 إلى 9999
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US);
                String date = sdf.format(new java.util.Date());
                int random = (int)(Math.random() * 90000) + 1000; // رقم بين 1000 و 9999
                final String accountNumber = "ACC" + date + random;
                
                if (executorService.submit(() -> accountDao.getAccountByNumberSync(accountNumber)).get() == null) {
                    return accountNumber;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
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

    // دالة تعيد جميع أرصدة الحسابات بالريال اليمني كخريطة
    public LiveData<Map<Long, Double>> getAllAccountsBalancesYemeniMap() {
        MutableLiveData<Map<Long, Double>> balancesLiveData = new MutableLiveData<>();
        getAllAccounts().observeForever(new Observer<List<Account>>() {
            @Override
            public void onChanged(List<Account> accounts) {
                if (accounts == null) {
                    balancesLiveData.postValue(new HashMap<>());
                    return;
                }
                Map<Long, Double> balancesMap = new HashMap<>();
                for (Account account : accounts) {
                    long accountId = account.getId();
                    LiveData<Double> balanceLiveData = transactionDao.getAccountBalanceYemeni(accountId);
                    balanceLiveData.observeForever(new Observer<Double>() {
                        @Override
                        public void onChanged(Double balance) {
                            balancesMap.put(accountId, balance != null ? balance : 0.0);
                            balancesLiveData.postValue(new HashMap<>(balancesMap));
                        }
                    });
                }
            }
        });
        return balancesLiveData;
    }

    // دالة جديدة: جلب الأرصدة دفعة واحدة فقط
    public Map<Long, Double> getAllAccountsBalancesYemeniMapOnce() {
        List<Account> accounts = accountDao.getAllAccountsSync();
        Map<Long, Double> balancesMap = new HashMap<>();
        for (Account account : accounts) {
            Double balance = transactionDao.getAccountBalanceYemeniSync(account.getId());
            balancesMap.put(account.getId(), balance != null ? balance : 0.0);
        }
        return balancesMap;
    }
} 
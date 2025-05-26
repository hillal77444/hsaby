package com.hillal.hhhhhhh.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.repository.AccountRepository;
import com.hillal.hhhhhhh.data.room.AppDatabase;
import java.util.List;

public class AccountViewModel extends AndroidViewModel {
    private final AccountRepository repository;
    private final LiveData<List<Account>> allAccounts;

    public AccountViewModel(Application application) {
        super(application);
        AppDatabase database = AppDatabase.getInstance(application);
        repository = new AccountRepository(database.accountDao(), database);
        allAccounts = repository.getAllAccounts();
    }

    public LiveData<List<Account>> getAllAccounts() {
        return allAccounts;
    }

    public void insertAccount(Account account) {
        repository.insertAccount(account);
    }

    public void updateAccount(Account account) {
        repository.updateAccount(account);
    }

    public void deleteAccount(Account account) {
        repository.deleteAccount(account);
    }

    public LiveData<Account> getAccountById(long id) {
        return repository.getAccountById(id);
    }

    public LiveData<List<Account>> searchAccounts(String query) {
        return repository.searchAccounts(query);
    }

    public LiveData<Double> getAccountBalanceYemeni(long accountId) {
        return repository.getAccountBalanceYemeni(accountId);
    }

    public Account getAccountByNumberSync(String accountNumber) {
        return repository.getAccountByNumberSync(accountNumber);
    }

    public Account getAccountByPhoneNumberSync(String phoneNumber) {
        return repository.getAccountByPhoneNumber(phoneNumber);
    }

    public Account getAccountByPhoneNumber(String phoneNumber) {
        return repository.getAccountByPhoneNumber(phoneNumber);
    }

    public String generateUniqueAccountNumber() {
        return repository.generateUniqueAccountNumber();
    }
} 
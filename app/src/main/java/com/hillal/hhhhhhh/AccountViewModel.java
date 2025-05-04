package com.hillal.hhhhhhh;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;

public class AccountViewModel extends AndroidViewModel {
    private final AccountRepository repository;
    private final LiveData<List<Account>> allAccounts;
    private final LiveData<Double> totalCreditor;
    private final LiveData<Double> totalDebtor;
    private final LiveData<Double> netBalance;

    public AccountViewModel(Application application) {
        super(application);
        repository = new AccountRepository(application);
        allAccounts = repository.getAllAccounts();
        totalCreditor = repository.getTotalCreditor();
        totalDebtor = repository.getTotalDebtor();
        netBalance = repository.getNetBalance();
    }

    public LiveData<List<Account>> getAllAccounts() {
        return allAccounts;
    }

    public LiveData<Account> getAccountById(int accountId) {
        return repository.getAccountById(accountId);
    }

    public LiveData<Double> getTotalCreditor() {
        return totalCreditor;
    }

    public LiveData<Double> getTotalDebtor() {
        return totalDebtor;
    }

    public LiveData<Double> getNetBalance() {
        return netBalance;
    }

    public void insert(Account account) {
        repository.insert(account);
    }

    public void update(Account account) {
        repository.update(account);
    }

    public void delete(Account account) {
        repository.delete(account);
    }
} 
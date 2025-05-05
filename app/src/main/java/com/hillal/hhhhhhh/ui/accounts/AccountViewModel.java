package com.hillal.hhhhhhh.ui.accounts;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.repository.AccountRepository;

import java.util.List;

public class AccountViewModel extends ViewModel {
    private final AccountRepository accountRepository;

    public AccountViewModel(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public LiveData<List<Account>> getAllAccounts() {
        return accountRepository.getAllAccounts();
    }

    public LiveData<Account> getAccountById(int accountId) {
        return accountRepository.getAccountById(accountId);
    }

    public LiveData<List<Account>> searchAccounts(String query) {
        return accountRepository.searchAccounts(query);
    }

    public void insertAccount(Account account) {
        accountRepository.insertAccount(account);
    }

    public void updateAccount(Account account) {
        accountRepository.updateAccount(account);
    }

    public void deleteAccount(Account account) {
        accountRepository.deleteAccount(account);
    }
} 
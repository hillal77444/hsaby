package com.hillal.hhhhhhh.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.repository.AccountRepository;
import java.util.List;

public class AccountViewModel extends AndroidViewModel {
    private final AccountRepository repository;
    private final LiveData<List<Account>> allAccounts;

    public AccountViewModel(Application application) {
        super(application);
        repository = new AccountRepository(application);
        allAccounts = repository.getAllAccounts();
    }

    public LiveData<List<Account>> getAllAccounts() {
        return allAccounts;
    }

    public void insertAccount(Account account) {
        repository.insert(account);
    }

    public void updateAccount(Account account) {
        repository.update(account);
    }

    public void deleteAccount(Account account) {
        repository.delete(account);
    }
} 
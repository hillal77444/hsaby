package com.hillal.hhhhhhh.ui.accounts;

import androidx.lifecycle.ViewModel;
import com.hillal.hhhhhhh.data.repository.AccountRepository;

public class AccountViewModel extends ViewModel {
    private final AccountRepository accountRepository;

    public AccountViewModel(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    // Add account methods here
} 
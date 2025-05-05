package com.hillal.hhhhhhh.ui.settings;

import androidx.lifecycle.ViewModel;
import com.hillal.hhhhhhh.data.repository.AccountRepository;

public class SettingsViewModel extends ViewModel {
    private final AccountRepository accountRepository;

    public SettingsViewModel(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    // Add settings methods here
} 
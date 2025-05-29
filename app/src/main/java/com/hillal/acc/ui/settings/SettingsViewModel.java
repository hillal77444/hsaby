package com.hillal.acc.ui.settings;

import androidx.lifecycle.ViewModel;
import com.hillal.acc.data.repository.AccountRepository;

public class SettingsViewModel extends ViewModel {
    private final AccountRepository accountRepository;

    public SettingsViewModel(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public void backupData() {
        accountRepository.backupData();
    }

    public void restoreData() {
        accountRepository.restoreData();
    }

    public void clearAllData() {
        accountRepository.clearAllData();
    }

    // Add settings methods here
} 
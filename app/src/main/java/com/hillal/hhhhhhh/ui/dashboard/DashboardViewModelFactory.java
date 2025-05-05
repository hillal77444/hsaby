package com.hillal.hhhhhhh.ui.dashboard;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.hillal.hhhhhhh.data.repository.AccountRepository;

public class DashboardViewModelFactory implements ViewModelProvider.Factory {
    private final AccountRepository accountRepository;

    public DashboardViewModelFactory(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(DashboardViewModel.class)) {
            return (T) new DashboardViewModel(accountRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
} 
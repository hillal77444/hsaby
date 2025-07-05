package com.hillal.acc.ui.dashboard;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.hillal.acc.data.repository.AccountRepository;
import com.hillal.acc.data.repository.TransactionRepository;

public class DashboardViewModelFactory implements ViewModelProvider.Factory {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public DashboardViewModelFactory(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(DashboardViewModel.class)) {
            return (T) new DashboardViewModel(accountRepository, transactionRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
} 
package com.hillal.acc.ui.accounts;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.hillal.acc.data.repository.AccountRepository;

public class AccountViewModelFactory implements ViewModelProvider.Factory {
    private final AccountRepository repository;

    public AccountViewModelFactory(AccountRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(AccountViewModel.class)) {
            return (T) new AccountViewModel(repository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
} 
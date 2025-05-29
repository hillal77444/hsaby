package com.hillal.acc.ui.transactions;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.hillal.acc.data.repository.AccountRepository;

public class TransactionViewModelFactory implements ViewModelProvider.Factory {
    private final AccountRepository accountRepository;

    public TransactionViewModelFactory(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(TransactionViewModel.class)) {
            return (T) new TransactionViewModel(accountRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
} 
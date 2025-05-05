package com.hillal.hhhhhhh.ui.transactions;

import androidx.lifecycle.ViewModel;
import com.hillal.hhhhhhh.data.repository.AccountRepository;

public class TransactionViewModel extends ViewModel {
    private final AccountRepository accountRepository;

    public TransactionViewModel(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    // Add transaction methods here
} 
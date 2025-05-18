package com.hillal.hhhhhhh.ui.account.statement;

import androidx.lifecycle.ViewModel;
import com.hillal.hhhhhhh.data.repository.AccountStatementRepository;

public class AccountStatementViewModel extends ViewModel {
    private final AccountStatementRepository repository;

    public AccountStatementViewModel(AccountStatementRepository repository) {
        this.repository = repository;
    }

    public double getAccountBalance(long accountId) {
        return repository.getAccountBalance(accountId);
    }
} 
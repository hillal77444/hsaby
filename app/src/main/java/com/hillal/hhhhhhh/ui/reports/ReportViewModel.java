package com.hillal.hhhhhhh.ui.reports;

import androidx.lifecycle.ViewModel;
import com.hillal.hhhhhhh.data.repository.AccountRepository;

public class ReportViewModel extends ViewModel {
    private final AccountRepository accountRepository;

    public ReportViewModel(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    // Add report methods here
} 
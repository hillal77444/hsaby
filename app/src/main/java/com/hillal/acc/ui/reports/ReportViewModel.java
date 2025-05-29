package com.hillal.acc.ui.reports;

import androidx.lifecycle.ViewModel;
import com.hillal.acc.data.repository.AccountRepository;

public class ReportViewModel extends ViewModel {
    private final AccountRepository accountRepository;

    public ReportViewModel(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    // Add report methods here
} 
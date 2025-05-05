package com.hillal.hhhhhhh.ui.dashboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.repository.AccountRepository;

import java.util.List;

public class DashboardViewModel extends ViewModel {
    private final AccountRepository accountRepository;
    private final MutableLiveData<List<Account>> recentAccounts = new MutableLiveData<>();
    private final MutableLiveData<Double> totalDebtors = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> totalCreditors = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> netBalance = new MutableLiveData<>(0.0);

    public DashboardViewModel(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
        loadDashboardData();
    }

    private void loadDashboardData() {
        // Load recent accounts
        accountRepository.getRecentAccounts().observeForever(accounts -> {
            recentAccounts.setValue(accounts);
            calculateTotals(accounts);
        });
    }

    private void calculateTotals(List<Account> accounts) {
        double debtors = 0.0;
        double creditors = 0.0;

        for (Account account : accounts) {
            if (account.isCreditor()) {
                creditors += account.getBalance();
            } else {
                debtors += account.getBalance();
            }
        }

        totalDebtors.setValue(debtors);
        totalCreditors.setValue(creditors);
        netBalance.setValue(creditors - debtors);
    }

    public LiveData<List<Account>> getRecentAccounts() {
        return recentAccounts;
    }

    public LiveData<Double> getTotalDebtors() {
        return totalDebtors;
    }

    public LiveData<Double> getTotalCreditors() {
        return totalCreditors;
    }

    public LiveData<Double> getNetBalance() {
        return netBalance;
    }
} 
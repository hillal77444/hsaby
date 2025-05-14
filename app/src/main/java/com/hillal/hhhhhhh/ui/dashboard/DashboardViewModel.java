package com.hillal.hhhhhhh.ui.dashboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.repository.AccountRepository;
import com.hillal.hhhhhhh.data.repository.TransactionRepository;

import java.util.List;

public class DashboardViewModel extends ViewModel {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final MutableLiveData<List<Account>> recentAccounts = new MutableLiveData<>();
    private final MutableLiveData<Double> totalDebtors = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> totalCreditors = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> netBalance = new MutableLiveData<>(0.0);

    public DashboardViewModel(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        loadDashboardData();
    }

    private void loadDashboardData() {
        // Load recent accounts
        accountRepository.getRecentAccounts().observeForever(accounts -> {
            recentAccounts.setValue(accounts);
        });

        // Load totals from transactions
        transactionRepository.getTotalDebtors().observeForever(debtors -> {
            totalDebtors.setValue(debtors != null ? debtors : 0.0);
            updateNetBalance();
        });

        transactionRepository.getTotalCreditors().observeForever(creditors -> {
            totalCreditors.setValue(creditors != null ? creditors : 0.0);
            updateNetBalance();
        });
    }

    private void updateNetBalance() {
        Double debtors = totalDebtors.getValue();
        Double creditors = totalCreditors.getValue();
        if (debtors != null && creditors != null) {
            netBalance.setValue(creditors - debtors);
        }
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
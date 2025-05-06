package com.hillal.hhhhhhh.ui.reports;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.model.Transaction;
import com.hillal.hhhhhhh.viewmodel.AccountViewModel;
import com.hillal.hhhhhhh.viewmodel.TransactionViewModel;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReportsFragment extends Fragment {
    private AccountViewModel accountViewModel;
    private TransactionViewModel transactionViewModel;
    private ReportAdapter reportAdapter;
    private TextView totalDebtorsText;
    private TextView totalCreditorsText;
    private TextView netBalanceText;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reports, container, false);
        
        // Initialize ViewModels
        accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);
        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
        
        // Initialize views
        totalDebtorsText = view.findViewById(R.id.total_debtors);
        totalCreditorsText = view.findViewById(R.id.total_creditors);
        netBalanceText = view.findViewById(R.id.net_balance);
        
        // Setup RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.accounts_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        reportAdapter = new ReportAdapter();
        recyclerView.setAdapter(reportAdapter);
        
        // Observe data
        observeAccounts();
        
        return view;
    }

    private void observeAccounts() {
        accountViewModel.getAllAccounts().observe(getViewLifecycleOwner(), accounts -> {
            double totalDebtors = 0;
            double totalCreditors = 0;
            List<Transaction> allTransactions = new ArrayList<>();

            for (Account account : accounts) {
                if (account.isDebtor()) {
                    totalDebtors += account.getOpeningBalance();
                } else {
                    totalCreditors += account.getOpeningBalance();
                }
            }

            // Update UI
            totalDebtorsText.setText(String.format("إجمالي المدينين: %.2f", totalDebtors));
            totalCreditorsText.setText(String.format("إجمالي الدائنين: %.2f", totalCreditors));
            netBalanceText.setText(String.format("الرصيد: %.2f", totalDebtors - totalCreditors));

            // Update adapter
            reportAdapter.setTransactions(allTransactions);
        });
    }
} 
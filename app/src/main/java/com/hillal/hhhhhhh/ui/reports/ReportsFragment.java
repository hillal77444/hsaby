package com.hillal.hhhhhhh.ui.reports;

import android.content.Intent;
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
import com.google.android.material.button.MaterialButton;

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
        
        // ربط زر كشف الحساب التفصيلي
        MaterialButton viewAccountStatementButton = view.findViewById(R.id.viewAccountStatementButton);
        viewAccountStatementButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), com.hillal.hhhhhhh.ui.AccountStatementActivity.class);
            startActivity(intent);
        });
        
        // Observe data
        observeTransactions();
        
        return view;
    }

    private void observeTransactions() {
        transactionViewModel.getAllTransactions().observe(getViewLifecycleOwner(), transactions -> {
            double totalDebit = 0;
            double totalCredit = 0;
            if (transactions != null) {
                for (Transaction transaction : transactions) {
                    if ("debit".equals(transaction.getType())) {
                        totalDebit += transaction.getAmount();
                    } else if ("credit".equals(transaction.getType())) {
                        totalCredit += transaction.getAmount();
                    }
                }
            }
            totalDebtorsText.setText(String.format("إجمالي المدينين: %.2f", totalDebit));
            totalCreditorsText.setText(String.format("إجمالي الدائنين: %.2f", totalCredit));
            netBalanceText.setText(String.format("الرصيد: %.2f", totalDebit - totalCredit));
        });
    }
} 
package com.hillal.acc.ui.reports;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.hillal.acc.R;
import com.hillal.acc.data.model.Account;
import com.hillal.acc.data.model.Transaction;
import com.hillal.acc.viewmodel.AccountViewModel;
import com.hillal.acc.viewmodel.TransactionViewModel;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReportsFragment extends Fragment {
    private AccountViewModel accountViewModel;
    private TransactionViewModel transactionViewModel;
    private TextView totalDebtorsText;
    private TextView totalCreditorsText;
    private TextView netBalanceText;
    private TextView totalTransactionsText;
    private TextView averageTransactionText;

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
        totalTransactionsText = view.findViewById(R.id.total_transactions);
        averageTransactionText = view.findViewById(R.id.average_transaction);
        
        // ربط زر كشف الحساب التفصيلي
        MaterialButton viewAccountStatementButton = view.findViewById(R.id.viewAccountStatementButton);
        viewAccountStatementButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), com.hillal.acc.ui.AccountStatementActivity.class);
            startActivity(intent);
        });
        
        // زر تقرير ملخص الحسابات
        MaterialButton btnAccountsSummaryReport = view.findViewById(R.id.btnAccountsSummaryReport);
        btnAccountsSummaryReport.setOnClickListener(v -> {
            androidx.navigation.Navigation.findNavController(v)
                .navigate(R.id.action_reports_to_accountsSummaryReport);
        });

        // زر كشف الصندوق
        MaterialButton btnCashboxStatement = view.findViewById(R.id.btnCashboxStatement);
        btnCashboxStatement.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), com.hillal.acc.ui.CashboxStatementActivity.class);
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
            int count = 0;
            double sum = 0;
            if (transactions != null) {
                for (Transaction transaction : transactions) {
                    if ("debit".equals(transaction.getType())) {
                        totalDebit += transaction.getAmount();
                    } else if ("credit".equals(transaction.getType())) {
                        totalCredit += transaction.getAmount();
                    }
                    sum += transaction.getAmount();
                    count++;
                }
            }
            totalDebtorsText.setText(String.format(Locale.ENGLISH, "إجمالي المدينين: %.2f", totalDebit));
            totalCreditorsText.setText(String.format(Locale.ENGLISH, "إجمالي الدائنين: %.2f", totalCredit));
            netBalanceText.setText(String.format(Locale.ENGLISH, "الرصيد: %.2f", totalCredit - totalDebit));
            totalTransactionsText.setText(String.format(Locale.ENGLISH, "%d", count));
            averageTransactionText.setText(count > 0 ? String.format(Locale.ENGLISH, "%.2f", sum / count) : "0");
        });
    }
} 
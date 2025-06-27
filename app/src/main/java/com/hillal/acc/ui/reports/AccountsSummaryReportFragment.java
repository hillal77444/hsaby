package com.hillal.acc.ui.reports;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hillal.acc.R;
import com.hillal.acc.data.model.Account;
import com.hillal.acc.data.model.Transaction;
import com.hillal.acc.ui.accounts.AccountViewModel;
import com.hillal.acc.ui.transactions.TransactionsViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AccountsSummaryReportFragment extends Fragment {
    private Button btnFilterYER, btnFilterSAR, btnFilterUSD;
    private RecyclerView recyclerView;
    private TextView tvTotalBalance, tvTotalCredit, tvTotalDebit;
    private AccountsSummaryAdapter adapter;
    private List<Account> allAccounts = new ArrayList<>();
    private List<Transaction> allTransactions = new ArrayList<>();
    private String selectedCurrency = "يمني";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_accounts_summary_report, container, false);
        btnFilterYER = view.findViewById(R.id.btnFilterYER);
        btnFilterSAR = view.findViewById(R.id.btnFilterSAR);
        btnFilterUSD = view.findViewById(R.id.btnFilterUSD);
        recyclerView = view.findViewById(R.id.accountsSummaryRecyclerView);
        tvTotalBalance = view.findViewById(R.id.tvTotalBalance);
        tvTotalCredit = view.findViewById(R.id.tvTotalCredit);
        tvTotalDebit = view.findViewById(R.id.tvTotalDebit);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AccountsSummaryAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);
        setupFilterButtons();
        loadData();
        return view;
    }

    private void setupFilterButtons() {
        btnFilterYER.setOnClickListener(v -> {
            selectedCurrency = "يمني";
            updateFilterButtons();
            updateReport();
        });
        btnFilterSAR.setOnClickListener(v -> {
            selectedCurrency = "سعودي";
            updateFilterButtons();
            updateReport();
        });
        btnFilterUSD.setOnClickListener(v -> {
            selectedCurrency = "دولار";
            updateFilterButtons();
            updateReport();
        });
        updateFilterButtons();
    }

    private void updateFilterButtons() {
        btnFilterYER.setBackgroundTintList(getResources().getColorStateList(selectedCurrency.equals("يمني") ? R.color.teal_700 : R.color.light_gray, null));
        btnFilterYER.setTextColor(getResources().getColor(selectedCurrency.equals("يمني") ? android.R.color.white : R.color.primary_text, null));
        btnFilterSAR.setBackgroundTintList(getResources().getColorStateList(selectedCurrency.equals("سعودي") ? R.color.teal_700 : R.color.light_gray, null));
        btnFilterSAR.setTextColor(getResources().getColor(selectedCurrency.equals("سعودي") ? android.R.color.white : R.color.primary_text, null));
        btnFilterUSD.setBackgroundTintList(getResources().getColorStateList(selectedCurrency.equals("دولار") ? R.color.teal_700 : R.color.light_gray, null));
        btnFilterUSD.setTextColor(getResources().getColor(selectedCurrency.equals("دولار") ? android.R.color.white : R.color.primary_text, null));
    }

    private void loadData() {
        AccountViewModel accountViewModel = new ViewModelProvider(requireActivity()).get(AccountViewModel.class);
        TransactionsViewModel transactionsViewModel = new ViewModelProvider(requireActivity()).get(TransactionsViewModel.class);
        accountViewModel.getAllAccounts().observe(getViewLifecycleOwner(), accounts -> {
            if (accounts != null) {
                allAccounts = accounts;
                updateReport();
            }
        });
        transactionsViewModel.getTransactions().observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null) {
                allTransactions = transactions;
                updateReport();
            }
        });
        transactionsViewModel.loadAllTransactions();
    }

    private void updateReport() {
        List<AccountsSummaryAdapter.AccountSummary> summaryList = new ArrayList<>();
        double totalCredit = 0, totalDebit = 0, totalBalance = 0;
        for (Account account : allAccounts) {
            double credit = 0, debit = 0;
            for (Transaction t : allTransactions) {
                if (t.getAccountId() == account.getId() && selectedCurrency.equals(t.getCurrency())) {
                    if (t.getType().equalsIgnoreCase("credit") || t.getType().equals("له")) {
                        credit += t.getAmount();
                    } else {
                        debit += t.getAmount();
                    }
                }
            }
            double balance = credit - debit;
            summaryList.add(new AccountsSummaryAdapter.AccountSummary(account.getName(), credit, debit, balance));
            totalCredit += credit;
            totalDebit += debit;
            totalBalance += balance;
        }
        adapter.setData(summaryList);
        tvTotalCredit.setText(String.format(Locale.US, "له: %,.0f", totalCredit));
        tvTotalDebit.setText(String.format(Locale.US, "عليه: %,.0f", totalDebit));
        tvTotalBalance.setText(String.format(Locale.US, "الرصيد: %,.0f", totalBalance));
    }
} 
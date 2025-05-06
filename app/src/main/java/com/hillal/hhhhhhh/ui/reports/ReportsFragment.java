package com.hillal.hhhhhhh.ui.reports;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.model.Transaction;
import com.hillal.hhhhhhh.viewmodel.AccountViewModel;
import com.hillal.hhhhhhh.viewmodel.TransactionViewModel;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.List;

public class ReportsFragment extends Fragment {
    private AccountViewModel accountViewModel;
    private TransactionViewModel transactionViewModel;
    private RecyclerView accountsRecyclerView;
    private AccountsAdapter accountsAdapter;
    private TextView totalDebtorsText;
    private TextView totalCreditorsText;
    private TextView totalTransactionsText;
    private TextView averageTransactionText;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);
        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reports, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupViews(view);
        setupRecyclerView();
        observeData();
    }

    private void setupViews(View view) {
        totalDebtorsText = view.findViewById(R.id.total_debtors);
        totalCreditorsText = view.findViewById(R.id.total_creditors);
        totalTransactionsText = view.findViewById(R.id.total_transactions);
        averageTransactionText = view.findViewById(R.id.average_transaction);
        accountsRecyclerView = view.findViewById(R.id.accounts_recycler_view);
    }

    private void setupRecyclerView() {
        accountsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        accountsAdapter = new AccountsAdapter(account -> {
            Bundle args = new Bundle();
            args.putLong("accountId", account.getId());
            Navigation.findNavController(requireView())
                .navigate(R.id.action_reports_to_accountStatement, args);
        });
        accountsRecyclerView.setAdapter(accountsAdapter);
    }

    private void observeData() {
        accountViewModel.getAllAccounts().observe(getViewLifecycleOwner(), accounts -> {
            accountsAdapter.setAccounts(accounts);
            updateAccountSummary(accounts);
        });

        transactionViewModel.getAllTransactions().observe(getViewLifecycleOwner(), transactions -> {
            updateTransactionSummary(transactions);
        });
    }

    private void updateAccountSummary(List<Account> accounts) {
        double totalDebtors = 0;
        double totalCreditors = 0;

        for (Account account : accounts) {
            if (account.getType().equals("debtor")) {
                totalDebtors += account.getBalance();
            } else {
                totalCreditors += account.getBalance();
            }
        }

        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.getDefault());
        totalDebtorsText.setText(formatter.format(totalDebtors));
        totalCreditorsText.setText(formatter.format(totalCreditors));
    }

    private void updateTransactionSummary(List<Transaction> transactions) {
        int totalTransactions = transactions.size();
        double totalAmount = 0;

        for (Transaction transaction : transactions) {
            totalAmount += transaction.getAmount();
        }

        double averageAmount = totalTransactions > 0 ? totalAmount / totalTransactions : 0;
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.getDefault());

        totalTransactionsText.setText(String.valueOf(totalTransactions));
        averageTransactionText.setText(formatter.format(averageAmount));
    }
} 
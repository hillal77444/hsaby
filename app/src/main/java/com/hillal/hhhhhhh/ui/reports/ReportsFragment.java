package com.hillal.hhhhhhh.ui.reports;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.model.Transaction;
import com.hillal.hhhhhhh.viewmodel.AccountViewModel;
import com.hillal.hhhhhhh.viewmodel.TransactionViewModel;

import java.text.NumberFormat;
import java.util.Locale;

public class ReportsFragment extends Fragment {
    private AccountViewModel accountViewModel;
    private TransactionViewModel transactionViewModel;
    private TextView totalDebtorsTextView;
    private TextView totalCreditorsTextView;
    private TextView netBalanceTextView;
    private TextView totalTransactionsTextView;
    private TextView averageTransactionTextView;
    private RecyclerView accountsRecyclerView;
    private AccountsAdapter accountsAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_reports, container, false);

        // Initialize views
        totalDebtorsTextView = root.findViewById(R.id.total_debtors);
        totalCreditorsTextView = root.findViewById(R.id.total_creditors);
        netBalanceTextView = root.findViewById(R.id.net_balance);
        totalTransactionsTextView = root.findViewById(R.id.total_transactions);
        averageTransactionTextView = root.findViewById(R.id.average_transaction);
        accountsRecyclerView = root.findViewById(R.id.accountsRecyclerView);
        accountsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        accountsAdapter = new AccountsAdapter(account -> {
            Bundle args = new Bundle();
            args.putLong("accountId", account.getId());
            Navigation.findNavController(requireView())
                .navigate(R.id.action_reports_to_accountStatement, args);
        });
        accountsRecyclerView.setAdapter(accountsAdapter);

        // Initialize ViewModels
        accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);
        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);

        // Observe account data
        accountViewModel.getAllAccounts().observe(getViewLifecycleOwner(), accounts -> {
            double totalDebtors = 0;
            double totalCreditors = 0;

            for (Account account : accounts) {
                if (account.isDebtor()) {
                    totalDebtors += account.getOpeningBalance();
                } else {
                    totalCreditors += account.getOpeningBalance();
                }
            }

            NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.getDefault());
            totalDebtorsTextView.setText(formatter.format(totalDebtors));
            totalCreditorsTextView.setText(formatter.format(totalCreditors));
            netBalanceTextView.setText(formatter.format(totalCreditors - totalDebtors));
            
            accountsAdapter.setAccounts(accounts);
        });

        // Observe transaction data
        transactionViewModel.getAllTransactions().observe(getViewLifecycleOwner(), transactions -> {
            int totalTransactions = transactions.size();
            double totalAmount = 0;

            for (Transaction transaction : transactions) {
                totalAmount += transaction.getAmount();
            }

            NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.getDefault());
            totalTransactionsTextView.setText(String.valueOf(totalTransactions));
            averageTransactionTextView.setText(formatter.format(totalTransactions > 0 ? totalAmount / totalTransactions : 0));
        });

        return root;
    }
} 
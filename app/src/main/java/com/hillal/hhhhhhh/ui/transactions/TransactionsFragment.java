package com.hillal.hhhhhhh.ui.transactions;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;
import com.google.android.material.tabs.TabLayout;
import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.model.Transaction;
import com.hillal.hhhhhhh.databinding.FragmentTransactionsBinding;
import com.hillal.hhhhhhh.ui.adapters.TransactionAdapter;
import com.hillal.hhhhhhh.viewmodel.AccountViewModel;
import com.hillal.hhhhhhh.data.model.Account;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionsFragment extends Fragment {
    private FragmentTransactionsBinding binding;
    private TransactionsViewModel viewModel;
    private TransactionAdapter adapter;
    private AccountViewModel accountViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TransactionsViewModel.class);
        accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTransactionsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView();
        setupTabLayout();
        setupCurrencyFilter();
        setupFab();
        observeAccountsAndTransactions();
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter(new TransactionAdapter.TransactionDiffCallback());
        adapter.setOnItemClickListener(transaction -> {
            // Navigate to edit transaction
            Bundle args = new Bundle();
            args.putLong("transactionId", transaction.getId());
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_transactions_to_editTransaction, args);
        });
        adapter.setOnItemLongClickListener(transaction -> {
            showDeleteConfirmationDialog(transaction);
            return true;
        });

        binding.transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.transactionsRecyclerView.setAdapter(adapter);
    }

    private void setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: // All transactions
                        viewModel.loadAllTransactions();
                        break;
                    case 1: // Debit
                        viewModel.loadTransactionsByType("مدين");
                        break;
                    case 2: // Credit
                        viewModel.loadTransactionsByType("دائن");
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupCurrencyFilter() {
        binding.currencyChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String currency = null;
            if (checkedId == R.id.chipYer) {
                currency = getString(R.string.currency_yer);
            } else if (checkedId == R.id.chipSar) {
                currency = getString(R.string.currency_sar);
            } else if (checkedId == R.id.chipUsd) {
                currency = getString(R.string.currency_usd);
            }
            viewModel.loadTransactionsByCurrency(currency);
        });
    }

    private void setupFab() {
        binding.fabAddTransaction.setOnClickListener(v -> {
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_transactions_to_addTransaction);
        });
    }

    private void observeAccountsAndTransactions() {
        accountViewModel.getAllAccounts().observe(getViewLifecycleOwner(), accounts -> {
            Map<Long, Account> accountMap = new HashMap<>();
            for (Account account : accounts) {
                accountMap.put(account.getId(), account);
            }
            adapter.setAccountMap(accountMap);
            // بعد تعيين الخريطة، راقب المعاملات
            viewModel.getTransactions().observe(getViewLifecycleOwner(), transactions -> {
                adapter.submitList(transactions);
                binding.transactionsRecyclerView.setVisibility(
                        transactions.isEmpty() ? View.GONE : View.VISIBLE);
            });
        });
    }

    private void showDeleteConfirmationDialog(Transaction transaction) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_transaction)
                .setMessage(R.string.confirm_delete)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    viewModel.deleteTransaction(transaction);
                    Toast.makeText(requireContext(), R.string.transaction_deleted, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 
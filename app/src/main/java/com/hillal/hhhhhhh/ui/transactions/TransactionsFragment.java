package com.hillal.hhhhhhh.ui.transactions;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;
import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.model.Transaction;
import com.hillal.hhhhhhh.databinding.FragmentTransactionsBinding;
import com.hillal.hhhhhhh.ui.adapters.TransactionAdapter;
import com.hillal.hhhhhhh.viewmodel.AccountViewModel;
import com.hillal.hhhhhhh.data.model.Account;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import com.hillal.hhhhhhh.App;
import com.google.android.material.datepicker.MaterialDatePicker;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TransactionsFragment extends Fragment {
    private FragmentTransactionsBinding binding;
    private TransactionsViewModel viewModel;
    private TransactionAdapter adapter;
    private AccountViewModel accountViewModel;
    private Calendar startDate;
    private Calendar endDate;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TransactionsViewModel.class);
        accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);
        
        // تهيئة التواريخ الافتراضية
        startDate = Calendar.getInstance();
        startDate.add(Calendar.DAY_OF_MONTH, -4); // قبل 4 أيام
        endDate = Calendar.getInstance(); // اليوم الحالي
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
        setupAccountFilter();
        setupCurrencyFilter();
        setupDateFilter();
        setupFab();
        observeAccountsAndTransactions();
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter(new TransactionAdapter.TransactionDiffCallback());
        adapter.setOnItemClickListener(transaction -> {
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

    private void setupAccountFilter() {
        accountViewModel.getAllAccounts().observe(getViewLifecycleOwner(), accounts -> {
            List<String> accountNames = new ArrayList<>();
            for (Account account : accounts) {
                accountNames.add(account.getName());
            }
            
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                accountNames
            );
            
            binding.accountFilterDropdown.setAdapter(adapter);
            binding.accountFilterDropdown.setOnItemClickListener((parent, view, position, id) -> {
                String selectedAccount = accountNames.get(position);
                viewModel.loadTransactionsByAccount(selectedAccount);
            });
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

    private void setupDateFilter() {
        // تعيين التواريخ الافتراضية
        updateDateInputs();

        // إعداد مستمعي النقر على حقول التاريخ
        binding.startDateFilter.setOnClickListener(v -> showMaterialDatePicker(true));
        binding.endDateFilter.setOnClickListener(v -> showMaterialDatePicker(false));
    }

    private void showMaterialDatePicker(boolean isStartDate) {
        Calendar calendar = isStartDate ? startDate : endDate;
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("اختر التاريخ")
                .setSelection(calendar.getTimeInMillis())
                .build();
        datePicker.show(getParentFragmentManager(), isStartDate ? "START_DATE_PICKER" : "END_DATE_PICKER");
        datePicker.addOnPositiveButtonClickListener(selection -> {
            calendar.setTimeInMillis((Long) selection);
            updateDateInputs();
            viewModel.loadTransactionsByDateRange(startDate.getTime(), endDate.getTime());
        });
    }

    private void updateDateInputs() {
        // تحديث نص حقول التاريخ
        binding.startDateFilter.setText(formatDate(startDate));
        binding.endDateFilter.setText(formatDate(endDate));
    }

    private String formatDate(Calendar calendar) {
        return String.format("%d/%d/%d",
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.YEAR));
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
            
            // تحميل المعاملات مع التصفية الافتراضية
            viewModel.loadTransactionsByDateRange(startDate.getTime(), endDate.getTime());
            
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
        try {
            App app = (App) requireActivity().getApplication();
            com.hillal.hhhhhhh.data.sync.SyncManager syncManager = new com.hillal.hhhhhhh.data.sync.SyncManager(
                requireContext(),
                app.getDatabase().accountDao(),
                app.getDatabase().transactionDao()
            );
            syncManager.syncData(new com.hillal.hhhhhhh.data.sync.SyncManager.SyncCallback() {
                @Override
                public void onSuccess() {
                    // لا تفعل شيء إضافي
                }
                @Override
                public void onError(String error) {
                    // لا تفعل شيء إضافي
                }
            });
        } catch (Exception e) {
            // تجاهل أي خطأ هنا حتى لا يتأثر الخروج من الصفحة
        }
        super.onDestroyView();
        binding = null;
    }
} 
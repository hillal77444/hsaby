package com.hillal.hhhhhhh.ui.transactions;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.bigkoo.pickerview.view.TimePickerView;
import com.bigkoo.pickerview.builder.TimePickerBuilder;

public class TransactionsFragment extends Fragment {
    private FragmentTransactionsBinding binding;
    private TransactionsViewModel viewModel;
    private TransactionAdapter adapter;
    private AccountViewModel accountViewModel;
    private Calendar startDate;
    private Calendar endDate;
    private String selectedAccount = null;
    private String selectedCurrency = null;
    private List<Transaction> allTransactions = new ArrayList<>();

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
                selectedAccount = accountNames.get(position);
                applyAllFilters();
            });
        });
    }

    private void setupCurrencyFilter() {
        binding.currencyChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipYer) {
                selectedCurrency = getString(R.string.currency_yer);
            } else if (checkedId == R.id.chipSar) {
                selectedCurrency = getString(R.string.currency_sar);
            } else if (checkedId == R.id.chipUsd) {
                selectedCurrency = getString(R.string.currency_usd);
            } else {
                selectedCurrency = null;
            }
            applyAllFilters();
        });
    }

    private void setupDateFilter() {
        updateDateInputs();
        binding.startDateFilter.setOnClickListener(v -> showWheelDatePicker(true));
        binding.endDateFilter.setOnClickListener(v -> showWheelDatePicker(false));
    }

    private void showWheelDatePicker(boolean isStartDate) {
        Calendar calendar = isStartDate ? startDate : endDate;
        TimePickerView pvTime = new TimePickerBuilder(requireContext(), (date, v) -> {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            if (isStartDate) {
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                startDate.setTime(cal.getTime());
            } else {
                cal.set(Calendar.HOUR_OF_DAY, 23);
                cal.set(Calendar.MINUTE, 59);
                cal.set(Calendar.SECOND, 59);
                cal.set(Calendar.MILLISECOND, 999);
                endDate.setTime(cal.getTime());
            }
            updateDateInputs();
            applyAllFilters();
        })
        .setType(new boolean[]{true, true, true, false, false, false}) // سنة، شهر، يوم فقط
        .setTitleText("اختر التاريخ")
        .setCancelText("إلغاء")
        .setSubmitText("تأكيد")
        .setDate(calendar)
        .setLabel("سنة", "شهر", "يوم", "", "", "")
        .setLayoutRes(R.layout.pickerview_custom_time, v -> {
            // تخصيص الأحداث للأزرار إذا لزم الأمر
        })
        .build();
        pvTime.show();
    }

    private void updateDateInputs() {
        // عرض التاريخ بالأرقام الإنجليزية فقط
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH);
        binding.startDateFilter.setText(sdf.format(startDate.getTime()));
        binding.endDateFilter.setText(sdf.format(endDate.getTime()));
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
                allTransactions = transactions != null ? transactions : new ArrayList<>();
                applyAllFilters();
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

    private void applyAllFilters() {
        List<Transaction> filtered = new ArrayList<>();
        for (Transaction t : allTransactions) {
            boolean match = true;
            if (selectedAccount != null && !selectedAccount.isEmpty()) {
                Account account = null;
                if (adapter != null && adapter.getAccountMap() != null) {
                    account = adapter.getAccountMap().get(t.getAccountId());
                }
                String accountName = (account != null) ? account.getName() : null;
                if (accountName == null || !accountName.equals(selectedAccount)) match = false;
            }
            if (selectedCurrency != null && !selectedCurrency.isEmpty()) {
                if (!selectedCurrency.equals(t.getCurrency())) match = false;
            }
            Date transactionDate = new Date(t.getDate());
            if (transactionDate.before(startDate.getTime()) || transactionDate.after(endDate.getTime())) {
                match = false;
            }
            if (match) filtered.add(t);
        }
        adapter.submitList(filtered);
        binding.transactionsRecyclerView.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
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
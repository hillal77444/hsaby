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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.model.Transaction;
import com.hillal.hhhhhhh.databinding.FragmentEditTransactionBinding;
import com.hillal.hhhhhhh.viewmodel.AccountViewModel;
import com.hillal.hhhhhhh.data.preferences.UserPreferences;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EditTransactionFragment extends Fragment {
    private FragmentEditTransactionBinding binding;
    private TransactionsViewModel transactionsViewModel;
    private AccountViewModel accountViewModel;
    private UserPreferences userPreferences;
    private long transactionId;
    private long selectedAccountId = -1;
    private final Calendar calendar = Calendar.getInstance();
    private Transaction oldTransaction;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", new Locale("ar"));

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        transactionsViewModel = new ViewModelProvider(this).get(TransactionsViewModel.class);
        accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);
        userPreferences = new UserPreferences(requireContext());
        
        if (getArguments() != null) {
            transactionId = getArguments().getLong("transactionId", -1);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEditTransactionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews();
        setupListeners();
        loadAccounts();
        loadTransaction();
    }

    private void setupViews() {
        // Set initial date
        updateDateField();
    }

    private void setupListeners() {
        binding.saveButton.setOnClickListener(v -> updateTransaction());
        binding.cancelButton.setOnClickListener(v -> Navigation.findNavController(requireView()).navigateUp());
        
        // Date picker listener
        binding.dateEditText.setOnClickListener(v -> showDatePicker());
    }

    private void loadAccounts() {
        accountViewModel.getAllAccounts().observe(getViewLifecycleOwner(), accounts -> {
            if (accounts != null && !accounts.isEmpty()) {
                setupAccountDropdown(accounts);
            }
        });
    }


    

    private void setupAccountDropdown(List<Account> accounts) {
        String[] accountNames = new String[accounts.size()];
        for (int i = 0; i < accounts.size(); i++) {
            accountNames[i] = accounts.get(i).getName();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                accountNames
        );

        binding.accountAutoComplete.setAdapter(adapter);
        binding.accountAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
            selectedAccountId = accounts.get(position).getId();
        });
    }

    private void loadTransaction() {
        if (transactionId != -1) {
            transactionsViewModel.getTransactionById(transactionId).observe(getViewLifecycleOwner(), transaction -> {
                if (transaction != null) {
                    oldTransaction = transaction;
                    populateTransactionData(transaction);
                }
            });
        }
    }

    private void populateTransactionData(Transaction transaction) {
        selectedAccountId = transaction.getAccountId();
        
        // Load account name
        accountViewModel.getAccountById(transaction.getAccountId()).observe(getViewLifecycleOwner(), account -> {
            if (account != null) {
                binding.accountAutoComplete.setText(account.getName(), false);
            }
        });

        binding.amountEditText.setText(String.valueOf(transaction.getAmount()));
        binding.descriptionEditText.setText(transaction.getDescription());
        
        // إضافة اسم المستخدم في الملاحظات إذا كانت فارغة
        String notes = transaction.getNotes();
        if (notes == null || notes.isEmpty()) {
            String userName = userPreferences.getUserName();
            if (!userName.isEmpty()) {
                binding.notesEditText.setText(userName);
            }
        } else {
            binding.notesEditText.setText(notes);
        }

        // Set transaction type
        if ("debit".equals(transaction.getType())) {
            binding.radioDebit.setChecked(true);
        } else {
            binding.radioCredit.setChecked(true);
        }

        // Set currency
        if (getString(R.string.currency_yer).equals(transaction.getCurrency())) {
            binding.radioYer.setChecked(true);
        } else if (getString(R.string.currency_sar).equals(transaction.getCurrency())) {
            binding.radioSar.setChecked(true);
        } else {
            binding.radioUsd.setChecked(true);
        }

        // Set date
        calendar.setTimeInMillis(transaction.getTransactionDate());
        updateDateField();
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    updateDateField();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void updateDateField() {
        binding.dateEditText.setText(dateFormat.format(calendar.getTime()));
    }

    private void updateTransaction() {
        if (selectedAccountId == -1) {
            Toast.makeText(requireContext(), "الرجاء اختيار الحساب", Toast.LENGTH_SHORT).show();
            return;
        }

        String amountStr = binding.amountEditText.getText().toString();
        String description = binding.descriptionEditText.getText().toString();
        String notes = binding.notesEditText.getText().toString();
        boolean isDebit = binding.radioDebit.isChecked();
        String currency = getSelectedCurrency();

        if (amountStr.isEmpty()) {
            binding.amountEditText.setError(getString(R.string.error_amount_required));
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);

            if (oldTransaction == null) {
                Toast.makeText(requireContext(), "حدث خطأ في تحميل بيانات المعاملة الأصلية", Toast.LENGTH_SHORT).show();
                return;
            }

            Transaction transaction = new Transaction();
            transaction.setId(transactionId);
            transaction.setAccountId(selectedAccountId);
            transaction.setAmount(amount);
            transaction.setType(isDebit ? "debit" : "credit");
            transaction.setDescription(description);
            transaction.setNotes(notes);
            transaction.setCurrency(currency);
            transaction.setTransactionDate(calendar.getTimeInMillis());
            transaction.setUpdatedAt(System.currentTimeMillis());
            transaction.setServerId(oldTransaction.getServerId());
            transaction.setSyncStatus(0); 

            transactionsViewModel.updateTransaction(transaction);
            Toast.makeText(requireContext(), R.string.transaction_updated, Toast.LENGTH_SHORT).show();
            Navigation.findNavController(requireView()).navigateUp();
        } catch (NumberFormatException e) {
            binding.amountEditText.setError(getString(R.string.error_invalid_amount));
        }
    }

    private String getSelectedCurrency() {
        int checkedId = binding.currencyRadioGroup.getCheckedRadioButtonId();
        if (checkedId == R.id.radioYer) {
            return getString(R.string.currency_yer);
        } else if (checkedId == R.id.radioSar) {
            return getString(R.string.currency_sar);
        } else {
            return getString(R.string.currency_usd);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 
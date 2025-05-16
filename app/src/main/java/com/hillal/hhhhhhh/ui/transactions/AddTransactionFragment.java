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
import com.hillal.hhhhhhh.databinding.FragmentAddTransactionBinding;
import com.hillal.hhhhhhh.viewmodel.AccountViewModel;
import com.hillal.hhhhhhh.data.preferences.UserPreferences;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class AddTransactionFragment extends Fragment {
    private FragmentAddTransactionBinding binding;
    private TransactionsViewModel transactionsViewModel;
    private AccountViewModel accountViewModel;
    private UserPreferences userPreferences;
    private long selectedAccountId = -1;
    private final Calendar calendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", new Locale("ar"));
    private List<Account> allAccounts = new ArrayList<>();
    private List<Transaction> allTransactions = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        transactionsViewModel = new ViewModelProvider(this).get(TransactionsViewModel.class);
        accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);
        userPreferences = new UserPreferences(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAddTransactionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews();
        setupListeners();
        loadAccounts();
        loadAllTransactions();
        setupAccountPicker();
    }

    private void setupViews() {
        // Set initial date
        updateDateField();

        // تعيين ريال يمني كخيار افتراضي
        binding.radioYer.setChecked(true);
        
        // إضافة اسم المستخدم في الملاحظات
        String userName = userPreferences.getUserName();
        if (!userName.isEmpty()) {
            binding.notesEditText.setText(userName);
        }
    }

    private void setupListeners() {
        binding.saveButton.setOnClickListener(v -> saveTransaction());
        binding.cancelButton.setOnClickListener(v -> Navigation.findNavController(requireView()).navigateUp());
        
        // Date picker listener
        binding.dateEditText.setOnClickListener(v -> showDatePicker());
    }

    private void loadAccounts() {
        accountViewModel.getAllAccounts().observe(getViewLifecycleOwner(), accounts -> {
            if (accounts != null && !accounts.isEmpty()) {
                allAccounts = accounts;
                setupAccountDropdown(accounts);
            }
        });
    }

    private void setupAccountDropdown(List<Account> accounts) {
        // لم يعد هناك داعي للـ AutoComplete التقليدي
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

    private void saveTransaction() {
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

            // الحصول على معلومات الحساب
            accountViewModel.getAccountById(selectedAccountId).observe(getViewLifecycleOwner(), account -> {
                if (account != null) {
                    Transaction transaction = new Transaction(
                        selectedAccountId,
                        amount,
                        isDebit ? "debit" : "credit",
                        description,
                        currency
                    );
                    transaction.setNotes(notes);
                    transaction.setDate(calendar.getTimeInMillis());
                    transaction.setUpdatedAt(System.currentTimeMillis());
                    transaction.setServerId(0); // تعيين serverId إلى 0 للمعاملات الجديدة
                    transaction.setWhatsappEnabled(account.isWhatsappEnabled()); // تعيين حالة واتساب الحساب

                    transactionsViewModel.insertTransaction(transaction);
                    Toast.makeText(requireContext(), R.string.transaction_saved, Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).navigateUp();
                }
            });
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

    private void setupAccountPicker() {
        binding.accountAutoComplete.setFocusable(false);
        binding.accountAutoComplete.setOnClickListener(v -> showAccountPickerBottomSheet());
    }

    private void showAccountPickerBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.bottomsheet_account_picker, null);
        dialog.setContentView(sheetView);
        EditText searchEditText = sheetView.findViewById(R.id.searchEditText);
        RecyclerView accountsRecyclerView = sheetView.findViewById(R.id.accountsRecyclerView);
        accountsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        // بناء خريطة الحسابات -> المعاملات
        Map<Long, List<Transaction>> accountTxMap = new HashMap<>();
        for (Transaction t : allTransactions) {
            if (!accountTxMap.containsKey(t.getAccountId())) accountTxMap.put(t.getAccountId(), new ArrayList<>());
            accountTxMap.get(t.getAccountId()).add(t);
        }
        AccountPickerAdapter adapter = new AccountPickerAdapter(requireContext(), allAccounts, accountTxMap, account -> {
            binding.accountAutoComplete.setText(account.getName());
            selectedAccountId = account.getId();
            dialog.dismiss();
        });
        accountsRecyclerView.setAdapter(adapter);
        // تصفية عند البحث
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    adapter.updateList(allAccounts);
                } else {
                    List<Account> filtered = new ArrayList<>();
                    for (Account acc : allAccounts) {
                        if (acc.getName() != null && acc.getName().contains(query)) {
                            filtered.add(acc);
                        }
                    }
                    adapter.updateList(filtered);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        dialog.show();
    }

    private void loadAllTransactions() {
        TransactionsViewModel txViewModel = new ViewModelProvider(this).get(TransactionsViewModel.class);
        txViewModel.getTransactions().observe(getViewLifecycleOwner(), txs -> {
            if (txs != null) allTransactions = txs;
        });
        txViewModel.loadAllTransactions();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
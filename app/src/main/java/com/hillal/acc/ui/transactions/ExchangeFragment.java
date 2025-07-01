package com.hillal.acc.ui.transactions;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.hillal.acc.R;
import com.hillal.acc.data.model.Account;
import com.hillal.acc.data.model.Transaction;
import com.hillal.acc.viewmodel.AccountViewModel;
import com.hillal.acc.ui.transactions.TransactionViewModel;
import com.hillal.acc.ui.transactions.TransactionViewModelFactory;
import com.hillal.acc.data.repository.AccountRepository;
import com.hillal.acc.App;
import java.util.List;
import com.hillal.acc.ui.common.AccountPickerBottomSheet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.math.BigDecimal;
import java.math.RoundingMode;
import com.hillal.acc.data.entities.Cashbox;
import com.hillal.acc.viewmodel.CashboxViewModel;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import com.google.android.material.button.MaterialButton;
import com.hillal.acc.data.repository.TransactionRepository;
import com.google.android.material.textfield.TextInputEditText;
import com.hillal.acc.ui.transactions.TransactionsViewModel;
import androidx.core.view.ViewCompat;
import com.hillal.acc.databinding.FragmentExchangeBinding;

public class ExchangeFragment extends Fragment {
    private Spinner fromCurrencySpinner, toCurrencySpinner, operationTypeSpinner, cashboxSpinner;
    private TextView accountBalanceText, exchangeAmountText;
    private EditText amountEditText, rateEditText, notesEditText;
    private com.google.android.material.textfield.TextInputEditText accountAutoComplete;
    private List<Account> accounts;
    private List<Cashbox> cashboxes = new ArrayList<>();
    private Account selectedAccount;
    private long selectedCashboxId = -1;
    private String[] currencies;
    private ArrayAdapter<String> currencyAdapter;
    private ArrayAdapter<String> cashboxAdapter;
    private ArrayAdapter<CharSequence> opTypeAdapter;
    private Map<Long, Map<String, Double>> accountBalancesMap = new HashMap<>();
    private CashboxViewModel cashboxViewModel;
    private AccountViewModel accountViewModel;
    private TransactionViewModel transactionViewModel;
    private TransactionRepository transactionRepository;
    private TransactionsViewModel transactionsViewModel;
    private FragmentExchangeBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentExchangeBinding.inflate(inflater, container, false);
        fromCurrencySpinner = binding.fromCurrencySpinner;
        toCurrencySpinner = binding.toCurrencySpinner;
        operationTypeSpinner = binding.operationTypeSpinner;
        cashboxSpinner = binding.cashboxSpinner;
        accountBalanceText = binding.accountBalanceText;
        exchangeAmountText = binding.exchangeAmountText;
        amountEditText = binding.amountEditText;
        rateEditText = binding.rateEditText;
        notesEditText = binding.notesEditText;
        accountAutoComplete = binding.accountAutoComplete;
        accountAutoComplete.setFocusable(false);
        accountAutoComplete.setOnClickListener(v -> openAccountPicker());
        accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);
        cashboxViewModel = new ViewModelProvider(this).get(CashboxViewModel.class);
        AccountRepository accountRepository = ((App) requireActivity().getApplication()).getAccountRepository();
        TransactionViewModelFactory transactionFactory = new TransactionViewModelFactory(accountRepository);
        transactionViewModel = new ViewModelProvider(this, transactionFactory).get(TransactionViewModel.class);
        transactionRepository = ((App) requireActivity().getApplication()).getTransactionRepository();
        currencies = getResources().getStringArray(R.array.currencies_array);
        currencyAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, currencies);
        currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fromCurrencySpinner.setAdapter(currencyAdapter);
        toCurrencySpinner.setAdapter(currencyAdapter);
        opTypeAdapter = ArrayAdapter.createFromResource(requireContext(), R.array.exchange_operation_types, android.R.layout.simple_spinner_item);
        opTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        operationTypeSpinner.setAdapter(opTypeAdapter);
        accountViewModel.getAllAccounts().observe(getViewLifecycleOwner(), accs -> {
            accounts = accs;
        });
        cashboxViewModel.getAllCashboxes().observe(getViewLifecycleOwner(), cbList -> {
            cashboxes = cbList != null ? cbList : new ArrayList<>();
            List<String> names = new ArrayList<>();
            for (Cashbox cb : cashboxes) names.add(cb.getName());
            cashboxAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, names);
            cashboxAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            cashboxSpinner.setAdapter(cashboxAdapter);
            if (!cashboxes.isEmpty()) {
                cashboxSpinner.setSelection(0);
                selectedCashboxId = cashboxes.get(0).getId();
            }
        });
        cashboxSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view1, int position, long id) {
                if (position >= 0 && position < cashboxes.size()) {
                    selectedCashboxId = cashboxes.get(position).getId();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        fromCurrencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view1, int position, long id) {
                updateToCurrencyOptions();
                updateBalanceText();
                updateExchangeAmount();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        toCurrencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view1, int position, long id) {
                updateExchangeAmount();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        operationTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view1, int position, long id) {
                updateExchangeAmount();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        amountEditText.addTextChangedListener(new SimpleTextWatcher(this::updateExchangeAmount));
        rateEditText.addTextChangedListener(new SimpleTextWatcher(this::updateExchangeAmount));
        transactionsViewModel = new ViewModelProvider(this).get(TransactionsViewModel.class);
        transactionsViewModel.getAccountBalancesMap().observe(getViewLifecycleOwner(), balancesMap -> {
            accountBalancesMap = balancesMap != null ? balancesMap : new HashMap<>();
        });
        Button exchangeButton = binding.exchangeButton;
        exchangeButton.setOnClickListener(v -> performExchange());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            int bottom = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime()).bottom;
            if (bottom == 0) {
                bottom = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()).bottom;
            }
            v.setPadding(0, 0, 0, bottom);
            return insets;
        });
    }

    private void openAccountPicker() {
        AccountPickerBottomSheet picker = new AccountPickerBottomSheet(accounts, new ArrayList<>(), accountBalancesMap, account -> {
            selectedAccount = account;
            accountAutoComplete.setText(account.getName());
            updateBalanceText();
        });
        picker.show(getParentFragmentManager(), "account_picker");
    }

    private void updateToCurrencyOptions() {
        String fromCurrency = fromCurrencySpinner.getSelectedItem() != null ? fromCurrencySpinner.getSelectedItem().toString() : "";
        List<String> toList = new ArrayList<>();
        for (String c : currencies) {
            if (!c.equals(fromCurrency)) toList.add(c);
        }
        ArrayAdapter<String> toAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, toList);
        toAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        toCurrencySpinner.setAdapter(toAdapter);
        if (!toList.isEmpty()) toCurrencySpinner.setSelection(0);
    }

    private String getSelectedFromCurrency() {
        return fromCurrencySpinner.getSelectedItem() != null ? fromCurrencySpinner.getSelectedItem().toString() : "";
    }

    private String getSelectedToCurrency() {
        return toCurrencySpinner.getSelectedItem() != null ? toCurrencySpinner.getSelectedItem().toString() : "";
    }

    private String getSelectedOperationType() {
        return operationTypeSpinner.getSelectedItem() != null ? operationTypeSpinner.getSelectedItem().toString() : "";
    }

    private void updateBalanceText() {
        if (selectedAccount == null) {
            accountBalanceText.setText("الرصيد: -");
            return;
        }
        String currency = getSelectedFromCurrency();
        long accountId = selectedAccount.getId();
        long now = System.currentTimeMillis();
        transactionRepository.getBalanceUntilDate(accountId, now, currency)
            .observe(getViewLifecycleOwner(), balance -> {
                double bal = (balance != null) ? balance : 0.0;
                accountBalanceText.setText("الرصيد: " + bal + " " + currency);
            });
    }

    private void updateExchangeAmount() {
        String amountStr = amountEditText.getText().toString().trim();
        String rateStr = rateEditText.getText().toString().trim();
        String opType = getSelectedOperationType();
        String fromCurrency = getSelectedFromCurrency();
        String toCurrency = getSelectedToCurrency();
        if (amountStr.isEmpty() || rateStr.isEmpty() || fromCurrency.equals(toCurrency)) {
            exchangeAmountText.setText("المبلغ بعد الصرف: -");
            return;
        }
        try {
            double amount = Double.parseDouble(amountStr);
            double rate = Double.parseDouble(rateStr);
            double toAmount = opType.equals("بيع") ? amount * rate : amount / rate;
            BigDecimal rounded = new BigDecimal(toAmount).setScale(2, RoundingMode.HALF_UP);
            exchangeAmountText.setText("المبلغ بعد الصرف: " + rounded.toPlainString() + " " + toCurrency);
        } catch (Exception e) {
            exchangeAmountText.setText("المبلغ بعد الصرف: -");
        }
    }

    private void performExchange() {
        if (selectedAccount == null) {
            Toast.makeText(getContext(), "يرجى اختيار الحساب", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedCashboxId == -1) {
            Toast.makeText(getContext(), "يرجى اختيار الصندوق", Toast.LENGTH_SHORT).show();
            return;
        }
        String fromCurrency = getSelectedFromCurrency();
        String toCurrency = getSelectedToCurrency();
        String opType = getSelectedOperationType();
        String amountStr = amountEditText.getText().toString().trim();
        String rateStr = rateEditText.getText().toString().trim();
        String notes = notesEditText.getText().toString().trim();
        if (TextUtils.isEmpty(amountStr) || TextUtils.isEmpty(rateStr)) {
            Toast.makeText(getContext(), "يرجى إدخال المبلغ وسعر الصرف", Toast.LENGTH_SHORT).show();
            return;
        }
        if (fromCurrency.equals(toCurrency)) {
            Toast.makeText(getContext(), "العملتان متطابقتان!", Toast.LENGTH_SHORT).show();
            return;
        }
        double amount = Double.parseDouble(amountStr);
        double rate = Double.parseDouble(rateStr);
        double toAmount = opType.equals("بيع") ? amount * rate : amount / rate;
        BigDecimal rounded = new BigDecimal(toAmount).setScale(2, RoundingMode.HALF_UP);
        String desc = opType + " عملة من " + fromCurrency + " إلى " + toCurrency + " بسعر صرف " + rate;
        if (!TextUtils.isEmpty(notes)) desc += " - " + notes;
        boolean whatsappEnabled = selectedAccount != null && selectedAccount.isWhatsappEnabled();
        // معاملة الخصم
        Transaction debitTx = new Transaction();
        debitTx.setAccountId(selectedAccount.getId());
        debitTx.setAmount(amount);
        debitTx.setCurrency(fromCurrency);
        debitTx.setType("debit");
        debitTx.setDescription(desc);
        debitTx.setWhatsappEnabled(whatsappEnabled);
        debitTx.setCashboxId(selectedCashboxId);
        // معاملة الإضافة
        Transaction creditTx = new Transaction();
        creditTx.setAccountId(selectedAccount.getId());
        creditTx.setAmount(rounded.doubleValue());
        creditTx.setCurrency(toCurrency);
        creditTx.setType("credit");
        creditTx.setDescription(desc);
        creditTx.setWhatsappEnabled(whatsappEnabled);
        creditTx.setCashboxId(selectedCashboxId);
        transactionViewModel.insertTransaction(debitTx);
        transactionViewModel.insertTransaction(creditTx);
        Toast.makeText(getContext(), "تمت عملية الصرف بنجاح", Toast.LENGTH_LONG).show();
        amountEditText.setText("");
        rateEditText.setText("");
        notesEditText.setText("");
        exchangeAmountText.setText("المبلغ بعد الصرف: -");
    }

    // أداة مساعدة لمراقبة النصوص
    private static class SimpleTextWatcher implements android.text.TextWatcher {
        private final Runnable callback;
        SimpleTextWatcher(Runnable callback) { this.callback = callback; }
        public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
        public void onTextChanged(CharSequence s, int st, int b, int c) { callback.run(); }
        public void afterTextChanged(android.text.Editable s) {}
    }
} 
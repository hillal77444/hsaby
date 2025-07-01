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
import com.hillal.acc.data.repository.AccountRepository;
import com.hillal.acc.App;
import java.util.List;
import com.hillal.acc.ui.common.AccountPickerBottomSheet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import com.hillal.acc.data.entities.Cashbox;
import com.hillal.acc.viewmodel.CashboxViewModel;
import com.hillal.acc.data.repository.TransactionRepository;
import com.hillal.acc.ui.transactions.TransactionsViewModel;
import java.math.BigDecimal;
import java.math.RoundingMode;
import com.google.android.material.textfield.TextInputEditText;

public class TransferFragment extends Fragment {
    private TextInputEditText fromAccountAutoComplete, toAccountAutoComplete;
    private TextView fromAccountBalanceText, toAccountBalanceText;
    private Spinner cashboxSpinner, currencySpinner;
    private EditText amountEditText, notesEditText;
    private List<Account> accounts;
    private List<Cashbox> cashboxes = new ArrayList<>();
    private Account fromAccount, toAccount;
    private long selectedCashboxId = -1;
    private String[] currencies;
    private ArrayAdapter<String> currencyAdapter;
    private ArrayAdapter<String> cashboxAdapter;
    private Map<Long, Map<String, Double>> accountBalancesMap = new HashMap<>();
    private CashboxViewModel cashboxViewModel;
    private AccountViewModel accountViewModel;
    private TransactionRepository transactionRepository;
    private TransactionsViewModel transactionsViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transfer, container, false);
        fromAccountAutoComplete = view.findViewById(R.id.fromAccountAutoComplete);
        toAccountAutoComplete = view.findViewById(R.id.toAccountAutoComplete);
        fromAccountBalanceText = view.findViewById(R.id.fromAccountBalanceText);
        toAccountBalanceText = view.findViewById(R.id.toAccountBalanceText);
        cashboxSpinner = view.findViewById(R.id.cashboxSpinner);
        currencySpinner = view.findViewById(R.id.currencySpinner);
        amountEditText = view.findViewById(R.id.amountEditText);
        notesEditText = view.findViewById(R.id.notesEditText);
        fromAccountAutoComplete.setFocusable(false);
        fromAccountAutoComplete.setOnClickListener(v -> openAccountPicker(true));
        toAccountAutoComplete.setFocusable(false);
        toAccountAutoComplete.setOnClickListener(v -> openAccountPicker(false));
        accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);
        cashboxViewModel = new ViewModelProvider(this).get(CashboxViewModel.class);
        AccountRepository accountRepository = ((App) requireActivity().getApplication()).getAccountRepository();
        transactionRepository = ((App) requireActivity().getApplication()).getTransactionRepository();
        currencies = getResources().getStringArray(R.array.currencies_array);
        currencyAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, currencies);
        currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        currencySpinner.setAdapter(currencyAdapter);
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
        currencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view1, int position, long id) {
                updateBalances();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        amountEditText.addTextChangedListener(new SimpleTextWatcher(this::updateBalances));
        transactionsViewModel = new ViewModelProvider(this).get(TransactionsViewModel.class);
        transactionsViewModel.getAccountBalancesMap().observe(getViewLifecycleOwner(), balancesMap -> {
            accountBalancesMap = balancesMap != null ? balancesMap : new HashMap<>();
            updateBalances();
        });
        Button transferButton = view.findViewById(R.id.transferButton);
        transferButton.setOnClickListener(v -> performTransfer());
        return view;
    }

    private void openAccountPicker(boolean isFrom) {
        AccountPickerBottomSheet picker = new AccountPickerBottomSheet(accounts, new ArrayList<>(), accountBalancesMap, account -> {
            if (isFrom) {
                fromAccount = account;
                fromAccountAutoComplete.setText(account.getName());
            } else {
                toAccount = account;
                toAccountAutoComplete.setText(account.getName());
            }
            updateBalances();
        });
        picker.show(getParentFragmentManager(), isFrom ? "from_account_picker" : "to_account_picker");
    }

    private void updateBalances() {
        String currency = getSelectedCurrency();
        if (fromAccount != null) {
            long accountId = fromAccount.getId();
            double bal = 0.0;
            if (accountBalancesMap.containsKey(accountId) && accountBalancesMap.get(accountId).containsKey(currency)) {
                bal = accountBalancesMap.get(accountId).get(currency);
            }
            fromAccountBalanceText.setText("الرصيد: " + bal + " " + currency);
        } else {
            fromAccountBalanceText.setText("الرصيد: -");
        }
        if (toAccount != null) {
            long accountId = toAccount.getId();
            double bal = 0.0;
            if (accountBalancesMap.containsKey(accountId) && accountBalancesMap.get(accountId).containsKey(currency)) {
                bal = accountBalancesMap.get(accountId).get(currency);
            }
            toAccountBalanceText.setText("الرصيد: " + bal + " " + currency);
        } else {
            toAccountBalanceText.setText("الرصيد: -");
        }
    }

    private String getSelectedCurrency() {
        return currencySpinner.getSelectedItem() != null ? currencySpinner.getSelectedItem().toString() : "";
    }

    private void performTransfer() {
        if (fromAccount == null || toAccount == null) {
            Toast.makeText(getContext(), "يرجى اختيار الحسابين", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedCashboxId == -1) {
            Toast.makeText(getContext(), "يرجى اختيار الصندوق", Toast.LENGTH_SHORT).show();
            return;
        }
        String currency = getSelectedCurrency();
        String amountStr = amountEditText.getText().toString().trim();
        String notes = notesEditText.getText().toString().trim();
        if (TextUtils.isEmpty(amountStr)) {
            Toast.makeText(getContext(), "يرجى إدخال المبلغ", Toast.LENGTH_SHORT).show();
            return;
        }
        double amount = Double.parseDouble(amountStr);
        String desc = "تحويل من " + fromAccount.getName() + " إلى " + toAccount.getName() + (TextUtils.isEmpty(notes) ? "" : (" - " + notes));
        // معاملة الخصم
        Transaction debitTx = new Transaction();
        debitTx.setAccountId(fromAccount.getId());
        debitTx.setAmount(amount);
        debitTx.setCurrency(currency);
        debitTx.setType("debit");
        debitTx.setDescription(desc);
        debitTx.setWhatsappEnabled(fromAccount.isWhatsappEnabled());
        debitTx.setCashboxId(selectedCashboxId);
        // معاملة الإضافة
        Transaction creditTx = new Transaction();
        creditTx.setAccountId(toAccount.getId());
        creditTx.setAmount(amount);
        creditTx.setCurrency(currency);
        creditTx.setType("credit");
        creditTx.setDescription(desc);
        creditTx.setWhatsappEnabled(toAccount.isWhatsappEnabled());
        creditTx.setCashboxId(selectedCashboxId);
        transactionRepository.insert(debitTx);
        transactionRepository.insert(creditTx);
        Toast.makeText(getContext(), "تمت عملية التحويل بنجاح", Toast.LENGTH_LONG).show();
        amountEditText.setText("");
        notesEditText.setText("");
        updateBalances();
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
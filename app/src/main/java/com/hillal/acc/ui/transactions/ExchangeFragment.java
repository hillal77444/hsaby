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

public class ExchangeFragment extends Fragment {
    private Spinner accountSpinner, fromCurrencySpinner, toCurrencySpinner, operationTypeSpinner;
    private EditText amountEditText, rateEditText, notesEditText;
    private Button exchangeButton;
    private AccountViewModel accountViewModel;
    private TransactionViewModel transactionViewModel;
    private List<Account> accounts;
    private ArrayAdapter<Account> accountAdapter;
    private ArrayAdapter<String> currencyAdapter;
    private String[] currencies;
    private TextView selectedAccountNameText, balanceText, exchangeAmountText;
    private MaterialButton selectAccountButton;
    private Account selectedAccount;
    private Map<Long, Map<String, Double>> accountBalancesMap = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_exchange, container, false);
        accountSpinner = view.findViewById(R.id.accountSpinner);
        fromCurrencySpinner = view.findViewById(R.id.fromCurrencySpinner);
        toCurrencySpinner = view.findViewById(R.id.toCurrencySpinner);
        operationTypeSpinner = view.findViewById(R.id.operationTypeSpinner);
        amountEditText = view.findViewById(R.id.amountEditText);
        rateEditText = view.findViewById(R.id.rateEditText);
        notesEditText = view.findViewById(R.id.notesEditText);
        exchangeButton = view.findViewById(R.id.exchangeButton);
        selectAccountButton = view.findViewById(R.id.selectAccountButton);
        selectedAccountNameText = view.findViewById(R.id.selectedAccountNameText);
        balanceText = view.findViewById(R.id.balanceText);
        exchangeAmountText = view.findViewById(R.id.exchangeAmountText);

        accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);
        AccountRepository accountRepository = ((App) requireActivity().getApplication()).getAccountRepository();
        TransactionViewModelFactory transactionFactory = new TransactionViewModelFactory(accountRepository);
        transactionViewModel = new ViewModelProvider(this, transactionFactory).get(TransactionViewModel.class);

        currencies = getResources().getStringArray(R.array.currencies_array);
        currencyAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, currencies);
        currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fromCurrencySpinner.setAdapter(currencyAdapter);
        toCurrencySpinner.setAdapter(currencyAdapter);

        ArrayAdapter<CharSequence> opTypeAdapter = ArrayAdapter.createFromResource(requireContext(), R.array.exchange_operation_types, android.R.layout.simple_spinner_item);
        opTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        operationTypeSpinner.setAdapter(opTypeAdapter);

        accountViewModel.getAllAccounts().observe(getViewLifecycleOwner(), accs -> {
            accounts = accs;
            accountAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, accounts);
            accountAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            accountSpinner.setAdapter(accountAdapter);
        });

        selectAccountButton.setOnClickListener(v -> openAccountPicker());
        exchangeButton.setOnClickListener(v -> performExchange());
        fromCurrencySpinner.setOnItemClickListener((parent, view1, position, id) -> updateBalanceText());
        amountEditText.addTextChangedListener(new SimpleTextWatcher(this::updateExchangeAmount));
        rateEditText.addTextChangedListener(new SimpleTextWatcher(this::updateExchangeAmount));
        operationTypeSpinner.setOnItemClickListener((parent, view12, position, id) -> updateExchangeAmount());
        return view;
    }

    private void openAccountPicker() {
        AccountPickerBottomSheet picker = new AccountPickerBottomSheet(accounts, /*transactions*/ new ArrayList<>(), accountBalancesMap, account -> {
            selectedAccount = account;
            selectedAccountNameText.setText(account.getName());
            updateBalanceText();
        });
        picker.show(getParentFragmentManager(), "account_picker");
    }

    private void updateBalanceText() {
        if (selectedAccount == null) {
            balanceText.setText("الرصيد: -");
            return;
        }
        String currency = fromCurrencySpinner.getText().toString();
        Map<String, Double> balances = accountBalancesMap.get(selectedAccount.getId());
        double balance = 0.0;
        if (balances != null && balances.containsKey(currency)) {
            balance = balances.get(currency);
        }
        balanceText.setText("الرصيد: " + balance + " " + currency);
        updateExchangeAmount();
    }

    private void updateExchangeAmount() {
        String amountStr = amountEditText.getText().toString().trim();
        String rateStr = rateEditText.getText().toString().trim();
        String opType = operationTypeSpinner.getText().toString();
        if (amountStr.isEmpty() || rateStr.isEmpty()) {
            exchangeAmountText.setText("المبلغ بعد الصرف: -");
            return;
        }
        try {
            double amount = Double.parseDouble(amountStr);
            double rate = Double.parseDouble(rateStr);
            double toAmount = opType.equals("بيع") ? amount * rate : amount / rate;
            BigDecimal rounded = new BigDecimal(toAmount).setScale(2, RoundingMode.HALF_UP);
            exchangeAmountText.setText("المبلغ بعد الصرف: " + rounded.toPlainString());
        } catch (Exception e) {
            exchangeAmountText.setText("المبلغ بعد الصرف: -");
        }
    }

    private void performExchange() {
        if (selectedAccount == null) {
            Toast.makeText(getContext(), "يرجى اختيار الحساب", Toast.LENGTH_SHORT).show();
            return;
        }
        String fromCurrency = fromCurrencySpinner.getText().toString();
        String toCurrency = toCurrencySpinner.getText().toString();
        String opType = operationTypeSpinner.getText().toString();
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
        // معاملة الإضافة
        Transaction creditTx = new Transaction();
        creditTx.setAccountId(selectedAccount.getId());
        creditTx.setAmount(rounded.doubleValue());
        creditTx.setCurrency(toCurrency);
        creditTx.setType("credit");
        creditTx.setDescription(desc);
        creditTx.setWhatsappEnabled(whatsappEnabled);
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
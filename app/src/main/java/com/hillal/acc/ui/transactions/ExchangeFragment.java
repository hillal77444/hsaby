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
import com.hillal.acc.data.local.entity.Account;
import com.hillal.acc.data.local.entity.Transaction;
import com.hillal.acc.ui.accounts.AccountViewModel;
import com.hillal.acc.ui.transactions.TransactionViewModel;
import java.util.List;

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

        accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);
        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);

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

        exchangeButton.setOnClickListener(v -> performExchange());
        return view;
    }

    private void performExchange() {
        int accountPos = accountSpinner.getSelectedItemPosition();
        if (accountPos == AdapterView.INVALID_POSITION) {
            Toast.makeText(getContext(), "يرجى اختيار الحساب", Toast.LENGTH_SHORT).show();
            return;
        }
        Account account = accounts.get(accountPos);
        String fromCurrency = (String) fromCurrencySpinner.getSelectedItem();
        String toCurrency = (String) toCurrencySpinner.getSelectedItem();
        String opType = (String) operationTypeSpinner.getSelectedItem();
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
        // الوصف التلقائي
        String desc = opType + " عملة من " + fromCurrency + " إلى " + toCurrency + " بسعر صرف " + rate;
        if (!TextUtils.isEmpty(notes)) desc += " - " + notes;
        // معاملة الخصم
        Transaction debitTx = new Transaction();
        debitTx.setAccountId(account.getId());
        debitTx.setAmount(amount);
        debitTx.setCurrency(fromCurrency);
        debitTx.setType("debit");
        debitTx.setDescription(desc);
        // معاملة الإضافة
        Transaction creditTx = new Transaction();
        creditTx.setAccountId(account.getId());
        creditTx.setAmount(toAmount);
        creditTx.setCurrency(toCurrency);
        creditTx.setType("credit");
        creditTx.setDescription(desc);
        // الحفظ
        transactionViewModel.insertTransaction(debitTx);
        transactionViewModel.insertTransaction(creditTx);
        Toast.makeText(getContext(), "تمت عملية الصرف بنجاح", Toast.LENGTH_LONG).show();
        amountEditText.setText("");
        rateEditText.setText("");
        notesEditText.setText("");
    }
} 
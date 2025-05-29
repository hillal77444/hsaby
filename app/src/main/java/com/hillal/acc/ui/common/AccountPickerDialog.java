package com.hillal.acc.ui.common;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.hillal.acc.R;
import com.hillal.acc.data.model.Account;
import com.hillal.acc.data.model.Transaction;
import com.hillal.acc.ui.transactions.AccountPickerAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountPickerDialog {
    private final Context context;
    private final List<Account> allAccounts;
    private final List<Transaction> allTransactions;
    private final Map<Long, Map<String, Double>> accountBalancesMap;
    private final OnAccountSelectedListener listener;

    public interface OnAccountSelectedListener {
        void onAccountSelected(Account account);
    }

    public AccountPickerDialog(Context context, 
                             List<Account> accounts, 
                             List<Transaction> transactions,
                             Map<Long, Map<String, Double>> balancesMap,
                             OnAccountSelectedListener listener) {
        this.context = context;
        this.allAccounts = accounts;
        this.allTransactions = transactions;
        this.accountBalancesMap = balancesMap;
        this.listener = listener;
    }

    public void show() {
        if (allAccounts == null || allAccounts.isEmpty()) {
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View sheetView = LayoutInflater.from(context).inflate(R.layout.bottomsheet_account_picker, null);
        dialog.setContentView(sheetView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        EditText searchEditText = sheetView.findViewById(R.id.searchEditText);
        RecyclerView accountsRecyclerView = sheetView.findViewById(R.id.accountsRecyclerView);
        accountsRecyclerView.setLayoutManager(new LinearLayoutManager(context));

        Map<Long, List<Transaction>> accountTxMap = new HashMap<>();
        for (Transaction t : allTransactions) {
            if (!accountTxMap.containsKey(t.getAccountId())) {
                accountTxMap.put(t.getAccountId(), new ArrayList<>());
            }
            accountTxMap.get(t.getAccountId()).add(t);
        }

        AccountPickerAdapter adapter = new AccountPickerAdapter(context, allAccounts, accountTxMap, account -> {
            listener.onAccountSelected(account);
            dialog.dismiss();
        });
        adapter.setBalancesMap(accountBalancesMap);
        accountsRecyclerView.setAdapter(adapter);

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
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

            @Override
            public void afterTextChanged(Editable s) {}
        });

        dialog.show();
    }
} 
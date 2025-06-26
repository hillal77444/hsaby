package com.hillal.acc.ui.common;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hillal.acc.R;
import com.hillal.acc.data.model.Account;
import com.hillal.acc.data.model.Transaction;
import com.hillal.acc.ui.transactions.AccountPickerAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountPickerBottomSheet extends DialogFragment {
    private List<Account> allAccounts = new ArrayList<>();
    private List<Transaction> allTransactions = new ArrayList<>();
    private Map<Long, Map<String, Double>> accountBalancesMap = new HashMap<>();
    private OnAccountSelectedListener listener;

    public interface OnAccountSelectedListener {
        void onAccountSelected(Account account);
    }

    public AccountPickerBottomSheet(List<Account> accounts, List<Transaction> transactions, Map<Long, Map<String, Double>> balancesMap, OnAccountSelectedListener listener) {
        this.allAccounts = accounts;
        this.allTransactions = transactions;
        this.accountBalancesMap = balancesMap;
        this.listener = listener;
    }

    public AccountPickerBottomSheet() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottomsheet_account_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText searchEditText = view.findViewById(R.id.searchEditText);
        RecyclerView accountsRecyclerView = view.findViewById(R.id.accountsRecyclerView);
        accountsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        Map<Long, List<Transaction>> accountTxMap = new HashMap<>();
        for (Transaction t : allTransactions) {
            if (!accountTxMap.containsKey(t.getAccountId())) {
                accountTxMap.put(t.getAccountId(), new ArrayList<>());
            }
            accountTxMap.get(t.getAccountId()).add(t);
        }

        AccountPickerAdapter adapter = new AccountPickerAdapter(requireContext(), allAccounts, accountTxMap, account -> {
            if (listener != null) listener.onAccountSelected(account);
            dismiss();
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
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setGravity(Gravity.BOTTOM);
            window.setBackgroundDrawableResource(android.R.color.white);
            WindowManager.LayoutParams params = window.getAttributes();
            params.windowAnimations = R.style.DialogAnimation_SlideUp;
            window.setAttributes(params);
        }
    }
} 
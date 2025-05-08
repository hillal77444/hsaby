package com.hillal.hhhhhhh.ui.accounts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.viewmodel.AccountViewModel;
import com.hillal.hhhhhhh.databinding.FragmentAddAccountBinding;

public class EditAccountFragment extends Fragment {
    private FragmentAddAccountBinding binding;
    private AccountViewModel accountViewModel;
    private long accountId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);
        if (getArguments() != null) {
            accountId = getArguments().getLong("accountId", -1);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAddAccountBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews();
        loadAccount();
    }

    private void setupViews() {
        binding.saveButton.setOnClickListener(v -> updateAccount());
        binding.cancelButton.setOnClickListener(v -> requireActivity().onBackPressed());
    }

    private void loadAccount() {
        if (accountId != -1) {
            accountViewModel.getAccountById(accountId).observe(getViewLifecycleOwner(), account -> {
                if (account != null) {
                    binding.nameEditText.setText(account.getName());
                    binding.phoneEditText.setText(account.getPhoneNumber());
                    binding.openingBalanceEditText.setText(String.valueOf(account.getBalance()));
                    binding.notesEditText.setText(account.getNotes());
                }
            });
        }
    }

    private void updateAccount() {
        String name = binding.nameEditText.getText().toString();
        String phone = binding.phoneEditText.getText().toString();
        String notes = binding.notesEditText.getText().toString();
        String balanceStr = binding.openingBalanceEditText.getText().toString();

        if (name.isEmpty()) {
            binding.nameEditText.setError("الرجاء إدخال اسم الحساب");
            return;
        }

        try {
            double balance = Double.parseDouble(balanceStr);
            accountViewModel.getAccountById(accountId).observe(getViewLifecycleOwner(), account -> {
                if (account != null) {
                    account.setName(name);
                    account.setPhoneNumber(phone);
                    account.setBalance(balance);
                    account.setNotes(notes);
                    account.setUpdatedAt(System.currentTimeMillis());
                    accountViewModel.updateAccount(account);
                    Toast.makeText(getContext(), R.string.account_saved, Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).navigateUp();
                }
            });
        } catch (NumberFormatException e) {
            binding.openingBalanceEditText.setError("الرجاء إدخال رصيد صحيح");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 
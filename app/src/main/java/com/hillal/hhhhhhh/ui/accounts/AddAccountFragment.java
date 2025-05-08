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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.viewmodel.AccountViewModel;
import com.hillal.hhhhhhh.databinding.FragmentAddAccountBinding;

public class AddAccountFragment extends Fragment {
    private FragmentAddAccountBinding binding;
    private AccountViewModel accountViewModel;
    private TextInputEditText nameEditText;
    private TextInputEditText phoneEditText;
    private TextInputEditText notesEditText;
    private TextInputEditText openingBalanceEditText;
    private MaterialButton saveButton;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);
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
    }

    private void setupViews() {
        nameEditText = binding.nameEditText;
        phoneEditText = binding.phoneEditText;
        notesEditText = binding.notesEditText;
        openingBalanceEditText = binding.openingBalanceEditText;
        saveButton = binding.saveButton;

        saveButton.setOnClickListener(v -> saveAccount());
        binding.cancelButton.setOnClickListener(v -> requireActivity().onBackPressed());
    }

    private void saveAccount() {
        String name = binding.nameEditText.getText().toString();
        String phone = binding.phoneEditText.getText().toString();
        String notes = binding.notesEditText.getText().toString();
        String balanceStr = binding.balanceEditText.getText().toString();

        if (name.isEmpty()) {
            binding.nameEditText.setError("الرجاء إدخال اسم الحساب");
            return;
        }

        try {
            double balance = Double.parseDouble(balanceStr);
            Account account = new Account(
                String.valueOf(System.currentTimeMillis()), // account number
                name,
                balance,
                phone,
                false // isDebtor
            );
            account.setNotes(notes);
            accountViewModel.insertAccount(account);
            Toast.makeText(getContext(), R.string.account_saved, Toast.LENGTH_SHORT).show();
            Navigation.findNavController(requireView()).navigateUp();
        } catch (NumberFormatException e) {
            binding.balanceEditText.setError("الرجاء إدخال رصيد صحيح");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 
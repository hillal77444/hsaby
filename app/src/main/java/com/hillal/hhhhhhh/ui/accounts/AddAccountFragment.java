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
import com.hillal.hhhhhhh.data.entities.Account;
import com.hillal.hhhhhhh.viewmodel.AccountViewModel;
import com.hillal.hhhhhhh.databinding.FragmentAddAccountBinding;

public class AddAccountFragment extends Fragment {
    private FragmentAddAccountBinding binding;
    private AccountViewModel accountViewModel;
    private TextInputEditText nameEditText;
    private TextInputEditText phoneEditText;
    private TextInputEditText notesEditText;
    private TextInputEditText balanceEditText;
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
        balanceEditText = binding.balanceEditText;
        saveButton = binding.saveButton;

        saveButton.setOnClickListener(v -> saveAccount());
        binding.cancelButton.setOnClickListener(v -> requireActivity().onBackPressed());
    }

    private void saveAccount() {
        String name = nameEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String notes = notesEditText.getText().toString().trim();
        String balanceStr = balanceEditText.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(getContext(), R.string.error_name_required, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double balance = balanceStr.isEmpty() ? 0.0 : Double.parseDouble(balanceStr);
            boolean isDebtor = balance < 0;

            Account account = new Account(
                0, // id
                name,
                phone.isEmpty() ? null : phone,
                notes.isEmpty() ? null : notes,
                Math.abs(balance),
                isDebtor,
                System.currentTimeMillis()
            );

            accountViewModel.insertAccount(account);
            Navigation.findNavController(requireView()).navigateUp();
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), R.string.error_invalid_balance, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 
package com.hillal.hhhhhhh;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.textfield.TextInputEditText;
import com.hillal.hhhhhhh.databinding.FragmentAddAccountBinding;

public class AddAccountFragment extends Fragment {
    private FragmentAddAccountBinding binding;
    private DashboardViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAddAccountBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.saveButton.setOnClickListener(v -> {
            String name = binding.accountNameInput.getText().toString().trim();
            String phone = binding.phoneInput.getText().toString().trim();
            String notes = binding.notesInput.getText().toString().trim();
            String balanceStr = binding.balanceInput.getText().toString().trim();
            boolean isCreditor = binding.creditorSwitch.isChecked();

            if (name.isEmpty()) {
                binding.accountNameInput.setError(getString(R.string.required_field));
                return;
            }

            if (balanceStr.isEmpty()) {
                binding.balanceInput.setError(getString(R.string.required_field));
                return;
            }

            try {
                double balance = Double.parseDouble(balanceStr);
                Account account = new Account(name, phone, notes, balance, isCreditor);
                viewModel.insertAccount(account);
                Toast.makeText(requireContext(), R.string.account_added, Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this).navigateUp();
            } catch (NumberFormatException e) {
                binding.balanceInput.setError(getString(R.string.invalid_number));
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 
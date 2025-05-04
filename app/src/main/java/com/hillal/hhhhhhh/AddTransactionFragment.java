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

import com.hillal.hhhhhhh.databinding.FragmentAddTransactionBinding;

import java.util.Date;

public class AddTransactionFragment extends Fragment {
    private FragmentAddTransactionBinding binding;
    private DashboardViewModel viewModel;
    private int accountId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAddTransactionBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        accountId = AddTransactionFragmentArgs.fromBundle(getArguments()).getAccountId();

        binding.saveButton.setOnClickListener(v -> {
            String description = binding.descriptionInput.getText().toString().trim();
            String amountStr = binding.amountInput.getText().toString().trim();
            boolean isCredit = binding.creditRadio.isChecked();

            if (description.isEmpty()) {
                binding.descriptionInput.setError(getString(R.string.required_field));
                return;
            }

            if (amountStr.isEmpty()) {
                binding.amountInput.setError(getString(R.string.required_field));
                return;
            }

            try {
                double amount = Double.parseDouble(amountStr);
                Transaction transaction = new Transaction(accountId, amount, description, new Date(), isCredit);
                viewModel.insertTransaction(transaction);
                Toast.makeText(requireContext(), R.string.transaction_added, Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this).navigateUp();
            } catch (NumberFormatException e) {
                binding.amountInput.setError(getString(R.string.invalid_number));
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 
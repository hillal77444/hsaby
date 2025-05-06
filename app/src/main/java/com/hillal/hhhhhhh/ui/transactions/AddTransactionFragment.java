package com.hillal.hhhhhhh.ui.transactions;

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
import com.hillal.hhhhhhh.data.model.Transaction;
import com.hillal.hhhhhhh.databinding.FragmentAddTransactionBinding;

import java.util.Calendar;
import java.util.Date;

public class AddTransactionFragment extends Fragment {
    private FragmentAddTransactionBinding binding;
    private TransactionsViewModel viewModel;
    private long accountId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TransactionsViewModel.class);
        accountId = getArguments() != null ? getArguments().getLong("accountId", -1) : -1;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAddTransactionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews();
        setupListeners();
    }

    private void setupViews() {
        // Set up date picker
        Calendar calendar = Calendar.getInstance();
        binding.datePicker.init(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
                null
        );
    }

    private void setupListeners() {
        binding.saveButton.setOnClickListener(v -> saveTransaction());
        binding.cancelButton.setOnClickListener(v -> Navigation.findNavController(requireView()).navigateUp());
    }

    private void saveTransaction() {
        String amountStr = binding.amountEditText.getText().toString();
        String description = binding.descriptionEditText.getText().toString();
        String notes = binding.notesEditText.getText().toString();
        boolean isDebit = binding.typeRadioGroup.getCheckedRadioButtonId() == R.id.radioDebit;
        String currency = getSelectedCurrency();

        if (amountStr.isEmpty()) {
            binding.amountEditText.setError(getString(R.string.error_amount_required));
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            Calendar calendar = Calendar.getInstance();
            calendar.set(
                    binding.datePicker.getYear(),
                    binding.datePicker.getMonth(),
                    binding.datePicker.getDayOfMonth()
            );

            Transaction transaction = new Transaction();
            transaction.setAccountId(accountId);
            transaction.setAmount(amount);
            transaction.setType(isDebit ? "مدين" : "دائن");
            transaction.setDescription(description);
            transaction.setNotes(notes);
            transaction.setCurrency(currency);
            transaction.setDate(calendar.getTime());

            viewModel.insertTransaction(transaction);
            Toast.makeText(requireContext(), R.string.transaction_saved, Toast.LENGTH_SHORT).show();
            Navigation.findNavController(requireView()).navigateUp();
        } catch (NumberFormatException e) {
            binding.amountEditText.setError(getString(R.string.error_invalid_amount));
        }
    }

    private String getSelectedCurrency() {
        int checkedId = binding.currencyRadioGroup.getCheckedRadioButtonId();
        if (checkedId == R.id.radioYer) {
            return getString(R.string.currency_yer);
        } else if (checkedId == R.id.radioSar) {
            return getString(R.string.currency_sar);
        } else {
            return getString(R.string.currency_usd);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
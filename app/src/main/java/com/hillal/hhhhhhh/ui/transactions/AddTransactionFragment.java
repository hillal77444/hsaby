package com.hillal.hhhhhhh.ui.transactions;

import android.app.DatePickerDialog;
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
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.model.Transaction;
import com.hillal.hhhhhhh.viewmodel.TransactionViewModel;
import com.hillal.hhhhhhh.databinding.FragmentAddTransactionBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddTransactionFragment extends Fragment {
    private FragmentAddTransactionBinding binding;
    private TransactionViewModel transactionViewModel;
    private TextInputEditText dateEditText;
    private TextInputEditText amountEditText;
    private TextInputEditText notesEditText;
    private MaterialButtonToggleGroup transactionTypeGroup;
    private MaterialButton saveButton;
    private long accountId;
    private final Calendar calendar = Calendar.getInstance();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accountId = getArguments() != null ? getArguments().getLong("accountId") : -1;
        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAddTransactionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews();
    }

    private void setupViews() {
        dateEditText = binding.dateEditText;
        amountEditText = binding.amountEditText;
        notesEditText = binding.notesEditText;
        transactionTypeGroup = binding.transactionTypeGroup;
        saveButton = binding.saveButton;

        dateEditText.setOnClickListener(v -> showDatePicker());
        saveButton.setOnClickListener(v -> saveTransaction());

        // Set default date to today
        updateDateDisplay();
    }

    private void showDatePicker() {
        DatePickerDialog datePicker = new DatePickerDialog(requireContext(),
            (view, year, month, day) -> {
                calendar.set(year, month, day);
                updateDateDisplay();
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePicker.show();
    }

    private void updateDateDisplay() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        dateEditText.setText(dateFormat.format(calendar.getTime()));
    }

    private void saveTransaction() {
        String amountStr = amountEditText.getText().toString().trim();
        String notes = notesEditText.getText().toString().trim();
        boolean isDebit = transactionTypeGroup.getCheckedButtonId() == R.id.debitButton;

        if (amountStr.isEmpty()) {
            amountEditText.setError(getString(R.string.error_amount_required));
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                amountEditText.setError(getString(R.string.error_invalid_amount));
                return;
            }

            Transaction transaction = new Transaction(accountId, amount, isDebit, notes);
            transaction.setDate(calendar.getTimeInMillis());

            transactionViewModel.insert(transaction);
            Toast.makeText(requireContext(), R.string.transaction_saved, Toast.LENGTH_SHORT).show();
            Navigation.findNavController(requireView()).navigateUp();
        } catch (NumberFormatException e) {
            amountEditText.setError(getString(R.string.error_invalid_amount));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
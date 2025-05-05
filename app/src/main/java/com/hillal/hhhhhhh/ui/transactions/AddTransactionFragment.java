package com.hillal.hhhhhhh.ui.transactions;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.entities.Transaction;
import com.hillal.hhhhhhh.viewmodel.TransactionViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddTransactionFragment extends Fragment {
    private TransactionViewModel transactionViewModel;
    private TextInputEditText dateEditText;
    private TextInputEditText amountEditText;
    private TextInputEditText notesEditText;
    private MaterialButtonToggleGroup transactionTypeGroup;
    private MaterialButton saveButton;
    private long accountId;
    private final Calendar calendar = Calendar.getInstance();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_add_transaction, container, false);

        // Get account ID from arguments
        accountId = getArguments() != null ? getArguments().getLong("accountId") : -1;
        if (accountId == -1) {
            Navigation.findNavController(root).navigateUp();
            return root;
        }

        // Initialize views
        dateEditText = root.findViewById(R.id.date_edit_text);
        amountEditText = root.findViewById(R.id.amount_edit_text);
        notesEditText = root.findViewById(R.id.notes_edit_text);
        transactionTypeGroup = root.findViewById(R.id.transaction_type_group);
        saveButton = root.findViewById(R.id.save_button);

        // Set current date
        updateDateDisplay();

        // Setup date picker
        dateEditText.setOnClickListener(v -> showDatePicker());

        // Initialize ViewModel
        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);

        // Setup save button
        saveButton.setOnClickListener(v -> saveTransaction());

        return root;
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            requireContext(),
            (view, year, month, dayOfMonth) -> {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDateDisplay();
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void updateDateDisplay() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        dateEditText.setText(dateFormat.format(calendar.getTime()));
    }

    private void saveTransaction() {
        String amountStr = amountEditText.getText().toString().trim();
        String notes = notesEditText.getText().toString().trim();

        if (amountStr.isEmpty()) {
            Toast.makeText(getContext(), R.string.error_amount_required, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                Toast.makeText(getContext(), R.string.error_invalid_amount, Toast.LENGTH_SHORT).show();
                return;
            }

            int selectedButtonId = transactionTypeGroup.getCheckedButtonId();
            boolean isDebit = selectedButtonId == R.id.debit_button;

            Transaction transaction = new Transaction();
            transaction.setAccountId(accountId);
            transaction.setAmount(amount);
            transaction.setDate(calendar.getTime());
            transaction.setNotes(notes.isEmpty() ? null : notes);
            transaction.setDebit(isDebit);

            transactionViewModel.insertTransaction(transaction);
            Navigation.findNavController(requireView()).navigateUp();
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), R.string.error_invalid_amount, Toast.LENGTH_SHORT).show();
        }
    }
} 
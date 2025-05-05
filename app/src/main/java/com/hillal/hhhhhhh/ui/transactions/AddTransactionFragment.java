package com.hillal.hhhhhhh.ui.transactions;

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
import com.google.android.material.textfield.TextInputEditText;
import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.entities.Transaction;
import com.hillal.hhhhhhh.viewmodel.TransactionViewModel;

import java.util.Date;

public class AddTransactionFragment extends Fragment {
    private TransactionViewModel transactionViewModel;
    private TextInputEditText amountEditText;
    private TextInputEditText notesEditText;
    private MaterialButton saveButton;
    private long accountId;

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
        amountEditText = root.findViewById(R.id.amount_edit_text);
        notesEditText = root.findViewById(R.id.notes_edit_text);
        saveButton = root.findViewById(R.id.save_button);

        // Initialize ViewModel
        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);

        // Setup save button
        saveButton.setOnClickListener(v -> saveTransaction());

        return root;
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

            Transaction transaction = new Transaction();
            transaction.setAccountId(accountId);
            transaction.setAmount(amount);
            transaction.setDate(new Date());
            transaction.setNotes(notes.isEmpty() ? null : notes);
            transaction.setDebit(true); // Default to debit

            transactionViewModel.insertTransaction(transaction);
            Navigation.findNavController(requireView()).navigateUp();
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), R.string.error_invalid_amount, Toast.LENGTH_SHORT).show();
        }
    }
} 
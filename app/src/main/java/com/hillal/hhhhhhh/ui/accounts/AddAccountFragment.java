package com.hillal.hhhhhhh.ui.accounts;

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
import com.google.android.material.textfield.TextInputLayout;
import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.entities.Account;
import com.hillal.hhhhhhh.viewmodel.AccountViewModel;

public class AddAccountFragment extends Fragment {
    private AccountViewModel accountViewModel;
    private TextInputEditText nameEditText;
    private TextInputEditText phoneEditText;
    private TextInputEditText notesEditText;
    private TextInputEditText balanceEditText;
    private MaterialButton saveButton;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_add_account, container, false);

        // Initialize views
        nameEditText = root.findViewById(R.id.name_edit_text);
        phoneEditText = root.findViewById(R.id.phone_edit_text);
        notesEditText = root.findViewById(R.id.notes_edit_text);
        balanceEditText = root.findViewById(R.id.balance_edit_text);
        saveButton = root.findViewById(R.id.save_button);

        // Initialize ViewModel
        accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);

        // Setup save button
        saveButton.setOnClickListener(v -> saveAccount());

        return root;
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

            Account account = new Account();
            account.setName(name);
            account.setPhoneNumber(phone.isEmpty() ? null : phone);
            account.setNotes(notes.isEmpty() ? null : notes);
            account.setOpeningBalance(Math.abs(balance));
            account.setDebtor(isDebtor);

            accountViewModel.insertAccount(account);
            Navigation.findNavController(requireView()).navigateUp();
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), R.string.error_invalid_balance, Toast.LENGTH_SHORT).show();
        }
    }
} 
package com.hillal.hhhhhhh.ui.accounts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
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
    private SwitchCompat whatsappSwitch;

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
        whatsappSwitch = binding.whatsappSwitch;

        whatsappSwitch.setText(getString(R.string.whatsapp_enabled));
        
        saveButton.setOnClickListener(v -> saveAccount());
        binding.cancelButton.setOnClickListener(v -> requireActivity().onBackPressed());
    }

    private void saveAccount() {
        String name = binding.nameEditText.getText().toString();
        String phone = binding.phoneEditText.getText().toString();
        String notes = binding.notesEditText.getText().toString();
        String balanceStr = binding.openingBalanceEditText.getText().toString();
        boolean whatsappEnabled = binding.whatsappSwitch.isChecked();

        if (name.isEmpty()) {
            binding.nameEditText.setError("الرجاء إدخال اسم الحساب");
            return;
        }

        if (phone.isEmpty()) {
            binding.phoneEditText.setError("الرجاء إدخال رقم الهاتف");
            return;
        }

        try {
            double balance = Double.parseDouble(balanceStr);
            
            // التحقق من وجود حساب بنفس رقم الهاتف
            Account existingAccountByPhone = accountViewModel.getAccountByPhoneNumberSync(phone);
            if (existingAccountByPhone != null) {
                binding.phoneEditText.setError("رقم الهاتف موجود مسبقاً");
                return;
            }

            // إنشاء حساب جديد برقم فريد
            Account account = new Account(
                accountViewModel.generateUniqueAccountNumber(),
                name,
                balance,
                phone,
                false // isDebtor
            );
            account.setNotes(notes);
            account.setWhatsappEnabled(whatsappEnabled);
            account.setServerId(-1);
            accountViewModel.insertAccount(account);
            Toast.makeText(getContext(), R.string.account_saved, Toast.LENGTH_SHORT).show();
            Navigation.findNavController(requireView()).navigateUp();
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
package com.hillal.acc.ui.accounts;

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
import androidx.core.view.ViewCompat;

import com.hillal.acc.R;
import com.hillal.acc.data.model.Account;
import com.hillal.acc.viewmodel.AccountViewModel;
import com.hillal.acc.databinding.FragmentAddAccountBinding;

public class EditAccountFragment extends Fragment {
    private FragmentAddAccountBinding binding;
    private AccountViewModel accountViewModel;
    private long accountId;
    private Account oldAccount;
    private SwitchCompat whatsappSwitch;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);
        if (getArguments() != null) {
            accountId = getArguments().getLong("accountId", -1);
        }
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
        loadAccount();
        // ضبط insets للجذر لرفع المحتوى مع الكيبورد وأزرار النظام
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int bottom = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime()).bottom;
            if (bottom == 0) {
                bottom = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()).bottom;
            }
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottom);
            return insets;
        });
    }

    private void setupViews() {
        whatsappSwitch = binding.whatsappSwitch;
        
        // تعيين النص بشكل صريح
        whatsappSwitch.setText(getString(R.string.whatsapp_enabled));
        
        binding.saveButton.setOnClickListener(v -> updateAccount());
        binding.cancelButton.setOnClickListener(v -> requireActivity().onBackPressed());
    }

    private void loadAccount() {
        if (accountId != -1) {
            accountViewModel.getAccountById(accountId).observe(getViewLifecycleOwner(), account -> {
                if (account != null) {
                    oldAccount = account;
                    binding.nameEditText.setText(account.getName());
                    binding.phoneEditText.setText(account.getPhoneNumber());
                    binding.openingBalanceEditText.setText(String.valueOf(account.getBalance()));
                    binding.notesEditText.setText(account.getNotes());
                    whatsappSwitch.setChecked(account.isWhatsappEnabled());
                }
            });
        }
    }

    private void updateAccount() {
        String name = binding.nameEditText.getText().toString();
        String phone = binding.phoneEditText.getText().toString();
        String notes = binding.notesEditText.getText().toString();
        String balanceStr = binding.openingBalanceEditText.getText().toString();
        boolean whatsappEnabled = whatsappSwitch.isChecked();

        if (name.isEmpty()) {
            binding.nameEditText.setError("الرجاء إدخال اسم الحساب");
            return;
        }

        try {
            double balance = Double.parseDouble(balanceStr);
            accountViewModel.getAccountById(accountId).observe(getViewLifecycleOwner(), account -> {
                if (account != null) {
                    if (oldAccount == null) {
                        Toast.makeText(getContext(), "حدث خطأ في تحميل بيانات الحساب الأصلية", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    account.setName(name);
                    account.setPhoneNumber(phone);
                    account.setBalance(balance);
                    account.setNotes(notes);
                    account.setWhatsappEnabled(whatsappEnabled);
                    account.setUpdatedAt(System.currentTimeMillis());
                    account.setServerId(oldAccount.getServerId()); 
                    account.setSyncStatus(0);
                    accountViewModel.updateAccount(account);
                    Toast.makeText(getContext(), R.string.account_saved, Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).navigateUp();
                }
            });
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
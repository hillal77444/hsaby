package com.hillal.acc.ui.accounts;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
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
import com.hillal.acc.R;
import com.hillal.acc.data.model.Account;
import com.hillal.acc.viewmodel.AccountViewModel;
import com.hillal.acc.databinding.FragmentAddAccountBinding;

public class AddAccountFragment extends Fragment {
    private static final int PICK_CONTACT_REQUEST = 1;
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

        // إضافة مستمع لزر اختيار جهة الاتصال
        binding.phoneEditText.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
            startActivityForResult(intent, PICK_CONTACT_REQUEST);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_CONTACT_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            Uri contactUri = data.getData();
            if (contactUri != null) {
                try {
                    Cursor cursor = requireActivity().getContentResolver().query(
                        contactUri,
                        new String[]{
                            ContactsContract.CommonDataKinds.Phone.NUMBER,
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                        },
                        null,
                        null,
                        null
                    );

                    if (cursor != null && cursor.moveToFirst()) {
                        String phoneNumber = cursor.getString(0);
                        String name = cursor.getString(1);
                        
                        // تنظيف رقم الهاتف من أي رموز غير ضرورية
                        phoneNumber = phoneNumber.replaceAll("[^0-9+]", "");
                        
                        // تعبئة الحقول
                        binding.nameEditText.setText(name);
                        binding.phoneEditText.setText(phoneNumber);
                        
                        cursor.close();
                    }
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "حدث خطأ أثناء اختيار جهة الاتصال", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void saveAccount() {
        String name = binding.nameEditText.getText().toString();
        String phone = binding.phoneEditText.getText().toString();
        String notes = binding.notesEditText.getText().toString();
        boolean whatsappEnabled = binding.whatsappSwitch.isChecked();

        if (name.isEmpty()) {
            binding.nameEditText.setError("الرجاء إدخال اسم الحساب");
            return;
        }

        if (phone.isEmpty()) {
            binding.phoneEditText.setError("الرجاء إدخال رقم الهاتف");
            return;
        }

        // التحقق من وجود حساب بنفس رقم الهاتف
        Account existingAccountByPhone = accountViewModel.getAccountByPhoneNumber(phone);
        if (existingAccountByPhone != null) {
            binding.phoneEditText.setError("رقم الهاتف موجود مسبقاً");
            return;
        }

        // إنشاء حساب جديد برقم فريد مع رصيد افتتاحي 100
        Account account = new Account(
            accountViewModel.generateUniqueAccountNumber(),
            name,
            100.0, // تعيين الرصيد الافتتاحي إلى 100
            phone,
            false // isDebtor
        );
        account.setNotes(notes);
        account.setWhatsappEnabled(whatsappEnabled);
        account.setServerId(-1);
        accountViewModel.insertAccount(account);
        Toast.makeText(getContext(), R.string.account_saved, Toast.LENGTH_SHORT).show();
        Navigation.findNavController(requireView()).navigateUp();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 
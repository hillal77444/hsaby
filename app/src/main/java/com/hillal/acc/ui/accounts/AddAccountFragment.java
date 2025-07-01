package com.hillal.acc.ui.accounts;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.core.view.ViewCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.hillal.acc.R;
import com.hillal.acc.data.model.Account;
import com.hillal.acc.viewmodel.AccountViewModel;
import com.hillal.acc.databinding.FragmentAddAccountBinding;

public class AddAccountFragment extends Fragment {
    private static final int PICK_CONTACT_REQUEST = 1;
    private static final int CONTACTS_PERMISSION_REQUEST = 2;
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            requireActivity().getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        }
        // ضبط insets للجذر عند ظهور الكيبورد أو أزرار النظام
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            int bottom = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime()).bottom;
            if (bottom == 0) {
                bottom = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()).bottom;
            }
            v.setPadding(0, 0, 0, bottom);
            return insets;
        });
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
        binding.contactsButton.setOnClickListener(v -> checkContactsPermission());
    }

    private void checkContactsPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            // إذا لم يكن لدينا الإذن، نطلبه
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.READ_CONTACTS},
                    CONTACTS_PERMISSION_REQUEST);
        } else {
            // إذا كان لدينا الإذن، نفتح قائمة جهات الاتصال
            openContactsPicker();
        }
    }

    private void openContactsPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(intent, PICK_CONTACT_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        if (requestCode == CONTACTS_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // تم منح الإذن، نفتح قائمة جهات الاتصال
                openContactsPicker();
            } else {
                // تم رفض الإذن
                Toast.makeText(requireContext(), 
                    "يجب السماح بالوصول إلى جهات الاتصال لاختيار جهة اتصال", 
                    Toast.LENGTH_LONG).show();
            }
        }
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
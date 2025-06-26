package com.hillal.acc.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.hillal.acc.databinding.FragmentSettingsBinding;
import com.hillal.acc.viewmodel.SettingsViewModel;
import com.hillal.acc.data.room.AppDatabase;
import com.hillal.acc.data.security.EncryptionManager;

public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding binding;
    private SettingsViewModel settingsViewModel;
    private EncryptionManager encryptionManager;
    private AppDatabase db;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        db = AppDatabase.getInstance(requireContext());
        encryptionManager = new EncryptionManager(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        setupUI();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews();
    }

    private void setupUI() {
        // إعدادات أخرى
        setupOtherSettings();
    }

    private void setupViews() {
        setupSecuritySettings();
        setupLogoutButton();
    }

    private void setupSecuritySettings() {
        binding.switchEncryption.setChecked(encryptionManager.isEncryptionEnabled());
        binding.switchEncryption.setOnCheckedChangeListener((buttonView, isChecked) -> {
            encryptionManager.enableEncryption(isChecked);
        });
    }

    private void setupLogoutButton() {
        binding.buttonLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                .setTitle("تأكيد تسجيل الخروج")
                .setMessage("هل أنت متأكد من تسجيل الخروج؟ سيتم حذف جميع البيانات المحلية.")
                .setPositiveButton("نعم", (dialog, which) -> {
                    performLogout();
                })
                .setNegativeButton("لا", null)
                .show();
        });
    }

    private void performLogout() {
        // حذف قاعدة البيانات المحلية
        db.clearAllTables();

        // مسح جميع SharedPreferences
        requireContext().getSharedPreferences("auth_prefs", 0).edit().clear().apply();
        requireContext().getSharedPreferences("user_prefs", 0).edit().clear().apply();
        requireContext().getSharedPreferences("app_settings", 0).edit().clear().apply();

        // إغلاق التطبيق نهائياً
        requireActivity().finishAffinity();
        System.exit(0);
    }

    private void setupOtherSettings() {
        // هنا يمكن إضافة إعدادات أخرى
        binding.buttonPrivacyPolicy.setOnClickListener(v -> {
            String url = "https://malyp.com/api/privacy-policy";
            startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)));
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
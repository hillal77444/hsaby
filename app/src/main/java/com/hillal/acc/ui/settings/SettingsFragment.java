package com.hillal.acc.ui.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.hillal.acc.R;
import com.hillal.acc.data.security.EncryptionManager;
import com.hillal.acc.databinding.FragmentSettingsBinding;
import com.hillal.acc.viewmodel.SettingsViewModel;
import com.hillal.acc.data.room.AppDatabase;
import com.hillal.acc.data.repository.AuthRepository;
import com.hillal.acc.App;

public class SettingsFragment extends Fragment {
    private static final String TAG = "SettingsFragment";
    private FragmentSettingsBinding binding;
    private SettingsViewModel settingsViewModel;
    private SharedPreferences sharedPreferences;
    private EncryptionManager encryptionManager;
    private AppDatabase db;
    private AuthRepository authRepository;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        db = AppDatabase.getInstance(requireContext());
        encryptionManager = new EncryptionManager(requireContext());
        sharedPreferences = requireContext().getSharedPreferences("app_settings", 0);
        authRepository = new AuthRepository(requireActivity().getApplication());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        db = AppDatabase.getInstance(requireContext());
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
            Toast.makeText(getContext(), 
                isChecked ? R.string.msg_encryption_enabled : R.string.msg_encryption_disabled, 
                Toast.LENGTH_SHORT).show();
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
        try {
            // حذف قاعدة البيانات المحلية
            db.clearAllTables();
            
            // تسجيل الخروج من AuthRepository
            authRepository.logout();
            
            // مسح جميع SharedPreferences
            SharedPreferences authPrefs = requireContext().getSharedPreferences("auth_prefs", 0);
            SharedPreferences userPrefs = requireContext().getSharedPreferences("user_prefs", 0);
            SharedPreferences appSettings = requireContext().getSharedPreferences("app_settings", 0);
            
            authPrefs.edit().clear().apply();
            userPrefs.edit().clear().apply();
            appSettings.edit().clear().apply();
            
            Toast.makeText(requireContext(), "تم تسجيل الخروج بنجاح", Toast.LENGTH_SHORT).show();
            
            // إغلاق النشاط الرئيسي وإعادة تشغيله من صفحة تسجيل الدخول
            Intent intent = new Intent(requireContext(), com.hillal.acc.MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
            
        } catch (Exception e) {
            Log.e(TAG, "Error during logout", e);
            Toast.makeText(requireContext(), "حدث خطأ أثناء تسجيل الخروج", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupOtherSettings() {
        // هنا يمكن إضافة إعدادات أخرى
        binding.buttonPrivacyPolicy.setOnClickListener(v -> {
            String url = "https://malyp.com/api/privacy-policy";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        });
    }

    private void showMessage(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 
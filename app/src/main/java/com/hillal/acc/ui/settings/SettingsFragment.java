package com.hillal.acc.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.hillal.acc.databinding.FragmentSettingsBinding;
import com.hillal.acc.viewmodel.SettingsViewModel;
import com.hillal.acc.data.room.AppDatabase;
import com.hillal.acc.data.security.EncryptionManager;
import java.io.File;
import java.util.concurrent.Executors;

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
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // 1. حذف جميع بيانات قاعدة البيانات
                db.clearAllTables();

                // 2. حذف جميع SharedPreferences تلقائياً لأي اسم (بما فيها security_prefs و backup_prefs)
                File sharedPrefsDir = new File(requireContext().getApplicationInfo().dataDir, "shared_prefs");
                if (sharedPrefsDir.exists() && sharedPrefsDir.isDirectory()) {
                    File[] files = sharedPrefsDir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            String fileName = file.getName();
                            if (fileName.endsWith(".xml")) {
                                String prefName = fileName.substring(0, fileName.length() - 4);
                                requireContext().getSharedPreferences(prefName, 0).edit().clear().apply();
                            }
                        }
                    }
                }
                // تأكيد مسح security_prefs و backup_prefs حتى لو لم تكن موجودة في shared_prefs
                requireContext().getSharedPreferences("security_prefs", 0).edit().clear().apply();
                requireContext().getSharedPreferences("backup_prefs", 0).edit().clear().apply();

                // 3. إغلاق التطبيق نهائياً على الـ UI Thread
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "تم تسجيل الخروج بنجاح", Toast.LENGTH_SHORT).show();
                    requireActivity().finishAffinity();
                    System.exit(0);
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(), "حدث خطأ أثناء تسجيل الخروج", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void setupOtherSettings() {
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
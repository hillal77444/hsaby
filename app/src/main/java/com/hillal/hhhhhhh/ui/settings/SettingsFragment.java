package com.hillal.hhhhhhh.ui.settings;

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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.backup.BackupManager;
import com.hillal.hhhhhhh.data.security.EncryptionManager;
import com.hillal.hhhhhhh.databinding.FragmentSettingsBinding;
import com.hillal.hhhhhhh.viewmodel.SettingsViewModel;
import com.hillal.hhhhhhh.data.room.AppDatabase;

public class SettingsFragment extends Fragment {
    private static final String TAG = "SettingsFragment";
    private FragmentSettingsBinding binding;
    private SettingsViewModel settingsViewModel;
    private SharedPreferences sharedPreferences;
    private BackupManager backupManager;
    private EncryptionManager encryptionManager;
    private AppDatabase db;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        backupManager = new BackupManager(requireContext());
        db = AppDatabase.getInstance(requireContext());
        encryptionManager = new EncryptionManager(requireContext());
        sharedPreferences = requireContext().getSharedPreferences("app_settings", 0);
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
        setupBackupSettings();
        setupSecuritySettings();
    }

    private void setupBackupSettings() {
        binding.switchAutoBackup.setChecked(backupManager.isAutoBackupEnabled());
        binding.switchAutoBackup.setOnCheckedChangeListener((buttonView, isChecked) -> {
            backupManager.enableAutoBackup(isChecked);
            Toast.makeText(getContext(), 
                isChecked ? R.string.msg_backup_enabled : R.string.msg_backup_disabled, 
                Toast.LENGTH_SHORT).show();
        });

        binding.buttonBackupNow.setOnClickListener(v -> {
            backupManager.createBackup(new BackupManager.BackupCallback() {
                @Override
                public void onSuccess(String backupPath) {
                    Toast.makeText(getContext(), R.string.msg_backup_success, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                }
            });
        });

        binding.buttonRestore.setOnClickListener(v -> {
            // TODO: Implement file picker for backup selection
            Toast.makeText(getContext(), R.string.msg_restore_not_implemented, Toast.LENGTH_SHORT).show();
        });
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

    private void setupOtherSettings() {
        // هنا يمكن إضافة إعدادات أخرى
        binding.buttonPrivacyPolicy.setOnClickListener(v -> {
            String url = "https://alhillal1.github.io/privacy-policy/";
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
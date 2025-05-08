package com.hillal.hhhhhhh.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
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
import com.hillal.hhhhhhh.data.sync.SyncManager;
import com.hillal.hhhhhhh.databinding.FragmentSettingsBinding;
import com.hillal.hhhhhhh.viewmodel.SettingsViewModel;
import com.hillal.hhhhhhh.data.database.AppDatabase;

public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding binding;
    private SettingsViewModel settingsViewModel;
    private SharedPreferences sharedPreferences;
    private BackupManager backupManager;
    private SyncManager syncManager;
    private EncryptionManager encryptionManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        backupManager = new BackupManager(requireContext());
        AppDatabase db = AppDatabase.getInstance(requireContext());
        syncManager = new SyncManager(requireContext(), db.accountDao(), db.transactionDao());
        encryptionManager = new EncryptionManager(requireContext());
        sharedPreferences = requireContext().getSharedPreferences("app_settings", 0);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews();
    }

    private void setupViews() {
        setupBackupSettings();
        setupSyncSettings();
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

    private void setupSyncSettings() {
        // تفعيل المزامنة التلقائية افتراضياً
        binding.switchAutoSync.setChecked(true);
        syncManager.enableAutoSync(true);
        
        binding.switchAutoSync.setOnCheckedChangeListener((buttonView, isChecked) -> {
            syncManager.enableAutoSync(isChecked);
            Toast.makeText(getContext(), 
                isChecked ? R.string.msg_sync_enabled : R.string.msg_sync_disabled, 
                Toast.LENGTH_SHORT).show();
        });

        binding.buttonSyncNow.setOnClickListener(v -> {
            syncManager.syncData(new SyncManager.SyncCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getContext(), R.string.msg_sync_success, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                }
            });
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 
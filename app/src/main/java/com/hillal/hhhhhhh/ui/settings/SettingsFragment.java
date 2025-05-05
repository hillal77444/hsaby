package com.hillal.hhhhhhh.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.databinding.FragmentSettingsBinding;
import com.hillal.hhhhhhh.viewmodel.SettingsViewModel;

public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding binding;
    private SettingsViewModel settingsViewModel;
    private SharedPreferences sharedPreferences;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
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

        // Load current settings
        boolean darkMode = sharedPreferences.getBoolean("dark_mode", false);
        boolean notifications = sharedPreferences.getBoolean("notifications", true);
        boolean autoBackup = sharedPreferences.getBoolean("auto_backup", true);

        binding.darkModeSwitch.setChecked(darkMode);
        binding.notificationsSwitch.setChecked(notifications);
        binding.autoBackupSwitch.setChecked(autoBackup);

        // Set up switch listeners
        binding.darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("dark_mode", isChecked).apply();
            // TODO: Apply theme change
        });

        binding.notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("notifications", isChecked).apply();
        });

        binding.autoBackupSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("auto_backup", isChecked).apply();
        });

        // Set up backup button
        binding.backupButton.setOnClickListener(v -> {
            // TODO: Implement backup functionality
        });

        // Set up restore button
        binding.restoreButton.setOnClickListener(v -> {
            // TODO: Implement restore functionality
        });

        // Set up clear data button
        binding.clearDataButton.setOnClickListener(v -> {
            // TODO: Show confirmation dialog and implement clear data
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 
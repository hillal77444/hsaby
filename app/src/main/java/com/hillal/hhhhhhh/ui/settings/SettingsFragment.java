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
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("app_settings", 0);

        // Load saved settings
        loadSettings();

        // Set up switch listeners
        setupSwitchListeners();
    }

    private void loadSettings() {
        // Load and set saved settings
        binding.switchDarkMode.setChecked(sharedPreferences.getBoolean("dark_mode", false));
        binding.switchNotifications.setChecked(sharedPreferences.getBoolean("notifications", true));
        binding.switchAutoBackup.setChecked(sharedPreferences.getBoolean("auto_backup", true));
        binding.switchCurrencyFormat.setChecked(sharedPreferences.getBoolean("currency_format", true));
    }

    private void setupSwitchListeners() {
        binding.switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("dark_mode", isChecked).apply();
            // TODO: Implement dark mode change
        });

        binding.switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("notifications", isChecked).apply();
            // TODO: Implement notifications toggle
        });

        binding.switchAutoBackup.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("auto_backup", isChecked).apply();
            // TODO: Implement auto backup toggle
        });

        binding.switchCurrencyFormat.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("currency_format", isChecked).apply();
            // TODO: Implement currency format change
        });

        // Set up backup button
        binding.buttonBackup.setOnClickListener(v -> {
            // TODO: Implement backup functionality
        });

        // Set up restore button
        binding.buttonRestore.setOnClickListener(v -> {
            // TODO: Implement restore functionality
        });

        // Set up clear data button
        binding.buttonClearData.setOnClickListener(v -> {
            // TODO: Implement clear data functionality with confirmation dialog
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 
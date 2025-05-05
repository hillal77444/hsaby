package com.hillal.hhhhhhh.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class SettingsViewModel extends AndroidViewModel {
    private final SharedPreferences sharedPreferences;
    private final MutableLiveData<Boolean> darkModeEnabled = new MutableLiveData<>();
    private final MutableLiveData<Boolean> notificationsEnabled = new MutableLiveData<>();
    private final MutableLiveData<Boolean> autoBackupEnabled = new MutableLiveData<>();
    private final MutableLiveData<Boolean> currencyFormatEnabled = new MutableLiveData<>();

    public SettingsViewModel(Application application) {
        super(application);
        sharedPreferences = application.getSharedPreferences("app_settings", 0);
        loadSettings();
    }

    private void loadSettings() {
        darkModeEnabled.setValue(sharedPreferences.getBoolean("dark_mode", false));
        notificationsEnabled.setValue(sharedPreferences.getBoolean("notifications", true));
        autoBackupEnabled.setValue(sharedPreferences.getBoolean("auto_backup", true));
        currencyFormatEnabled.setValue(sharedPreferences.getBoolean("currency_format", true));
    }

    public LiveData<Boolean> isDarkModeEnabled() {
        return darkModeEnabled;
    }

    public LiveData<Boolean> areNotificationsEnabled() {
        return notificationsEnabled;
    }

    public LiveData<Boolean> isAutoBackupEnabled() {
        return autoBackupEnabled;
    }

    public LiveData<Boolean> isCurrencyFormatEnabled() {
        return currencyFormatEnabled;
    }

    public void setDarkModeEnabled(boolean enabled) {
        darkModeEnabled.setValue(enabled);
        sharedPreferences.edit().putBoolean("dark_mode", enabled).apply();
    }

    public void setNotificationsEnabled(boolean enabled) {
        notificationsEnabled.setValue(enabled);
        sharedPreferences.edit().putBoolean("notifications", enabled).apply();
    }

    public void setAutoBackupEnabled(boolean enabled) {
        autoBackupEnabled.setValue(enabled);
        sharedPreferences.edit().putBoolean("auto_backup", enabled).apply();
    }

    public void setCurrencyFormatEnabled(boolean enabled) {
        currencyFormatEnabled.setValue(enabled);
        sharedPreferences.edit().putBoolean("currency_format", enabled).apply();
    }
} 
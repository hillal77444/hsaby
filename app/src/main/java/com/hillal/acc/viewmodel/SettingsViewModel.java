package com.hillal.acc.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.hillal.acc.data.model.Settings;
import com.hillal.acc.data.repository.SettingsRepository;
import java.util.List;

public class SettingsViewModel extends AndroidViewModel {
    private final SettingsRepository repository;
    private final LiveData<List<Settings>> allSettings;

    public SettingsViewModel(Application application) {
        super(application);
        repository = new SettingsRepository(application);
        allSettings = repository.getSettings();
    }

    public LiveData<List<Settings>> getAllSettings() {
        return allSettings;
    }

    public void insertSettings(Settings settings) {
        repository.insert(settings);
    }

    public void updateSettings(Settings settings) {
        repository.update(settings);
    }

    public void deleteSettings(Settings settings) {
        repository.delete(settings);
    }
} 
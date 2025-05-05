package com.hillal.hhhhhhh.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.hillal.hhhhhhh.data.model.Settings;
import com.hillal.hhhhhhh.data.room.AppDatabase;
import com.hillal.hhhhhhh.data.room.SettingsDao;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsRepository {
    private final SettingsDao settingsDao;
    private final ExecutorService executorService;

    public SettingsRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        settingsDao = database.settingsDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<Settings>> getAllSettings() {
        return settingsDao.getAllSettings();
    }

    public void insertSettings(Settings settings) {
        executorService.execute(() -> settingsDao.insert(settings));
    }

    public void updateSettings(Settings settings) {
        executorService.execute(() -> settingsDao.update(settings));
    }

    public void deleteSettings(Settings settings) {
        executorService.execute(() -> settingsDao.delete(settings));
    }
} 
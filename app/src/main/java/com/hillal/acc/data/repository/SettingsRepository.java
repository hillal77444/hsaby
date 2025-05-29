package com.hillal.acc.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.hillal.acc.App;
import com.hillal.acc.data.model.Settings;
import com.hillal.acc.data.room.SettingsDao;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;

public class SettingsRepository {
    private final SettingsDao settingsDao;
    private final LiveData<List<Settings>> settings;
    private final ExecutorService executorService;

    public SettingsRepository(Application application) {
        App app = (App) application;
        settingsDao = app.getDatabase().settingsDao();
        settings = settingsDao.getAllSettings();
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<Settings>> getSettings() {
        return settings;
    }

    public void insert(Settings settings) {
        executorService.execute(() -> settingsDao.insert(settings));
    }

    public void update(Settings settings) {
        executorService.execute(() -> settingsDao.update(settings));
    }

    public void delete(Settings settings) {
        executorService.execute(() -> settingsDao.delete(settings));
    }
} 
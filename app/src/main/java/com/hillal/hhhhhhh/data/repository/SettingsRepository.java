package com.hillal.hhhhhhh.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.hillal.hhhhhhh.App;
import com.hillal.hhhhhhh.data.model.Settings;
import com.hillal.hhhhhhh.data.room.SettingsDao;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsRepository {
    private final SettingsDao settingsDao;
    private final LiveData<Settings> settings;
    private final ExecutorService executorService;

    public SettingsRepository(Application application) {
        App app = (App) application;
        settingsDao = app.getDatabase().settingsDao();
        settings = settingsDao.getSettings();
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<Settings> getSettings() {
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
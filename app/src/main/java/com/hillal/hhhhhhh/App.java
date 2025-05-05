package com.hillal.hhhhhhh;

import android.app.Application;
import android.util.Log;
import com.hillal.hhhhhhh.data.AppDatabase;

public class App extends Application {
    private static final String TAG = "App";
    private static App instance;
    private AppDatabase database;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            instance = this;
            database = AppDatabase.getInstance(this);
            Log.d(TAG, "Application initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing application", e);
            throw new RuntimeException("Failed to initialize application", e);
        }
    }

    public static App getInstance() {
        return instance;
    }

    public AppDatabase getDatabase() {
        return database;
    }
} 
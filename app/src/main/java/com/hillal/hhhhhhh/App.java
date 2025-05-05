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
            Log.d(TAG, "Starting application initialization");
            
            // تهيئة قاعدة البيانات
            database = AppDatabase.getInstance(this);
            Log.d(TAG, "Database initialized successfully");
            
            // تهيئة أي إعدادات أخرى
            initializeAppSettings();
            
            Log.d(TAG, "Application initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing application: " + e.getMessage(), e);
            throw new RuntimeException("Failed to initialize application: " + e.getMessage(), e);
        }
    }

    private void initializeAppSettings() {
        try {
            // يمكنك إضافة أي إعدادات إضافية هنا
            Log.d(TAG, "App settings initialized");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing app settings: " + e.getMessage(), e);
        }
    }

    public static App getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Application instance is null. Make sure to initialize the Application class.");
        }
        return instance;
    }

    public AppDatabase getDatabase() {
        if (database == null) {
            throw new IllegalStateException("Database is null. Make sure to initialize the Application class.");
        }
        return database;
    }
} 
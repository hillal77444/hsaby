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
            Log.d(TAG, "Application onCreate started");
            instance = this;
            
            // تهيئة قاعدة البيانات
            Log.d(TAG, "Initializing database...");
            database = AppDatabase.getInstance(this);
            if (database == null) {
                throw new RuntimeException("Database initialization failed - database is null");
            }
            Log.d(TAG, "Database initialized successfully");
            
            // تهيئة أي إعدادات أخرى
            Log.d(TAG, "Initializing app settings...");
            initializeAppSettings();
            
            Log.d(TAG, "Application initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Critical error during application initialization: " + e.getMessage(), e);
            // إعادة رمي الخطأ لضمان إغلاق التطبيق
            throw new RuntimeException("Failed to initialize application: " + e.getMessage(), e);
        }
    }

    private void initializeAppSettings() {
        try {
            // يمكنك إضافة أي إعدادات إضافية هنا
            Log.d(TAG, "App settings initialized");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing app settings: " + e.getMessage(), e);
            throw new RuntimeException("Failed to initialize app settings: " + e.getMessage(), e);
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
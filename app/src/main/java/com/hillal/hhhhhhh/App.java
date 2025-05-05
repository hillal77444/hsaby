package com.hillal.hhhhhhh;

import android.app.Application;
import android.util.Log;

import com.hillal.hhhhhhh.data.room.AppDatabase;
import com.hillal.hhhhhhh.data.repository.AccountRepository;

public class App extends Application {
    private static final String TAG = "App";
    private static App instance;
    private AppDatabase database;
    private AccountRepository accountRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Application onCreate started");
        
        try {
            instance = this;
            
            // Initialize database
            database = AppDatabase.getInstance(this);
            Log.d(TAG, "Database initialized successfully");
            
            // Initialize repository
            accountRepository = new AccountRepository(database.accountDao());
            Log.d(TAG, "AccountRepository initialized successfully");
            
            Log.d(TAG, "Application initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing application: " + e.getMessage(), e);
            throw new RuntimeException("Failed to initialize application", e);
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

    public AccountRepository getAccountRepository() {
        if (accountRepository == null) {
            throw new IllegalStateException("AccountRepository is null. Make sure to initialize the Application class.");
        }
        return accountRepository;
    }
} 
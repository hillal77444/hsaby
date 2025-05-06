package com.hillal.hhhhhhh;

import android.app.Application;
import android.util.Log;

import com.hillal.hhhhhhh.data.room.AppDatabase;
import com.hillal.hhhhhhh.data.repository.AccountRepository;
import com.hillal.hhhhhhh.data.repository.TransactionRepository;
import com.hillal.hhhhhhh.data.repository.SettingsRepository;

public class App extends Application {
    private static final String TAG = "App";
    private static App instance;
    private AppDatabase database;
    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;
    private SettingsRepository settingsRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Application onCreate started");
        
        try {
            instance = this;
            
            // Initialize database
            database = AppDatabase.getInstance(this);
            Log.d(TAG, "Database initialized successfully");
            
            // Initialize repositories
            accountRepository = new AccountRepository(database.accountDao(), database);
            transactionRepository = new TransactionRepository(this);
            settingsRepository = new SettingsRepository(this);
            Log.d(TAG, "Repositories initialized successfully");
            
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

    public TransactionRepository getTransactionRepository() {
        if (transactionRepository == null) {
            throw new IllegalStateException("TransactionRepository is null. Make sure to initialize the Application class.");
        }
        return transactionRepository;
    }

    public SettingsRepository getSettingsRepository() {
        if (settingsRepository == null) {
            throw new IllegalStateException("SettingsRepository is null. Make sure to initialize the Application class.");
        }
        return settingsRepository;
    }
} 
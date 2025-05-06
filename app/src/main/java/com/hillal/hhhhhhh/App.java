package com.hillal.hhhhhhh;

import android.app.Application;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.widget.Toast;

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
        
        // Set up uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            String errorMessage = "خطأ في التطبيق:\n" + throwable.getMessage() + 
                                "\n\nStack Trace:\n" + Log.getStackTraceString(throwable);
            
            // Log the error
            Log.e(TAG, errorMessage);
            
            // Copy error to clipboard
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Error Log", errorMessage);
            clipboard.setPrimaryClip(clip);
            
            // Show error dialog
            new AlertDialog.Builder(getApplicationContext())
                .setTitle("خطأ في التطبيق")
                .setMessage("حدث خطأ في التطبيق. تم نسخ تفاصيل الخطأ تلقائياً.\n\n" + 
                           "يرجى إرسال تفاصيل الخطأ للمطور.")
                .setPositiveButton("موافق", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        android.os.Process.killProcess(android.os.Process.myPid());
                        System.exit(1);
                    }
                })
                .setCancelable(false)
                .show();
        });

        Log.d(TAG, "Application onCreate started");
        
        try {
            instance = this;
            Log.d(TAG, "Instance set");
            
            // Initialize database
            Log.d(TAG, "Initializing database...");
            try {
                database = AppDatabase.getInstance(this);
                Log.d(TAG, "Database initialized successfully");
            } catch (Exception dbException) {
                Log.e(TAG, "Database initialization failed", dbException);
                throw dbException;
            }
            
            // Initialize repositories
            Log.d(TAG, "Initializing repositories...");
            try {
                accountRepository = new AccountRepository(database.accountDao(), database);
                Log.d(TAG, "AccountRepository initialized");
            } catch (Exception accountException) {
                Log.e(TAG, "AccountRepository initialization failed", accountException);
                throw accountException;
            }
            
            try {
                transactionRepository = new TransactionRepository(this);
                Log.d(TAG, "TransactionRepository initialized");
            } catch (Exception transactionException) {
                Log.e(TAG, "TransactionRepository initialization failed", transactionException);
                throw transactionException;
            }
            
            try {
                settingsRepository = new SettingsRepository(this);
                Log.d(TAG, "SettingsRepository initialized");
            } catch (Exception settingsException) {
                Log.e(TAG, "SettingsRepository initialization failed", settingsException);
                throw settingsException;
            }
            
            Log.d(TAG, "All repositories initialized successfully");
            Log.d(TAG, "Application initialized successfully");
        } catch (Exception e) {
            String errorMessage = "خطأ في تهيئة التطبيق:\n" + e.getMessage() + 
                                "\n\nStack Trace:\n" + Log.getStackTraceString(e);
            Log.e(TAG, errorMessage);
            
            // Copy error to clipboard
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Error Log", errorMessage);
            clipboard.setPrimaryClip(clip);
            
            // Show error dialog
            new AlertDialog.Builder(this)
                .setTitle("خطأ في التهيئة")
                .setMessage("حدث خطأ أثناء تهيئة التطبيق. تم نسخ تفاصيل الخطأ تلقائياً.\n\n" + 
                           "يرجى إرسال تفاصيل الخطأ للمطور.")
                .setPositiveButton("موافق", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        android.os.Process.killProcess(android.os.Process.myPid());
                        System.exit(1);
                    }
                })
                .setCancelable(false)
                .show();
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
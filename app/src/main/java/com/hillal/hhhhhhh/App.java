package com.hillal.hhhhhhh;

import android.app.Application;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.hillal.hhhhhhh.data.room.AppDatabase;
import com.hillal.hhhhhhh.data.repository.AccountRepository;
import com.hillal.hhhhhhh.data.repository.TransactionRepository;
import com.hillal.hhhhhhh.data.repository.SettingsRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
        instance = this;
        
        // Set up uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            String errorMessage = "حدث خطأ غير متوقع:\n\n" +
                                "نوع الخطأ: " + throwable.getClass().getSimpleName() + "\n" +
                                "الرسالة: " + throwable.getMessage() + "\n\n" +
                                "تفاصيل الخطأ:\n" + Log.getStackTraceString(throwable) + "\n\n" +
                                "معلومات النظام:\n" +
                                "نظام التشغيل: Android " + Build.VERSION.RELEASE + "\n" +
                                "الجهاز: " + Build.MANUFACTURER + " " + Build.MODEL + "\n" +
                                "وقت حدوث الخطأ: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            
            Log.e(TAG, errorMessage);
            
            // Copy error to clipboard
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Error Details", errorMessage);
            clipboard.setPrimaryClip(clip);
            
            // Show error dialog
            new Handler(Looper.getMainLooper()).post(() -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.Theme_Hhhhhhh));
                builder.setTitle("خطأ في التطبيق")
                       .setMessage("حدث خطأ غير متوقع. تم نسخ تفاصيل الخطأ إلى الحافظة.\n\n" +
                                 "يرجى مشاركة هذه المعلومات مع المطور.")
                       .setPositiveButton("موافق", (dialog, which) -> {
                           dialog.dismiss();
                           System.exit(1);
                       })
                       .setCancelable(false)
                       .show();
            });
        });

        try {
            Log.d(TAG, "Initializing application...");
            
            // Initialize database
            Log.d(TAG, "Initializing database...");
            database = AppDatabase.getInstance(this);
            Log.d(TAG, "Database initialized successfully");
            
            // Initialize repositories
            Log.d(TAG, "Initializing repositories...");
            accountRepository = new AccountRepository(database.accountDao());
            transactionRepository = new TransactionRepository(database.transactionDao());
            settingsRepository = new SettingsRepository(database.settingsDao());
            Log.d(TAG, "Repositories initialized successfully");
            
            Log.d(TAG, "Application initialized successfully");
        } catch (Exception e) {
            String errorMessage = "خطأ في تهيئة التطبيق:\n\n" +
                                "نوع الخطأ: " + e.getClass().getSimpleName() + "\n" +
                                "الرسالة: " + e.getMessage() + "\n\n" +
                                "تفاصيل الخطأ:\n" + Log.getStackTraceString(e) + "\n\n" +
                                "معلومات النظام:\n" +
                                "نظام التشغيل: Android " + Build.VERSION.RELEASE + "\n" +
                                "الجهاز: " + Build.MANUFACTURER + " " + Build.MODEL + "\n" +
                                "وقت حدوث الخطأ: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            
            Log.e(TAG, errorMessage);
            
            // Copy error to clipboard
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Error Details", errorMessage);
            clipboard.setPrimaryClip(clip);
            
            // Show error dialog
            new Handler(Looper.getMainLooper()).post(() -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.Theme_Hhhhhhh));
                builder.setTitle("خطأ في التطبيق")
                       .setMessage("حدث خطأ أثناء تهيئة التطبيق. تم نسخ تفاصيل الخطأ إلى الحافظة.\n\n" +
                                 "يرجى مشاركة هذه المعلومات مع المطور.")
                       .setPositiveButton("موافق", (dialog, which) -> {
                           dialog.dismiss();
                           System.exit(1);
                       })
                       .setCancelable(false)
                       .show();
            });
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
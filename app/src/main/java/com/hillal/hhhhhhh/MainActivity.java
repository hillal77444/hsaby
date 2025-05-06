package com.hillal.hhhhhhh;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.navigation.fragment.NavHostFragment;

import com.hillal.hhhhhhh.data.repository.AccountRepository;
import com.hillal.hhhhhhh.databinding.ActivityMainBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private NavController navController;
    private AccountRepository accountRepository;
    private AppBarConfiguration appBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate started");

        try {
            // Initialize view binding
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            Log.d(TAG, "Layout inflated successfully");

            // Setup toolbar
            setSupportActionBar(binding.toolbar);
            Log.d(TAG, "Toolbar set successfully");

            // Initialize App instance first
            App app = (App) getApplication();
            if (app == null) {
                throw new IllegalStateException("Application instance is null");
            }
            Log.d(TAG, "Application instance initialized");

            // Initialize repository
            accountRepository = app.getAccountRepository();
            if (accountRepository == null) {
                throw new IllegalStateException("AccountRepository is null");
            }
            Log.d(TAG, "AccountRepository initialized successfully");

            // Setup navigation
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment_content_main);
            if (navHostFragment == null) {
                throw new IllegalStateException("NavHostFragment not found");
            }
            
            navController = navHostFragment.getNavController();
            if (navController == null) {
                throw new IllegalStateException("NavController is null");
            }

            appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.navigation_dashboard, R.id.navigation_accounts,
                    R.id.navigation_reports, R.id.navigation_settings)
                    .build();

            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
            NavigationUI.setupWithNavController(binding.navView, navController);
            Log.d(TAG, "Navigation setup completed successfully");

        } catch (IllegalStateException e) {
            String errorMessage = "=== خطأ في تهيئة التطبيق ===\n\n" +
                                "نوع الخطأ: " + e.getClass().getSimpleName() + "\n" +
                                "الرسالة: " + e.getMessage() + "\n\n" +
                                "=== تفاصيل الخطأ التقنية ===\n" +
                                Log.getStackTraceString(e) + "\n\n" +
                                "=== معلومات النظام ===\n" +
                                "نظام التشغيل: Android " + Build.VERSION.RELEASE + "\n" +
                                "الجهاز: " + Build.MANUFACTURER + " " + Build.MODEL + "\n" +
                                "وقت حدوث الخطأ: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            Log.e(TAG, errorMessage, e);
            showErrorAndExit(errorMessage);
        } catch (Exception e) {
            String errorMessage = "=== خطأ غير متوقع ===\n\n" +
                                "نوع الخطأ: " + e.getClass().getSimpleName() + "\n" +
                                "الرسالة: " + e.getMessage() + "\n\n" +
                                "=== تفاصيل الخطأ التقنية ===\n" +
                                Log.getStackTraceString(e) + "\n\n" +
                                "=== معلومات النظام ===\n" +
                                "نظام التشغيل: Android " + Build.VERSION.RELEASE + "\n" +
                                "الجهاز: " + Build.MANUFACTURER + " " + Build.MODEL + "\n" +
                                "وقت حدوث الخطأ: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            Log.e(TAG, errorMessage, e);
            showErrorAndExit(errorMessage);
        }
    }

    private void showErrorAndExit(String errorMessage) {
        // Copy error to clipboard
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("تفاصيل الخطأ", errorMessage);
        clipboard.setPrimaryClip(clip);
        
        // Show error dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.Theme_Hhhhhhh));
        builder.setTitle("خطأ في التطبيق")
               .setMessage("حدث خطأ غير متوقع. تم نسخ تفاصيل الخطأ إلى الحافظة.\n\n" +
                         "يمكنك لصق التفاصيل في أي مكان لمشاركتها مع المطور.")
               .setPositiveButton("نسخ التفاصيل", (dialog, which) -> {
                   // نسخ التفاصيل مرة أخرى للتأكد
                   clipboard.setPrimaryClip(clip);
                   Toast.makeText(this, "تم نسخ تفاصيل الخطأ إلى الحافظة", Toast.LENGTH_LONG).show();
                   dialog.dismiss();
                   finish();
                   System.exit(1);
               })
               .setCancelable(false)
               .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            try {
                Navigation.findNavController(this, R.id.nav_host_fragment_content_main)
                        .navigate(R.id.navigation_settings);
                return true;
            } catch (Exception e) {
                String errorMessage = "=== خطأ في التنقل ===\n\n" +
                                    "نوع الخطأ: " + e.getClass().getSimpleName() + "\n" +
                                    "الرسالة: " + e.getMessage() + "\n\n" +
                                    "=== تفاصيل الخطأ التقنية ===\n" +
                                    Log.getStackTraceString(e);
                Log.e(TAG, errorMessage, e);
                Toast.makeText(this, "حدث خطأ أثناء التنقل إلى الإعدادات", Toast.LENGTH_LONG).show();
                return false;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        try {
            return NavigationUI.navigateUp(navController, appBarConfiguration)
                    || super.onSupportNavigateUp();
        } catch (Exception e) {
            String errorMessage = "=== خطأ في التنقل ===\n\n" +
                                "نوع الخطأ: " + e.getClass().getSimpleName() + "\n" +
                                "الرسالة: " + e.getMessage() + "\n\n" +
                                "=== تفاصيل الخطأ التقنية ===\n" +
                                Log.getStackTraceString(e);
            Log.e(TAG, errorMessage, e);
            Toast.makeText(this, "حدث خطأ أثناء التنقل", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    public AccountRepository getAccountRepository() {
        return accountRepository;
    }
}
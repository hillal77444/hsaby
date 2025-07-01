package com.hillal.acc;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.navigation.fragment.NavHostFragment;

import com.hillal.acc.data.repository.AccountRepository;
import com.hillal.acc.data.room.AccountDao;
import com.hillal.acc.data.room.TransactionDao;
import com.hillal.acc.data.room.PendingOperationDao;
import com.hillal.acc.databinding.ActivityMainBinding;
import com.hillal.acc.viewmodel.AuthViewModel;
import com.hillal.acc.data.room.AppDatabase;
import com.hillal.acc.data.update.AppUpdateHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.fragment.app.Fragment;
import com.hillal.acc.ui.dashboard.DashboardFragment;
import com.hillal.acc.ui.settings.SettingsFragment;
import com.hillal.acc.ui.transactions.TransactionsFragment;
import com.hillal.acc.ui.accounts.AddAccountFragment;
import com.hillal.acc.ui.reports.AccountStatementFragment;
import androidx.core.view.WindowCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private NavController navController;
    private AccountRepository accountRepository;
    private AppBarConfiguration appBarConfiguration;
    private AuthViewModel authViewModel;
    private App app;
    private AppDatabase db;
    private AppUpdateHelper appUpdateHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate started");

        try {
            // Initialize view binding
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
            Log.d(TAG, "Layout inflated successfully");

            // Setup toolbar
            setSupportActionBar(binding.appBarMain.toolbar);
            Log.d(TAG, "Toolbar set successfully");

            // Initialize App instance first
            app = (App) getApplication();
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

            // Initialize AuthViewModel
            authViewModel = new AuthViewModel(getApplication());
            
            // Check if user is logged in
            if (authViewModel.isLoggedIn()) {
                // User is logged in, navigate to dashboard
                navController.navigate(R.id.navigation_dashboard);
                binding.bottomNavigation.setVisibility(View.VISIBLE);
            } else {
                // User is not logged in, navigate to login
                navController.navigate(R.id.loginFragment);
                binding.bottomNavigation.setVisibility(View.GONE);
            }

            // Setup bottom navigation
            binding.bottomNavigation.setOnNavigationItemSelectedListener(navListener);

            db = AppDatabase.getInstance(getApplicationContext());
            setupUI();

            // تهيئة مدير التحديثات
            appUpdateHelper = new AppUpdateHelper(
                this,
                db.accountDao(),
                db.transactionDao(),
                db.pendingOperationDao()
            );

            // بعد إعداد BottomNavigationView
            BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
            ViewCompat.setOnApplyWindowInsetsListener(bottomNavigationView, (v, insets) -> {
                int bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
                v.setPadding(0, 0, 0, bottom);
                return insets;
            });

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
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // التحقق من وجود تحديثات عند استئناف النشاط
        appUpdateHelper.checkForUpdates(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // معالجة نتيجة التحديث
        appUpdateHelper.onActivityResult(requestCode, resultCode);
    }

    private void setupUI() {
        // إعدادات أخرى
        setupOtherSettings();
    }

    private void setupOtherSettings() {
        // هنا يمكن إضافة إعدادات أخرى
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private void showErrorAndExit(String errorMessage) {
        new AlertDialog.Builder(this)
                .setTitle("خطأ في التطبيق")
                .setMessage(errorMessage)
                .setPositiveButton("خروج", (dialog, which) -> finish())
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

    public AccountRepository getAccountRepository() {
        return accountRepository;
    }

    @Override
    public void onBackPressed() {
        // إذا كنا في لوحة التحكم، نعرض مربع حوار للتأكيد قبل الخروج
        if (navController.getCurrentDestination().getId() == R.id.navigation_dashboard) {
            new AlertDialog.Builder(this)
                .setTitle("تأكيد الخروج")
                .setMessage("هل تريد الخروج من التطبيق؟")
                .setPositiveButton("نعم", (dialog, which) -> finish())
                .setNegativeButton("لا", null)
                .show();
        } else {
            // في أي صفحة أخرى، نترك سلوك الرجوع الافتراضي
            super.onBackPressed();
        }
    }

    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
        new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                
                if (itemId == R.id.nav_dashboard) {
                    navController.navigate(R.id.navigation_dashboard);
                    return true;
                } else if (itemId == R.id.nav_add_account) {
                    navController.navigate(R.id.navigation_accounts);
                    return true;
                } else if (itemId == R.id.nav_transactions) {
                    navController.navigate(R.id.transactionsFragment);
                    return true;
                } else if (itemId == R.id.nav_reports) {
                    navController.navigate(R.id.navigation_reports);
                    return true;
                }
                return false;
            }
        };

    // دالة للتحكم في ظهور/إخفاء شريط التنقل السفلي
    public void setBottomNavigationVisibility(boolean isVisible) {
        binding.bottomNavigation.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    public AccountDao getAccountDao() {
        return ((App) getApplication()).getDatabase().accountDao();
    }

    public TransactionDao getTransactionDao() {
        return ((App) getApplication()).getDatabase().transactionDao();
    }

    public PendingOperationDao getPendingOperationDao() {
        return ((App) getApplication()).getDatabase().pendingOperationDao();
    }
}
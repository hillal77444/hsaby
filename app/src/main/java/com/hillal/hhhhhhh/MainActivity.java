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
import android.view.View;
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
import com.hillal.hhhhhhh.data.sync.SyncManager;
import com.hillal.hhhhhhh.data.room.AccountDao;
import com.hillal.hhhhhhh.data.room.TransactionDao;
import com.hillal.hhhhhhh.databinding.ActivityMainBinding;
import com.hillal.hhhhhhh.viewmodel.AuthViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.fragment.app.Fragment;
import com.hillal.hhhhhhh.ui.dashboard.DashboardFragment;
import com.hillal.hhhhhhh.ui.settings.SettingsFragment;
import com.hillal.hhhhhhh.ui.transactions.TransactionsFragment;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private NavController navController;
    private AccountRepository accountRepository;
    private AppBarConfiguration appBarConfiguration;
    private AuthViewModel authViewModel;
    private SyncManager syncManager;
    private App app;

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

            // Initialize SyncManager
            AccountDao accountDao = app.getDatabase().accountDao();
            TransactionDao transactionDao = app.getDatabase().transactionDao();
            syncManager = new SyncManager(this, accountDao, transactionDao);
            Log.d(TAG, "SyncManager initialized successfully");

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
            } else {
                // User is not logged in, navigate to login
                navController.navigate(R.id.loginFragment);
            }

            // Setup navigation drawer
            appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.navigation_dashboard,
                    R.id.nav_accounts,
                    R.id.nav_transactions,
                    R.id.nav_reports,
                    R.id.nav_settings)
                    .setOpenableLayout(binding.drawerLayout)
                    .build();

            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
            NavigationUI.setupWithNavController(binding.navView, navController);
            Log.d(TAG, "Navigation setup completed successfully");

            BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
            bottomNav.setOnNavigationItemSelectedListener(navListener);

            // Set default fragment
            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new DashboardFragment())
                        .commit();
            }

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
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        // إعادة تشغيل المزامنة التلقائية عند عودة التطبيق للواجهة
        if (syncManager != null && syncManager.isAutoSyncEnabled()) {
            syncManager.startAutoSync();
        }
        if (syncManager != null) {
            syncManager.onDashboardEntered();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called");
        // لا نقوم بإيقاف المزامنة التلقائية هنا لأنها قد تكون قيد التنفيذ
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
                    Fragment selectedFragment = null;

                    int itemId = item.getItemId();
                    if (itemId == R.id.nav_dashboard) {
                        selectedFragment = new DashboardFragment();
                    } else if (itemId == R.id.nav_transactions) {
                        selectedFragment = new TransactionsFragment();
                    } else if (itemId == R.id.nav_settings) {
                        selectedFragment = new SettingsFragment();
                    }

                    if (selectedFragment != null) {
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, selectedFragment)
                                .commit();
                        return true;
                    }
                    return false;
                }
            };

    // دالة للتحكم في ظهور/إخفاء شريط التنقل السفلي
    public void setBottomNavigationVisibility(boolean isVisible) {
        binding.bottomNavigation.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }
}
package com.hillal.hhhhhhh;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.navigation.fragment.NavHostFragment;

import com.hillal.hhhhhhh.data.repository.AccountRepository;
import com.hillal.hhhhhhh.databinding.ActivityMainBinding;

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

            // Setup navigation
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment_content_main);
            if (navHostFragment != null) {
                navController = navHostFragment.getNavController();
                appBarConfiguration = new AppBarConfiguration.Builder(
                        R.id.navigation_dashboard, R.id.navigation_accounts,
                        R.id.navigation_reports, R.id.navigation_settings)
                        .build();
                NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
                NavigationUI.setupWithNavController(binding.navView, navController);
                Log.d(TAG, "Navigation setup completed successfully");
            } else {
                String errorMessage = "=== خطأ في تهيئة التطبيق ===\n\n" +
                                    "السبب: لم يتم العثور على NavHostFragment\n" +
                                    "التفاصيل: fragment ID: R.id.nav_host_fragment_content_main";
                Log.e(TAG, errorMessage);
                showErrorAndExit(errorMessage);
                return;
            }

            // Initialize repository
            accountRepository = App.getInstance().getAccountRepository();
            Log.d(TAG, "AccountRepository initialized successfully");
        } catch (Exception e) {
            String errorMessage = "=== خطأ في تهيئة التطبيق ===\n\n" +
                                "نوع الخطأ: " + e.getClass().getSimpleName() + "\n" +
                                "الرسالة: " + e.getMessage() + "\n\n" +
                                "=== تفاصيل الخطأ التقنية ===\n" +
                                Log.getStackTraceString(e);
            Log.e(TAG, errorMessage, e);
            showErrorAndExit(errorMessage);
        }
    }

    private void showErrorAndExit(String errorMessage) {
        // Show error toast
        Toast.makeText(this, "حدث خطأ في التطبيق. سيتم إغلاقه.", Toast.LENGTH_LONG).show();
        
        // Log error
        Log.e(TAG, errorMessage);
        
        // Exit after a delay to allow toast to be shown
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            finish();
            System.exit(1);
        }, 2000);
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
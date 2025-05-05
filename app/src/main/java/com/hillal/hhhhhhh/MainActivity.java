package com.hillal.hhhhhhh;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.hillal.hhhhhhh.databinding.ActivityMainBinding;

import android.view.Menu;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private NavController navController;
    private AppBarConfiguration appBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate started");
        try {
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            Log.d(TAG, "Layout inflated successfully");

            // إعداد NavController
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment_content_main);
            if (navHostFragment != null) {
                navController = navHostFragment.getNavController();
                Log.d(TAG, "NavController initialized");
                
                appBarConfiguration = new AppBarConfiguration.Builder(
                        R.id.navigation_dashboard,
                        R.id.navigation_reports
                ).build();
                NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
                Log.d(TAG, "ActionBar setup completed");
            } else {
                Log.e(TAG, "NavHostFragment is null - This is a critical error");
                finish();
                return;
            }

            // ربط BottomNavigationView مع NavController
            BottomNavigationView bottomNav = binding.bottomNavView;
            if (bottomNav != null) {
                NavigationUI.setupWithNavController(bottomNav, navController);
                Log.d(TAG, "BottomNavigationView setup completed");
            } else {
                Log.e(TAG, "BottomNavigationView is null");
                finish();
                return;
            }

            Log.d(TAG, "onCreate completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Critical error in onCreate", e);
            Toast.makeText(this, "1خطأ في تهيئة التطبيق", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        try {
            getMenuInflater().inflate(R.menu.menu_main, menu);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error creating options menu", e);
            return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            int id = item.getItemId();
            if (id == R.id.action_settings) {
                return true;
            }
            return super.onOptionsItemSelected(item);
        } catch (Exception e) {
            Log.e(TAG, "Error handling options item selection", e);
            return false;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        try {
            if (navController != null) {
                return navController.navigateUp() || super.onSupportNavigateUp();
            }
            return super.onSupportNavigateUp();
        } catch (Exception e) {
            Log.e(TAG, "Error in onSupportNavigateUp", e);
            return super.onSupportNavigateUp();
        }
    }
}
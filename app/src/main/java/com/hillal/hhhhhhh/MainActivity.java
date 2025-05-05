package com.hillal.hhhhhhh;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.hillal.hhhhhhh.data.repository.AccountRepository;
import com.hillal.hhhhhhh.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private AccountRepository accountRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "MainActivity onCreate started");

        try {
            // Initialize ViewBinding
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            // Initialize repository
            accountRepository = ((App) getApplication()).getAccountRepository();
            Log.d(TAG, "AccountRepository initialized successfully");

            // Setup toolbar
            Toolbar toolbar = binding.toolbar;
            setSupportActionBar(toolbar);

            // Setup navigation
            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
            appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.navigation_dashboard,
                    R.id.navigation_accounts,
                    R.id.navigation_reports,
                    R.id.navigation_settings)
                    .build();
            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
            NavigationUI.setupWithNavController(binding.navView, navController);

            Log.d(TAG, "MainActivity initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing MainActivity: " + e.getMessage(), e);
            throw new RuntimeException("Failed to initialize MainActivity", e);
        }
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
            Navigation.findNavController(this, R.id.nav_host_fragment_content_main)
                    .navigate(R.id.navigation_settings);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    public AccountRepository getAccountRepository() {
        return accountRepository;
    }
}
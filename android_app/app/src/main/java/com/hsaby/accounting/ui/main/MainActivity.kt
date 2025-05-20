package com.hsaby.accounting.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hsaby.accounting.R
import com.hsaby.accounting.databinding.ActivityMainBinding
import com.hsaby.accounting.ui.login.LoginActivity
import com.hsaby.accounting.util.PreferencesManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        preferencesManager = PreferencesManager(this)
        
        setupNavigation()
        setupListeners()
    }
    
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_dashboard,
                R.id.navigation_accounts,
                R.id.navigation_transactions,
                R.id.navigation_reports,
                R.id.navigation_settings
            )
        )
        
        setSupportActionBar(binding.toolbar)
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNav.setupWithNavController(navController)
    }
    
    private fun setupListeners() {
        binding.fabAdd.setOnClickListener {
            when (navController.currentDestination?.id) {
                R.id.navigation_accounts -> {
                    // TODO: Show add account dialog
                }
                R.id.navigation_transactions -> {
                    // TODO: Show add transaction dialog
                }
            }
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sync -> {
                syncData()
                true
            }
            R.id.action_logout -> {
                showLogoutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
    
    private fun syncData() {
        lifecycleScope.launch {
            try {
                val userId = preferencesManager.userId.first()
                if (userId != null) {
                    // Sync accounts
                    (application as com.hsaby.accounting.AccountingApp)
                        .accountRepository.syncAccounts(userId)
                    
                    // Sync transactions
                    (application as com.hsaby.accounting.AccountingApp)
                        .transactionRepository.syncTransactions(userId)
                }
            } catch (e: Exception) {
                // Handle sync error
            }
        }
    }
    
    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("تسجيل الخروج")
            .setMessage("هل أنت متأكد من تسجيل الخروج؟")
            .setPositiveButton("نعم") { _, _ ->
                logout()
            }
            .setNegativeButton("لا", null)
            .show()
    }
    
    private fun logout() {
        lifecycleScope.launch {
            preferencesManager.clearPreferences()
            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
            finish()
        }
    }
} 
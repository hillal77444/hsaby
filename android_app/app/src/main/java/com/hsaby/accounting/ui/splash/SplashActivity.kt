package com.hsaby.accounting.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hsaby.accounting.R
import com.hsaby.accounting.ui.login.LoginActivity
import com.hsaby.accounting.ui.main.MainActivity
import com.hsaby.accounting.util.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {
    
    private lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        preferencesManager = PreferencesManager(this)
        
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserSession()
        }, 2000) // 2 seconds delay
    }
    
    private fun checkUserSession() {
        lifecycleScope.launch {
            val token = preferencesManager.token.first()
            val userId = preferencesManager.userId.first()
            
            if (token != null && userId != null) {
                // User is logged in, go to MainActivity
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            } else {
                // User is not logged in, go to LoginActivity
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
            }
            finish()
        }
    }
} 
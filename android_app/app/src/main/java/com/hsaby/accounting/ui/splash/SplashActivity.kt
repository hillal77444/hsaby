package com.hsaby.accounting.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.hsaby.accounting.R
import com.hsaby.accounting.ui.login.LoginActivity
import com.hsaby.accounting.ui.main.MainActivity
import com.hsaby.accounting.util.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {
    
    @Inject
    lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserSession()
        }, 2000) // 2 seconds delay
    }
    
    private fun checkUserSession() {
        val token = preferencesManager.getToken()
        val userId = preferencesManager.getUserId()
        
        if (token != null && userId != null) {
            // User is logged in, go to MainActivity
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            // User is not logged in, go to LoginActivity
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()
    }
} 
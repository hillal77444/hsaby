package com.hsaby.accounting.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hsaby.accounting.AccountingApp
import com.hsaby.accounting.databinding.ActivityLoginBinding
import com.hsaby.accounting.ui.main.MainActivity
import com.hsaby.accounting.ui.register.RegisterActivity
import com.hsaby.accounting.util.PreferencesManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        preferencesManager = PreferencesManager(this)
        
        setupViews()
        setupListeners()
    }
    
    private fun setupViews() {
        binding.toolbar.title = getString(R.string.login_title)
    }
    
    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val phone = binding.etPhone.text.toString()
            val password = binding.etPassword.text.toString()
            val rememberMe = binding.cbRememberMe.isChecked
            
            if (validateInput(phone, password)) {
                login(phone, password, rememberMe)
            }
        }
        
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
    
    private fun validateInput(phone: String, password: String): Boolean {
        if (phone.isEmpty()) {
            binding.etPhone.error = "يرجى إدخال رقم الهاتف"
            return false
        }
        
        if (password.isEmpty()) {
            binding.etPassword.error = "يرجى إدخال كلمة المرور"
            return false
        }
        
        return true
    }
    
    private fun login(phone: String, password: String, rememberMe: Boolean) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false
        
        lifecycleScope.launch {
            try {
                val result = (application as AccountingApp).userRepository.login(phone, password)
                result.fold(
                    onSuccess = { response ->
                        // Save user data
                        preferencesManager.saveToken(response.token)
                        preferencesManager.saveUserId(response.userId)
                        preferencesManager.saveUsername(response.username)
                        preferencesManager.saveRememberMe(rememberMe)
                        
                        // Sync data
                        syncData(response.userId)
                        
                        // Go to MainActivity
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    },
                    onFailure = { error ->
                        Toast.makeText(
                            this@LoginActivity,
                            error.message ?: "حدث خطأ أثناء تسجيل الدخول",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(
                    this@LoginActivity,
                    "حدث خطأ غير متوقع",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true
            }
        }
    }
    
    private fun syncData(userId: String) {
        lifecycleScope.launch {
            try {
                // Sync accounts
                (application as AccountingApp).accountRepository.syncAccounts(userId)
                
                // Sync transactions
                (application as AccountingApp).transactionRepository.syncTransactions(userId)
            } catch (e: Exception) {
                // Handle sync error
            }
        }
    }
} 
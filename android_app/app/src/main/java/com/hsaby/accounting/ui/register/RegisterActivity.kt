package com.hsaby.accounting.ui.register

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hsaby.accounting.AccountingApp
import com.hsaby.accounting.R
import com.hsaby.accounting.databinding.ActivityRegisterBinding
import com.hsaby.accounting.ui.login.LoginActivity
import com.hsaby.accounting.util.PreferencesManager
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        preferencesManager = PreferencesManager(this)
        
        setupViews()
        setupListeners()
    }
    
    private fun setupViews() {
        binding.toolbar.title = getString(R.string.register_title)
    }
    
    private fun setupListeners() {
        binding.btnRegister.setOnClickListener {
            val username = binding.etUsername.text.toString()
            val phone = binding.etPhone.text.toString()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()
            
            if (validateInput(username, phone, password, confirmPassword)) {
                register(username, phone, password)
            }
        }
    }
    
    private fun validateInput(username: String, phone: String, password: String, confirmPassword: String): Boolean {
        if (username.isEmpty()) {
            binding.etUsername.error = "يرجى إدخال اسم المستخدم"
            return false
        }
        
        if (phone.isEmpty()) {
            binding.etPhone.error = "يرجى إدخال رقم الهاتف"
            return false
        }
        
        if (password.isEmpty()) {
            binding.etPassword.error = "يرجى إدخال كلمة المرور"
            return false
        }
        
        if (confirmPassword.isEmpty()) {
            binding.etConfirmPassword.error = "يرجى تأكيد كلمة المرور"
            return false
        }
        
        if (password != confirmPassword) {
            binding.etConfirmPassword.error = "كلمة المرور غير متطابقة"
            return false
        }
        
        return true
    }
    
    private fun register(username: String, phone: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnRegister.isEnabled = false
        
        lifecycleScope.launch {
            try {
                val result = (application as AccountingApp).userRepository.register(username, phone, password)
                result.fold(
                    onSuccess = { response ->
                        // Save user data
                        preferencesManager.saveToken(response.token)
                        preferencesManager.saveUserId(response.userId)
                        preferencesManager.saveUsername(response.username)
                        
                        // Go to LoginActivity
                        Toast.makeText(
                            this@RegisterActivity,
                            "تم إنشاء الحساب بنجاح",
                            Toast.LENGTH_LONG
                        ).show()
                        
                        startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                        finish()
                    },
                    onFailure = { error ->
                        Toast.makeText(
                            this@RegisterActivity,
                            error.message ?: "حدث خطأ أثناء إنشاء الحساب",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(
                    this@RegisterActivity,
                    "حدث خطأ غير متوقع",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnRegister.isEnabled = true
            }
        }
    }
} 
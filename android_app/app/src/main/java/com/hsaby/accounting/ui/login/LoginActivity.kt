package com.hsaby.accounting.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.hsaby.accounting.AccountingApp
import com.hsaby.accounting.data.local.PreferencesManager
import com.hsaby.accounting.databinding.ActivityLoginBinding
import com.hsaby.accounting.ui.main.MainActivity
import com.hsaby.accounting.ui.register.RegisterActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel
    private lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        preferencesManager = PreferencesManager(this)
        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]
        
        setupViews()
        observeViewModel()
    }
    
    private fun setupViews() {
        binding.toolbar.title = getString(R.string.login_title)
        
        binding.loginButton.setOnClickListener {
            val phone = binding.phoneEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            
            if (validateInput(phone, password)) {
                login(phone, password)
            }
        }
        
        binding.registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
    
    private fun observeViewModel() {
        viewModel.loginResult.observe(this) { result ->
            when (result) {
                is LoginResult.Success -> {
                    // حفظ بيانات تسجيل الدخول
                    preferencesManager.saveLoginCredentials(
                        phone = result.phone,
                        password = result.password,
                        userId = result.userId,
                        token = result.token
                    )
                    
                    // الانتقال إلى الشاشة الرئيسية
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                is LoginResult.Error -> {
                    Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun validateInput(phone: String, password: String): Boolean {
        var isValid = true
        
        if (phone.isEmpty()) {
            binding.phoneEditText.error = "الرجاء إدخال رقم الهاتف"
            isValid = false
        } else if (!isValidPhoneNumber(phone)) {
            binding.phoneEditText.error = "رقم الهاتف غير صحيح"
            isValid = false
        }
        
        if (password.isEmpty()) {
            binding.passwordEditText.error = "الرجاء إدخال كلمة المرور"
            isValid = false
        }
        
        return isValid
    }
    
    private fun isValidPhoneNumber(phone: String): Boolean {
        // التحقق من صحة رقم الهاتف (يمكن تعديل النمط حسب متطلباتك)
        val phonePattern = "^[0-9]{10}$"
        return phone.matches(phonePattern.toRegex())
    }
    
    private fun login(phone: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.loginButton.isEnabled = false
        
        lifecycleScope.launch {
            try {
                val result = (application as AccountingApp).userRepository.login(phone, password)
                result.fold(
                    onSuccess = { response ->
                        // حفظ بيانات المستخدم
                        preferencesManager.saveLoginCredentials(
                            phone = phone,
                            password = password,
                            userId = response.userId,
                            token = response.token
                        )
                        
                        // مزامنة البيانات
                        syncData(response.userId)
                        
                        // الانتقال إلى الشاشة الرئيسية
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
                binding.loginButton.isEnabled = true
            }
        }
    }
    
    private fun syncData(userId: String) {
        lifecycleScope.launch {
            try {
                // مزامنة الحسابات
                (application as AccountingApp).accountRepository.syncAccounts(userId)
                
                // مزامنة المعاملات
                (application as AccountingApp).transactionRepository.syncTransactions(userId)
            } catch (e: Exception) {
                // معالجة خطأ المزامنة
            }
        }
    }
} 
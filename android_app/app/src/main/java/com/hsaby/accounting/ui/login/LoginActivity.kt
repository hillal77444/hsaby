package com.hsaby.accounting.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hsaby.accounting.R
import com.hsaby.accounting.databinding.ActivityLoginBinding
import com.hsaby.accounting.ui.main.MainActivity
import com.hsaby.accounting.util.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    
    @Inject
    lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupViews()
        observeViewModel()
    }
    
    private fun setupViews() {
        binding.toolbar.title = getString(R.string.login_title)
        
        binding.btnLogin.setOnClickListener {
            if (validateInput()) {
                viewModel.login(
                    binding.etPhone.text.toString(),
                    binding.etPassword.text.toString()
                )
            }
        }
        
        binding.tvRegister.setOnClickListener {
            if (validateInput()) {
                viewModel.register(
                    binding.etName.text.toString(),
                    binding.etPhone.text.toString(),
                    binding.etPassword.text.toString()
                )
            }
        }
    }
    
    private fun observeViewModel() {
        viewModel.loginResult.observe(this) { result ->
            when (result) {
                is LoginResult.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnLogin.isEnabled = false
                    binding.tvRegister.isEnabled = false
                }
                is LoginResult.Success -> {
                    binding.progressBar.visibility = View.GONE
                    lifecycleScope.launch {
                        preferencesManager.saveToken(result.response.token)
                        preferencesManager.saveRefreshToken(result.response.refreshToken)
                        preferencesManager.saveUserId(result.response.userId)
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    }
                }
                is LoginResult.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                    binding.btnLogin.isEnabled = true
                    binding.tvRegister.isEnabled = true
                }
            }
        }
    }
    
    private fun validateInput(): Boolean {
        var isValid = true
        
        if (binding.etPhone.text.isNullOrBlank()) {
            binding.etPhone.error = getString(R.string.error_phone_required)
            isValid = false
        } else if (!isValidPhoneNumber(binding.etPhone.text.toString())) {
            binding.etPhone.error = getString(R.string.error_invalid_phone)
            isValid = false
        }
        
        if (binding.etPassword.text.isNullOrBlank()) {
            binding.etPassword.error = getString(R.string.error_password_required)
            isValid = false
        }
        
        if (binding.tvRegister.visibility == View.VISIBLE) {
            if (binding.etName.text.isNullOrBlank()) {
                binding.etName.error = getString(R.string.error_name_required)
                isValid = false
            }
        }
        
        return isValid
    }
    
    private fun isValidPhoneNumber(phone: String): Boolean {
        val phonePattern = "^[0-9]{10}$"
        return phone.matches(phonePattern.toRegex())
    }
} 
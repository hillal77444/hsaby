package com.hsaby.accounting.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.hsaby.accounting.R
import com.hsaby.accounting.databinding.ActivityLoginBinding
import com.hsaby.accounting.ui.main.MainActivity
import com.hsaby.accounting.util.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
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
        
        binding.loginButton.setOnClickListener {
            if (validateInput()) {
                viewModel.login(
                    binding.phoneEditText.text.toString(),
                    binding.passwordEditText.text.toString()
                )
            }
        }
        
        binding.registerButton.setOnClickListener {
            if (validateInput()) {
                viewModel.register(
                    binding.nameEditText.text.toString(),
                    binding.phoneEditText.text.toString(),
                    binding.passwordEditText.text.toString()
                )
            }
        }
    }
    
    private fun observeViewModel() {
        viewModel.loginResult.observe(this) { result ->
            when (result) {
                is LoginResult.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.loginButton.isEnabled = false
                    binding.registerButton.isEnabled = false
                }
                is LoginResult.Success -> {
                    binding.progressBar.visibility = View.GONE
                    preferencesManager.saveToken(result.response.token)
                    preferencesManager.saveRefreshToken(result.response.refreshToken)
                    preferencesManager.saveUserId(result.response.userId)
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                is LoginResult.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                    binding.loginButton.isEnabled = true
                    binding.registerButton.isEnabled = true
                }
            }
        }
    }
    
    private fun validateInput(): Boolean {
        var isValid = true
        
        if (binding.phoneEditText.text.isNullOrBlank()) {
            binding.phoneEditText.error = getString(R.string.error_phone_required)
            isValid = false
        } else if (!isValidPhoneNumber(binding.phoneEditText.text.toString())) {
            binding.phoneEditText.error = getString(R.string.error_invalid_phone)
            isValid = false
        }
        
        if (binding.passwordEditText.text.isNullOrBlank()) {
            binding.passwordEditText.error = getString(R.string.error_password_required)
            isValid = false
        }
        
        if (binding.registerButton.visibility == View.VISIBLE) {
            if (binding.nameEditText.text.isNullOrBlank()) {
                binding.nameEditText.error = getString(R.string.error_name_required)
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
package com.hsaby.accounting.ui.register

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hsaby.accounting.R
import com.hsaby.accounting.databinding.ActivityRegisterBinding
import com.hsaby.accounting.ui.login.LoginActivity
import com.hsaby.accounting.util.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: RegisterViewModel by viewModels()
    
    @Inject
    lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupViews()
        observeViewModel()
    }
    
    private fun setupViews() {
        binding.toolbar.title = getString(R.string.register_title)
        
        binding.btnRegister.setOnClickListener {
            if (validateInput()) {
                viewModel.register(
                    binding.etUsername.text.toString(),
                    binding.etPhone.text.toString(),
                    binding.etPassword.text.toString()
                )
            }
        }
        
        binding.tvLogin.setOnClickListener {
            finish()
        }
    }
    
    private fun observeViewModel() {
        viewModel.registerResult.observe(this) { result ->
            when (result) {
                is RegisterResult.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnRegister.isEnabled = false
                    binding.tvLogin.isEnabled = false
                }
                is RegisterResult.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this,
                        getString(R.string.register_success),
                        Toast.LENGTH_LONG
                    ).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                is RegisterResult.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                    binding.btnRegister.isEnabled = true
                    binding.tvLogin.isEnabled = true
                }
                else -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnRegister.isEnabled = true
                    binding.tvLogin.isEnabled = true
                }
            }
        }
    }
    
    private fun validateInput(): Boolean {
        var isValid = true
        
        if (binding.etUsername.text.isNullOrBlank()) {
            binding.etUsername.error = getString(R.string.error_name_required)
            isValid = false
        }
        
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
        } else if (binding.etPassword.text.toString().length < 6) {
            binding.etPassword.error = getString(R.string.error_password_too_short)
            isValid = false
        }
        
        if (binding.etConfirmPassword.text.toString() != binding.etPassword.text.toString()) {
            binding.etConfirmPassword.error = getString(R.string.error_passwords_dont_match)
            isValid = false
        }
        
        return isValid
    }
    
    private fun isValidPhoneNumber(phone: String): Boolean {
        val phonePattern = "^[0-9]{10}$"
        return phone.matches(phonePattern.toRegex())
    }
} 
package com.hsaby.accounting.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hsaby.accounting.data.model.AuthResult
import com.hsaby.accounting.data.model.RegisterRequest
import com.hsaby.accounting.data.model.RegisterResponse
import com.hsaby.accounting.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _registerResult = MutableStateFlow<AuthResult<RegisterResponse>?>(null)
    val registerResult: StateFlow<AuthResult<RegisterResponse>?> = _registerResult

    fun register(name: String, email: String, phone: String, password: String) {
        if (name.isBlank() || email.isBlank() || phone.isBlank() || password.isBlank()) {
            _registerResult.value = AuthResult.Error("يرجى ملء جميع الحقول")
            return
        }

        if (!isValidEmail(email)) {
            _registerResult.value = AuthResult.Error("البريد الإلكتروني غير صالح")
            return
        }

        if (!isValidPhone(phone)) {
            _registerResult.value = AuthResult.Error("رقم الهاتف غير صالح")
            return
        }

        if (password.length < 6) {
            _registerResult.value = AuthResult.Error("كلمة المرور يجب أن تكون 6 أحرف على الأقل")
            return
        }

        viewModelScope.launch {
            _registerResult.value = AuthResult.Loading
            _registerResult.value = authRepository.register(RegisterRequest(name, email, password, phone))
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return email.matches(emailPattern.toRegex())
    }

    private fun isValidPhone(phone: String): Boolean {
        val phonePattern = "^[0-9]{10}$"
        return phone.matches(phonePattern.toRegex())
    }
} 
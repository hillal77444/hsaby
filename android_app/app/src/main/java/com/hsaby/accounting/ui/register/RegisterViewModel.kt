package com.hsaby.accounting.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hsaby.accounting.data.model.AuthResult
import com.hsaby.accounting.data.model.RegisterRequest
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

    private val _registerResult = MutableStateFlow<AuthResult<Unit>>(AuthResult.Loading)
    val registerResult: StateFlow<AuthResult<Unit>> = _registerResult

    fun register(name: String, phone: String, password: String) {
        if (name.isBlank() || phone.isBlank() || password.isBlank()) {
            _registerResult.value = AuthResult.Error("يرجى ملء جميع الحقول")
            return
        }

        if (!phone.matches(Regex("^[0-9]{10}$"))) {
            _registerResult.value = AuthResult.Error("يرجى إدخال رقم هاتف صحيح")
            return
        }

        if (password.length < 6) {
            _registerResult.value = AuthResult.Error("يجب أن تكون كلمة المرور 6 أحرف على الأقل")
            return
        }

        viewModelScope.launch {
            _registerResult.value = AuthResult.Loading
            when (val result = authRepository.register(RegisterRequest(name, phone, password))) {
                is AuthResult.Success -> {
                    _registerResult.value = AuthResult.Success(Unit)
                }
                is AuthResult.Error -> {
                    _registerResult.value = AuthResult.Error(result.message)
                }
                is AuthResult.Loading -> {
                    _registerResult.value = AuthResult.Loading
                }
            }
        }
    }
} 
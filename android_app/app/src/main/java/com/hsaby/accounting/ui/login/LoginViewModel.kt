package com.hsaby.accounting.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hsaby.accounting.data.model.AuthResult
import com.hsaby.accounting.data.model.LoginRequest
import com.hsaby.accounting.data.model.LoginResponse
import com.hsaby.accounting.data.model.RegisterRequest
import com.hsaby.accounting.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loginResult = MutableStateFlow<AuthResult<Unit>>(AuthResult.Loading)
    val loginResult: StateFlow<AuthResult<Unit>> = _loginResult

    fun login(phone: String, password: String) {
        if (phone.isBlank() || password.isBlank()) {
            _loginResult.value = AuthResult.Error("يرجى ملء جميع الحقول")
            return
        }

        if (!phone.matches(Regex("^[0-9]{10}$"))) {
            _loginResult.value = AuthResult.Error("يرجى إدخال رقم هاتف صحيح")
            return
        }

        viewModelScope.launch {
            _loginResult.value = AuthResult.Loading
            when (val result = authRepository.login(LoginRequest(phone, password))) {
                is AuthResult.Success -> {
                    _loginResult.value = AuthResult.Success(Unit)
                }
                is AuthResult.Error -> {
                    _loginResult.value = AuthResult.Error(result.message)
                }
                is AuthResult.Loading -> {
                    _loginResult.value = AuthResult.Loading
                }
            }
        }
    }

    fun register(name: String, phone: String, password: String) {
        viewModelScope.launch {
            _loginResult.value = AuthResult.Loading
            try {
                val result = authRepository.register(RegisterRequest(name, phone, password))
                when (result) {
                    is Result.Success -> {
                        _loginResult.value = AuthResult.Success(Unit)
                    }
                    is Result.Error -> {
                        _loginResult.value = AuthResult.Error(result.message)
                    }
                }
            } catch (e: Exception) {
                _loginResult.value = AuthResult.Error(e.message ?: "حدث خطأ غير معروف")
            }
        }
    }
}

sealed class LoginResult {
    object Initial : LoginResult()
    object Loading : LoginResult()
    data class Success(val response: LoginResponse) : LoginResult()
    data class Error(val message: String) : LoginResult()
} 
package com.hsaby.accounting.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hsaby.accounting.data.model.LoginRequest
import com.hsaby.accounting.data.model.LoginResponse
import com.hsaby.accounting.data.model.RegisterRequest
import com.hsaby.accounting.data.remote.Result
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

    private val _loginResult = MutableStateFlow<LoginResult>(LoginResult.Initial)
    val loginResult: StateFlow<LoginResult> = _loginResult

    fun login(phone: String, password: String) {
        viewModelScope.launch {
            _loginResult.value = LoginResult.Loading
            try {
                val result = authRepository.login(LoginRequest(phone, password))
                when (result) {
                    is Result.Success -> {
                        _loginResult.value = LoginResult.Success(result.data)
                    }
                    is Result.Error -> {
                        _loginResult.value = LoginResult.Error(result.message)
                    }
                }
            } catch (e: Exception) {
                _loginResult.value = LoginResult.Error(e.message ?: "حدث خطأ غير معروف")
            }
        }
    }

    fun register(name: String, phone: String, password: String) {
        viewModelScope.launch {
            _loginResult.value = LoginResult.Loading
            try {
                val result = authRepository.register(RegisterRequest(name, phone, password))
                when (result) {
                    is Result.Success -> {
                        _loginResult.value = LoginResult.Success(result.data)
                    }
                    is Result.Error -> {
                        _loginResult.value = LoginResult.Error(result.message)
                    }
                }
            } catch (e: Exception) {
                _loginResult.value = LoginResult.Error(e.message ?: "حدث خطأ غير معروف")
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
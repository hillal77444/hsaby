package com.hsaby.accounting.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hsaby.accounting.data.model.LoginRequest
import com.hsaby.accounting.data.model.RegisterRequest
import com.hsaby.accounting.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    fun login(phone: String, password: String) {
        viewModelScope.launch {
            try {
                val result = authRepository.login(LoginRequest(phone, password))
                _loginResult.value = LoginResult.Success(result)
            } catch (e: Exception) {
                _loginResult.value = LoginResult.Error(e.message ?: "حدث خطأ أثناء تسجيل الدخول")
            }
        }
    }

    fun register(name: String, phone: String, password: String) {
        viewModelScope.launch {
            try {
                val result = authRepository.register(RegisterRequest(phone, password, name))
                _loginResult.value = LoginResult.Success(result)
            } catch (e: Exception) {
                _loginResult.value = LoginResult.Error(e.message ?: "حدث خطأ أثناء التسجيل")
            }
        }
    }
}

sealed class LoginResult {
    data class Success(val response: com.hsaby.accounting.data.model.LoginResponse) : LoginResult()
    data class Error(val message: String) : LoginResult()
} 
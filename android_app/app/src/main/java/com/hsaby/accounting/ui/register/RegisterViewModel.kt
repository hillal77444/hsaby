package com.hsaby.accounting.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hsaby.accounting.data.model.RegisterRequest
import com.hsaby.accounting.data.model.RegisterResponse
import com.hsaby.accounting.data.remote.Result
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

    private val _registerResult = MutableStateFlow<RegisterResult>(RegisterResult.Initial)
    val registerResult: StateFlow<RegisterResult> = _registerResult

    fun register(name: String, phone: String, password: String) {
        viewModelScope.launch {
            _registerResult.value = RegisterResult.Loading
            try {
                val result = authRepository.register(RegisterRequest(name, phone, password))
                when (result) {
                    is Result.Success -> {
                        _registerResult.value = RegisterResult.Success(result.data)
                    }
                    is Result.Error -> {
                        _registerResult.value = RegisterResult.Error(result.message)
                    }
                }
            } catch (e: Exception) {
                _registerResult.value = RegisterResult.Error(e.message ?: "حدث خطأ غير معروف")
            }
        }
    }
}

sealed class RegisterResult {
    object Initial : RegisterResult()
    object Loading : RegisterResult()
    data class Success(val response: RegisterResponse) : RegisterResult()
    data class Error(val message: String) : RegisterResult()
} 
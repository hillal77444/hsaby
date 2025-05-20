package com.hsaby.accounting.data.repository

import com.hsaby.accounting.data.model.LoginRequest
import com.hsaby.accounting.data.model.LoginResponse
import com.hsaby.accounting.data.model.RegisterRequest
import com.hsaby.accounting.data.model.RegisterResponse
import com.hsaby.accounting.data.remote.ApiService
import com.hsaby.accounting.data.remote.Result
import com.hsaby.accounting.util.PreferencesManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val preferencesManager: PreferencesManager
) {
    suspend fun login(request: LoginRequest): Result<LoginResponse> {
        return try {
            val response = apiService.login(request)
            if (response.isSuccessful && response.body() != null) {
                val loginResponse = response.body()!!
                preferencesManager.saveToken(loginResponse.token)
                preferencesManager.saveRefreshToken(loginResponse.refreshToken)
                preferencesManager.saveUserId(loginResponse.userId)
                Result.Success(loginResponse)
            } else {
                Result.Error("فشل تسجيل الدخول: ${response.errorBody()?.string() ?: "خطأ غير معروف"}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "حدث خطأ أثناء تسجيل الدخول")
        }
    }

    suspend fun register(request: RegisterRequest): Result<RegisterResponse> {
        return try {
            val response = apiService.register(request)
            if (response.isSuccessful && response.body() != null) {
                val registerResponse = response.body()!!
                preferencesManager.saveToken(registerResponse.token)
                preferencesManager.saveRefreshToken(registerResponse.refreshToken)
                preferencesManager.saveUserId(registerResponse.userId)
                Result.Success(registerResponse)
            } else {
                Result.Error("فشل التسجيل: ${response.errorBody()?.string() ?: "خطأ غير معروف"}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "حدث خطأ أثناء التسجيل")
        }
    }

    suspend fun refreshToken(refreshToken: String): Result<LoginResponse> {
        return try {
            val response = apiService.refreshToken(mapOf("refresh_token" to refreshToken))
            if (response.isSuccessful && response.body() != null) {
                val loginResponse = response.body()!!
                preferencesManager.saveToken(loginResponse.token)
                preferencesManager.saveRefreshToken(loginResponse.refreshToken)
                preferencesManager.saveUserId(loginResponse.userId)
                Result.Success(loginResponse)
            } else {
                Result.Error("فشل تحديث التوكن: ${response.errorBody()?.string() ?: "خطأ غير معروف"}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "حدث خطأ أثناء تحديث التوكن")
        }
    }

    fun getToken(): String? = preferencesManager.getToken()
    fun getRefreshToken(): String? = preferencesManager.getRefreshToken()
    fun getUserId(): String? = preferencesManager.getUserId()
    fun clearAuthData() = preferencesManager.clearAuthData()
} 
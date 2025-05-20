package com.hsaby.accounting.data.repository

import com.hsaby.accounting.data.model.LoginRequest
import com.hsaby.accounting.data.model.LoginResponse
import com.hsaby.accounting.data.model.RegisterRequest
import com.hsaby.accounting.data.model.RegisterResponse
import com.hsaby.accounting.data.remote.ApiService
import com.hsaby.accounting.data.remote.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun login(request: LoginRequest): Result<LoginResponse> {
        return try {
            val response = apiService.login(request)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error("فشل تسجيل الدخول: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "حدث خطأ أثناء تسجيل الدخول")
        }
    }

    suspend fun register(request: RegisterRequest): Result<RegisterResponse> {
        return try {
            val response = apiService.register(request)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error("فشل التسجيل: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "حدث خطأ أثناء التسجيل")
        }
    }

    suspend fun refreshToken(refreshToken: String): Result<LoginResponse> {
        return try {
            val response = apiService.refreshToken(mapOf("refresh_token" to refreshToken))
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error("فشل تحديث التوكن: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "حدث خطأ أثناء تحديث التوكن")
        }
    }
} 
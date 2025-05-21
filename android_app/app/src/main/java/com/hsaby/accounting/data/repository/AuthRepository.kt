package com.hsaby.accounting.data.repository

import com.hsaby.accounting.data.local.PreferencesManager
import com.hsaby.accounting.data.model.*
import com.hsaby.accounting.data.remote.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val preferencesManager: PreferencesManager
) {
    suspend fun login(request: LoginRequest): AuthResult<LoginResponse> {
        return try {
            val response = apiService.login(request)
            preferencesManager.saveLoginCredentials(
                phone = request.phone,
                password = request.password,
                userId = response.user.id,
                token = response.token
            )
            preferencesManager.saveRefreshToken(response.refreshToken)
            AuthResult.Success(response)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "حدث خطأ في تسجيل الدخول")
        }
    }

    suspend fun register(request: RegisterRequest): AuthResult<RegisterResponse> {
        return try {
            val response = apiService.register(request)
            preferencesManager.saveLoginCredentials(
                phone = request.phone,
                password = request.password,
                userId = response.user.id,
                token = response.token
            )
            preferencesManager.saveRefreshToken(response.refreshToken)
            AuthResult.Success(response)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "حدث خطأ في التسجيل")
        }
    }

    suspend fun refreshToken(): AuthResult<RefreshTokenResponse> {
        return try {
            val refreshToken = preferencesManager.getRefreshToken().first()
                ?: return AuthResult.Error("لم يتم العثور على رمز التحديث")
            
            val response = apiService.refreshToken(RefreshTokenRequest(refreshToken))
            preferencesManager.saveToken(response.token)
            preferencesManager.saveRefreshToken(response.refreshToken)
            AuthResult.Success(response)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "حدث خطأ في تحديث الرمز")
        }
    }

    fun isLoggedIn(): Flow<Boolean> {
        return preferencesManager.isLoggedIn
    }

    suspend fun logout() {
        preferencesManager.clearLoginCredentials()
    }
} 
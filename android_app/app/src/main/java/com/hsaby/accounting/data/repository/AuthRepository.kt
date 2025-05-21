package com.hsaby.accounting.data.repository

import com.hsaby.accounting.data.model.*
import com.hsaby.accounting.data.remote.ApiService
import com.hsaby.accounting.util.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val preferencesManager: PreferencesManager
) {
    val isLoggedIn: Flow<Boolean>
        get() = preferencesManager.accessToken.map { it != null }

    suspend fun login(request: LoginRequest): AuthResult<LoginResponse> {
        return try {
            val response = apiService.login(request)
            if (response.isSuccessful) {
                response.body()?.let { loginResponse ->
                    preferencesManager.saveAuthData(
                        accessToken = loginResponse.accessToken,
                        refreshToken = loginResponse.refreshToken,
                        userId = loginResponse.userId,
                        userName = "", // سيتم تحديثه لاحقاً
                        userEmail = request.email
                    )
                    AuthResult.Success(loginResponse)
                } ?: AuthResult.Error("Empty response body")
            } else {
                AuthResult.Error("Login failed: ${response.message()}")
            }
        } catch (e: Exception) {
            AuthResult.Error("Network error: ${e.message}")
        }
    }

    suspend fun register(request: RegisterRequest): AuthResult<RegisterResponse> {
        return try {
            val response = apiService.register(request)
            if (response.isSuccessful) {
                response.body()?.let { registerResponse ->
                    AuthResult.Success(registerResponse)
                } ?: AuthResult.Error("Empty response body")
            } else {
                AuthResult.Error("Registration failed: ${response.message()}")
            }
        } catch (e: Exception) {
            AuthResult.Error("Network error: ${e.message}")
        }
    }

    suspend fun refreshToken(): AuthResult<LoginResponse> {
        val refreshToken = preferencesManager.refreshToken.first() ?: return AuthResult.Error("No refresh token")
        
        return try {
            val response = apiService.refreshToken(RefreshTokenRequest(refreshToken))
            if (response.isSuccessful) {
                response.body()?.let { loginResponse ->
                    preferencesManager.saveAuthData(
                        accessToken = loginResponse.accessToken,
                        refreshToken = loginResponse.refreshToken,
                        userId = loginResponse.userId,
                        userName = preferencesManager.userName.first() ?: "",
                        userEmail = preferencesManager.userEmail.first() ?: ""
                    )
                    AuthResult.Success(loginResponse)
                } ?: AuthResult.Error("Empty response body")
            } else {
                AuthResult.Error("Token refresh failed: ${response.message()}")
            }
        } catch (e: Exception) {
            AuthResult.Error("Network error: ${e.message}")
        }
    }

    suspend fun logout() {
        preferencesManager.clearAuthData()
    }
} 
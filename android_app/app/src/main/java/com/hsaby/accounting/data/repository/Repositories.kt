package com.hsaby.accounting.data.repository

import com.hsaby.accounting.data.local.dao.UserDao
import com.hsaby.accounting.data.local.entity.UserEntity
import com.hsaby.accounting.data.model.*
import com.hsaby.accounting.data.remote.ApiService
import com.hsaby.accounting.util.PreferencesManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val apiService: ApiService,
    private val preferencesManager: PreferencesManager
) {
    suspend fun register(username: String, phone: String, password: String): Result<RegisterResponse> {
        return try {
            val response = apiService.register(RegisterRequest(username, phone, password))
            if (response.isSuccessful) {
                response.body()?.let {
                    userDao.insertUser(
                        UserEntity(
                            id = it.user.id,
                            username = it.user.name,
                            phone = phone,
                            passwordHash = password,
                            lastSync = System.currentTimeMillis()
                        )
                    )
                    preferencesManager.saveToken(it.token)
                    preferencesManager.saveRefreshToken(it.refreshToken)
                    preferencesManager.saveUserId(it.user.id)
                    Result.success(it)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Registration failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun login(phone: String, password: String): Result<LoginResponse> {
        return try {
            val response = apiService.login(LoginRequest(phone, password))
            if (response.isSuccessful) {
                response.body()?.let {
                    userDao.insertUser(
                        UserEntity(
                            id = it.user.id,
                            username = it.user.name,
                            phone = phone,
                            passwordHash = password,
                            lastSync = System.currentTimeMillis()
                        )
                    )
                    preferencesManager.saveToken(it.token)
                    preferencesManager.saveRefreshToken(it.refreshToken)
                    preferencesManager.saveUserId(it.user.id)
                    Result.success(it)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Login failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserById(userId: String): UserEntity? {
        return userDao.getUserById(userId)
    }

    fun getCurrentUserId(): String? {
        return preferencesManager.getUserId()
    }

    fun getCurrentUsername(): String? {
        return preferencesManager.getUsername()
    }

    fun clearUserData() {
        preferencesManager.clearAuthData()
    }
} 
package com.hsaby.accounting.data.model

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val accessToken: String,
    val userId: String,
    val refreshToken: String,
    val expiresIn: Long
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val phone: String
)

data class RegisterResponse(
    val userId: String,
    val message: String
)

data class RefreshTokenRequest(
    val refreshToken: String
)

sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String) : AuthResult<Nothing>()
    object Loading : AuthResult<Nothing>()
}

data class User(
    val id: String,
    val name: String,
    val phone: String,
    val createdAt: Long? = null,
    val updatedAt: Long? = null
) 
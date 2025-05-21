package com.hsaby.accounting.data.model

data class LoginRequest(
    val phone: String,
    val password: String
)

data class RegisterRequest(
    val name: String,
    val phone: String,
    val password: String
)

data class LoginResponse(
    val accessToken: String,
    val user: User
)

data class RegisterResponse(
    val message: String,
    val user: User
)

data class User(
    val id: String,
    val name: String,
    val phone: String,
    val createdAt: Long? = null,
    val updatedAt: Long? = null
) 
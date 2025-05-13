package com.accounting.app.models

data class LoginResponse(
    val token: String,
    val user: User
) 
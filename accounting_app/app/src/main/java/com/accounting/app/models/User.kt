package com.accounting.app.models

data class User(
    val id: Long = 0,
    val username: String,
    val phone: String,
    val passwordHash: String = ""
) 
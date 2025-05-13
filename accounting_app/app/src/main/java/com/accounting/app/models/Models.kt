package com.accounting.app.models

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val user: User
)

data class User(
    val id: Long = 0,
    val username: String,
    val phone: String,
    val passwordHash: String = ""
)

data class Account(
    val id: Long = 0,
    val accountNumber: String,
    val accountName: String,
    val balance: Double,
    val phoneNumber: String? = null,
    val isDebtor: Boolean = false,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class Transaction(
    val id: Long = 0,
    val date: Long,
    val amount: Double,
    val description: String,
    val type: String,
    val currency: String,
    val notes: String? = null,
    val accountId: Long
)

data class SyncData(
    val accounts: List<Account>,
    val transactions: List<Transaction>,
    val lastSyncTimestamp: Long
)

data class SyncResponse(
    val accounts: List<Account>,
    val transactions: List<Transaction>,
    val syncTimestamp: Long
) 
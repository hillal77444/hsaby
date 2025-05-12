package com.accounting.app

data class Account(
    val id: Long = 0,
    val accountNumber: String,
    val accountName: String,
    val balance: Double,
    val phoneNumber: String?,
    val isDebtor: Boolean,
    val notes: String?,
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
    val notes: String?,
    val accountId: Long
)

data class User(
    val id: Long = 0,
    val username: String,
    val phone: String,
    val passwordHash: String
) 
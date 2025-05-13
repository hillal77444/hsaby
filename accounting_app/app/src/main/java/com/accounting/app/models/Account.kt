package com.accounting.app.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true)
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
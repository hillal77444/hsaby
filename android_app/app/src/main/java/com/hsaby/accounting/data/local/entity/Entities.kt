package com.hsaby.accounting.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val phone: String,
    val passwordHash: String,
    val lastSync: Long
)

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val serverId: Long,
    val accountName: String,
    val balance: Double,
    val currency: String,
    val phoneNumber: String?,
    val notes: String?,
    val isDebtor: Boolean,
    val whatsappEnabled: Boolean,
    val userId: String,
    val lastSync: Long
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val serverId: Long,
    val accountId: String,
    val amount: Double,
    val type: String,
    val description: String,
    val date: Long,
    val currency: String,
    val notes: String?,
    val whatsappEnabled: Boolean,
    val userId: String,
    val isSynced: Boolean,
    val lastSync: Long
) 
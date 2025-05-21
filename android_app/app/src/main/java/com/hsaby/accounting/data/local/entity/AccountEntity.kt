package com.hsaby.accounting.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey
    val id: String,
    val serverId: String? = null,
    val accountName: String,
    val balance: Double,
    val currency: String,
    val phoneNumber: String? = null,
    val notes: String? = null,
    val isDebtor: Boolean = false,
    val whatsappEnabled: Boolean = false,
    val userId: String,
    val isActive: Boolean = true,
    val isSynced: Boolean = false,
    val lastSync: Long = System.currentTimeMillis()
) 
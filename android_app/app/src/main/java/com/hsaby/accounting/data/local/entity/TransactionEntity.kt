package com.hsaby.accounting.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    val serverId: String? = null,
    val accountId: String,
    val amount: Double,
    val type: String,
    val description: String? = null,
    val date: Long,
    val currency: String,
    val notes: String? = null,
    val whatsappEnabled: Boolean = false,
    val isSynced: Boolean = false,
    val lastSync: Long = System.currentTimeMillis()
) 
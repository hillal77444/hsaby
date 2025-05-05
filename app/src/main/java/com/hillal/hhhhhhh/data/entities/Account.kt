package com.hillal.hhhhhhh.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phoneNumber: String? = null,
    val notes: String? = null,
    val openingBalance: Double = 0.0,
    val isDebtor: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) 
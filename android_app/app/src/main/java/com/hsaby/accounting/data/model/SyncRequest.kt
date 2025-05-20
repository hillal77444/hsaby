package com.hsaby.accounting.data.model

data class SyncRequest(
    val userId: String,
    val lastSyncTime: Long
) 
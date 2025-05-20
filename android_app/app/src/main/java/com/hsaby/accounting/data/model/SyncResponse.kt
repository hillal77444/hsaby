package com.hsaby.accounting.data.model

data class SyncResponse(
    val accounts: List<Account>,
    val transactions: List<Transaction>,
    val lastSyncTime: Long
) 
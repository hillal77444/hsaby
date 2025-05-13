package com.accounting.app.models

data class SyncData(
    val accounts: List<Account>,
    val transactions: List<Transaction>,
    val lastSyncTimestamp: Long
) 
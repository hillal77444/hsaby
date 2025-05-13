package com.accounting.app.models

data class SyncResponse(
    val accounts: List<Account>,
    val transactions: List<Transaction>,
    val syncTimestamp: Long
) 
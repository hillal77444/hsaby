package com.hsaby.accounting.data.model

data class SyncChangesResponse(
    val success: Boolean,
    val message: String,
    val syncedAccounts: List<Account>,
    val syncedTransactions: List<Transaction>
) 
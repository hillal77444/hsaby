package com.hsaby.accounting.data.model

data class SyncChangesRequest(
    val userId: String,
    val accounts: List<Account>,
    val transactions: List<Transaction>
) 
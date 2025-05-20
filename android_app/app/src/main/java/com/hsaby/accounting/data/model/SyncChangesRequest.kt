package com.hsaby.accounting.data.model

data class SyncChangesRequest(
    val accounts: List<Account>,
    val transactions: List<Transaction>
) 
package com.hillal.acc.data.room;

import androidx.room.ColumnInfo;

public class AccountTransactionCount {
    @ColumnInfo(name = "account_id")
    public long accountId;
    
    @ColumnInfo(name = "transaction_count")
    public int transactionCount;

    public AccountTransactionCount(long accountId, int transactionCount) {
        this.accountId = accountId;
        this.transactionCount = transactionCount;
    }
} 
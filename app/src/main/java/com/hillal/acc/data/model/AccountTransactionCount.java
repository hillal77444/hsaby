package com.hillal.acc.data.model;

public class AccountTransactionCount {
    public long accountId;
    public int transactionCount;

    public AccountTransactionCount(long accountId, int transactionCount) {
        this.accountId = accountId;
        this.transactionCount = transactionCount;
    }
} 
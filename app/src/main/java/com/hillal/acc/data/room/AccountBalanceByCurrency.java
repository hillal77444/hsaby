package com.hillal.acc.data.room;

import androidx.room.ColumnInfo;

public class AccountBalanceByCurrency {
    @ColumnInfo(name = "account_id")
    public long accountId;
    
    @ColumnInfo(name = "currency")
    public String currency;
    
    @ColumnInfo(name = "balance")
    public double balance;

    public AccountBalanceByCurrency(long accountId, String currency, double balance) {
        this.accountId = accountId;
        this.currency = currency;
        this.balance = balance;
    }
} 
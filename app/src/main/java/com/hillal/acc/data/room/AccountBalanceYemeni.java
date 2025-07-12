package com.hillal.acc.data.room;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;

public class AccountBalanceYemeni {
    @ColumnInfo(name = "account_id")
    public long accountId;

    @ColumnInfo(name = "balance")
    public double balance;

    public AccountBalanceYemeni(long accountId, double balance) {
        this.accountId = accountId;
        this.balance = balance;
    }
} 
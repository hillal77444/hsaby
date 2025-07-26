package com.hillal.acc.data.room;

public class AccountBalanceByCurrency {
    public long accountId;
    public String currency;
    public double balance;

    public AccountBalanceByCurrency(long accountId, String currency, double balance) {
        this.accountId = accountId;
        this.currency = currency;
        this.balance = balance;
    }
} 
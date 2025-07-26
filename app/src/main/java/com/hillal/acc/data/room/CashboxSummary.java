package com.hillal.acc.data.room;

import androidx.room.ColumnInfo;

public class CashboxSummary {
    @ColumnInfo(name = "cashbox_id")
    public long cashboxId;
    
    @ColumnInfo(name = "currency")
    public String currency;
    
    @ColumnInfo(name = "total_credit")
    public double totalCredit;
    
    @ColumnInfo(name = "total_debit")
    public double totalDebit;
    
    @ColumnInfo(name = "balance")
    public double balance;

    public CashboxSummary(long cashboxId, String currency, double totalCredit, double totalDebit, double balance) {
        this.cashboxId = cashboxId;
        this.currency = currency;
        this.totalCredit = totalCredit;
        this.totalDebit = totalDebit;
        this.balance = balance;
    }
} 
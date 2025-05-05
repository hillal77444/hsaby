package com.hillal.hhhhhhh.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "transactions",
        foreignKeys = @ForeignKey(entity = Account.class,
                parentColumns = "id",
                childColumns = "accountId",
                onDelete = ForeignKey.CASCADE))
public class Transaction {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int accountId;
    private String date;
    private double amount;
    private String description;
    private boolean isDebit;

    public Transaction(int accountId, String date, double amount, String description, boolean isDebit) {
        this.accountId = accountId;
        this.date = date;
        this.amount = amount;
        this.description = description;
        this.isDebit = isDebit;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAccountId() {
        return accountId;
    }

    public String getDate() {
        return date;
    }

    public double getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public boolean isDebit() {
        return isDebit;
    }
} 
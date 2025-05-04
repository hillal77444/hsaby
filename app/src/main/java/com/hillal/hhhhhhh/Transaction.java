package com.hillal.hhhhhhh;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(
    tableName = "transactions",
    foreignKeys = @ForeignKey(
        entity = Account.class,
        parentColumns = "id",
        childColumns = "accountId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("accountId")}
)
public class Transaction {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int accountId;
    private double amount;
    private String description;
    private Date date;
    private boolean isCredit; // true for credit, false for debit

    public Transaction(int accountId, double amount, String description, Date date, boolean isCredit) {
        this.accountId = accountId;
        this.amount = amount;
        this.description = description;
        this.date = date;
        this.isCredit = isCredit;
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

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public boolean isCredit() {
        return isCredit;
    }

    public void setCredit(boolean credit) {
        isCredit = credit;
    }
} 
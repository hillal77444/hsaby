package com.hillal.acc.data.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "transactions",
        foreignKeys = @ForeignKey(entity = Account.class,
                parentColumns = "id",
                childColumns = "accountId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("accountId")})
public class Transaction {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long accountId;
    private String type; // "debit" or "credit"
    private double amount;
    private String description;
    private Date date;
    private boolean whatsappEnabled = true;

    public Transaction(long accountId, String type, double amount, String description, Date date) {
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.date = date;
        this.whatsappEnabled = true;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public boolean isWhatsappEnabled() {
        return whatsappEnabled;
    }

    public void setWhatsappEnabled(boolean whatsappEnabled) {
        this.whatsappEnabled = whatsappEnabled;
    }
} 
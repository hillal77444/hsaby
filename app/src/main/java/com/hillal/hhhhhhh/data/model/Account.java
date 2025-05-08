package com.hillal.hhhhhhh.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;

@Entity(tableName = "accounts")
public class Account {
    @PrimaryKey(autoGenerate = true)
    private long id;

    @SerializedName("account_number")
    private String accountNumber;

    @SerializedName("account_name")
    private String name;

    @SerializedName("balance")
    private double balance;

    @SerializedName("phone_number")
    private String phoneNumber;

    @SerializedName("notes")
    private String notes;

    @SerializedName("is_debtor")
    private boolean isDebtor;

    @SerializedName("created_at")
    private long createdAt;

    @SerializedName("updated_at")
    private long updatedAt;

    // Constructor
    public Account(String accountNumber, String name, double balance, String phoneNumber, boolean isDebtor) {
        this.accountNumber = accountNumber;
        this.name = name;
        this.balance = balance;
        this.phoneNumber = phoneNumber;
        this.isDebtor = isDebtor;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Empty constructor for Room
    public Account() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isDebtor() {
        return isDebtor;
    }

    public void setDebtor(boolean debtor) {
        isDebtor = debtor;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return name;
    }
} 
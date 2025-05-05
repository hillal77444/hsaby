package com.hillal.hhhhhhh.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "accounts")
public class Account {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String name;
    private String phoneNumber;
    private double balance;
    private boolean isCreditor;
    private String notes;
    private long createdAt;
    private long updatedAt;

    public Account(String name, String phoneNumber, double balance, boolean isCreditor, String notes) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.balance = balance;
        this.isCreditor = isCreditor;
        this.notes = notes;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public boolean isCreditor() {
        return isCreditor;
    }

    public void setCreditor(boolean creditor) {
        isCreditor = creditor;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
} 
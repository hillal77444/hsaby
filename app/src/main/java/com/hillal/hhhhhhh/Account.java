package com.hillal.hhhhhhh;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "accounts")
public class Account {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private String phone;
    private String notes;
    private double openingBalance;
    private boolean isCreditor; // true for creditor, false for debtor

    public Account(String name, String phone, String notes, double openingBalance, boolean isCreditor) {
        this.name = name;
        this.phone = phone;
        this.notes = notes;
        this.openingBalance = openingBalance;
        this.isCreditor = isCreditor;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public double getOpeningBalance() {
        return openingBalance;
    }

    public void setOpeningBalance(double openingBalance) {
        this.openingBalance = openingBalance;
    }

    public boolean isCreditor() {
        return isCreditor;
    }

    public void setCreditor(boolean creditor) {
        isCreditor = creditor;
    }

    public double getBalance() {
        return isCreditor ? openingBalance : -openingBalance;
    }
}
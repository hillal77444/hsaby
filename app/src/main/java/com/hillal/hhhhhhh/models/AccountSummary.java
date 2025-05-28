package com.hillal.hhhhhhh.models;

import com.google.gson.annotations.SerializedName;

public class AccountSummary {
    @SerializedName("userId")
    private long userId;
    
    @SerializedName("userName")
    private String userName;
    
    @SerializedName("balance")
    private double balance;
    
    @SerializedName("totalDebits")
    private double totalDebits;
    
    @SerializedName("totalCredits")
    private double totalCredits;
    
    @SerializedName("currency")
    private String currency;

    // Constructor فارغ مطلوب لـ Gson
    public AccountSummary() {
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public double getTotalDebits() {
        return totalDebits;
    }

    public void setTotalDebits(double totalDebits) {
        this.totalDebits = totalDebits;
    }

    public double getTotalCredits() {
        return totalCredits;
    }

    public void setTotalCredits(double totalCredits) {
        this.totalCredits = totalCredits;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    @Override
    public String toString() {
        return "AccountSummary{" +
                "userId=" + userId +
                ", userName='" + userName + '\'' +
                ", balance=" + balance +
                ", totalDebits=" + totalDebits +
                ", totalCredits=" + totalCredits +
                ", currency='" + currency + '\'' +
                '}';
    }
} 
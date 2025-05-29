package com.hillal.acc.models;

import com.google.gson.annotations.SerializedName;

public class CurrencySummary {
    @SerializedName("currency")
    private String currency;
    
    @SerializedName("totalBalance")
    private double totalBalance;
    
    @SerializedName("totalDebits")
    private double totalDebits;
    
    @SerializedName("totalCredits")
    private double totalCredits;

    // Constructor فارغ مطلوب لـ Gson
    public CurrencySummary() {
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public double getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(double totalBalance) {
        this.totalBalance = totalBalance;
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

    @Override
    public String toString() {
        return "CurrencySummary{" +
                "currency='" + currency + '\'' +
                ", totalBalance=" + totalBalance +
                ", totalDebits=" + totalDebits +
                ", totalCredits=" + totalCredits +
                '}';
    }
} 
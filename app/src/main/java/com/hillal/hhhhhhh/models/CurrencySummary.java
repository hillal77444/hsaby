package com.hillal.hhhhhhh.models;

public class CurrencySummary {
    private String currency;
    private double totalBalance;
    private double totalDebits;
    private double totalCredits;

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
} 
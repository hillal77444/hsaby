package com.hillal.hhhhhhh.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AccountReport {
    @SerializedName("accountId")
    private int accountId;

    @SerializedName("userName")
    private String userName;

    @SerializedName("accountName")
    private String accountName;

    @SerializedName("balance")
    private double balance;

    @SerializedName("totalDebits")
    private double totalDebits;

    @SerializedName("totalCredits")
    private double totalCredits;

    @SerializedName("currency")
    private String currency;

    @SerializedName("transactions")
    private List<Transaction> transactions;

    // Constructor فارغ مطلوب لـ Gson
    public AccountReport() {
    }

    // Getters
    public int getAccountId() {
        return accountId;
    }

    public String getUserName() {
        return userName;
    }

    public String getAccountName() {
        return accountName;
    }

    public double getBalance() {
        return balance;
    }

    public double getTotalDebits() {
        return totalDebits;
    }

    public double getTotalCredits() {
        return totalCredits;
    }

    public String getCurrency() {
        return currency;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    // Setters
    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public void setTotalDebits(double totalDebits) {
        this.totalDebits = totalDebits;
    }

    public void setTotalCredits(double totalCredits) {
        this.totalCredits = totalCredits;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    public static class Transaction {
        @SerializedName("date")
        private String date;

        @SerializedName("type")
        private String type;

        @SerializedName("amount")
        private double amount;

        @SerializedName("description")
        private String description;

        // Constructor فارغ مطلوب لـ Gson
        public Transaction() {
        }

        // Getters
        public String getDate() {
            return date;
        }

        public String getType() {
            return type;
        }

        public double getAmount() {
            return amount;
        }

        public String getDescription() {
            return description;
        }

        // Setters
        public void setDate(String date) {
            this.date = date;
        }

        public void setType(String type) {
            this.type = type;
        }

        public void setAmount(double amount) {
            this.amount = amount;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
} 
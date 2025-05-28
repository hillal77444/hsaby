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

    public static class Transaction {
        @SerializedName("transactionId")
        private int transactionId;

        @SerializedName("type")
        private String type;

        @SerializedName("amount")
        private double amount;

        @SerializedName("currency")
        private String currency;

        @SerializedName("date")
        private String date;

        @SerializedName("description")
        private String description;

        public int getTransactionId() {
            return transactionId;
        }

        public String getType() {
            return type;
        }

        public double getAmount() {
            return amount;
        }

        public String getCurrency() {
            return currency;
        }

        public String getDate() {
            return date;
        }

        public String getDescription() {
            return description;
        }
    }
} 
package com.hillal.hhhhhhh.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DetailedTransactionsResponse {
    @SerializedName("transactions")
    private List<Transaction> transactions;

    @SerializedName("currentBalance")
    private double currentBalance;

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public double getCurrentBalance() {
        return currentBalance;
    }

    public static class Transaction {
        @SerializedName("date")
        private String date;

        @SerializedName("amount")
        private double amount;

        @SerializedName("type")
        private String type;

        @SerializedName("description")
        private String description;

        public String getDate() {
            return date;
        }

        public double getAmount() {
            return amount;
        }

        public String getType() {
            return type;
        }

        public String getDescription() {
            return description;
        }
    }
} 
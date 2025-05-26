package com.hillal.hhhhhhh.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AccountSummaryResponse {
    @SerializedName("accounts")
    private List<AccountSummary> accounts;

    @SerializedName("totalBalance")
    private double totalBalance;

    @SerializedName("totalDebits")
    private double totalDebits;

    @SerializedName("totalCredits")
    private double totalCredits;

    public List<AccountSummary> getAccounts() {
        return accounts;
    }

    public double getTotalBalance() {
        return totalBalance;
    }

    public double getTotalDebits() {
        return totalDebits;
    }

    public double getTotalCredits() {
        return totalCredits;
    }

    public static class AccountSummary {
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

        public long getUserId() {
            return userId;
        }

        public String getUserName() {
            return userName;
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
    }
} 
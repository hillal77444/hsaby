package com.hillal.hhhhhhh.data.model;

import java.util.List;

public class AccountSummaryResponse {
    private List<AccountSummary> accounts;
    private double totalBalance;
    private double totalDebits;
    private double totalCredits;

    public List<AccountSummary> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<AccountSummary> accounts) {
        this.accounts = accounts;
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

    public static class AccountSummary {
        private long userId;
        private String userName;
        private double balance;
        private double totalDebits;
        private double totalCredits;

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
    }
} 
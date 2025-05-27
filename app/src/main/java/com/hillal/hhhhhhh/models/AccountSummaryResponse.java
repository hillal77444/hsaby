package com.hillal.hhhhhhh.models;

import java.util.List;

public class AccountSummaryResponse {
    private List<AccountSummary> accounts;
    private List<CurrencySummary> currencySummary;

    public List<AccountSummary> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<AccountSummary> accounts) {
        this.accounts = accounts;
    }

    public List<CurrencySummary> getCurrencySummary() {
        return currencySummary;
    }

    public void setCurrencySummary(List<CurrencySummary> currencySummary) {
        this.currencySummary = currencySummary;
    }
} 
package com.hillal.hhhhhhh.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AccountSummaryResponse {
    @SerializedName("accounts")
    private List<AccountSummary> accounts;
    
    @SerializedName("currencySummary")
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AccountSummaryResponse{");
        if (accounts != null) {
            sb.append("accounts=").append(accounts.size()).append(" items");
        } else {
            sb.append("accounts=null");
        }
        sb.append(", ");
        if (currencySummary != null) {
            sb.append("currencySummary=").append(currencySummary.size()).append(" items");
        } else {
            sb.append("currencySummary=null");
        }
        sb.append("}");
        return sb.toString();
    }
} 
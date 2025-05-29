package com.hillal.acc.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AccountSummaryResponse {
    @SerializedName("accounts")
    private List<AccountSummary> accounts;
    
    @SerializedName("currencySummary")
    private List<CurrencySummary> currencySummary;

    // Constructor فارغ مطلوب لـ Gson
    public AccountSummaryResponse() {
    }

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
            sb.append("accounts=[");
            for (AccountSummary account : accounts) {
                sb.append(account.toString()).append(", ");
            }
            sb.append("]");
        } else {
            sb.append("accounts=null");
        }
        sb.append(", ");
        if (currencySummary != null) {
            sb.append("currencySummary=[");
            for (CurrencySummary summary : currencySummary) {
                sb.append(summary.toString()).append(", ");
            }
            sb.append("]");
        } else {
            sb.append("currencySummary=null");
        }
        sb.append("}");
        return sb.toString();
    }
} 
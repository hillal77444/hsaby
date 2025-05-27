package com.hillal.hhhhhhh.ui.summary;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.models.AccountSummary;
import com.hillal.hhhhhhh.models.CurrencySummary;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AccountSummaryAdapter extends RecyclerView.Adapter<AccountSummaryAdapter.ViewHolder> {
    private List<AccountSummary> accounts = new ArrayList<>();
    private List<CurrencySummary> currencySummaries = new ArrayList<>();
    private final NumberFormat numberFormat;

    public AccountSummaryAdapter() {
        numberFormat = NumberFormat.getNumberInstance(Locale.US);
        numberFormat.setMinimumFractionDigits(2);
        numberFormat.setMaximumFractionDigits(2);
    }

    public void setData(List<AccountSummary> accounts, List<CurrencySummary> currencySummaries) {
        this.accounts = accounts;
        this.currencySummaries = currencySummaries;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_account_summary, parent, false);
        return new ViewHolder(view, numberFormat);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position < currencySummaries.size()) {
            holder.bind(currencySummaries.get(position));
        } else {
            int accountPosition = position - currencySummaries.size();
            holder.bind(accounts.get(accountPosition));
        }
    }

    @Override
    public int getItemCount() {
        return accounts.size() + currencySummaries.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;
        private final NumberFormat numberFormat;

        ViewHolder(View view, NumberFormat numberFormat) {
            super(view);
            this.textView = view.findViewById(R.id.textView);
            this.numberFormat = numberFormat;
        }

        void bind(CurrencySummary summary) {
            String text = String.format("%s: %s (مدين: %s, دائن: %s)",
                    summary.getCurrency(),
                    numberFormat.format(summary.getTotalBalance()),
                    numberFormat.format(summary.getTotalDebits()),
                    numberFormat.format(summary.getTotalCredits()));
            textView.setText(text);
        }

        void bind(AccountSummary account) {
            String text = String.format("%s - %s: %s (مدين: %s, دائن: %s)",
                    account.getUserName(),
                    account.getCurrency(),
                    numberFormat.format(account.getBalance()),
                    numberFormat.format(account.getTotalDebits()),
                    numberFormat.format(account.getTotalCredits()));
            textView.setText(text);
        }
    }
} 
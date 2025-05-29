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
        private final TextView userNameTextView;
        private final TextView balanceTextView;
        private final TextView debitsTextView;
        private final TextView creditsTextView;
        private final NumberFormat numberFormat;

        ViewHolder(View view, NumberFormat numberFormat) {
            super(view);
            this.userNameTextView = view.findViewById(R.id.userNameTextView);
            this.balanceTextView = view.findViewById(R.id.balanceTextView);
            this.debitsTextView = view.findViewById(R.id.debitsTextView);
            this.creditsTextView = view.findViewById(R.id.creditsTextView);
            this.numberFormat = numberFormat;
        }

        void bind(CurrencySummary summary) {
            userNameTextView.setText(summary.getCurrency());
            balanceTextView.setText(numberFormat.format(summary.getTotalBalance()));
            debitsTextView.setText("مدين: " + numberFormat.format(summary.getTotalDebits()));
            creditsTextView.setText("دائن: " + numberFormat.format(summary.getTotalCredits()));
        }

        void bind(AccountSummary account) {
            userNameTextView.setText(account.getUserName());
            balanceTextView.setText(numberFormat.format(account.getBalance()));
            debitsTextView.setText("مدين: " + numberFormat.format(account.getTotalDebits()));
            creditsTextView.setText("دائن: " + numberFormat.format(account.getTotalCredits()));
        }
    }
} 
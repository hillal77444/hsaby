package com.hillal.hhhhhhh.ui.summary;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hillal.hhhhhhh.databinding.ItemAccountSummaryBinding;

import java.util.ArrayList;
import java.util.List;

public class AccountSummaryAdapter extends RecyclerView.Adapter<AccountSummaryAdapter.ViewHolder> {
    private List<AccountSummary> accounts = new ArrayList<>();
    private List<CurrencySummary> currencySummaries = new ArrayList<>();

    public void setData(List<AccountSummary> accounts, List<CurrencySummary> currencySummaries) {
        this.accounts = accounts;
        this.currencySummaries = currencySummaries;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAccountSummaryBinding binding = ItemAccountSummaryBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(accounts.get(position));
    }

    @Override
    public int getItemCount() {
        return accounts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemAccountSummaryBinding binding;

        ViewHolder(ItemAccountSummaryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(AccountSummary account) {
            binding.userNameTextView.setText(account.getUserName());
            binding.balanceTextView.setText(String.format("%.2f %s", account.getBalance(), account.getCurrency()));
            binding.debitsTextView.setText(String.format("المصروفات: %.2f", account.getTotalDebits()));
            binding.creditsTextView.setText(String.format("الإيرادات: %.2f", account.getTotalCredits()));
        }
    }
} 
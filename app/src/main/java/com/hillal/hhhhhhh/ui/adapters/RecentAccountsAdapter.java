package com.hillal.hhhhhhh.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.model.Account;

import java.util.ArrayList;
import java.util.List;

public class RecentAccountsAdapter extends RecyclerView.Adapter<RecentAccountsAdapter.ViewHolder> {
    private List<Account> accounts = new ArrayList<>();

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_account, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Account account = accounts.get(position);
        holder.nameTextView.setText(account.getName());
        holder.phoneTextView.setText(account.getPhoneNumber());
        holder.balanceTextView.setText(String.format("%s %s", 
            account.getBalance(), holder.itemView.getContext().getString(R.string.currency_symbol)));
    }

    @Override
    public int getItemCount() {
        return accounts.size();
    }

    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView phoneTextView;
        TextView balanceTextView;

        ViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.account_name);
            phoneTextView = itemView.findViewById(R.id.account_phone);
            balanceTextView = itemView.findViewById(R.id.account_balance);
        }
    }
} 
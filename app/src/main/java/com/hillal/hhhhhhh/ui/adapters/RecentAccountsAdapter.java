package com.hillal.hhhhhhh.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.viewmodel.AccountViewModel;
import com.hillal.hhhhhhh.viewmodel.TransactionViewModel;

import java.util.List;

public class RecentAccountsAdapter extends RecyclerView.Adapter<RecentAccountsAdapter.ViewHolder> {
    private List<Account> accounts;
    private AccountViewModel accountViewModel;
    private TransactionViewModel transactionViewModel;

    public RecentAccountsAdapter() {
        this.accounts = null;
    }

    public void updateAccounts(List<Account> accounts) {
        this.accounts = accounts;
        notifyDataSetChanged();
    }

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
        holder.balanceTextView.setText(String.format("%.2f", account.getBalance()));
    }

    @Override
    public int getItemCount() {
        return accounts != null ? accounts.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView phoneTextView;
        TextView balanceTextView;

        ViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.account_name);
            phoneTextView = itemView.findViewById(R.id.accountPhone);
            balanceTextView = itemView.findViewById(R.id.accountBalance);
        }
    }
} 
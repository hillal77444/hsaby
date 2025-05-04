package com.hillal.hhhhhhh;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

public class AccountAdapter extends ListAdapter<Account, AccountAdapter.AccountViewHolder> {
    private OnAccountClickListener listener;

    public interface OnAccountClickListener {
        void onAccountClick(Account account);
    }

    public AccountAdapter(OnAccountClickListener listener) {
        super(new DiffUtil.ItemCallback<Account>() {
            @Override
            public boolean areItemsTheSame(@NonNull Account oldItem, @NonNull Account newItem) {
                return oldItem.getId() == newItem.getId();
            }

            @Override
            public boolean areContentsTheSame(@NonNull Account oldItem, @NonNull Account newItem) {
                return oldItem.getName().equals(newItem.getName()) &&
                        oldItem.getPhone().equals(newItem.getPhone()) &&
                        oldItem.getOpeningBalance() == newItem.getOpeningBalance() &&
                        oldItem.isCreditor() == newItem.isCreditor();
            }
        });
        this.listener = listener;
    }

    @NonNull
    @Override
    public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_account, parent, false);
        return new AccountViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AccountViewHolder holder, int position) {
        Account account = getItem(position);
        holder.nameTextView.setText(account.getName());
        holder.phoneTextView.setText(account.getPhone());
        holder.balanceTextView.setText(String.format("%.2f", account.getBalance()));
        holder.balanceTextView.setTextColor(account.isCreditor() ?
                holder.itemView.getContext().getResources().getColor(android.R.color.holo_green_dark) :
                holder.itemView.getContext().getResources().getColor(android.R.color.holo_red_dark));
        holder.itemView.setOnClickListener(v -> listener.onAccountClick(account));
    }

    class AccountViewHolder extends RecyclerView.ViewHolder {
        private TextView nameTextView;
        private TextView phoneTextView;
        private TextView balanceTextView;

        public AccountViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.account_name);
            phoneTextView = itemView.findViewById(R.id.account_phone);
            balanceTextView = itemView.findViewById(R.id.account_balance);
        }
    }
} 
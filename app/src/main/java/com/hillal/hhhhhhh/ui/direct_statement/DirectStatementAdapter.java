package com.hillal.hhhhhhh.ui.direct_statement;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.model.AccountSummary;
import java.util.ArrayList;
import java.util.List;

public class DirectStatementAdapter extends RecyclerView.Adapter<DirectStatementAdapter.ViewHolder> {
    private List<AccountSummary> accounts = new ArrayList<>();
    private OnAccountClickListener listener;
    private int selectedPosition = -1;

    public interface OnAccountClickListener {
        void onAccountClick(AccountSummary account);
    }

    public DirectStatementAdapter(OnAccountClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_account, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AccountSummary account = accounts.get(position);
        holder.accountName.setText(account.getAccountName());
        holder.balance.setText(String.valueOf(account.getBalance()));
        holder.itemView.setSelected(position == selectedPosition);
        
        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);
            listener.onAccountClick(account);
        });
    }

    @Override
    public int getItemCount() {
        return accounts.size();
    }

    public void setAccounts(List<AccountSummary> accounts) {
        this.accounts = accounts;
        notifyDataSetChanged();
    }

    public long getSelectedAccountId() {
        if (selectedPosition != -1 && selectedPosition < accounts.size()) {
            return accounts.get(selectedPosition).getUserId();
        }
        return -1;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView accountName;
        TextView balance;

        ViewHolder(View view) {
            super(view);
            accountName = view.findViewById(R.id.accountName);
            balance = view.findViewById(R.id.balance);
        }
    }
} 
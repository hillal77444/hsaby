package com.hillal.acc.ui.reports;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hillal.acc.R;
import com.hillal.acc.data.model.Account;

import java.util.List;

public class AccountsSummaryAdapter extends RecyclerView.Adapter<AccountsSummaryAdapter.ViewHolder> {
    public static class AccountSummary {
        public final String accountName;
        public final double credit;
        public final double debit;
        public final double balance;
        public AccountSummary(String accountName, double credit, double debit, double balance) {
            this.accountName = accountName;
            this.credit = credit;
            this.debit = debit;
            this.balance = balance;
        }
    }

    private List<AccountSummary> data;

    public AccountsSummaryAdapter(List<AccountSummary> data) {
        this.data = data;
    }

    public void setData(List<AccountSummary> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_account_summary, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AccountSummary item = data.get(position);
        holder.tvAccountName.setText(item.accountName);
        holder.tvCredit.setText(String.format("%,.0f", item.credit));
        holder.tvDebit.setText(String.format("%,.0f", item.debit));
        holder.tvBalance.setText(String.format("%,.0f", item.balance));
        if (item.balance > 0) {
            holder.ivArrow.setImageResource(R.drawable.ic_arrow_upward);
            holder.ivArrow.setColorFilter(Color.parseColor("#4CAF50"));
            holder.tvBalance.setTextColor(Color.parseColor("#4CAF50"));
        } else if (item.balance < 0) {
            holder.ivArrow.setImageResource(R.drawable.ic_arrow_downward);
            holder.ivArrow.setColorFilter(Color.parseColor("#F44336"));
            holder.tvBalance.setTextColor(Color.parseColor("#F44336"));
        } else {
            holder.ivArrow.setImageResource(R.drawable.ic_arrow_right);
            holder.ivArrow.setColorFilter(Color.GRAY);
            holder.tvBalance.setTextColor(Color.GRAY);
        }
    }

    @Override
    public int getItemCount() {
        return data != null ? data.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAccountName, tvCredit, tvDebit, tvBalance;
        ImageView ivArrow;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAccountName = itemView.findViewById(R.id.tvAccountName);
            tvCredit = itemView.findViewById(R.id.tvCredit);
            tvDebit = itemView.findViewById(R.id.tvDebit);
            tvBalance = itemView.findViewById(R.id.tvBalance);
            ivArrow = itemView.findViewById(R.id.ivArrow);
        }
    }
} 
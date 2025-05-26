package com.hillal.hhhhhhh.ui.direct_statement;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.hillal.hhhhhhh.R;
import java.util.ArrayList;
import java.util.List;

public class DirectStatementAdapter extends RecyclerView.Adapter<DirectStatementAdapter.AccountViewHolder> {
    private List<AccountSummary> accounts = new ArrayList<>();
    private OnAccountClickListener listener;

    public interface OnAccountClickListener {
        void onAccountClick(AccountSummary account);
    }

    public void setOnAccountClickListener(OnAccountClickListener listener) {
        this.listener = listener;
    }

    public void setAccounts(List<AccountSummary> accounts) {
        this.accounts = accounts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_direct_account, parent, false);
        return new AccountViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AccountViewHolder holder, int position) {
        AccountSummary account = accounts.get(position);
        holder.bind(account);
    }

    @Override
    public int getItemCount() {
        return accounts.size();
    }

    class AccountViewHolder extends RecyclerView.ViewHolder {
        private TextView accountNameText;
        private TextView accountBalanceText;
        private TextView totalDebitText;
        private TextView totalCreditText;

        public AccountViewHolder(@NonNull View itemView) {
            super(itemView);
            accountNameText = itemView.findViewById(R.id.accountNameText);
            accountBalanceText = itemView.findViewById(R.id.accountBalanceText);
            totalDebitText = itemView.findViewById(R.id.totalDebitText);
            totalCreditText = itemView.findViewById(R.id.totalCreditText);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onAccountClick(accounts.get(position));
                }
            });
        }

        public void bind(AccountSummary account) {
            accountNameText.setText(account.getUserName());
            accountBalanceText.setText(String.format("الرصيد: %.2f", account.getBalance()));
            totalDebitText.setText(String.format("إجمالي المدفوعات: %.2f", account.getTotalDebits()));
            totalCreditText.setText(String.format("إجمالي الديون: %.2f", account.getTotalCredits()));
        }
    }

    public static class AccountSummary {
        private long userId;
        private String userName;
        private double balance;
        private double totalDebits;
        private double totalCredits;

        public long getUserId() {
            return userId;
        }

        public String getUserName() {
            return userName;
        }

        public double getBalance() {
            return balance;
        }

        public double getTotalDebits() {
            return totalDebits;
        }

        public double getTotalCredits() {
            return totalCredits;
        }
    }
} 
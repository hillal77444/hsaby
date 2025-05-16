package com.hillal.hhhhhhh.ui.transactions;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.model.Transaction;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

public class AccountPickerAdapter extends RecyclerView.Adapter<AccountPickerAdapter.AccountViewHolder> {
    public interface OnAccountClickListener {
        void onAccountClick(Account account);
    }
    private List<Account> accounts;
    private Map<Long, List<Transaction>> accountTransactions;
    private OnAccountClickListener listener;
    private Context context;

    public AccountPickerAdapter(Context context, List<Account> accounts, Map<Long, List<Transaction>> accountTransactions, OnAccountClickListener listener) {
        this.context = context;
        this.accounts = accounts;
        this.accountTransactions = accountTransactions;
        this.listener = listener;
    }

    public void updateList(List<Account> filtered) {
        this.accounts = filtered;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_account_picker, parent, false);
        return new AccountViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AccountViewHolder holder, int position) {
        Account account = accounts.get(position);
        holder.accountNameTextView.setText(account.getName());
        holder.balancesContainer.removeAllViews();
        List<Transaction> transactions = accountTransactions.get(account.getId());
        if (transactions != null && !transactions.isEmpty()) {
            Map<String, Double> currencyBalances = new HashMap<>();
            Map<String, String> currencyType = new HashMap<>();
            for (Transaction t : transactions) {
                String currency = t.getCurrency();
                double amount = t.getAmount();
                String type = t.getType();
                double prev = currencyBalances.getOrDefault(currency, 0.0);
                if (type.equals("عليه") || type.equalsIgnoreCase("debit")) {
                    prev -= amount;
                } else {
                    prev += amount;
                }
                currencyBalances.put(currency, prev);
            }
            for (String currency : currencyBalances.keySet()) {
                double balance = currencyBalances.get(currency);
                String label;
                if (balance > 0) {
                    label = context.getString(R.string.label_credit) + " " + String.format(Locale.US, "%.2f", balance) + " " + currency;
                } else if (balance < 0) {
                    label = context.getString(R.string.label_debit) + " " + String.format(Locale.US, "%.2f", Math.abs(balance)) + " " + currency;
                } else {
                    label = context.getString(R.string.label_zero_balance) + " " + currency;
                }
                TextView tv = new TextView(context);
                tv.setText(label);
                tv.setTextSize(15f);
                tv.setTextColor(context.getResources().getColor(R.color.text_secondary));
                holder.balancesContainer.addView(tv);
            }
        } else {
            TextView tv = new TextView(context);
            tv.setText(context.getString(R.string.label_zero_balance));
            tv.setTextSize(15f);
            tv.setTextColor(context.getResources().getColor(R.color.text_secondary));
            holder.balancesContainer.addView(tv);
        }
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onAccountClick(account);
        });
    }

    @Override
    public int getItemCount() {
        return accounts != null ? accounts.size() : 0;
    }

    static class AccountViewHolder extends RecyclerView.ViewHolder {
        TextView accountNameTextView;
        LinearLayout balancesContainer;
        public AccountViewHolder(@NonNull View itemView) {
            super(itemView);
            accountNameTextView = itemView.findViewById(R.id.accountNameTextView);
            balancesContainer = itemView.findViewById(R.id.balancesContainer);
        }
    }
} 
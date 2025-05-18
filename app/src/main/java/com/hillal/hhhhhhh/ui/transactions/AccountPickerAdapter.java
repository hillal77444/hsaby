package com.hillal.hhhhhhh.ui.transactions;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.RecyclerView;
import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.model.Transaction;
import com.hillal.hhhhhhh.data.repository.TransactionRepository;
import com.hillal.hhhhhhh.data.room.AppDatabase;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;

public class AccountPickerAdapter extends RecyclerView.Adapter<AccountPickerAdapter.AccountViewHolder> {
    public interface OnAccountClickListener {
        void onAccountClick(Account account);
    }
    private List<Account> accounts;
    private Map<Long, List<Transaction>> accountTransactions;
    private OnAccountClickListener listener;
    private Context context;
    private TransactionRepository transactionRepository;
    private LifecycleOwner lifecycleOwner;
    private Map<Long, Map<String, Double>> balancesMap = new HashMap<>();

    public AccountPickerAdapter(Context context, List<Account> accounts, Map<Long, List<Transaction>> accountTransactions, OnAccountClickListener listener) {
        this.context = context;
        this.accounts = accounts;
        this.accountTransactions = accountTransactions;
        this.listener = listener;
        this.transactionRepository = new TransactionRepository(((App) context.getApplicationContext()).getDatabase());
        if (context instanceof LifecycleOwner) {
            this.lifecycleOwner = (LifecycleOwner) context;
        } else {
            throw new IllegalArgumentException("Context must be a LifecycleOwner");
        }
    }

    public void updateList(List<Account> filtered) {
        this.accounts = filtered;
        notifyDataSetChanged();
    }

    public void setBalancesMap(Map<Long, Map<String, Double>> balancesMap) {
        this.balancesMap = balancesMap;
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
        holder.accountNameTextView.setText(account.getName() != null ? account.getName() : "بدون اسم");
        holder.accountNameTextView.setTextColor(0xFF222222);
        String icon = account.getName() != null && !account.getName().isEmpty() ? account.getName().substring(0, 1) : "?";
        holder.accountIconTextView.setText(icon);
        holder.balancesContainer.removeAllViews();
        Map<String, Double> currencyBalances = balancesMap.get(account.getId());
        if (currencyBalances != null && !currencyBalances.isEmpty()) {
            for (String currency : currencyBalances.keySet()) {
                double balance = currencyBalances.get(currency);
                String label;
                int bgColor;
                int textColor;
                if (balance > 0) {
                    label = "له " + String.format(Locale.US, "%.2f", balance) + " " + currency;
                    bgColor = 0xFFE8F5E9;
                    textColor = 0xFF388E3C;
                } else if (balance < 0) {
                    label = "عليه " + String.format(Locale.US, "%.2f", Math.abs(balance)) + " " + currency;
                    bgColor = 0xFFFFEBEE;
                    textColor = 0xFFD32F2F;
                } else {
                    label = "رصيد صفر " + currency;
                    bgColor = 0xFFECEFF1;
                    textColor = 0xFF607D8B;
                }
                TextView tv = new TextView(context);
                tv.setText(label);
                tv.setTextSize(14f);
                tv.setTextColor(textColor);
                tv.setPadding(18, 6, 18, 6);
                tv.setBackgroundResource(R.drawable.bg_balance_chip);
                tv.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bgColor));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMarginEnd(12);
                tv.setLayoutParams(params);
                holder.balancesContainer.addView(tv);
            }
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
        TextView accountIconTextView;
        public AccountViewHolder(@NonNull View itemView) {
            super(itemView);
            accountNameTextView = itemView.findViewById(R.id.accountNameTextView);
            balancesContainer = itemView.findViewById(R.id.balancesContainer);
            accountIconTextView = itemView.findViewById(R.id.accountIconTextView);
        }
    }
} 
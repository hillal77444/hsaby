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

    public AccountPickerAdapter(Context context, List<Account> accounts, Map<Long, List<Transaction>> accountTransactions, OnAccountClickListener listener) {
        this.context = context;
        this.accounts = accounts;
        this.accountTransactions = accountTransactions;
        this.listener = listener;
        this.transactionRepository = new TransactionRepository((android.app.Application) context.getApplicationContext());
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
        String icon = account.getName() != null && !account.getName().isEmpty() ? account.getName().substring(0, 1) : "?";
        holder.accountIconTextView.setText(icon);
        holder.balancesContainer.removeAllViews();
        List<Transaction> transactions = accountTransactions.get(account.getId());
        if (transactions != null && !transactions.isEmpty()) {
            HashSet<String> currencies = new HashSet<>();
            for (Transaction t : transactions) {
                if (t.getCurrency() != null) currencies.add(t.getCurrency());
            }
            for (String currency : currencies) {
                LiveData<Double> balanceLiveData = transactionRepository.getBalanceUntilDate(account.getId(), System.currentTimeMillis(), currency);
                balanceLiveData.observe(lifecycleOwner, balance -> {
                    String label;
                    int color;
                    if (balance != null && balance > 0) {
                        label = context.getString(R.string.label_credit) + " " + String.format(Locale.US, "%.2f", balance) + " " + currency;
                        color = context.getResources().getColor(R.color.green_700);
                    } else if (balance != null && balance < 0) {
                        label = context.getString(R.string.label_debit) + " " + String.format(Locale.US, "%.2f", Math.abs(balance)) + " " + currency;
                        color = context.getResources().getColor(R.color.red_700);
                    } else {
                        label = context.getString(R.string.label_zero_balance) + " " + currency;
                        color = context.getResources().getColor(R.color.text_secondary);
                    }
                    TextView tv = new TextView(context);
                    tv.setText(label);
                    tv.setTextSize(15f);
                    tv.setTextColor(color);
                    holder.balancesContainer.addView(tv);
                });
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
        TextView accountIconTextView;
        public AccountViewHolder(@NonNull View itemView) {
            super(itemView);
            accountNameTextView = itemView.findViewById(R.id.accountNameTextView);
            balancesContainer = itemView.findViewById(R.id.balancesContainer);
            accountIconTextView = itemView.findViewById(R.id.accountIconTextView);
        }
    }
} 
package com.hillal.acc.ui.reports;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.hillal.acc.R;
import com.hillal.acc.data.model.Transaction;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Date;

public class TransactionsAdapter extends ListAdapter<Transaction, TransactionsAdapter.TransactionViewHolder> {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public TransactionsAdapter() {
        super(new DiffUtil.ItemCallback<Transaction>() {
            @Override
            public boolean areItemsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
                return oldItem.getId() == newItem.getId();
            }

            @Override
            public boolean areContentsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
                return oldItem.equals(newItem);
            }
        });
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = getItem(position);
        holder.bind(transaction);
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final TextView dateTextView;
        private final TextView amountTextView;
        private final TextView descriptionTextView;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            dateTextView = itemView.findViewById(R.id.transactionDate);
            amountTextView = itemView.findViewById(R.id.transactionAmount);
            descriptionTextView = itemView.findViewById(R.id.transactionDescription);
        }

        public void bind(Transaction transaction) {
            dateTextView.setText(dateFormat.format(new Date(transaction.getTransactionDate())));
            descriptionTextView.setText(transaction.getDescription());
            
            double amount = transaction.getAmount();
            String type = transaction.getType() != null ? transaction.getType().trim() : "";

            String amountStr;
            if (Math.abs(amount - Math.round(amount)) < 0.00001) {
                amountStr = String.format("%.0f", amount);
            } else {
                amountStr = String.format("%.2f", amount);
            }

            amountTextView.setText(amountStr + " " + transaction.getCurrency());

            if ((type.equals("عليه") || type.equalsIgnoreCase("debit")) && amount != 0) {
                itemView.setActivated(true);
                itemView.setSelected(false);
            } else if ((type.equals("له") || type.equalsIgnoreCase("credit")) && amount != 0) {
                itemView.setActivated(false);
                itemView.setSelected(true);
            } else {
                itemView.setActivated(false);
                itemView.setSelected(false);
            }
        }
    }
} 
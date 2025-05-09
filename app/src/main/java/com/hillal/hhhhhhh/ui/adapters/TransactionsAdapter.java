package com.hillal.hhhhhhh.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.model.Transaction;
import java.text.SimpleDateFormat;
import java.util.Locale;

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
        private final TextView debitTextView;
        private final TextView creditTextView;
        private final TextView descriptionTextView;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            dateTextView = itemView.findViewById(R.id.transactionDate);
            debitTextView = itemView.findViewById(R.id.transactionDebit);
            creditTextView = itemView.findViewById(R.id.transactionCredit);
            descriptionTextView = itemView.findViewById(R.id.transactionDescription);
        }

        public void bind(Transaction transaction) {
            dateTextView.setText(dateFormat.format(transaction.getDate()));
            descriptionTextView.setText(transaction.getDescription());
            if (transaction.getType().equals("عليه") || transaction.getType().equalsIgnoreCase("debit")) {
                debitTextView.setText(String.format("%.2f %s", transaction.getAmount(), transaction.getCurrency()));
                creditTextView.setText(String.format("0 %s", transaction.getCurrency()));
            } else {
                debitTextView.setText(String.format("0 %s", transaction.getCurrency()));
                creditTextView.setText(String.format("%.2f %s", transaction.getAmount(), transaction.getCurrency()));
            }
        }
    }
} 
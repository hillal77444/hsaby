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
import java.util.Date;
import java.util.Locale;

public class TransactionsAdapter extends ListAdapter<Transaction, TransactionsAdapter.TransactionViewHolder> {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

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
        private final TextView notesTextView;

        TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            dateTextView = itemView.findViewById(R.id.transaction_date);
            amountTextView = itemView.findViewById(R.id.transaction_amount);
            notesTextView = itemView.findViewById(R.id.transaction_notes);
        }

        void bind(Transaction transaction) {
            dateTextView.setText(DATE_FORMAT.format(new Date(transaction.getDate())));
            String amount = String.format(Locale.getDefault(), "%.2f", transaction.getAmount());
            amountTextView.setText(amount);
            amountTextView.setTextColor(itemView.getContext().getColor(
                transaction.isDebit() ? R.color.red : R.color.green
            ));
            notesTextView.setText(transaction.getNotes());
        }
    }
} 
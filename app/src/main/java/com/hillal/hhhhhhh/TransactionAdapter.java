package com.hillal.hhhhhhh;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {
    private List<Transaction> transactions;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction currentTransaction = transactions.get(position);
        holder.descriptionTextView.setText(currentTransaction.getDescription());
        holder.amountTextView.setText(String.format("%.2f", currentTransaction.getAmount()));
        holder.dateTextView.setText(dateFormat.format(currentTransaction.getDate()));
        
        holder.amountTextView.setTextColor(currentTransaction.isCredit() ?
                holder.itemView.getContext().getResources().getColor(android.R.color.holo_green_dark) :
                holder.itemView.getContext().getResources().getColor(android.R.color.holo_red_dark));
    }

    @Override
    public int getItemCount() {
        return transactions != null ? transactions.size() : 0;
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        private TextView descriptionTextView;
        private TextView amountTextView;
        private TextView dateTextView;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            descriptionTextView = itemView.findViewById(R.id.transaction_description);
            amountTextView = itemView.findViewById(R.id.transaction_amount);
            dateTextView = itemView.findViewById(R.id.transaction_date);
        }
    }
} 
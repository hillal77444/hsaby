package com.hillal.hhhhhhh.ui.reports;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.model.Transaction;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {
    private final List<Transaction> transactions;
    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public ReportAdapter(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);
        holder.bind(transaction);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public void updateTransactions(List<Transaction> newTransactions) {
        this.transactions = newTransactions;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView typeTextView;
        private final TextView amountTextView;
        private final TextView descriptionTextView;
        private final TextView dateTextView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            typeTextView = itemView.findViewById(R.id.transactionType);
            amountTextView = itemView.findViewById(R.id.transactionAmount);
            descriptionTextView = itemView.findViewById(R.id.transactionDescription);
            dateTextView = itemView.findViewById(R.id.transactionDate);
        }

        void bind(Transaction transaction) {
            typeTextView.setText(transaction.getType());
            amountTextView.setText(String.format("%.2f %s", 
                    transaction.getAmount(), transaction.getCurrency()));
            descriptionTextView.setText(transaction.getDescription());
            dateTextView.setText(DATE_FORMAT.format(transaction.getDate()));
            
            // Set color based on transaction type
            int colorResId = transaction.getType().equals("مدين") ? 
                    R.color.debit_color : R.color.credit_color;
            typeTextView.setTextColor(
                    itemView.getContext().getResources().getColor(colorResId, null));
        }
    }
} 
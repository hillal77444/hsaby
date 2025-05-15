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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {
    private List<Transaction> transactions = new ArrayList<>();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

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

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView dateTextView;
        private final TextView amountTextView;
        private final TextView descriptionTextView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            dateTextView = itemView.findViewById(R.id.transactionDate);
            amountTextView = itemView.findViewById(R.id.transactionAmount);
            descriptionTextView = itemView.findViewById(R.id.transactionDescription);
        }

        void bind(Transaction transaction) {
            dateTextView.setText(DATE_FORMAT.format(transaction.getDate()));
            descriptionTextView.setText(transaction.getDescription());
            
            double amount = transaction.getAmount();
            String type = transaction.getType() != null ? transaction.getType().trim() : "";

            String amountStr;
            if (Math.abs(amount - Math.round(amount)) < 0.00001) {
                amountStr = String.format("%.0f", amount);
            } else {
                amountStr = String.format("%.2f", amount);
            }

            if ((type.equals("عليه") || type.equalsIgnoreCase("debit")) && amount != 0) {
                amountTextView.setText(amountStr + " " + transaction.getCurrency());
                amountTextView.setTextColor(itemView.getContext().getResources().getColor(R.color.debit_color));
                itemView.setBackgroundColor(itemView.getContext().getResources().getColor(R.color.red_100));
            } else if ((type.equals("له") || type.equalsIgnoreCase("credit")) && amount != 0) {
                amountTextView.setText(amountStr + " " + transaction.getCurrency());
                amountTextView.setTextColor(itemView.getContext().getResources().getColor(R.color.credit_color));
                itemView.setBackgroundColor(itemView.getContext().getResources().getColor(R.color.green_100));
            } else {
                amountTextView.setText("");
                amountTextView.setTextColor(itemView.getContext().getResources().getColor(R.color.text_primary));
                itemView.setBackgroundColor(itemView.getContext().getResources().getColor(R.color.white));
            }
        }
    }
} 
package com.hillal.hhhhhhh.ui.reports;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.entities.Transaction;

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
        private final TextView typeTextView;
        private final TextView amountTextView;
        private final TextView descriptionTextView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            dateTextView = itemView.findViewById(R.id.transaction_date);
            typeTextView = itemView.findViewById(R.id.transaction_type);
            amountTextView = itemView.findViewById(R.id.transaction_amount);
            descriptionTextView = itemView.findViewById(R.id.transaction_description);
        }

        void bind(Transaction transaction) {
            dateTextView.setText(DATE_FORMAT.format(transaction.getDate()));
            typeTextView.setText(transaction.getType());
            amountTextView.setText(String.format("%.2f", transaction.getAmount()));
            descriptionTextView.setText(transaction.getDescription());
            
            // Set color based on transaction type
            int colorResId = transaction.getType().equals("مدين") ? 
                    R.color.debit_color : R.color.credit_color;
            typeTextView.setTextColor(
                    itemView.getContext().getResources().getColor(colorResId, null));
        }
    }
} 
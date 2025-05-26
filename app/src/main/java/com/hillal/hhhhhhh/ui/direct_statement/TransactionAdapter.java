package com.hillal.hhhhhhh.ui.direct_statement;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.model.TransactionResponse.Transaction;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {
    private List<Transaction> transactions = new ArrayList<>();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

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
        holder.date.setText(dateFormat.format(transaction.getDate()));
        holder.amount.setText(String.valueOf(transaction.getAmount()));
        holder.type.setText(transaction.getType());
        holder.description.setText(transaction.getDescription());
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
        TextView date;
        TextView amount;
        TextView type;
        TextView description;

        ViewHolder(View view) {
            super(view);
            date = view.findViewById(R.id.date);
            amount = view.findViewById(R.id.amount);
            type = view.findViewById(R.id.type);
            description = view.findViewById(R.id.description);
        }
    }
} 
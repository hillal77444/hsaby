package com.hillal.hhhhhhh.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.model.Transaction;
import java.util.ArrayList;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {
    private List<Transaction> transactions = new ArrayList<>();

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
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

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        private TextView amountText;
        private TextView descriptionText;
        private TextView dateText;
        private TextView statusText;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            amountText = itemView.findViewById(R.id.amount_text);
            descriptionText = itemView.findViewById(R.id.description_text);
            dateText = itemView.findViewById(R.id.date_text);
            statusText = itemView.findViewById(R.id.status_text);
        }

        public void bind(Transaction transaction) {
            amountText.setText(String.valueOf(transaction.getAmount()));
            descriptionText.setText(transaction.getDescription());
            dateText.setText(transaction.getDate().toString());
            
            // عرض حالة المزامنة
            if (transaction.getServerId() != null) {
                statusText.setText("مزامن");
                statusText.setTextColor(itemView.getContext().getResources().getColor(R.color.synced));
            } else {
                statusText.setText("في الانتظار");
                statusText.setTextColor(itemView.getContext().getResources().getColor(R.color.pending));
            }
        }
    }
} 
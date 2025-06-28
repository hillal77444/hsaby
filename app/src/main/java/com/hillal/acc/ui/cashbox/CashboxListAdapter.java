package com.hillal.acc.ui.cashbox;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.hillal.acc.R;
import com.hillal.acc.data.entities.Cashbox;

public class CashboxListAdapter extends ListAdapter<Cashbox, CashboxListAdapter.CashboxViewHolder> {
    public CashboxListAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public CashboxViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cashbox, parent, false);
        return new CashboxViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CashboxViewHolder holder, int position) {
        Cashbox cashbox = getItem(position);
        holder.name.setText(cashbox.name);
        holder.createdAt.setText(cashbox.createdAt);
    }

    static class CashboxViewHolder extends RecyclerView.ViewHolder {
        TextView name, createdAt;
        CashboxViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.text_cashbox_name);
            createdAt = itemView.findViewById(R.id.text_cashbox_created_at);
        }
    }

    public static final DiffUtil.ItemCallback<Cashbox> DIFF_CALLBACK = new DiffUtil.ItemCallback<Cashbox>() {
        @Override
        public boolean areItemsTheSame(@NonNull Cashbox oldItem, @NonNull Cashbox newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Cashbox oldItem, @NonNull Cashbox newItem) {
            return oldItem.name.equals(newItem.name) &&
                    oldItem.createdAt.equals(newItem.createdAt);
        }
    };
} 
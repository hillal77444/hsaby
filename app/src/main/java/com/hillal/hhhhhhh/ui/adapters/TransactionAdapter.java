package com.hillal.hhhhhhh.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.model.Transaction;
import com.hillal.hhhhhhh.databinding.ItemTransactionBinding;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class TransactionAdapter extends ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder> {
    private OnItemClickListener onItemClickListener;
    private OnItemLongClickListener onItemLongClickListener;
    private Map<Long, Account> accountMap;

    public TransactionAdapter(@NonNull DiffUtil.ItemCallback<Transaction> diffCallback) {
        super(diffCallback);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.onItemLongClickListener = listener;
    }

    public void setAccountMap(Map<Long, Account> accountMap) {
        this.accountMap = accountMap;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTransactionBinding binding = ItemTransactionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new TransactionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final ItemTransactionBinding binding;
        private final SimpleDateFormat dateFormat;

        TransactionViewHolder(ItemTransactionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onItemClickListener != null) {
                    onItemClickListener.onItemClick(getItem(position));
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onItemLongClickListener != null) {
                    return onItemLongClickListener.onItemLongClick(getItem(position));
                }
                return false;
            });
        }

        void bind(Transaction transaction) {
            binding.transactionType.setText(transaction.getType());
            binding.transactionAmount.setText(String.format("%.2f %s", 
                    transaction.getAmount(), transaction.getCurrency()));
            binding.transactionDescription.setText(transaction.getDescription());
            binding.transactionDate.setText(dateFormat.format(transaction.getDate()));
            
            // Set color based on transaction type
            int colorResId = transaction.getType().equals("مدين") ? 
                    R.color.debit_color : R.color.credit_color;
            binding.transactionType.setTextColor(
                    itemView.getContext().getResources().getColor(colorResId, null));

            // عرض اسم الحساب
            final String accountName;
            final String phoneNumber;
            if (accountMap != null && accountMap.containsKey(transaction.getAccountId())) {
                Account account = accountMap.get(transaction.getAccountId());
                accountName = account.getName();
                phoneNumber = account.getPhoneNumber();
            } else {
                accountName = "";
                phoneNumber = "";
            }
            binding.accountNameTextView.setText(accountName);

            // زر إرسال واتساب
            binding.btnSendWhatsApp.setOnClickListener(v -> {
                double balanceAfter = calculateBalance(transaction.getAccountId(), transaction.getId());
                String message = buildWhatsAppMessage(accountName, transaction, balanceAfter);
                sendWhatsAppMessage(itemView.getContext(), phoneNumber, message);
            });
        }

        // حساب الرصيد بعد القيد (يمكنك تحسينه حسب منطقك)
        private double calculateBalance(long accountId, long transactionId) {
            double balance = 0;
            for (int i = 0; i < getItemCount(); i++) {
                Transaction t = getItem(i);
                if (t.getAccountId() == accountId) {
                    if (t.getType().equals("credit")) {
                        balance += t.getAmount();
                    } else {
                        balance -= t.getAmount();
                    }
                    if (t.getId() == transactionId) break;
                }
            }
            return balance;
        }

        private String buildWhatsAppMessage(String accountName, Transaction transaction, double balanceAfter) {
            String amount = String.format(Locale.getDefault(), "%.2f %s", transaction.getAmount(), transaction.getCurrency());
            String type = transaction.getType().equals("debit") ? "دين" : "دائن";
            String actionText = transaction.getType().equals("debit") ? "على حسابكم" : "إلى حسابكم";
            String balanceText = balanceAfter < 0 ? "عليكم: " : "لكم: ";
            String balanceAmount = String.format(Locale.getDefault(), "%.2f %s", Math.abs(balanceAfter), transaction.getCurrency());
            return "السيد/ " + accountName + "\n"
                 + "نود اشعاركم أنه تم قيد مبلغ: \n"
                 + amount + " " + actionText + "\n"
                 + "البيان/ " + type + "\n"
                 + "رصيدكم/ " + balanceText + balanceAmount;
        }

        private void sendWhatsAppMessage(Context context, String phoneNumber, String message) {
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                // يمكنك عرض رسالة خطأ هنا
                return;
            }
            String url = "https://wa.me/" + phoneNumber + "?text=" + Uri.encode(message);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            context.startActivity(intent);
        }
    }

    public static class TransactionDiffCallback extends DiffUtil.ItemCallback<Transaction> {
        @Override
        public boolean areItemsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
            return oldItem.equals(newItem);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Transaction transaction);
    }

    public interface OnItemLongClickListener {
        boolean onItemLongClick(Transaction transaction);
    }
} 
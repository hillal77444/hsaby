package com.hillal.acc.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.hillal.acc.R;
import com.hillal.acc.data.model.Account;
import com.hillal.acc.data.model.Transaction;
import com.hillal.acc.databinding.ItemTransactionBinding;
import com.hillal.acc.data.repository.TransactionRepository;
import com.hillal.acc.viewmodel.AccountViewModel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import com.hillal.acc.App;
import com.google.android.material.card.MaterialCardView;

public class TransactionAdapter extends ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder> {
    private OnItemClickListener onItemClickListener;
    private OnItemLongClickListener onItemLongClickListener;
    private OnDeleteClickListener onDeleteClickListener;
    private OnEditClickListener onEditClickListener;
    private OnWhatsAppClickListener onWhatsAppClickListener;
    private Map<Long, Account> accountMap;
    private final TransactionRepository transactionRepository;
    private final LifecycleOwner lifecycleOwner;
    private final AccountViewModel accountViewModel;

    public TransactionAdapter(@NonNull DiffUtil.ItemCallback<Transaction> diffCallback, Context context, LifecycleOwner lifecycleOwner) {
        super(diffCallback);
        this.transactionRepository = new TransactionRepository(((App) context.getApplicationContext()).getDatabase());
        this.lifecycleOwner = lifecycleOwner;
        this.accountViewModel = new ViewModelProvider((ViewModelStoreOwner) context).get(AccountViewModel.class);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(Transaction transaction);
    }

    public interface OnEditClickListener {
        void onEditClick(Transaction transaction);
    }

    public interface OnWhatsAppClickListener {
        void onWhatsAppClick(Transaction transaction, String phoneNumber);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.onItemLongClickListener = listener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.onDeleteClickListener = listener;
    }

    public void setOnEditClickListener(OnEditClickListener listener) {
        this.onEditClickListener = listener;
    }

    public void setOnWhatsAppClickListener(OnWhatsAppClickListener listener) {
        this.onWhatsAppClickListener = listener;
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
        Transaction transaction = getItem(position);
        if (transaction != null) {
            holder.bind(transaction);
            
            // إضافة مستمع النقر الطويل
            holder.itemView.setOnLongClickListener(v -> {
                if (onItemLongClickListener != null) {
                    onItemLongClickListener.onItemLongClick(transaction);
                    return true;
                }
                return false;
            });

            // إضافة مستمع النقر لزر الحذف
            holder.binding.deleteButton.setOnClickListener(v -> {
                if (onDeleteClickListener != null) {
                    onDeleteClickListener.onDeleteClick(transaction);
                }
            });

            // إضافة مستمع النقر لزر التعديل
            holder.binding.editButton.setOnClickListener(v -> {
                if (onEditClickListener != null) {
                    onEditClickListener.onEditClick(transaction);
                }
            });

            // إضافة مستمع النقر لزر واتساب
            holder.binding.whatsappButton.setOnClickListener(v -> {
                if (onWhatsAppClickListener != null && accountMap != null && accountMap.containsKey(transaction.getAccountId())) {
                    String phoneNumber = accountMap.get(transaction.getAccountId()).getPhoneNumber();
                    onWhatsAppClickListener.onWhatsAppClick(transaction, phoneNumber);
                }
            });
        }
    }

    class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final ItemTransactionBinding binding;
        private final SimpleDateFormat dateFormat;
        private final TransactionRepository transactionRepository;
        private final AccountViewModel accountViewModel;

        TransactionViewHolder(ItemTransactionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
            this.transactionRepository = new TransactionRepository(((App) binding.getRoot().getContext().getApplicationContext()).getDatabase());
            this.accountViewModel = new ViewModelProvider((ViewModelStoreOwner) binding.getRoot().getContext()).get(AccountViewModel.class);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onItemClickListener != null) {
                    onItemClickListener.onItemClick(getItem(position));
                }
            });
        }

        void bind(Transaction transaction) {
            // ربط اسم الحساب
            final String accountName;
            final String phoneNumber;
            if (accountMap != null && accountMap.containsKey(transaction.getAccountId())) {
                Account account = accountMap.get(transaction.getAccountId());
                accountName = account != null && account.getName() != null ? account.getName() : "حساب غير معروف";
                phoneNumber = account != null ? account.getPhoneNumber() : "";
            } else {
                accountName = "جاري التحميل...";
                phoneNumber = "";
                // محاولة تحميل بيانات الحساب إذا لم تكن موجودة
                if (accountViewModel != null) {
                    accountViewModel.getAccountById(transaction.getAccountId()).observe(lifecycleOwner, account -> {
                        if (account != null) {
                            binding.accountNameTextView.setText(account.getName());
                        }
                    });
                }
            }
            binding.accountNameTextView.setText(accountName);

            // ربط التاريخ بالإنجليزي
            SimpleDateFormat dateFormatEn = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
            binding.transactionDate.setText(dateFormatEn.format(new Date(transaction.getTransactionDate())));

            // ربط البيان
            binding.transactionDescription.setText(transaction.getDescription());

            double amount = transaction.getAmount();
            String type = transaction.getType() != null ? transaction.getType().trim() : "";
            

            // تحديد نوع المعاملة وتغيير لون البطاقة والمبلغ
            String amountStr;
            if (Math.abs(amount - Math.round(amount)) < 0.00001) {
                amountStr = String.format(Locale.US, "%.0f", amount);
            } else {
                amountStr = String.format(Locale.US, "%.2f", amount);
            }
            binding.transactionAmount.setText(amountStr + " " + transaction.getCurrency());

            // تغيير خلفية الـ LinearLayout الداخلي ولون النص حسب نوع المعاملة
            Context context = binding.getRoot().getContext();
            if ((type.equals("عليه") || type.equalsIgnoreCase("debit")) && amount != 0) {
                binding.innerLayout.setBackgroundResource(R.drawable.rounded_inner_background_debit);
                binding.transactionAmount.setTextColor(context.getResources().getColor(R.color.debit_color));
            } else if ((type.equals("له") || type.equalsIgnoreCase("credit")) && amount != 0) {
                binding.innerLayout.setBackgroundResource(R.drawable.rounded_inner_background_credit);
                binding.transactionAmount.setTextColor(context.getResources().getColor(R.color.credit_color));
            } else {
                binding.innerLayout.setBackgroundResource(R.drawable.rounded_inner_background_default);
                binding.transactionAmount.setTextColor(context.getResources().getColor(R.color.text_primary));
            }
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

    public Map<Long, Account> getAccountMap() {
        return accountMap;
    }
} 
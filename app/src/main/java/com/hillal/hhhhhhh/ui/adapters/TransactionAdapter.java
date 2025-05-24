package com.hillal.hhhhhhh.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.model.Transaction;
import com.hillal.hhhhhhh.databinding.ItemTransactionBinding;
import com.hillal.hhhhhhh.data.repository.TransactionRepository;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import com.hillal.hhhhhhh.App;

public class TransactionAdapter extends ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder> {
    private OnItemClickListener onItemClickListener;
    private OnItemLongClickListener onItemLongClickListener;
    private OnDeleteClickListener onDeleteClickListener;
    private OnEditClickListener onEditClickListener;
    private OnWhatsAppClickListener onWhatsAppClickListener;
    private Map<Long, Account> accountMap;
    private final TransactionRepository transactionRepository;
    private final LifecycleOwner lifecycleOwner;

    public TransactionAdapter(@NonNull DiffUtil.ItemCallback<Transaction> diffCallback, Context context, LifecycleOwner lifecycleOwner) {
        super(diffCallback);
        this.transactionRepository = new TransactionRepository(((App) context.getApplicationContext()).getDatabase());
        this.lifecycleOwner = lifecycleOwner;
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

            // إضافة مستمع النقر لزر الإرسال
            holder.binding.btnSendWhatsApp.setOnClickListener(v -> {
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

        TransactionViewHolder(ItemTransactionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
            this.transactionRepository = new TransactionRepository(((App) binding.getRoot().getContext().getApplicationContext()).getDatabase());

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
                if (transactionRepository != null) {
                    transactionRepository.getAccountById(transaction.getAccountId()).observe(lifecycleOwner, account -> {
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

            if ((type.equals("عليه") || type.equalsIgnoreCase("debit")) && amount != 0) {
                binding.transactionAmount.setText(String.format(Locale.US, "%.2f %s", amount, transaction.getCurrency()));
                binding.transactionAmount.setTextColor(itemView.getContext().getResources().getColor(R.color.debit_color));
                binding.getRoot().setBackgroundColor(itemView.getContext().getResources().getColor(R.color.red_100));
            } else if ((type.equals("له") || type.equalsIgnoreCase("credit")) && amount != 0) {
                binding.transactionAmount.setText(String.format(Locale.US, "%.2f %s", amount, transaction.getCurrency()));
                binding.transactionAmount.setTextColor(itemView.getContext().getResources().getColor(R.color.credit_color));
                binding.getRoot().setBackgroundColor(itemView.getContext().getResources().getColor(R.color.green_100));
            } else {
                binding.transactionAmount.setText("");
                binding.transactionAmount.setTextColor(itemView.getContext().getResources().getColor(R.color.text_primary));
                binding.getRoot().setBackgroundColor(itemView.getContext().getResources().getColor(R.color.white));
            }
        }

        private String buildWhatsAppMessage(String accountName, Transaction transaction, double balanceAfter) {
            // جميع الأرقام بالإنجليزي
            String amount = String.format(Locale.US, "%.2f %s", transaction.getAmount(), transaction.getCurrency());
            String type = transaction.getType(); // استخدام نوع القيد مباشرة
            String actionText = type.equals("عليه") ? "على حسابكم" : "إلى حسابكم";
            String balanceText = balanceAfter < 0 ? "عليكم: " : "لكم: ";
            String balanceAmount = String.format(Locale.US, "%.2f %s", Math.abs(balanceAfter), transaction.getCurrency());
            String description = transaction.getDescription();

            return "السيد/ " + accountName + "\n"
                 + "نود اشعاركم أنه تم قيد مبلغ: \n"
                 + amount + " " + actionText + "\n"
                 + "البيان/ " + description + "\n"
                 + "رصيدكم/ " + balanceText + balanceAmount;
        }

        private void sendWhatsAppMessage(Context context, String phoneNumber, String message) {
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                Toast.makeText(context, "رقم الهاتف غير متوفر لهذا الحساب", Toast.LENGTH_SHORT).show();
                return;
            }
            // إضافة مفتاح الدولة تلقائياً إذا لم يكن موجوداً
            String normalizedPhone = phoneNumber.trim();
            if (normalizedPhone.startsWith("0")) {
                normalizedPhone = "967" + normalizedPhone.substring(1);
            } else if (!normalizedPhone.startsWith("967") && !normalizedPhone.startsWith("00")) {
                normalizedPhone = "967" + normalizedPhone;
            }
            String url = "https://wa.me/" + normalizedPhone + "?text=" + Uri.encode(message);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            intent.setPackage("com.whatsapp");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                context.startActivity(intent);
            } catch (Exception e) {
                // إذا لم يكن واتساب العادي مثبت، جرب واتساب الأعمال
                intent.setPackage("com.whatsapp.w4b");
                try {
                    context.startActivity(intent);
                } catch (Exception ex) {
                    Toast.makeText(context, "واتساب غير مثبت على الجهاز", Toast.LENGTH_SHORT).show();
                }
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
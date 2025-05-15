package com.hillal.hhhhhhh.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

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
            // ربط اسم الحساب
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

            // ربط التاريخ بالإنجليزي
            SimpleDateFormat dateFormatEn = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
            binding.transactionDate.setText(dateFormatEn.format(transaction.getDate()));

            // ربط البيان
            binding.transactionDescription.setText(transaction.getDescription());

            double amount = transaction.getAmount();
            String type = transaction.getType() != null ? transaction.getType().trim() : "";

            if ((type.equals("عليه") || type.equalsIgnoreCase("debit")) && amount != 0) {
                binding.transactionDebit.setText(String.format(Locale.US, "%.2f \n %s", amount, transaction.getCurrency()));
                binding.transactionCredit.setText("0");
                binding.getRoot().setBackgroundColor(itemView.getContext().getResources().getColor(R.color.red_100));
            } else if ((type.equals("له") || type.equalsIgnoreCase("credit")) && amount != 0) {
                binding.transactionDebit.setText("0");
                binding.transactionCredit.setText(String.format(Locale.US, "%.2f\n %s", amount, transaction.getCurrency()));
                binding.getRoot().setBackgroundColor(itemView.getContext().getResources().getColor(R.color.green_100));
            } else {
                binding.transactionDebit.setText("0");
                binding.transactionCredit.setText("0");
                binding.getRoot().setBackgroundColor(itemView.getContext().getResources().getColor(R.color.white));
            }

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
            // تأكد من الترتيب من الأقدم إلى الأحدث
            List<Transaction> sortedList = new ArrayList<>();
            for (int i = 0; i < getItemCount(); i++) {
                Transaction t = getItem(i);
                if (t.getAccountId() == accountId) {
                    sortedList.add(t);
                }
            }
            Collections.sort(sortedList, Comparator.comparing(Transaction::getDate));
            for (Transaction t : sortedList) {
                if (t.getType().equals("عليه") || t.getType().equalsIgnoreCase("debit")) {
                    balance -= t.getAmount();
                } else {
                    balance += t.getAmount();
                }
                if (t.getId() == transactionId) break;
            }
            return balance;
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
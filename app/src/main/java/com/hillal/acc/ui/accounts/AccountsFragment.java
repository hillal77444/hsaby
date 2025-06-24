package com.hillal.acc.ui.accounts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.TextView;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.LifecycleOwner;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.hillal.acc.R;
import com.hillal.acc.data.model.Account;
import com.hillal.acc.data.model.Transaction;
import com.hillal.acc.viewmodel.AccountViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;

public class AccountsFragment extends Fragment {
    private AccountViewModel accountViewModel;
    private RecyclerView accountsRecyclerView;
    private AccountsAdapter accountsAdapter;
    private TextInputEditText searchEditText;
    private MaterialButton filterButton;
    private MaterialButton sortButton;
    private TextView totalAccountsText;
    private TextView activeAccountsText;
    private Map<Long, Double> accountBalances = new HashMap<>();
    private boolean isAscendingSort = true; // true = من الأصغر إلى الأكبر، false = من الأكبر إلى الأصغر
    private String currentSortType = "balance"; // balance, name, date

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_accounts, container, false);

        // Initialize views
        accountsRecyclerView = root.findViewById(R.id.accounts_list);
        searchEditText = root.findViewById(R.id.search_edit_text);
        filterButton = root.findViewById(R.id.filterButton);
        sortButton = root.findViewById(R.id.sortButton);
        totalAccountsText = root.findViewById(R.id.totalAccountsText);
        activeAccountsText = root.findViewById(R.id.activeAccountsText);
        FloatingActionButton addAccountButton = root.findViewById(R.id.fab_add_account);

        // Initialize ViewModel
        accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);

        // Setup RecyclerView
        accountsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        accountsAdapter = new AccountsAdapter(new ArrayList<>(), accountViewModel, getViewLifecycleOwner());
        accountsRecyclerView.setAdapter(accountsAdapter);

        // قائمة الحسابات الحالية
        final List<Account>[] currentAccounts = new List[]{new ArrayList<>()};

        // Observe accounts data
        accountViewModel.getAllAccounts().observe(getViewLifecycleOwner(), accounts -> {
            currentAccounts[0] = new ArrayList<>(accounts);
            
            // تحديث الإحصائيات
            totalAccountsText.setText(String.valueOf(accounts.size()));
            long activeCount = accounts.stream().filter(Account::isWhatsappEnabled).count();
            activeAccountsText.setText(String.valueOf(activeCount));
            
            // تحديث الأرصدة
            for (Account account : accounts) {
                accountViewModel.getAccountBalanceYemeni(account.getId()).observe(getViewLifecycleOwner(), balance -> {
                    accountBalances.put(account.getId(), balance != null ? balance : 0.0);
                });
            }
            accountsAdapter.updateAccounts(accounts);
        });

        // Setup search functionality
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString();
                if (!query.isEmpty()) {
                    accountViewModel.searchAccounts(query).observe(getViewLifecycleOwner(), accounts -> {
                        currentAccounts[0] = new ArrayList<>(accounts);
                        accountsAdapter.updateAccounts(accounts);
                    });
                } else {
                    accountViewModel.getAllAccounts().observe(getViewLifecycleOwner(), accounts -> {
                        currentAccounts[0] = new ArrayList<>(accounts);
                        accountsAdapter.updateAccounts(accounts);
                    });
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Setup filter button
        filterButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "سيتم إضافة خيارات التصفية قريباً", Toast.LENGTH_SHORT).show();
        });

        // Setup sort button
        sortButton.setOnClickListener(v -> {
            // منع انتشار الحدث
            v.setEnabled(false);
            
            List<Account> sorted = new ArrayList<>(currentAccounts[0]);
            
            // تبديل نوع الترتيب
            switch (currentSortType) {
                case "balance":
                    currentSortType = "name";
                    sortButton.setText("ترتيب (الاسم)");
                    break;
                case "name":
                    currentSortType = "number";
                    sortButton.setText("ترتيب (الرقم)");
                    break;
                case "number":
                    currentSortType = "date";
                    sortButton.setText("ترتيب (التاريخ)");
                    break;
                case "date":
                    currentSortType = "balance";
                    sortButton.setText("ترتيب (الرصيد)");
                    break;
            }
            
            // تطبيق الترتيب حسب النوع
            switch (currentSortType) {
                case "balance":
                    if (isAscendingSort) {
                        // ترتيب من الأصغر إلى الأكبر (رصيد)
                        Collections.sort(sorted, (a, b) -> Double.compare(
                            accountBalances.getOrDefault(a.getId(), 0.0),
                            accountBalances.getOrDefault(b.getId(), 0.0)
                        ));
                        Toast.makeText(getContext(), "تم الترتيب حسب الرصيد (من الأصغر إلى الأكبر)", Toast.LENGTH_SHORT).show();
                    } else {
                        // ترتيب من الأكبر إلى الأصغر (رصيد)
                        Collections.sort(sorted, (a, b) -> Double.compare(
                            accountBalances.getOrDefault(b.getId(), 0.0),
                            accountBalances.getOrDefault(a.getId(), 0.0)
                        ));
                        Toast.makeText(getContext(), "تم الترتيب حسب الرصيد (من الأكبر إلى الأصغر)", Toast.LENGTH_SHORT).show();
                    }
                    break;
                    
                case "name":
                    if (isAscendingSort) {
                        // ترتيب من أ إلى ي (اسم)
                        Collections.sort(sorted, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                        Toast.makeText(getContext(), "تم الترتيب حسب الاسم (أ → ي)", Toast.LENGTH_SHORT).show();
                    } else {
                        // ترتيب من ي إلى أ (اسم)
                        Collections.sort(sorted, (a, b) -> b.getName().compareToIgnoreCase(a.getName()));
                        Toast.makeText(getContext(), "تم الترتيب حسب الاسم (ي → أ)", Toast.LENGTH_SHORT).show();
                    }
                    break;
                    
                case "number":
                    if (isAscendingSort) {
                        // ترتيب من الأصغر إلى الأكبر (server_id)
                        Collections.sort(sorted, (a, b) -> Long.compare(a.getServerId(), b.getServerId()));
                        Toast.makeText(getContext(), "تم الترتيب حسب رقم الحساب (من الأصغر إلى الأكبر)", Toast.LENGTH_SHORT).show();
                    } else {
                        // ترتيب من الأكبر إلى الأصغر (server_id)
                        Collections.sort(sorted, (a, b) -> Long.compare(b.getServerId(), a.getServerId()));
                        Toast.makeText(getContext(), "تم الترتيب حسب رقم الحساب (من الأكبر إلى الأصغر)", Toast.LENGTH_SHORT).show();
                    }
                    break;
                    
                case "date":
                    if (isAscendingSort) {
                        // ترتيب من الأقدم إلى الأحدث (تاريخ)
                        Collections.sort(sorted, (a, b) -> Long.compare(a.getCreatedAt(), b.getCreatedAt()));
                        Toast.makeText(getContext(), "تم الترتيب حسب التاريخ (الأقدم أولاً)", Toast.LENGTH_SHORT).show();
                    } else {
                        // ترتيب من الأحدث إلى الأقدم (تاريخ)
                        Collections.sort(sorted, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                        Toast.makeText(getContext(), "تم الترتيب حسب التاريخ (الأحدث أولاً)", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
            
            isAscendingSort = !isAscendingSort; // تبديل اتجاه الترتيب
            accountsAdapter.updateAccounts(sorted);
            
            // إعادة تفعيل الزر بعد فترة قصيرة
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                sortButton.setEnabled(true);
            }, 500);
        });

        // Setup add account button
        addAccountButton.setOnClickListener(v -> {
            Navigation.findNavController(root).navigate(R.id.addAccountFragment);
        });

        return root;
    }

    private static class AccountsAdapter extends RecyclerView.Adapter<AccountsAdapter.ViewHolder> {
        private List<Account> accounts;
        private AccountViewModel accountViewModel;
        private LifecycleOwner lifecycleOwner;

        public AccountsAdapter(List<Account> accounts, AccountViewModel viewModel, LifecycleOwner owner) {
            this.accounts = accounts;
            this.accountViewModel = viewModel;
            this.lifecycleOwner = owner;
        }

        public void updateAccounts(List<Account> newAccounts) {
            this.accounts = newAccounts;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_account, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Account account = accounts.get(position);
            holder.accountName.setText(account.getName());
            holder.phone.setText(account.getPhoneNumber());
            
            // عرض رقم الحساب مع تنسيق
            long serverId = account.getServerId();
            if (serverId > 0) {
                holder.accountNumber.setText("رقم: " + serverId);
            } else {
                holder.accountNumber.setText("رقم: غير محدد");
            }

            // راقب الرصيد اليمني فقط
            accountViewModel.getAccountBalanceYemeni(account.getId()).observe(lifecycleOwner, balance -> {
                double value = balance != null ? balance : 0;
                String balanceText;
                if (value < 0) {
                    balanceText = String.format(java.util.Locale.US, "عليه %,d يمني", Math.abs((long)value));
                    holder.balance.setTextColor(holder.itemView.getContext().getColor(R.color.debit_red));
                } else {
                    balanceText = String.format(java.util.Locale.US, "له %,d يمني", (long)value);
                    holder.balance.setTextColor(holder.itemView.getContext().getColor(R.color.credit_green));
                }
                holder.balance.setText(balanceText);
            });

            // Setup WhatsApp switch
            // منع الاستدعاء المزدوج
            holder.whatsappSwitch.setOnCheckedChangeListener(null);
            holder.whatsappSwitch.setChecked(account.isWhatsappEnabled());
            
            // تعيين اللون الأولي
            if (account.isWhatsappEnabled()) {
                holder.whatsappSwitch.setThumbTintList(android.content.res.ColorStateList.valueOf(holder.itemView.getContext().getColor(R.color.credit_green)));
                holder.whatsappSwitch.setTrackTintList(android.content.res.ColorStateList.valueOf(holder.itemView.getContext().getColor(R.color.credit_green)));
            } else {
                holder.whatsappSwitch.setThumbTintList(android.content.res.ColorStateList.valueOf(holder.itemView.getContext().getColor(R.color.gray)));
                holder.whatsappSwitch.setTrackTintList(android.content.res.ColorStateList.valueOf(holder.itemView.getContext().getColor(R.color.gray)));
            }
            
            holder.whatsappSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // منع التحديث إذا كانت القيمة نفسها
                if (account.isWhatsappEnabled() == isChecked) {
                    return;
                }
                
                // منع انتشار الحدث
                buttonView.setEnabled(false);
                
                // تحديث حالة واتساب الحساب
                account.setWhatsappEnabled(isChecked);
                account.setUpdatedAt(System.currentTimeMillis());
                accountViewModel.updateAccount(account);
                
                // تغيير لون الزر
                if (isChecked) {
                    holder.whatsappSwitch.setThumbTintList(android.content.res.ColorStateList.valueOf(holder.itemView.getContext().getColor(R.color.credit_green)));
                    holder.whatsappSwitch.setTrackTintList(android.content.res.ColorStateList.valueOf(holder.itemView.getContext().getColor(R.color.credit_green)));
                } else {
                    holder.whatsappSwitch.setThumbTintList(android.content.res.ColorStateList.valueOf(holder.itemView.getContext().getColor(R.color.gray)));
                    holder.whatsappSwitch.setTrackTintList(android.content.res.ColorStateList.valueOf(holder.itemView.getContext().getColor(R.color.gray)));
                }
                
                String message = isChecked ? "تم تفعيل واتساب للحساب" : "تم إيقاف واتساب للحساب";
                Toast.makeText(buttonView.getContext(), message, Toast.LENGTH_SHORT).show();
                
                // إعادة تفعيل الزر بعد فترة قصيرة
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    holder.whatsappSwitch.setEnabled(true);
                }, 300);
            });

            // Setup edit button
            holder.editButton.setOnClickListener(v -> {
                // منع انتشار الحدث
                v.setEnabled(false);
                
                Bundle args = new Bundle();
                args.putLong("accountId", account.getId());
                Navigation.findNavController(v).navigate(R.id.editAccountFragment, args);
                
                // إعادة تفعيل الزر بعد فترة قصيرة
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    holder.editButton.setEnabled(true);
                }, 300);
            });

            // Setup item click
            holder.itemView.setOnClickListener(v -> {
                // منع انتشار الحدث
                v.setEnabled(false);
                
                Bundle args = new Bundle();
                args.putLong("accountId", account.getId());
                Navigation.findNavController(v).navigate(R.id.accountDetailsFragment, args);
                
                // إعادة تفعيل العنصر بعد فترة قصيرة
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    holder.itemView.setEnabled(true);
                }, 300);
            });
        }

        @Override
        public int getItemCount() {
            return accounts.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView accountName;
            TextView phone;
            TextView accountNumber;
            TextView balance;
            SwitchMaterial whatsappSwitch;
            MaterialButton editButton;

            ViewHolder(View itemView) {
                super(itemView);
                accountName = itemView.findViewById(R.id.account_name);
                phone = itemView.findViewById(R.id.phone);
                accountNumber = itemView.findViewById(R.id.account_number);
                balance = itemView.findViewById(R.id.balance);
                whatsappSwitch = itemView.findViewById(R.id.whatsapp_switch);
                editButton = itemView.findViewById(R.id.edit_button);
            }
        }
    }
} 
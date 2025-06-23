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
            List<Account> sorted = new ArrayList<>(currentAccounts[0]);
            Collections.sort(sorted, (a, b) -> Double.compare(
                accountBalances.getOrDefault(b.getId(), 0.0),
                accountBalances.getOrDefault(a.getId(), 0.0)
            ));
            accountsAdapter.updateAccounts(sorted);
            Toast.makeText(getContext(), "تم الترتيب حسب الرصيد (من الأكبر)", Toast.LENGTH_SHORT).show();
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

            // Setup WhatsApp button
            holder.whatsappButton.setOnClickListener(v -> {
                Toast.makeText(v.getContext(), "سيتم إرسال كشف الحساب عبر واتساب", Toast.LENGTH_SHORT).show();
            });

            // Setup edit button
            holder.editButton.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putLong("accountId", account.getId());
                Navigation.findNavController(v).navigate(R.id.editAccountFragment, args);
            });

            // Setup item click
            holder.itemView.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putLong("accountId", account.getId());
                Navigation.findNavController(v).navigate(R.id.accountDetailsFragment, args);
            });
        }

        @Override
        public int getItemCount() {
            return accounts.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView accountName;
            TextView phone;
            TextView balance;
            MaterialButton whatsappButton;
            MaterialButton editButton;

            ViewHolder(View itemView) {
                super(itemView);
                accountName = itemView.findViewById(R.id.account_name);
                phone = itemView.findViewById(R.id.phone);
                balance = itemView.findViewById(R.id.balance);
                whatsappButton = itemView.findViewById(R.id.whatsapp_button);
                editButton = itemView.findViewById(R.id.edit_button);
            }
        }
    }
} 
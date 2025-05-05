package com.hillal.hhhhhhh.ui.accounts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.entities.Account;
import com.hillal.hhhhhhh.viewmodel.AccountViewModel;

import java.util.ArrayList;
import java.util.List;

public class AccountsFragment extends Fragment {
    private AccountViewModel accountViewModel;
    private RecyclerView accountsRecyclerView;
    private AccountsAdapter accountsAdapter;
    private TextInputEditText searchEditText;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_accounts, container, false);

        // Initialize views
        accountsRecyclerView = root.findViewById(R.id.accounts_list);
        searchEditText = root.findViewById(R.id.search_edit_text);
        FloatingActionButton addAccountButton = root.findViewById(R.id.fab_add_account);

        // Setup RecyclerView
        accountsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        accountsAdapter = new AccountsAdapter(new ArrayList<>());
        accountsRecyclerView.setAdapter(accountsAdapter);

        // Initialize ViewModel
        accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);

        // Observe accounts data
        accountViewModel.getAllAccounts().observe(getViewLifecycleOwner(), accounts -> {
            accountsAdapter.updateAccounts(accounts);
        });

        // Setup search functionality
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            String query = v.getText().toString();
            if (!query.isEmpty()) {
                accountViewModel.searchAccounts(query).observe(getViewLifecycleOwner(), accounts -> {
                    accountsAdapter.updateAccounts(accounts);
                });
            } else {
                accountViewModel.getAllAccounts().observe(getViewLifecycleOwner(), accounts -> {
                    accountsAdapter.updateAccounts(accounts);
                });
            }
            return true;
        });

        // Setup add account button
        addAccountButton.setOnClickListener(v -> {
            Navigation.findNavController(root).navigate(R.id.addAccountFragment);
        });

        return root;
    }

    private static class AccountsAdapter extends RecyclerView.Adapter<AccountsAdapter.ViewHolder> {
        private List<Account> accounts;

        public AccountsAdapter(List<Account> accounts) {
            this.accounts = accounts;
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
            holder.balance.setText(String.format("%,.2f", account.getOpeningBalance()));
            holder.balance.setTextColor(account.isDebtor() ? 
                holder.itemView.getContext().getColor(R.color.red) : 
                holder.itemView.getContext().getColor(R.color.green));
        }

        @Override
        public int getItemCount() {
            return accounts.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView accountName;
            TextView phone;
            TextView balance;

            ViewHolder(View itemView) {
                super(itemView);
                accountName = itemView.findViewById(R.id.account_name);
                phone = itemView.findViewById(R.id.phone);
                balance = itemView.findViewById(R.id.balance);
            }
        }
    }
} 
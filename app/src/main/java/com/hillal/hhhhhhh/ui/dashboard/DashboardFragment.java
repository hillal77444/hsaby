package com.hillal.hhhhhhh.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.entities.Account;
import com.hillal.hhhhhhh.viewmodel.AccountViewModel;

import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {
    private AccountViewModel accountViewModel;
    private TextView totalDebtors, totalCreditors, netBalance;
    private RecyclerView recentAccountsRecyclerView;
    private RecentAccountsAdapter recentAccountsAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Initialize views
        totalDebtors = root.findViewById(R.id.total_debtors);
        totalCreditors = root.findViewById(R.id.total_creditors);
        netBalance = root.findViewById(R.id.net_balance);
        recentAccountsRecyclerView = root.findViewById(R.id.recent_accounts_recycler);

        // Setup RecyclerView
        recentAccountsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recentAccountsAdapter = new RecentAccountsAdapter(new ArrayList<>());
        recentAccountsRecyclerView.setAdapter(recentAccountsAdapter);

        // Initialize ViewModel
        accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);

        // Observe accounts data
        accountViewModel.getAllAccounts().observe(getViewLifecycleOwner(), accounts -> {
            updateDashboardStats(accounts);
            recentAccountsAdapter.updateAccounts(accounts);
        });

        return root;
    }

    private void updateDashboardStats(List<Account> accounts) {
        double totalDebt = 0;
        double totalCredit = 0;

        for (Account account : accounts) {
            if (account.isDebtor()) {
                totalDebt += account.getOpeningBalance();
            } else {
                totalCredit += account.getOpeningBalance();
            }
        }

        totalDebtors.setText(String.format("%,.2f", totalDebt));
        totalCreditors.setText(String.format("%,.2f", totalCredit));
        netBalance.setText(String.format("%,.2f", totalCredit - totalDebt));
    }

    private static class RecentAccountsAdapter extends RecyclerView.Adapter<RecentAccountsAdapter.ViewHolder> {
        private List<Account> accounts;

        public RecentAccountsAdapter(List<Account> accounts) {
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
                    .inflate(R.layout.item_recent_account, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Account account = accounts.get(position);
            holder.accountName.setText(account.getName());
            holder.balance.setText(String.format("%,.2f", account.getOpeningBalance()));
            holder.balance.setTextColor(account.isDebtor() ? 
                holder.itemView.getContext().getColor(R.color.red) : 
                holder.itemView.getContext().getColor(R.color.green));
        }

        @Override
        public int getItemCount() {
            return Math.min(accounts.size(), 5); // Show only 5 most recent accounts
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView accountName;
            TextView balance;

            ViewHolder(View itemView) {
                super(itemView);
                accountName = itemView.findViewById(R.id.account_name);
                balance = itemView.findViewById(R.id.account_balance);
            }
        }
    }
} 
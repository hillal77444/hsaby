package com.hillal.hhhhhhh.ui.accounts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.entities.Account;
import com.hillal.hhhhhhh.data.entities.Transaction;
import com.hillal.hhhhhhh.viewmodel.AccountViewModel;
import com.hillal.hhhhhhh.viewmodel.TransactionViewModel;

import java.util.ArrayList;
import java.util.List;

public class AccountDetailsFragment extends Fragment {
    private AccountViewModel accountViewModel;
    private TransactionViewModel transactionViewModel;
    private TextView accountName;
    private TextView accountBalance;
    private TextView accountPhone;
    private TextView accountNotes;
    private RecyclerView transactionsRecyclerView;
    private TransactionsAdapter transactionsAdapter;
    private long accountId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_account_details, container, false);

        // Get account ID from arguments
        accountId = getArguments() != null ? getArguments().getLong("accountId") : -1;
        if (accountId == -1) {
            Navigation.findNavController(root).navigateUp();
            return root;
        }

        // Initialize views
        accountName = root.findViewById(R.id.account_name);
        accountBalance = root.findViewById(R.id.balance);
        accountPhone = root.findViewById(R.id.phone);
        accountNotes = root.findViewById(R.id.notes);
        transactionsRecyclerView = root.findViewById(R.id.transactions_list);
        FloatingActionButton addTransactionButton = root.findViewById(R.id.add_transaction_button);

        // Setup RecyclerView
        transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        transactionsAdapter = new TransactionsAdapter(new ArrayList<>());
        transactionsRecyclerView.setAdapter(transactionsAdapter);

        // Initialize ViewModels
        accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);
        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);

        // Observe account data
        accountViewModel.getAccountById(accountId).observe(getViewLifecycleOwner(), account -> {
            if (account != null) {
                updateAccountDetails(account);
            }
        });

        // Observe transactions data
        transactionViewModel.getTransactionsForAccount(accountId).observe(getViewLifecycleOwner(), transactions -> {
            transactionsAdapter.updateTransactions(transactions);
        });

        // Observe account balance
        transactionViewModel.getAccountBalance(accountId).observe(getViewLifecycleOwner(), balance -> {
            if (balance != null) {
                accountBalance.setText(String.format("%,.2f", balance));
                accountBalance.setTextColor(balance < 0 ? 
                    getContext().getColor(R.color.red) : 
                    getContext().getColor(R.color.green));
            }
        });

        // Setup add transaction button
        addTransactionButton.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putLong("accountId", accountId);
            Navigation.findNavController(root).navigate(R.id.nav_add_transaction, args);
        });

        return root;
    }

    private void updateAccountDetails(Account account) {
        accountName.setText(account.getName());
        accountPhone.setText(account.getPhoneNumber() != null ? account.getPhoneNumber() : getString(R.string.not_available));
        accountNotes.setText(account.getNotes() != null ? account.getNotes() : getString(R.string.no_notes));
    }

    private static class TransactionsAdapter extends RecyclerView.Adapter<TransactionsAdapter.ViewHolder> {
        private List<Transaction> transactions;

        public TransactionsAdapter(List<Transaction> transactions) {
            this.transactions = transactions;
        }

        public void updateTransactions(List<Transaction> newTransactions) {
            this.transactions = newTransactions;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_transaction, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Transaction transaction = transactions.get(position);
            holder.date.setText(android.text.format.DateFormat.format("dd/MM/yyyy", transaction.getDate()));
            holder.amount.setText(String.format("%,.2f", transaction.getAmount()));
            holder.amount.setTextColor(transaction.isDebit() ? 
                holder.itemView.getContext().getColor(R.color.red) : 
                holder.itemView.getContext().getColor(R.color.green));
            holder.notes.setText(transaction.getNotes() != null ? transaction.getNotes() : "");
        }

        @Override
        public int getItemCount() {
            return transactions.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView date;
            TextView amount;
            TextView notes;

            ViewHolder(View itemView) {
                super(itemView);
                date = itemView.findViewById(R.id.transaction_date);
                amount = itemView.findViewById(R.id.transaction_amount);
                notes = itemView.findViewById(R.id.transaction_notes);
            }
        }
    }
} 
package com.hillal.hhhhhhh.ui.accounts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.hillal.hhhhhhh.databinding.FragmentAccountDetailsBinding;
import androidx.navigation.fragment.NavHostFragment;

import java.util.ArrayList;
import java.util.List;

public class AccountDetailsFragment extends Fragment {
    private FragmentAccountDetailsBinding binding;
    private AccountViewModel accountViewModel;
    private TransactionViewModel transactionViewModel;
    private TextView accountName;
    private TextView accountBalance;
    private TextView accountPhone;
    private TextView accountNotes;
    private RecyclerView transactionsRecyclerView;
    private TransactionsAdapter transactionsAdapter;
    private int accountId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accountId = getArguments() != null ? getArguments().getInt("accountId") : -1;
        accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAccountDetailsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews();
        loadAccountDetails();
    }

    private void setupViews() {
        binding.addTransactionButton.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putInt("accountId", accountId);
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_accountDetailsFragment_to_addTransactionFragment, bundle);
        });
    }

    private void loadAccountDetails() {
        // Get account ID from arguments
        if (accountId == -1) {
            Navigation.findNavController(binding.getRoot()).navigateUp();
            return;
        }

        // Initialize views
        accountName = binding.accountName;
        accountBalance = binding.balance;
        accountPhone = binding.phone;
        accountNotes = binding.notes;
        transactionsRecyclerView = binding.transactionsList;
        FloatingActionButton addTransactionButton = binding.addTransactionButton;

        // Setup RecyclerView
        transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        transactionsAdapter = new TransactionsAdapter(new ArrayList<>());
        transactionsRecyclerView.setAdapter(transactionsAdapter);

        // Initialize ViewModels
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
    }

    private void updateAccountDetails(Account account) {
        accountName.setText(account.getName());
        accountPhone.setText(account.getPhoneNumber() != null ? account.getPhoneNumber() : getString(R.string.not_available));
        accountNotes.setText(account.getNotes() != null ? account.getNotes() : getString(R.string.no_notes));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
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
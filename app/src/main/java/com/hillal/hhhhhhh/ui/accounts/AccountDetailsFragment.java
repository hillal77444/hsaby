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
import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.model.Transaction;
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
    private long accountId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accountId = getArguments() != null ? getArguments().getLong("accountId") : -1;
        accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);
        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
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
        accountName = binding.accountName;
        accountBalance = binding.accountBalance;
        accountPhone = binding.accountPhone;
        accountNotes = binding.accountNotes;
        transactionsRecyclerView = binding.transactionsRecyclerView;
        transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        transactionsAdapter = new TransactionsAdapter();
        transactionsRecyclerView.setAdapter(transactionsAdapter);

        FloatingActionButton fab = binding.fabAddTransaction;
        fab.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putLong("accountId", accountId);
            Navigation.findNavController(v).navigate(R.id.nav_add_transaction, bundle);
        });

        binding.viewTransactionsButton.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putLong("accountId", accountId);
            Navigation.findNavController(v).navigate(R.id.nav_transactions, args);
        });
    }

    private void loadAccountDetails() {
        accountViewModel.getAccountById(accountId).observe(getViewLifecycleOwner(), account -> {
            if (account != null) {
                updateAccountDetails(account);
            }
        });

        transactionViewModel.getTransactionsForAccount(accountId).observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null) {
                transactionsAdapter.submitList(transactions);
            }
        });

        transactionViewModel.getAccountBalance(accountId).observe(getViewLifecycleOwner(), balance -> {
            if (balance != null) {
                accountBalance.setText(String.format("%.2f", balance));
            }
        });
    }

    private void updateAccountDetails(Account account) {
        accountName.setText(account.getName());
        accountPhone.setText(account.getPhoneNumber());
        accountNotes.setText(account.getNotes());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static class TransactionsAdapter extends RecyclerView.Adapter<TransactionsAdapter.ViewHolder> {
        private List<Transaction> transactions;

        public TransactionsAdapter() {
            this.transactions = new ArrayList<>();
        }

        public void submitList(List<Transaction> newTransactions) {
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
            holder.amount.setText(String.format("%.2f", transaction.getAmount()));
            holder.notes.setText(transaction.getNotes());
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
package com.hillal.hhhhhhh.ui.accounts;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.app.AlertDialog;
import android.widget.Toast;

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
import com.hillal.hhhhhhh.ui.AccountStatementActivity;
import com.hillal.hhhhhhh.viewmodel.AccountViewModel;
import com.hillal.hhhhhhh.viewmodel.TransactionViewModel;
import com.hillal.hhhhhhh.databinding.FragmentAccountDetailsBinding;
import androidx.navigation.fragment.NavHostFragment;

import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;

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
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

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

        // إضافة زر تعديل الحساب
        binding.editAccountButton.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putLong("accountId", accountId);
            Navigation.findNavController(v).navigate(R.id.editAccountFragment, args);
        });

        // إضافة زر حذف الحساب
        binding.deleteAccountButton.setOnClickListener(v -> {
            showDeleteConfirmationDialog();
        });

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

        // إضافة زر كشف الحساب التفصيلي
        binding.viewAccountStatementButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AccountStatementActivity.class);
            intent.putExtra("account_id", accountId);
            startActivity(intent);
        });
    }

    private void loadAccountDetails() {
        accountViewModel.getAccountById(accountId).observe(getViewLifecycleOwner(), account -> {
            if (account != null) {
                updateAccountDetails(account);
            }
        });

        loadTransactions();
    }

    private void loadTransactions() {
        transactionViewModel.getTransactionsByAccount(accountId).observe(getViewLifecycleOwner(), transactions -> {
            transactionsAdapter.submitList(transactions);
        });

        transactionViewModel.getTotalDebit(accountId).observe(getViewLifecycleOwner(), debit -> {
            transactionViewModel.getTotalCredit(accountId).observe(getViewLifecycleOwner(), credit -> {
                double balance = (credit != null ? credit : 0) - (debit != null ? debit : 0);
                accountBalance.setText(String.format("%.2f", balance));
            });
        });
    }

    private void updateAccountDetails(Account account) {
        accountName.setText(account.getName());
        accountPhone.setText(account.getPhoneNumber());
        accountNotes.setText(account.getNotes());
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_account)
            .setMessage(R.string.delete_account_confirmation)
            .setPositiveButton(R.string.delete, (dialog, which) -> {
                accountViewModel.getAccountById(accountId).observe(getViewLifecycleOwner(), account -> {
                    if (account != null) {
                        accountViewModel.deleteAccount(account);
                        Toast.makeText(getContext(), R.string.account_deleted, Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(requireView()).navigateUp();
                    }
                });
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
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
            holder.bind(transaction);
        }

        @Override
        public int getItemCount() {
            return transactions.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView debitTextView;
            private final TextView creditTextView;
            private final TextView descriptionTextView;
            private final TextView dateTextView;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                debitTextView = itemView.findViewById(R.id.transactionDebit);
                creditTextView = itemView.findViewById(R.id.transactionCredit);
                descriptionTextView = itemView.findViewById(R.id.transactionDescription);
                dateTextView = itemView.findViewById(R.id.transactionDate);
            }

            void bind(Transaction transaction) {
                dateTextView.setText(DATE_FORMAT.format(transaction.getDate()));
                descriptionTextView.setText(transaction.getDescription());
                if (transaction.getType().equals("عليه") || transaction.getType().equalsIgnoreCase("debit")) {
                    debitTextView.setText(String.format("%.2f %s", transaction.getAmount(), transaction.getCurrency()));
                    creditTextView.setText(String.format("0 %s", transaction.getCurrency()));
                } else {
                    debitTextView.setText(String.format("0 %s", transaction.getCurrency()));
                    creditTextView.setText(String.format("%.2f %s", transaction.getAmount(), transaction.getCurrency()));
                }
            }
        }
    }
} 
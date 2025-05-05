package com.hillal.hhhhhhh.ui.transactions;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.hillal.hhhhhhh.databinding.FragmentTransactionsBinding;
import com.hillal.hhhhhhh.ui.adapters.TransactionsAdapter;
import com.hillal.hhhhhhh.viewmodel.TransactionViewModel;

public class TransactionsFragment extends Fragment {
    private FragmentTransactionsBinding binding;
    private TransactionViewModel transactionViewModel;
    private TransactionsAdapter transactionsAdapter;
    private long accountId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accountId = getArguments() != null ? getArguments().getLong("accountId") : -1;
        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
        transactionsAdapter = new TransactionsAdapter();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTransactionsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView();
        setupObservers();
        setupFab();
        setupToolbar();
    }

    private void setupRecyclerView() {
        binding.transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.transactionsRecyclerView.setAdapter(transactionsAdapter);
    }

    private void setupObservers() {
        transactionViewModel.getTransactionsForAccount(accountId).observe(getViewLifecycleOwner(), transactions -> {
            transactionsAdapter.submitList(transactions);
            binding.emptyView.setVisibility(transactions.isEmpty() ? View.VISIBLE : View.GONE);
        });

        transactionViewModel.getAccountBalance(accountId).observe(getViewLifecycleOwner(), balance -> {
            binding.balanceTextView.setText(String.format("%.2f", balance));
        });
    }

    private void setupFab() {
        binding.addTransactionFab.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putLong("accountId", accountId);
            Navigation.findNavController(v).navigate(R.id.nav_add_transaction, args);
        });
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 
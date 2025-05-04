package com.hillal.hhhhhhh;

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

import com.google.android.material.button.MaterialButton;
import com.hillal.hhhhhhh.databinding.FragmentDashboardBinding;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private TextView totalCreditorTextView;
    private TextView totalDebtorTextView;
    private TextView netBalanceTextView;
    private MaterialButton addAccountButton;
    private MaterialButton searchFilterButton;
    private RecyclerView accountsRecyclerView;
    private AccountAdapter accountAdapter;
    private AccountViewModel accountViewModel;
    private DashboardViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // تهيئة العناصر
        totalCreditorTextView = binding.totalCreditorTextView;
        totalDebtorTextView = binding.totalDebtorTextView;
        netBalanceTextView = binding.netBalanceTextView;
        addAccountButton = binding.addAccountButton;
        searchFilterButton = binding.searchFilterButton;
        accountsRecyclerView = binding.accountsRecyclerView;

        // إعداد RecyclerView
        accountAdapter = new AccountAdapter(account -> {
            Bundle args = new Bundle();
            args.putInt("accountId", account.getId());
            Navigation.findNavController(root).navigate(R.id.accountDetailsFragment, args);
        });
        accountsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        accountsRecyclerView.setAdapter(accountAdapter);

        // تهيئة ViewModel
        accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        // مراقبة التغييرات في الإحصائيات
        accountViewModel.getTotalCreditor().observe(getViewLifecycleOwner(), total -> {
            totalCreditorTextView.setText(String.format("%.2f ريال", total));
        });

        accountViewModel.getTotalDebtor().observe(getViewLifecycleOwner(), total -> {
            totalDebtorTextView.setText(String.format("%.2f ريال", total));
        });

        accountViewModel.getNetBalance().observe(getViewLifecycleOwner(), total -> {
            netBalanceTextView.setText(String.format("%.2f ريال", total));
        });

        // مراقبة التغييرات في قائمة الحسابات
        viewModel.getAllAccounts().observe(getViewLifecycleOwner(), accounts -> {
            accountAdapter.setAccounts(accounts);
        });

        // إعداد أزرار التنقل
        addAccountButton.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.addAccountFragment));
        
        searchFilterButton.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.searchFilterFragment));

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 
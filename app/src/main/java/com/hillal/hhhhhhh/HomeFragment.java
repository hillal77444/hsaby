package com.hillal.hhhhhhh;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.hillal.hhhhhhh.databinding.FragmentHomeBinding;
import java.util.List;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private AccountViewModel accountViewModel;
    private AccountAdapter accountAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // إعداد ViewModels
        accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);

        // إعداد RecyclerView
        accountAdapter = new AccountAdapter(account -> {
            Bundle args = new Bundle();
            args.putInt("accountId", account.getId());
            Navigation.findNavController(view).navigate(R.id.action_homeFragment_to_accountDetailsFragment, args);
        });
        
        binding.accountsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.accountsRecyclerView.setAdapter(accountAdapter);

        // مراقبة تغييرات الحسابات
        accountViewModel.getAllAccounts().observe(getViewLifecycleOwner(), accounts -> {
            accountAdapter.submitList(accounts);
            updateTotalBalances(accounts);
        });

        // إعداد الأزرار
        binding.addAccountButton.setOnClickListener(v -> 
            Navigation.findNavController(view).navigate(R.id.action_homeFragment_to_addAccountFragment));
            
        binding.searchFilterButton.setOnClickListener(v -> 
            Navigation.findNavController(view).navigate(R.id.action_homeFragment_to_searchFilterFragment));
    }

    private void updateTotalBalances(List<Account> accounts) {
        double totalDebtor = 0;
        double totalCreditor = 0;
        
        for (Account account : accounts) {
            if (account.getBalance() < 0) {
                totalDebtor += Math.abs(account.getBalance());
            } else {
                totalCreditor += account.getBalance();
            }
        }
        
        binding.totalDebtorText.setText(String.format("%.2f", totalDebtor));
        binding.totalCreditorText.setText(String.format("%.2f", totalCreditor));
        binding.netBalanceText.setText(String.format("%.2f", totalCreditor - totalDebtor));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 
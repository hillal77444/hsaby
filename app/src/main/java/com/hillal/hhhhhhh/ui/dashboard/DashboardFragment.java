package com.hillal.hhhhhhh.ui.dashboard;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.repository.AccountRepository;
import com.hillal.hhhhhhh.databinding.FragmentDashboardBinding;
import com.hillal.hhhhhhh.ui.adapters.RecentAccountsAdapter;
import com.hillal.hhhhhhh.App;

public class DashboardFragment extends Fragment {
    private static final String TAG = "DashboardFragment";
    private FragmentDashboardBinding binding;
    private DashboardViewModel dashboardViewModel;
    private RecentAccountsAdapter recentAccountsAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "DashboardFragment onCreate started");

        try {
            // Initialize ViewModel
            AccountRepository accountRepository = App.getInstance().getAccountRepository();
            dashboardViewModel = new ViewModelProvider(this, 
                new DashboardViewModelFactory(accountRepository)).get(DashboardViewModel.class);
            Log.d(TAG, "DashboardViewModel initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing DashboardFragment: " + e.getMessage(), e);
            throw new RuntimeException("Failed to initialize DashboardFragment", e);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "DashboardFragment onViewCreated started");

        try {
            // Setup RecyclerView
            RecyclerView recentAccountsList = binding.recentAccountsList;
            recentAccountsList.setLayoutManager(new LinearLayoutManager(requireContext()));
            recentAccountsAdapter = new RecentAccountsAdapter();
            recentAccountsList.setAdapter(recentAccountsAdapter);

            // Observe data
            dashboardViewModel.getRecentAccounts().observe(getViewLifecycleOwner(), accounts -> {
                if (accounts != null) {
                    recentAccountsAdapter.updateAccounts(accounts);
                    Log.d(TAG, "Recent accounts updated: " + accounts.size() + " accounts");
                }
            });

            dashboardViewModel.getTotalDebtors().observe(getViewLifecycleOwner(), total -> {
                if (total != null) {
                    binding.totalDebtors.setText(String.format("%.2f %s", total, getString(R.string.currency_symbol)));
                }
            });

            dashboardViewModel.getTotalCreditors().observe(getViewLifecycleOwner(), total -> {
                if (total != null) {
                    binding.totalCreditors.setText(String.format("%.2f %s", total, getString(R.string.currency_symbol)));
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in onViewCreated: " + e.getMessage(), e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 
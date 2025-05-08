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
import androidx.navigation.Navigation;

import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.repository.AccountRepository;
import com.hillal.hhhhhhh.databinding.FragmentDashboardBinding;
import com.hillal.hhhhhhh.App;
import com.hillal.hhhhhhh.data.sync.SyncManager;

public class DashboardFragment extends Fragment {
    private static final String TAG = "DashboardFragment";
    private FragmentDashboardBinding binding;
    private DashboardViewModel dashboardViewModel;

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
            // إضافة المزامنة عند دخول لوحة التحكم
            App app = (App) requireActivity().getApplication();
            SyncManager syncManager = new SyncManager(
                requireContext(),
                app.getDatabase().accountDao(),
                app.getDatabase().transactionDao()
            );
            syncManager.onDashboardEntered();

            setupClickListeners();
            observeData();
        } catch (Exception e) {
            Log.e(TAG, "Error in onViewCreated: " + e.getMessage(), e);
        }
    }

    private void setupClickListeners() {
        // زر عرض القيود المحاسبية
        binding.viewTransactionsButton.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.transactionsFragment));

        // زر عرض الحسابات
        binding.viewAccountsButton.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.navigation_accounts));

        // زر عرض التقارير
        binding.viewReportsButton.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.navigation_reports));
    }

    private void observeData() {
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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 
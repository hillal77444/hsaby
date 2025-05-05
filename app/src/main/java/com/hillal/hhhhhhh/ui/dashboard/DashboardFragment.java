package com.hillal.hhhhhhh.ui.dashboard;

import android.os.Bundle;
import android.util.Log;
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
import com.hillal.hhhhhhh.databinding.FragmentDashboardBinding;
import com.hillal.hhhhhhh.ui.adapters.RecentAccountsAdapter;

public class DashboardFragment extends Fragment {
    private static final String TAG = "DashboardFragment";
    private FragmentDashboardBinding binding;
    private DashboardViewModel dashboardViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        try {
            dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
            binding = FragmentDashboardBinding.inflate(inflater, container, false);
            View root = binding.getRoot();

            // إعداد RecyclerView
            RecyclerView recentAccountsList = binding.recentAccountsList;
            recentAccountsList.setLayoutManager(new LinearLayoutManager(getContext()));
            recentAccountsList.setAdapter(new RecentAccountsAdapter());

            // مراقبة التغييرات في البيانات
            dashboardViewModel.getTotalDebtors().observe(getViewLifecycleOwner(), total -> {
                binding.totalDebtors.setText(String.valueOf(total));
            });

            dashboardViewModel.getTotalCreditors().observe(getViewLifecycleOwner(), total -> {
                binding.totalCreditors.setText(String.valueOf(total));
            });

            dashboardViewModel.getNetBalance().observe(getViewLifecycleOwner(), balance -> {
                binding.netBalance.setText(String.valueOf(balance));
            });

            Log.d(TAG, "DashboardFragment created successfully");
            return root;
        } catch (Exception e) {
            Log.e(TAG, "Error creating DashboardFragment: " + e.getMessage(), e);
            throw new RuntimeException("Failed to create DashboardFragment: " + e.getMessage(), e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 
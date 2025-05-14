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
import com.hillal.hhhhhhh.data.repository.TransactionRepository;
import com.hillal.hhhhhhh.databinding.FragmentDashboardBinding;
import com.hillal.hhhhhhh.App;
import com.hillal.hhhhhhh.data.sync.SyncManager;
import com.hillal.hhhhhhh.data.preferences.UserPreferences;
import com.hillal.hhhhhhh.data.room.AppDatabase;

public class DashboardFragment extends Fragment {
    private static final String TAG = "DashboardFragment";
    private FragmentDashboardBinding binding;
    private DashboardViewModel dashboardViewModel;
    private UserPreferences userPreferences;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "DashboardFragment onCreate started");

        try {
            // Initialize ViewModel
            App app = (App) requireActivity().getApplication();
            AppDatabase db = app.getDatabase();
            AccountRepository accountRepository = new AccountRepository(db.accountDao(), db);
            TransactionRepository transactionRepository = new TransactionRepository(requireActivity().getApplication());
            dashboardViewModel = new ViewModelProvider(this, new DashboardViewModelFactory(accountRepository, transactionRepository))
                    .get(DashboardViewModel.class);
            userPreferences = new UserPreferences(requireContext());
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
            updateUserName();
        } catch (Exception e) {
            Log.e(TAG, "Error in onViewCreated: " + e.getMessage(), e);
        }
    }

    private void setupClickListeners() {
        // زر تعديل الملف الشخصي
        binding.editProfileButton.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.editProfileFragment));

        // زر عرض القيود المحاسبية
        binding.viewTransactionsButton.setOnClickListener(v -> {
            // تنفيذ المزامنة
            App app = (App) requireActivity().getApplication();
            SyncManager syncManager = new SyncManager(
                requireContext(),
                app.getDatabase().accountDao(),
                app.getDatabase().transactionDao()
            );
            
            syncManager.syncData(new SyncManager.SyncCallback() {
                @Override
                public void onSuccess() {
                    // بعد نجاح المزامنة، الانتقال إلى صفحة القيود
                    Navigation.findNavController(v).navigate(R.id.transactionsFragment);
                }

                @Override
                public void onError(String error) {
                    // في حالة حدوث خطأ، نعرض رسالة الخطأ ثم ننتقل إلى صفحة القيود
                    android.widget.Toast.makeText(requireContext(), error, android.widget.Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(v).navigate(R.id.transactionsFragment);
                }
            });
        });

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
                binding.totalDebtors.setText(String.format("%.2f ريال يمني", total));
            }
        });

        dashboardViewModel.getTotalCreditors().observe(getViewLifecycleOwner(), total -> {
            if (total != null) {
                binding.totalCreditors.setText(String.format("%.2f ريال يمني", total));
            }
        });
    }

    private void updateUserName() {
        String userName = userPreferences.getUserName();
        if (!userName.isEmpty()) {
            binding.userNameText.setText(userName);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 
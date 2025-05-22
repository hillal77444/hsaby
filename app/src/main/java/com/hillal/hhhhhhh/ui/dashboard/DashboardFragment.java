package com.hillal.hhhhhhh.ui.dashboard;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

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
    private SyncManager syncManager;
    private ProgressDialog progressDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "DashboardFragment onCreate started");

        try {
            // Initialize ViewModel
            App app = (App) requireActivity().getApplication();
            AppDatabase db = app.getDatabase();
            AccountRepository accountRepository = new AccountRepository(db.accountDao(), db);
            TransactionRepository transactionRepository = new TransactionRepository(((App) requireActivity().getApplication()).getDatabase());
            dashboardViewModel = new ViewModelProvider(this, new DashboardViewModelFactory(accountRepository, transactionRepository))
                    .get(DashboardViewModel.class);
            userPreferences = new UserPreferences(requireContext());
            syncManager = new SyncManager(
                requireContext(),
                db.accountDao(),
                db.transactionDao()
            );
            Log.d(TAG, "DashboardViewModel initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing DashboardFragment: " + e.getMessage(), e);
            throw new RuntimeException("Failed to initialize DashboardFragment", e);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        
        // إضافة زر المزامنة الكاملة
        Button fullSyncButton = binding.getRoot().findViewById(R.id.full_sync_button);
        fullSyncButton.setOnClickListener(v -> {
            showLoadingDialog("جاري المزامنة الكاملة...");
            syncManager.performFullSync(new SyncManager.SyncCallback() {
                @Override
                public void onSuccess() {
                    hideLoadingDialog();
                    showSuccessDialog("تمت المزامنة الكاملة بنجاح");
                    // تحديث البيانات في الواجهة
                    loadAccounts();
                    loadTransactions();
                }

                @Override
                public void onError(String error) {
                    hideLoadingDialog();
                    showErrorDialog("فشلت المزامنة الكاملة: " + error);
                }
            });
        });

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "DashboardFragment onViewCreated started");

        try {
            // إضافة المزامنة عند دخول لوحة التحكم
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
            Navigation.findNavController(requireView()).navigate(R.id.editProfileFragment));

        // زر عرض القيود المحاسبية
        binding.viewTransactionsButton.setOnClickListener(v -> 
         
            Navigation.findNavController(requireView()).navigate(R.id.transactionsFragment));

        // زر عرض الحسابات
        binding.viewAccountsButton.setOnClickListener(v -> 
            Navigation.findNavController(requireView()).navigate(R.id.navigation_accounts));

        // زر عرض التقارير
        binding.viewReportsButton.setOnClickListener(v -> 
            Navigation.findNavController(requireView()).navigate(R.id.navigation_reports));
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

    private void showLoadingDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(requireContext());
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void hideLoadingDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void showSuccessDialog(String message) {
        new AlertDialog.Builder(requireContext())
            .setTitle("نجاح")
            .setMessage(message)
            .setPositiveButton("حسناً", null)
            .show();
    }

    private void showErrorDialog(String message) {
        new AlertDialog.Builder(requireContext())
            .setTitle("خطأ")
            .setMessage(message)
            .setPositiveButton("حسناً", null)
            .show();
    }

    private void loadAccounts() {
        dashboardViewModel.getAccounts().observe(getViewLifecycleOwner(), accounts -> {
            // تحديث واجهة المستخدم بالحسابات الجديدة
            // يمكنك إضافة الكود الخاص بك هنا
        });
    }

    private void loadTransactions() {
        dashboardViewModel.getTransactions().observe(getViewLifecycleOwner(), transactions -> {
            // تحديث واجهة المستخدم بالمعاملات الجديدة
            // يمكنك إضافة الكود الخاص بك هنا
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
        binding = null;
    }
} 
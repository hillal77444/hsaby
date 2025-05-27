package com.hillal.hhhhhhh.ui.dashboard;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import com.hillal.hhhhhhh.data.preferences.UserPreferences;
import com.hillal.hhhhhhh.data.room.AppDatabase;
import com.hillal.hhhhhhh.data.sync.SyncManager;
import com.hillal.hhhhhhh.data.remote.DataManager;
import com.hillal.hhhhhhh.data.sync.MigrationManager;

import java.util.Locale;

public class DashboardFragment extends Fragment {
    private static final String TAG = "DashboardFragment";
    private FragmentDashboardBinding binding;
    private DashboardViewModel dashboardViewModel;
    private UserPreferences userPreferences;
    private ProgressDialog progressDialog;
    private AppDatabase db;
    private SyncManager syncManager;
    private MigrationManager migrationManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        Log.d(TAG, "DashboardFragment onCreate started");

        try {
            // Initialize ViewModel
            App app = (App) requireActivity().getApplication();
            db = app.getDatabase();
            AccountRepository accountRepository = new AccountRepository(db.accountDao(), db);
            TransactionRepository transactionRepository = new TransactionRepository(((App) requireActivity().getApplication()).getDatabase());
            dashboardViewModel = new ViewModelProvider(this, new DashboardViewModelFactory(accountRepository, transactionRepository))
                    .get(DashboardViewModel.class);
            userPreferences = new UserPreferences(requireContext());
            
            // Initialize SyncManager
            DataManager dataManager = new DataManager(
                requireContext(),
                db.accountDao(),
                db.transactionDao(),
                db.pendingOperationDao()
            );
            syncManager = new SyncManager(
                requireContext(),
                dataManager,
                db.accountDao(),
                db.transactionDao(),
                db.pendingOperationDao()
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
        db = AppDatabase.getInstance(requireContext());
        setupUI();
        return binding.getRoot();
    }

    private void setupUI() {
        // إعدادات أخرى
        setupOtherSettings();
    }

    private void setupOtherSettings() {
        // هنا يمكن إضافة إعدادات أخرى
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "DashboardFragment onViewCreated started");

        try {
            setupClickListeners();
            observeData();
            updateUserName();
            migrationManager = new MigrationManager(requireContext());

            // تشغيل الترحيل تلقائياً عند فتح الصفحة
            migrationManager.migrateLocalData();
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

    }

    private void observeData() {
        dashboardViewModel.getTotalDebtors().observe(getViewLifecycleOwner(), total -> {
            if (total != null) {
                binding.totalDebtors.setText(String.format(Locale.US, "%d يمني", (int) Math.round(total)));
            }
        });

        dashboardViewModel.getTotalCreditors().observe(getViewLifecycleOwner(), total -> {
            if (total != null) {
                binding.totalCreditors.setText(String.format(Locale.US, "%d يمني", (int) Math.round(total)));
            }
        });

        dashboardViewModel.getAccounts().observe(getViewLifecycleOwner(), accounts -> {
            if (accounts != null) {
                binding.totalAccounts.setText(String.format(Locale.US, "%d", accounts.size()));
            }
        });

        dashboardViewModel.getNetBalance().observe(getViewLifecycleOwner(), balance -> {
            if (balance != null) {
                binding.totalBalance.setText(String.format(Locale.US, "%d يمني", (int) Math.round(balance)));
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

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_dashboard, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_full_sync) {
            showLoadingDialog("جاري المزامنة الكاملة...");
            syncManager.performFullSync(new SyncManager.SyncCallback() {
                @Override
                public void onSuccess() {
                    hideLoadingDialog();
                    showSuccessDialog("تمت المزامنة الكاملة بنجاح");
                    loadAccounts();
                    loadTransactions();
                }
                @Override
                public void onError(String error) {
                    hideLoadingDialog();
                    showErrorDialog("فشلت المزامنة الكاملة: " + error);
                }
            });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 
package com.hillal.acc.ui.dashboard;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.content.pm.PackageManager;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.activity.OnBackPressedCallback;
import androidx.core.view.ViewCompat;
import androidx.compose.ui.platform.ComposeView;

import com.hillal.acc.R;
import com.hillal.acc.data.repository.AccountRepository;
import com.hillal.acc.data.repository.TransactionRepository;
import com.hillal.acc.App;
import com.hillal.acc.data.preferences.UserPreferences;
import com.hillal.acc.data.room.AppDatabase;
import com.hillal.acc.data.sync.SyncManager;
import com.hillal.acc.data.remote.DataManager;
import com.hillal.acc.data.sync.MigrationManager;
import com.hillal.acc.data.model.ServerAppUpdateInfo;
import com.hillal.acc.ui.AccountStatementActivity;
import com.hillal.acc.ui.dashboard.DashboardScreen;

import org.json.JSONObject;
import org.json.JSONException;

import java.util.Locale;

import com.google.android.material.snackbar.Snackbar;

public class DashboardFragment extends Fragment {
    private static final String TAG = "DashboardFragment";
    private DashboardViewModel dashboardViewModel;
    private UserPreferences userPreferences;
    private ProgressDialog progressDialog;
    private AppDatabase db;
    private SyncManager syncManager;
    private MigrationManager migrationManager;
    private DataManager dataManager;

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
            this.dataManager = dataManager;

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
        // استبدال الواجهة بـ ComposeView
        ComposeView composeView = new ComposeView(requireContext());
        composeView.setContent(() -> {
            // جلب البيانات من ViewModel وUserPreferences
            String userName = userPreferences.getUserName();
            String totalAccounts = String.valueOf(dashboardViewModel.getAccounts().getValue() != null ? dashboardViewModel.getAccounts().getValue().size() : 0);
            String totalCreditors = String.valueOf(dashboardViewModel.getTotalCreditors().getValue() != null ? (int)Math.round(dashboardViewModel.getTotalCreditors().getValue()) : 0);
            String totalDebtors = String.valueOf(dashboardViewModel.getTotalDebtors().getValue() != null ? (int)Math.round(dashboardViewModel.getTotalDebtors().getValue()) : 0);
            String totalBalance = String.valueOf(dashboardViewModel.getNetBalance().getValue() != null ? (int)Math.round(dashboardViewModel.getNetBalance().getValue()) : 0) + " يمني";
            return DashboardScreen(
                userName,
                () -> Navigation.findNavController(requireView()).navigate(R.id.editProfileFragment),
                totalAccounts,
                totalCreditors,
                totalDebtors,
                totalBalance,
                () -> Navigation.findNavController(requireView()).navigate(R.id.addTransactionFragment),
                () -> Navigation.findNavController(requireView()).navigate(R.id.addAccountFragment),
                () -> { Intent intent = new Intent(requireContext(), AccountStatementActivity.class); startActivity(intent); },
                () -> Navigation.findNavController(requireView()).navigate(R.id.navigation_accounts),
                () -> Navigation.findNavController(requireView()).navigate(R.id.transactionsFragment),
                () -> Navigation.findNavController(requireView()).navigate(R.id.navigation_reports),
                () -> Navigation.findNavController(requireView()).navigate(R.id.nav_summary),
                () -> Navigation.findNavController(requireView()).navigate(R.id.action_dashboard_to_exchange),
                () -> Navigation.findNavController(requireView()).navigate(R.id.action_dashboard_to_transfer)
            );
        });
        return composeView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "DashboardFragment onViewCreated started");

        try {
            migrationManager = new MigrationManager(requireContext());

            // تشغيل الترحيل تلقائياً عند فتح الصفحة
            migrationManager.migrateLocalData();

            // Send user details to server
            sendUserDetailsToServer();

            // إغلاق التطبيق عند الضغط على زر الرجوع في هذه الصفحة
            requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        requireActivity().finish(); // يغلق التطبيق
                    }
                }
            );

            // ضبط insets للجذر لرفع المحتوى مع الكيبورد وأزرار النظام
            ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
                int bottom = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime()).bottom;
                if (bottom == 0) {
                    bottom = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()).bottom;
                }
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottom);
                return insets;
            });

        } catch (Exception e) {
            Log.e(TAG, "Error in onViewCreated: " + e.getMessage(), e);
        }
    }

    private void sendUserDetailsToServer() {
        long lastSeenTimestamp = System.currentTimeMillis();
        String appVersion = "Unknown";
        try {
            appVersion = requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting app version: " + e.getMessage());
        }

        String deviceName = Build.MODEL + " (Android " + Build.VERSION.RELEASE + ")";

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("last_seen", lastSeenTimestamp);
            requestBody.put("android_version", appVersion);
            requestBody.put("device_name", deviceName);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON for user details: " + e.getMessage());
            return;
        }

        if (dataManager != null) {
            dataManager.updateUserDetails(requestBody, new DataManager.ApiCallback() {
                @Override
                public void onSuccess(ServerAppUpdateInfo updateInfo) {
                    Log.d(TAG, "User details updated successfully on server.");
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Failed to update user details on server: " + error);
                }
            });
        } else {
            Log.e(TAG, "DataManager is not initialized.");
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

    private void showErrorSnackbar(String message) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
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
                }
                @Override
                public void onError(String error) {
                    hideLoadingDialog();
                    showErrorSnackbar("فشلت المزامنة الكاملة: " + error);
                }
            });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        // تحقق إذا كانت القاعدة فارغة (لا يوجد حسابات ولا صناديق)
        new Thread(() -> {
            boolean noAccounts = db.accountDao().getAllAccountsSync().isEmpty();
            boolean noCashboxes = db.cashboxDao().getAllSync().isEmpty();
            if (noAccounts && noCashboxes) {
                requireActivity().runOnUiThread(() -> {
                    showLoadingDialog("جاري المزامنة التلقائية...");
                    syncManager.performFullSync(new SyncManager.SyncCallback() {
                        @Override
                        public void onSuccess() {
                            hideLoadingDialog();
                            showSuccessDialog("تمت المزامنة التلقائية بنجاح");
                        }
                        @Override
                        public void onError(String error) {
                            hideLoadingDialog();
                            showErrorSnackbar("فشلت المزامنة التلقائية: " + error);
                        }
                    });
                });
            }
        }).start();
    }
} 
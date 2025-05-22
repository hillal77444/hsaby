package com.hillal.hhhhhhh.data.sync;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import android.app.AlertDialog;

import com.hillal.hhhhhhh.data.room.AppDatabase;
import com.hillal.hhhhhhh.data.room.AccountDao;
import com.hillal.hhhhhhh.data.room.TransactionDao;
import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.model.Transaction;
import com.hillal.hhhhhhh.data.remote.ApiService;
import com.hillal.hhhhhhh.data.remote.RetrofitClient;
import com.hillal.hhhhhhh.data.remote.ApiService.SyncRequest;
import com.hillal.hhhhhhh.data.remote.ApiService.SyncResponse;
import com.hillal.hhhhhhh.data.preferences.UserPreferences;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MigrationManager {
    private final Context context;
    private final AccountDao accountDao;
    private final TransactionDao transactionDao;
    private final ApiService apiService;
    private final UserPreferences userPreferences;
    private final ExecutorService executor;
    private int migratedAccountsCount = 0;
    private int migratedTransactionsCount = 0;

    public MigrationManager(Context context) {
        this.context = context;
        AppDatabase database = AppDatabase.getInstance(context);
        this.accountDao = database.accountDao();
        this.transactionDao = database.transactionDao();
        this.apiService = RetrofitClient.getApiService();
        this.userPreferences = new UserPreferences(context);
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void startMigration() {
        if (!isNetworkAvailable()) {
            showError("لا يوجد اتصال بالإنترنت");
            return;
        }

        String token = userPreferences.getToken();
        if (token == null) {
            showError("المستخدم غير مسجل الدخول");
            return;
        }

        // جلب الحسابات والمعاملات التي لم تتم مزامنتها بعد
        List<Account> accountsToMigrate = accountDao.getNewAccounts();
        List<Transaction> transactionsToMigrate = transactionDao.getNewTransactions();

        if (accountsToMigrate.isEmpty() && transactionsToMigrate.isEmpty()) {
            showError("لا توجد بيانات للمزامنة");
            return;
        }

        // إرسال البيانات إلى الخادم
        SyncRequest request = new SyncRequest(accountsToMigrate, transactionsToMigrate);
        apiService.syncData("Bearer " + token, request).enqueue(new Callback<SyncResponse>() {
            @Override
            public void onResponse(Call<SyncResponse> call, Response<SyncResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SyncResponse syncResponse = response.body();
                    
                    executor.execute(() -> {
                        for (Account account : accountsToMigrate) {
                            Long serverId = syncResponse.getAccountServerId(account.getId());
                            if (serverId != null) {
                                account.setServerId(serverId);
                                accountDao.update(account);
                                migratedAccountsCount++;
                            }
                        }

                        for (Transaction transaction : transactionsToMigrate) {
                            Long serverId = syncResponse.getTransactionServerId(transaction.getId());
                            if (serverId != null) {
                                transaction.setServerId(serverId);
                                transactionDao.update(transaction);
                                migratedTransactionsCount++;
                            }
                        }

                        new Handler(Looper.getMainLooper()).post(() -> showMigrationSummary());
                    });
                } else {
                    showError("فشلت المزامنة: " + (response.errorBody() != null ? response.errorBody().string() : "خطأ غير معروف"));
                }
            }

            @Override
            public void onFailure(Call<SyncResponse> call, Throwable t) {
                showError("فشلت المزامنة: " + t.getMessage());
            }
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void showError(String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        });
    }

    private void showMigrationSummary() {
        new AlertDialog.Builder(context)
                .setTitle("نتيجة المزامنة")
                .setMessage(String.format("تمت مزامنة %d حساب و %d معاملة بنجاح", migratedAccountsCount, migratedTransactionsCount))
                .setPositiveButton("موافق", null)
                .show();
    }
} 
package com.hillal.hhhhhhh.data.sync;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import com.hillal.hhhhhhh.data.AppDatabase;
import com.hillal.hhhhhhh.data.dao.AccountDao;
import com.hillal.hhhhhhh.data.dao.TransactionDao;
import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.model.Transaction;
import com.hillal.hhhhhhh.network.ApiService;
import com.hillal.hhhhhhh.network.RetrofitClient;
import com.hillal.hhhhhhh.data.remote.ApiService.SyncRequest;
import com.hillal.hhhhhhh.data.remote.ApiService.SyncResponse;
import com.hillal.hhhhhhh.data.preferences.UserPreferences;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MigrationManager {
    private final AppDatabase database;
    private final AccountDao accountDao;
    private final TransactionDao transactionDao;
    private final ApiService apiService;
    private final Context context;
    private final UserPreferences userPreferences;
    private int migratedAccountsCount = 0;
    private int migratedTransactionsCount = 0;

    public MigrationManager(Context context) {
        this.context = context;
        this.database = AppDatabase.getInstance(context);
        this.accountDao = database.accountDao();
        this.transactionDao = database.transactionDao();
        this.apiService = RetrofitClient.getApiService();
        this.userPreferences = new UserPreferences(context);
    }

    public void migrateLocalData() {
        if (!isNetworkAvailable()) {
            Toast.makeText(context, "لا يوجد اتصال بالإنترنت", Toast.LENGTH_SHORT).show();
            return;
        }

        String token = getAuthToken();
        if (token == null) {
            return;
        }

        // إعادة تعيين العدادات
        migratedAccountsCount = 0;
        migratedTransactionsCount = 0;

        // جلب الحسابات والمعاملات التي تحتاج إلى ترحيل
        List<Account> accountsToMigrate = accountDao.getAccountsToMigrate();
        List<Transaction> transactionsToMigrate = transactionDao.getTransactionsToMigrate();

        if (accountsToMigrate.isEmpty() && transactionsToMigrate.isEmpty()) {
            Toast.makeText(context, "لا توجد حسابات أو معاملات تحتاج إلى ترحيل", Toast.LENGTH_LONG).show();
            return;
        }

        // إرسال البيانات إلى الخادم
        SyncRequest request = new SyncRequest(accountsToMigrate, transactionsToMigrate);
        apiService.syncData("Bearer " + token, request).enqueue(new Callback<SyncResponse>() {
            @Override
            public void onResponse(Call<SyncResponse> call, Response<SyncResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SyncResponse syncResponse = response.body();
                    
                    // تحديث server_id للحسابات
                    for (Account account : accountsToMigrate) {
                        Long serverId = syncResponse.getAccountServerId(account.getId());
                        if (serverId != null) {
                            account.setServerId(serverId);
                            accountDao.update(account);
                            migratedAccountsCount++;
                        }
                    }

                    // تحديث server_id للمعاملات
                    for (Transaction transaction : transactionsToMigrate) {
                        Long serverId = syncResponse.getTransactionServerId(transaction.getId());
                        if (serverId != null) {
                            transaction.setServerId(serverId);
                            transactionDao.update(transaction);
                            migratedTransactionsCount++;
                        }
                    }

                    // عرض ملخص الترحيل
                    showMigrationSummary();
                } else {
                    Toast.makeText(context, "فشل في ترحيل البيانات", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SyncResponse> call, Throwable t) {
                Toast.makeText(context, "فشل في الاتصال بالخادم: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showMigrationSummary() {
        String summary = String.format("تم ترحيل %d حساب و %d معاملة بنجاح", 
            migratedAccountsCount, migratedTransactionsCount);
        Toast.makeText(context, summary, Toast.LENGTH_LONG).show();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private String getAuthToken() {
        String token = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .getString("token", null);
        if (token == null || token.isEmpty()) {
            Toast.makeText(context, "يرجى تسجيل الدخول أولاً", Toast.LENGTH_SHORT).show();
            return null;
        }
        return token;
    }
} 
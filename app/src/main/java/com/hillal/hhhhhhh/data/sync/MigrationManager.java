package com.hillal.hhhhhhh.data.sync;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import android.app.AlertDialog;
import android.util.Log;

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
import java.util.concurrent.atomic.AtomicBoolean;

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
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
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
        executor.execute(() -> {
            if (!isNetworkAvailable()) {
                new Handler(Looper.getMainLooper()).post(() -> 
                    Toast.makeText(context, "لا يوجد اتصال بالإنترنت", Toast.LENGTH_SHORT).show());
                return;
            }

            String token = getAuthToken();
            if (token == null) {
                return;
            }

            migratedAccountsCount = 0;
            migratedTransactionsCount = 0;

            List<Account> accountsToMigrate = accountDao.getNewAccounts();
            List<Transaction> transactionsToMigrate = transactionDao.getNewTransactions();

            if (accountsToMigrate.isEmpty() && transactionsToMigrate.isEmpty()) {
                new Handler(Looper.getMainLooper()).post(() -> 
                    Toast.makeText(context, "لا توجد حسابات أو معاملات تحتاج إلى ترحيل", Toast.LENGTH_LONG).show());
                return;
            }

            Log.d("MigrationManager", "Starting migration with " + accountsToMigrate.size() + " accounts and " + 
                transactionsToMigrate.size() + " transactions");

            SyncRequest request = new SyncRequest(accountsToMigrate, transactionsToMigrate);
            apiService.syncData("Bearer " + token, request).enqueue(new Callback<SyncResponse>() {
                @Override
                public void onResponse(Call<SyncResponse> call, Response<SyncResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        SyncResponse syncResponse = response.body();
                        Log.d("MigrationManager", "Received response from server");
                        
                        executor.execute(() -> {
                            // تحديث الحسابات
                            for (Account account : accountsToMigrate) {
                                Long serverId = syncResponse.getAccountServerId(account.getId());
                                Log.d("MigrationManager", "Account mapping: localId=" + account.getId() + 
                                    ", serverId=" + serverId);
                                
                                if (serverId != null && serverId > 0) {
                                    account.setServerId(serverId);
                                    accountDao.update(account);
                                    migratedAccountsCount++;
                                    Log.d("MigrationManager", "Account migrated: localId=" + account.getId() + 
                                        ", serverId=" + serverId + ", total migrated: " + migratedAccountsCount);
                                } else {
                                    Log.e("MigrationManager", "Failed to get server ID for account: localId=" + 
                                        account.getId());
                                }
                            }

                            // تحديث المعاملات
                            for (Transaction transaction : transactionsToMigrate) {
                                Long serverId = syncResponse.getTransactionServerId(transaction.getId());
                                Log.d("MigrationManager", "Transaction mapping: localId=" + transaction.getId() + 
                                    ", serverId=" + serverId);
                                
                                if (serverId != null && serverId > 0) {
                                    transaction.setServerId(serverId);
                                    transactionDao.update(transaction);
                                    migratedTransactionsCount++;
                                    Log.d("MigrationManager", "Transaction migrated: localId=" + transaction.getId() + 
                                        ", serverId=" + serverId + ", total migrated: " + migratedTransactionsCount);
                                } else {
                                    Log.e("MigrationManager", "Failed to get server ID for transaction: localId=" + 
                                        transaction.getId());
                                }
                            }

                            Log.d("MigrationManager", "Final counts - Accounts: " + migratedAccountsCount + 
                                ", Transactions: " + migratedTransactionsCount);

                            new Handler(Looper.getMainLooper()).post(() -> {
                                if (migratedAccountsCount > 0 || migratedTransactionsCount > 0) {
                                    String summary = String.format("تم ترحيل %d حساب و %d معاملة بنجاح", 
                                        migratedAccountsCount, migratedTransactionsCount);
                                    Toast.makeText(context, summary, Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(context, "لم يتم تحديث أي بيانات", Toast.LENGTH_LONG).show();
                                }
                            });
                        });
                    } else {
                        StringBuilder errorBuilder = new StringBuilder("فشل في ترحيل البيانات");
                        if (response.errorBody() != null) {
                            try {
                                errorBuilder.append("\n\nالسبب: ").append(response.errorBody().string());
                            } catch (Exception e) {
                                errorBuilder.append("\n\nالسبب: ").append(response.message());
                            }
                        }
                        final String errorMessage = errorBuilder.toString();
                        Log.e("MigrationManager", "Sync failed: " + errorMessage);
                        new Handler(Looper.getMainLooper()).post(() -> {
                            new AlertDialog.Builder(context)
                                .setTitle("خطأ في الترحيل")
                                .setMessage(errorMessage)
                                .setPositiveButton("حسناً", null)
                                .show();
                        });
                    }
                }

                @Override
                public void onFailure(Call<SyncResponse> call, Throwable t) {
                    Log.e("MigrationManager", "Network error: " + t.getMessage());
                    new Handler(Looper.getMainLooper()).post(() -> 
                        Toast.makeText(context, "فشل في الاتصال بالخادم: " + t.getMessage(), Toast.LENGTH_SHORT).show());
                }
            });
        });
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
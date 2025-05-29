package com.hillal.acc.data.sync;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import android.app.AlertDialog;
import android.util.Log;

import com.hillal.acc.data.room.AppDatabase;
import com.hillal.acc.data.room.AccountDao;
import com.hillal.acc.data.room.TransactionDao;
import com.hillal.acc.data.model.Account;
import com.hillal.acc.data.model.Transaction;
import com.hillal.acc.data.remote.ApiService;
import com.hillal.acc.data.remote.RetrofitClient;
import com.hillal.acc.data.remote.ApiService.SyncRequest;
import com.hillal.acc.data.remote.ApiService.SyncResponse;
import com.hillal.acc.data.preferences.UserPreferences;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.IOException;
import java.util.ArrayList;

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

            List<Account> accountsToMigrate = accountDao.getNewOrModifiedAccounts();
            List<Transaction> transactionsToMigrate = transactionDao.getNewOrModifiedTransactions();

            if (accountsToMigrate.isEmpty() && transactionsToMigrate.isEmpty()) {
                // Toast.makeText(context, "لا توجد حسابات أو معاملات تحتاج إلى ترحيل", Toast.LENGTH_LONG).show());
                return;
            }

            // إنشاء نسخة من المعاملات مع account_id المحدث للخادم
            List<Transaction> transactionsForServer = new ArrayList<>();
            for (Transaction transaction : transactionsToMigrate) {
                Transaction serverTransaction = new Transaction(); // إنشاء نسخة جديدة
                // نسخ جميع البيانات من المعاملة الأصلية
                serverTransaction.setId(transaction.getId());
                serverTransaction.setAmount(transaction.getAmount());
                serverTransaction.setType(transaction.getType());
                serverTransaction.setDescription(transaction.getDescription());
                serverTransaction.setTransactionDate(transaction.getTransactionDate());
                serverTransaction.setCreatedAt(transaction.getCreatedAt());
                serverTransaction.setUpdatedAt(transaction.getUpdatedAt());
                serverTransaction.setLastSyncTime(transaction.getLastSyncTime());
                serverTransaction.setModified(transaction.isModified());
                serverTransaction.setNotes(transaction.getNotes());
                serverTransaction.setWhatsappEnabled(transaction.isWhatsappEnabled());
                
                // تحديث account_id بالـ server_id الخاص بالحساب
                Account relatedAccount = accountDao.getAccountByIdSync(transaction.getAccountId());
                if (relatedAccount != null && relatedAccount.getServerId() > 0) {
                    serverTransaction.setAccountId(relatedAccount.getServerId());
                    Log.d("MigrationManager", "Using server_id " + relatedAccount.getServerId() + 
                        " for account " + transaction.getAccountId());
                } else {
                    serverTransaction.setAccountId(transaction.getAccountId());
                    Log.e("MigrationManager", "Could not find server_id for account: " + 
                        transaction.getAccountId());
                }
                
                transactionsForServer.add(serverTransaction);
            }

            Log.d("MigrationManager", "Starting migration with " + accountsToMigrate.size() + " accounts and " + 
                transactionsToMigrate.size() + " transactions");

            SyncRequest request = new SyncRequest(accountsToMigrate, transactionsForServer);
            apiService.syncData("Bearer " + token, request).enqueue(new Callback<SyncResponse>() {
                @Override
                public void onResponse(Call<SyncResponse> call, Response<SyncResponse> response) {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            SyncResponse syncResponse = response.body();
                            Log.d("MigrationManager", "Received response from server: " + response.body());
                            
                            executor.execute(() -> {
                                try {
                                    // تحديث الحسابات
                                    for (Account account : accountsToMigrate) {
                                        Long serverId = syncResponse.getAccountServerId(account.getId());
                                        Log.d("MigrationManager", "Account mapping: localId=" + account.getId() + 
                                            ", serverId=" + serverId);
                                        
                                        if (serverId != null && serverId > 0) {
                                            account.setServerId(serverId);
                                            account.setSyncStatus(2); 
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
                                            transaction.setSyncStatus(2);
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
                                } catch (Exception e) {
                                    Log.e("MigrationManager", "Error processing server response: " + e.getMessage(), e);
                                    new Handler(Looper.getMainLooper()).post(() -> 
                                        Toast.makeText(context, "خطأ في معالجة البيانات: " + e.getMessage(), Toast.LENGTH_LONG).show());
                                }
                            });
                        } else {
                            String errorBody = "خطأ غير معروف";
                            try {
                                if (response.errorBody() != null) {
                                    errorBody = response.errorBody().string();
                                }
                            } catch (IOException e) {
                                Log.e("MigrationManager", "Error reading error body", e);
                            }
                            final String finalErrorBody = errorBody;
                            new Handler(Looper.getMainLooper()).post(() -> {
                                new AlertDialog.Builder(context)
                                    .setTitle("خطأ في المزامنة")
                                    .setMessage("فشل في الاتصال بالخادم: " + response.code() + "\n" + finalErrorBody)
                                    .setPositiveButton("حسناً", null)
                                    .show();
                            });
                        }
                    } catch (Exception e) {
                        Log.e("MigrationManager", "Unexpected error: " + e.getMessage(), e);
                        new Handler(Looper.getMainLooper()).post(() -> 
                            Toast.makeText(context, "خطأ غير متوقع: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    }
                }

                @Override
                public void onFailure(Call<SyncResponse> call, Throwable t) {
                    Log.e("MigrationManager", "Network error: " + t.getMessage(), t);
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
            new Handler(Looper.getMainLooper()).post(() -> 
                Toast.makeText(context, "يرجى تسجيل الدخول أولاً", Toast.LENGTH_SHORT).show());
            return null;
        }
        return token;
    }
} 
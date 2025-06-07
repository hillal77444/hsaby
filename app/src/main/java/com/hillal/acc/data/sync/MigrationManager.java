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
                return;
            }

            Log.d("MigrationManager", "Starting migration with " + accountsToMigrate.size() + " accounts and " + 
                transactionsToMigrate.size() + " transactions");

            // تحويل المعرفات المحلية إلى معرفات الخادم
            for (Transaction transaction : transactionsToMigrate) {
                Account account = accountDao.getAccountByIdSync(transaction.getAccountId());
                if (account != null && account.getServerId() > 0) {
                    Log.d("MigrationManager", String.format("تحويل معرف الحساب: المحلي=%d, الخادم=%d", 
                        transaction.getAccountId(), account.getServerId()));
                    transaction.setAccountId(account.getServerId());
                } else {
                    Log.e("MigrationManager", "لم يتم العثور على معرف الخادم للحساب: " + transaction.getAccountId());
                }
            }

            SyncRequest request = new SyncRequest(accountsToMigrate, transactionsToMigrate);
            apiService.syncData("Bearer " + token, request).enqueue(new Callback<SyncResponse>() {
                @Override
                public void onResponse(Call<SyncResponse> call, Response<SyncResponse> response) {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            SyncResponse syncResponse = response.body();
                            
                            executor.execute(() -> {
                                try {
                                    // تحديث الحسابات
                                    for (Account account : accountsToMigrate) {
                                        Long serverId = syncResponse.getAccountServerId(account.getId());
                                        
                                        if (serverId != null && serverId > 0) {
                                            Log.d("MigrationManager", String.format("تحديث الحساب: ID=%d, ServerID=%d, Status=%d", 
                                                account.getId(), serverId, account.getSyncStatus()));
                                            
                                            account.setServerId(serverId);
                                            account.setSyncStatus(2); // تم المزامنة
                                            try {
                                                accountDao.update(account);
                                                migratedAccountsCount++;
                                                Log.d("MigrationManager", "تم تحديث الحساب بنجاح");
                                            } catch (Exception e) {
                                                Log.e("MigrationManager", "فشل في تحديث الحساب: " + e.getMessage());
                                            }
                                        } else {
                                            Log.e("MigrationManager", String.format("لم يتم استلام معرف خادم صالح للحساب: %d", account.getId()));
                                        }
                                    }

                                    // تحديث المعاملات
                                    for (Transaction transaction : transactionsToMigrate) {
                                        Long serverId = syncResponse.getTransactionServerId(transaction.getId());
                                        
                                        if (serverId != null && serverId > 0) {
                                            Log.d("MigrationManager", String.format("تحديث المعاملة: ID=%d, ServerID=%d, Status=%d", 
                                                transaction.getId(), serverId, transaction.getSyncStatus()));
                                            
                                            transaction.setServerId(serverId);
                                            transaction.setSyncStatus(2); // تم المزامنة
                                            try {
                                                transactionDao.update(transaction);
                                                migratedTransactionsCount++;
                                                Log.d("MigrationManager", "تم تحديث المعاملة بنجاح");
                                            } catch (Exception e) {
                                                Log.e("MigrationManager", "فشل في تحديث المعاملة: " + e.getMessage());
                                            }
                                        } else {
                                            Log.e("MigrationManager", String.format("لم يتم استلام معرف خادم صالح للمعاملة: %d", transaction.getId()));
                                        }
                                    }

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
                                    Log.e("MigrationManager", "Error processing server response", e);
                                    new Handler(Looper.getMainLooper()).post(() -> 
                                        Toast.makeText(context, "حدث خطأ أثناء معالجة البيانات", Toast.LENGTH_LONG).show());
                                }
                            });
                        } else {
                            String errorMessage = "فشل في الاتصال بالخادم";
                            if (response.code() == 401) {
                                errorMessage = "انتهت صلاحية الجلسة، يرجى تسجيل الدخول مرة أخرى";
                            } else if (response.code() == 403) {
                                errorMessage = "ليس لديك صلاحية للوصول إلى هذه البيانات";
                            } else if (response.code() >= 500) {
                                errorMessage = "الخادم غير متاح حالياً، يرجى المحاولة لاحقاً";
                            }

                            final String finalErrorMessage = errorMessage;
                            new Handler(Looper.getMainLooper()).post(() -> {
                                new AlertDialog.Builder(context)
                                    .setTitle("خطأ في المزامنة")
                                    .setMessage(finalErrorMessage)
                                    .setPositiveButton("حسناً", null)
                                    .show();
                            });
                        }
                    } catch (Exception e) {
                        Log.e("MigrationManager", "Unexpected error", e);
                        new Handler(Looper.getMainLooper()).post(() -> 
                            Toast.makeText(context, "حدث خطأ غير متوقع", Toast.LENGTH_LONG).show());
                    }
                }

                @Override
                public void onFailure(Call<SyncResponse> call, Throwable t) {
                    Log.e("MigrationManager", "Network error", t);
                    String errorMessage = "فشل في الاتصال بالخادم";
                    if (t instanceof java.net.UnknownHostException) {
                        errorMessage = "لا يمكن الوصول إلى الخادم، يرجى التحقق من اتصال الإنترنت";
                    } else if (t instanceof java.net.SocketTimeoutException) {
                        errorMessage = "انتهت مهلة الاتصال بالخادم، يرجى المحاولة مرة أخرى";
                    }
                    
                    final String finalErrorMessage = errorMessage;
                    new Handler(Looper.getMainLooper()).post(() -> 
                        Toast.makeText(context, finalErrorMessage, Toast.LENGTH_LONG).show());
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
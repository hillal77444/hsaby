package com.hillal.hhhhhhh.data.remote;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.model.Transaction;
import com.hillal.hhhhhhh.data.room.AccountDao;
import com.hillal.hhhhhhh.data.room.TransactionDao;
import com.google.gson.Gson;
import com.hillal.hhhhhhh.data.model.PendingOperation;
import com.hillal.hhhhhhh.data.room.PendingOperationDao;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class DataManager {
    private static final String TAG = "DataManager";
    private final Context context;
    private final ApiService apiService;
    private final AccountDao accountDao;
    private final TransactionDao transactionDao;
    private final PendingOperationDao pendingOperationDao;
    private final Handler handler;
    private final ExecutorService executor;
    private final Gson gson;
    private static final long TOKEN_REFRESH_THRESHOLD = 3600000; // ساعة واحدة قبل انتهاء الصلاحية
    private static final String TOKEN_EXPIRY_KEY = "token_expiry_time";
    private static final long INVALID_SERVER_ID = 0;
    private static final long NEW_TRANSACTION_SERVER_ID = -System.currentTimeMillis();
    private static final int MAX_SYNC_ATTEMPTS = 3;
    private static final long SYNC_TIMEOUT = 5 * 60 * 1000; // 5 دقائق
    private final Map<Long, Integer> syncAttempts = new ConcurrentHashMap<>();
    private final Map<Long, Long> lastSyncTimes = new ConcurrentHashMap<>();

    public DataManager(Context context, AccountDao accountDao, TransactionDao transactionDao, PendingOperationDao pendingOperationDao) {
        this.context = context;
        this.apiService = RetrofitClient.getApiService();
        this.accountDao = accountDao;
        this.transactionDao = transactionDao;
        this.pendingOperationDao = pendingOperationDao;
        this.handler = new Handler(Looper.getMainLooper());
        this.executor = Executors.newSingleThreadExecutor();
        this.gson = new Gson();
    }

    public interface DataCallback {
        void onSuccess();
        void onError(String error);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void checkAndRefreshToken(DataCallback callback) {
        if (!isNetworkAvailable()) {
            callback.onSuccess();
            return;
        }

        String currentToken = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .getString("token", null);
        
        if (currentToken == null) {
            callback.onError("لا يوجد توكن للتجديد");
            return;
        }

        // إزالة "Bearer " إذا كان موجوداً
        if (currentToken.startsWith("Bearer ")) {
            currentToken = currentToken.substring(7);
        }

        // محاولة تجديد التوكن مباشرة
        apiService.refreshToken("Bearer " + currentToken).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String newToken = response.body().get("token");
                    if (newToken == null) {
                        Log.e(TAG, "Token is null in response");
                        callback.onError("Token is null in response");
                        return;
                    }
                    
                    long newExpiryTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000); // 24 ساعة
                    
                    // حفظ التوكن الجديد ووقت انتهاء الصلاحية
                    context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                            .edit()
                            .putString("token", newToken)
                            .putLong(TOKEN_EXPIRY_KEY, newExpiryTime)
                            .apply();
                    
                    Log.d(TAG, "تم تجديد التوكن بنجاح");
                    callback.onSuccess();
                } else {
                    String errorMessage = "Unknown error";
                    try {
                        if (response.errorBody() != null) {
                            errorMessage = response.errorBody().string();
                        }
                    } catch (IOException e) {
                        errorMessage = "Error reading response";
                    }
                    Log.e(TAG, "فشل في تجديد التوكن: " + errorMessage + ", Response code: " + response.code());
                    callback.onError("فشل في تجديد التوكن: " + errorMessage);
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Log.e(TAG, "خطأ في تجديد التوكن: " + t.getMessage());
                callback.onError("خطأ في تجديد التوكن: " + t.getMessage());
            }
        });
    }

    public void fetchDataFromServer(DataCallback callback) {
        String token = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .getString("token", null);

        if (token == null) {
            Log.e(TAG, "Token is null");
            callback.onError("User not authenticated");
            return;
        }

        Log.d(TAG, "Starting fetchDataFromServer with token: " + token);

        if (!isNetworkAvailable()) {
            Log.e(TAG, "No network connection");
            callback.onError("No internet connection");
            return;
        }

        // حذف جميع البيانات المحلية أولاً
        executor.execute(() -> {
            try {
                // حذف جميع الحسابات والمعاملات
                accountDao.deleteAllAccounts();
                transactionDao.deleteAllTransactions();
                if (pendingOperationDao != null) {
                    pendingOperationDao.deleteAllPendingOperations();
                }
                Log.d(TAG, "Local data deleted successfully");

                // جلب الحسابات من السيرفر
                Log.d(TAG, "Fetching accounts from server...");
                apiService.getAccounts("Bearer " + token).enqueue(new Callback<List<Account>>() {
                    @Override
                    public void onResponse(Call<List<Account>> call, Response<List<Account>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Account> accounts = response.body();
                            Log.d(TAG, "Received " + accounts.size() + " accounts from server");
                            
                            executor.execute(() -> {
                                try {
                                    // تحديث وقت المزامنة وحالة المزامنة لجميع الحسابات
                                    for (Account account : accounts) {
                                        try {
                                            account.setLastSyncTime(System.currentTimeMillis());
                                            account.setSyncStatus(2); // SYNCED
                                            Log.d(TAG, "Processing account: " + account.getName() + 
                                                  " (ID: " + account.getServerId() + 
                                                  ", Phone: " + account.getPhoneNumber() + ")");
                                            
                                            // التحقق من وجود حساب بنفس server_id
                                            Account existingAccount = accountDao.getAccountByServerIdSync(account.getServerId());
                                            if (existingAccount != null) {
                                                // تحديث الحساب الموجود
                                                account.setId(existingAccount.getId());
                                                accountDao.update(account);
                                                Log.d(TAG, "Updated existing account: " + account.getServerId());
                                            } else {
                                                // إضافة حساب جديد
                                                accountDao.insert(account);
                                                Log.d(TAG, "Added new account: " + account.getServerId());
                                            }
                                        } catch (Exception e) {
                                            StringBuilder errorBuilder = new StringBuilder("Error processing account: ")
                                                    .append(account.getServerId())
                                                    .append("\nError: ")
                                                    .append(e.getMessage())
                                                    .append("\nStack trace: ")
                                                    .append(Log.getStackTraceString(e));
                                            final String errorMessage = errorBuilder.toString();
                                            Log.e(TAG, errorMessage);
                                            throw e;
                                        }
                                    }
                                    Log.d(TAG, "All accounts processed successfully");

                                    // بعد اكتمال جلب الحسابات، نقوم بجلب المعاملات
                                    fetchTransactions(token, callback);
                                } catch (Exception e) {
                                    final String errorMessage = "Error saving accounts: " + e.getMessage() + 
                                                       "\nStack trace: " + Log.getStackTraceString(e);
                                    Log.e(TAG, errorMessage);
                                    handler.post(() -> callback.onError(errorMessage));
                                }
                            });
                        } else {
                            StringBuilder errorBuilder = new StringBuilder("Failed to fetch accounts: ")
                                    .append(response.code());
                            try {
                                if (response.errorBody() != null) {
                                    errorBuilder.append("\n").append(response.errorBody().string());
                                }
                            } catch (IOException e) {
                                errorBuilder.append("\nError reading response: ").append(e.getMessage());
                            }
                            final String errorMessage = errorBuilder.toString();
                            Log.e(TAG, errorMessage);
                            handler.post(() -> callback.onError(errorMessage));
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Account>> call, Throwable t) {
                        final String errorMessage = "Network error while fetching accounts: " + t.getMessage() + 
                                           "\nStack trace: " + Log.getStackTraceString(t);
                        Log.e(TAG, errorMessage);
                        handler.post(() -> callback.onError(errorMessage));
                    }
                });
            } catch (Exception e) {
                final String errorMessage = "Error in fetchDataFromServer: " + e.getMessage() + 
                                   "\nStack trace: " + Log.getStackTraceString(e);
                Log.e(TAG, errorMessage);
                handler.post(() -> callback.onError(errorMessage));
            }
        });
    }

    private void fetchTransactions(String token, DataCallback callback) {
        apiService.getTransactions("Bearer " + token).enqueue(new Callback<List<Transaction>>() {
            @Override
            public void onResponse(Call<List<Transaction>> call, Response<List<Transaction>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Transaction> transactions = response.body();
                    Log.d(TAG, "Received " + transactions.size() + " transactions from server");
                    
                    executor.execute(() -> {
                        try {
                            for (Transaction transaction : transactions) {
                                try {
                                    transaction.setLastSyncTime(System.currentTimeMillis());
                                    transaction.setSyncStatus(2); // SYNCED
                                    
                                    // التحقق من وجود معاملة بنفس server_id
                                    Transaction existingTransaction = transactionDao.getTransactionByServerIdSync(transaction.getServerId());
                                    if (existingTransaction != null) {
                                        // تحديث المعاملة الموجودة
                                        transaction.setId(existingTransaction.getId());
                                        transactionDao.update(transaction);
                                        Log.d(TAG, "Updated existing transaction: " + transaction.getServerId());
                                    } else {
                                        // إضافة معاملة جديدة
                                        transactionDao.insert(transaction);
                                        Log.d(TAG, "Added new transaction: " + transaction.getServerId());
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error processing transaction: " + transaction.getServerId() + 
                                          "\nError: " + e.getMessage() + 
                                          "\nStack trace: " + Log.getStackTraceString(e));
                                }
                            }
                            Log.d(TAG, "All transactions processed successfully");
                            handler.post(() -> callback.onSuccess());
                        } catch (Exception e) {
                            final String errorMessage = "Error saving transactions: " + e.getMessage() + 
                                               "\nStack trace: " + Log.getStackTraceString(e);
                            Log.e(TAG, errorMessage);
                            handler.post(() -> callback.onError(errorMessage));
                        }
                    });
                } else {
                    StringBuilder errorBuilder = new StringBuilder("Failed to fetch transactions: ")
                            .append(response.code());
                    try {
                        if (response.errorBody() != null) {
                            errorBuilder.append("\n").append(response.errorBody().string());
                        }
                    } catch (IOException e) {
                        errorBuilder.append("\nError reading response: ").append(e.getMessage());
                    }
                    final String errorMessage = errorBuilder.toString();
                    Log.e(TAG, errorMessage);
                    handler.post(() -> callback.onError(errorMessage));
                }
            }

            @Override
            public void onFailure(Call<List<Transaction>> call, Throwable t) {
                final String errorMessage = "Network error while fetching transactions: " + t.getMessage() + 
                                   "\nStack trace: " + Log.getStackTraceString(t);
                Log.e(TAG, errorMessage);
                handler.post(() -> callback.onError(errorMessage));
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
} 
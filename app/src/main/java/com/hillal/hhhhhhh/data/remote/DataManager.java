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

        // التحقق من صلاحية التوكن وتجديده إذا لزم الأمر
        checkAndRefreshToken(new DataCallback() {
            @Override
            public void onSuccess() {
                // بعد تجديد التوكن، نقوم بجلب البيانات
                String newToken = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                        .getString("token", null);
                
                Log.d(TAG, "Token refreshed successfully, new token: " + newToken);
                Log.d(TAG, "Starting full sync - deleting local data first...");

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
                        apiService.getAccounts("Bearer " + newToken).enqueue(new Callback<List<Account>>() {
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
                                            fetchTransactions(newToken, callback);
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

            @Override
            public void onError(String error) {
                String errorMessage = "Error refreshing token: " + error;
                Log.e(TAG, errorMessage);
                callback.onError(errorMessage);
            }
        });
    }

    private void fetchTransactions(String token, DataCallback callback) {
        Log.d(TAG, "Fetching transactions from server...");
        
        apiService.getTransactions("Bearer " + token).enqueue(new Callback<List<Transaction>>() {
            @Override
            public void onResponse(Call<List<Transaction>> call, Response<List<Transaction>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Transaction> transactions = response.body();
                    Log.d(TAG, "Received " + transactions.size() + " transactions from server");
                    
                    executor.execute(() -> {
                        try {
                            // إضافة جميع المعاملات
                            for (Transaction transaction : transactions) {
                                try {
                                    // التحقق من وجود الحساب المرتبط بالمعاملة
                                    Account account = accountDao.getAccountByServerIdSync(transaction.getAccountId());
                                    if (account == null) {
                                        Log.e(TAG, "Account not found for transaction: " + transaction.getServerId() + 
                                              ", Account ID: " + transaction.getAccountId());
                                        continue; // تخطي هذه المعاملة
                                    }

                                    // تحديث معرف الحساب المحلي
                                    transaction.setAccountId(account.getId());

                                    // التحقق من وجود معاملة بنفس server_id
                                    Transaction existingTransaction = transactionDao.getTransactionByServerIdSync(transaction.getServerId());
                                    if (existingTransaction != null) {
                                        // تحديث المعاملة الموجودة
                                        transaction.setId(existingTransaction.getId());
                                        transactionDao.update(transaction);
                                        Log.d(TAG, "تم تحديث معاملة موجودة: server_id=" + transaction.getServerId() + 
                                              ", amount=" + transaction.getAmount() + 
                                              ", date=" + transaction.getTransactionDate());
                                    } else {
                                        // إضافة معاملة جديدة
                                        transactionDao.insert(transaction);
                                        Log.d(TAG, "تم إضافة معاملة جديدة: server_id=" + transaction.getServerId() + 
                                              ", amount=" + transaction.getAmount() + 
                                              ", date=" + transaction.getTransactionDate());
                                    }
                                } catch (Exception e) {
                                    StringBuilder errorBuilder = new StringBuilder("خطأ في معالجة المعاملة: server_id=")
                                            .append(transaction.getServerId())
                                            .append(", amount=")
                                            .append(transaction.getAmount())
                                            .append(", date=")
                                            .append(transaction.getTransactionDate())
                                            .append("\nالخطأ: ")
                                            .append(e.getMessage())
                                            .append("\nStack trace: ")
                                            .append(Log.getStackTraceString(e));
                                    final String errorMessage = errorBuilder.toString();
                                    Log.e(TAG, errorMessage);
                                    throw e;
                                }
                            }
                            
                            // تحديث وقت آخر مزامنة
                            context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
                                    .edit()
                                    .putLong("last_sync_time", System.currentTimeMillis())
                                    .apply();

                            Log.d(TAG, "Full sync completed successfully");
                            handler.post(() -> callback.onSuccess());
                        } catch (Exception e) {
                            StringBuilder errorBuilder = new StringBuilder("خطأ في حفظ المعاملات: ")
                                    .append(e.getMessage())
                                    .append("\nStack trace: ")
                                    .append(Log.getStackTraceString(e));
                            final String errorMessage = errorBuilder.toString();
                            Log.e(TAG, errorMessage);
                            handler.post(() -> callback.onError(errorMessage));
                        }
                    });
                } else {
                    StringBuilder errorBuilder = new StringBuilder("فشل في جلب المعاملات: ")
                            .append(response.code());
                    try {
                        if (response.errorBody() != null) {
                            errorBuilder.append("\n").append(response.errorBody().string());
                        }
                    } catch (IOException e) {
                        errorBuilder.append("\nخطأ في قراءة رسالة الخطأ: ").append(e.getMessage());
                    }
                    final String errorMessage = errorBuilder.toString();
                    Log.e(TAG, errorMessage);
                    handler.post(() -> callback.onError(errorMessage));
                }
            }

            @Override
            public void onFailure(Call<List<Transaction>> call, Throwable t) {
                StringBuilder errorBuilder = new StringBuilder("خطأ في الاتصال: ")
                        .append(t.getMessage())
                        .append("\nStack trace: ")
                        .append(Log.getStackTraceString(t));
                final String errorMessage = errorBuilder.toString();
                Log.e(TAG, errorMessage);
                handler.post(() -> callback.onError(errorMessage));
            }
        });
    }

    public void deleteAllData() {
        executor.execute(() -> {
            try {
                accountDao.deleteAllAccounts();
                transactionDao.deleteAllTransactions();
                if (pendingOperationDao != null) {
                    pendingOperationDao.deleteAllPendingOperations();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error deleting all data: " + e.getMessage());
            }
        });
    }

    private void proceedWithFullSync(String token, DataCallback callback) {
        Log.d(TAG, "بدء المزامنة الكاملة...");

        // جلب جميع الحسابات
        apiService.getAccounts("Bearer " + token).enqueue(new Callback<List<Account>>() {
            @Override
            public void onResponse(Call<List<Account>> call, Response<List<Account>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Account> accounts = response.body();
                    Log.d(TAG, "تم جلب " + accounts.size() + " حساب من السيرفر");
                    
                    executor.execute(() -> {
                        try {
                            // إضافة جميع الحسابات
                            for (Account account : accounts) {
                                try {
                                    // التحقق من وجود حساب بنفس server_id
                                    Account existingAccount = accountDao.getAccountByServerIdSync(account.getServerId());
                                    if (existingAccount != null) {
                                        // تحديث الحساب الموجود
                                        account.setId(existingAccount.getId());
                                        accountDao.update(account);
                                        Log.d(TAG, "تم تحديث حساب موجود: server_id=" + account.getServerId() + 
                                              ", name=" + account.getName() + 
                                              ", phone=" + account.getPhoneNumber());
                                    } else {
                                        // إضافة حساب جديد
                                        accountDao.insert(account);
                                        Log.d(TAG, "تم إضافة حساب جديد: server_id=" + account.getServerId() + 
                                              ", name=" + account.getName() + 
                                              ", phone=" + account.getPhoneNumber());
                                    }
                                } catch (Exception e) {
                                    StringBuilder errorBuilder = new StringBuilder("خطأ في معالجة الحساب: server_id=")
                                            .append(account.getServerId())
                                            .append(", name=")
                                            .append(account.getName())
                                            .append(", phone=")
                                            .append(account.getPhoneNumber())
                                            .append("\nالخطأ: ")
                                            .append(e.getMessage())
                                            .append("\nStack trace: ")
                                            .append(Log.getStackTraceString(e));
                                    final String errorMessage = errorBuilder.toString();
                                    Log.e(TAG, errorMessage);
                                    throw e;
                                }
                            }
                            
                            // جلب جميع المعاملات
                            fetchAllTransactions(token, callback);
                        } catch (Exception e) {
                            StringBuilder errorBuilder = new StringBuilder("خطأ في حفظ الحسابات: ")
                                    .append(e.getMessage())
                                    .append("\nStack trace: ")
                                    .append(Log.getStackTraceString(e));
                            final String errorMessage = errorBuilder.toString();
                            Log.e(TAG, errorMessage);
                            handler.post(() -> callback.onError(errorMessage));
                        }
                    });
                } else {
                    StringBuilder errorBuilder = new StringBuilder("فشل في جلب الحسابات: ")
                            .append(response.code());
                    try {
                        if (response.errorBody() != null) {
                            errorBuilder.append("\n").append(response.errorBody().string());
                        }
                    } catch (IOException e) {
                        errorBuilder.append("\nخطأ في قراءة رسالة الخطأ: ").append(e.getMessage());
                    }
                    final String errorMessage = errorBuilder.toString();
                    Log.e(TAG, errorMessage);
                    handler.post(() -> callback.onError(errorMessage));
                }
            }

            @Override
            public void onFailure(Call<List<Account>> call, Throwable t) {
                StringBuilder errorBuilder = new StringBuilder("خطأ في الاتصال: ")
                        .append(t.getMessage())
                        .append("\nStack trace: ")
                        .append(Log.getStackTraceString(t));
                final String errorMessage = errorBuilder.toString();
                Log.e(TAG, errorMessage);
                handler.post(() -> callback.onError(errorMessage));
            }
        });
    }

    private void fetchAllTransactions(String token, DataCallback callback) {
        Log.d(TAG, "جلب جميع المعاملات...");
        
        apiService.getTransactions("Bearer " + token).enqueue(new Callback<List<Transaction>>() {
            @Override
            public void onResponse(Call<List<Transaction>> call, Response<List<Transaction>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Transaction> transactions = response.body();
                    Log.d(TAG, "تم جلب " + transactions.size() + " معاملة من السيرفر");
                    
                    executor.execute(() -> {
                        try {
                            // إضافة جميع المعاملات
                            for (Transaction transaction : transactions) {
                                try {
                                    // التحقق من وجود الحساب المرتبط بالمعاملة
                                    Account account = accountDao.getAccountByServerIdSync(transaction.getAccountId());
                                    if (account == null) {
                                        Log.e(TAG, "Account not found for transaction: " + transaction.getServerId() + 
                                              ", Account ID: " + transaction.getAccountId());
                                        continue; // تخطي هذه المعاملة
                                    }

                                    // تحديث معرف الحساب المحلي
                                    transaction.setAccountId(account.getId());

                                    // التحقق من وجود معاملة بنفس server_id
                                    Transaction existingTransaction = transactionDao.getTransactionByServerIdSync(transaction.getServerId());
                                    if (existingTransaction != null) {
                                        // تحديث المعاملة الموجودة
                                        transaction.setId(existingTransaction.getId());
                                        transactionDao.update(transaction);
                                        Log.d(TAG, "تم تحديث معاملة موجودة: server_id=" + transaction.getServerId() + 
                                              ", amount=" + transaction.getAmount() + 
                                              ", date=" + transaction.getTransactionDate());
                                    } else {
                                        // إضافة معاملة جديدة
                                        transactionDao.insert(transaction);
                                        Log.d(TAG, "تم إضافة معاملة جديدة: server_id=" + transaction.getServerId() + 
                                              ", amount=" + transaction.getAmount() + 
                                              ", date=" + transaction.getTransactionDate());
                                    }
                                } catch (Exception e) {
                                    StringBuilder errorBuilder = new StringBuilder("خطأ في معالجة المعاملة: server_id=")
                                            .append(transaction.getServerId())
                                            .append(", amount=")
                                            .append(transaction.getAmount())
                                            .append(", date=")
                                            .append(transaction.getTransactionDate())
                                            .append("\nالخطأ: ")
                                            .append(e.getMessage())
                                            .append("\nStack trace: ")
                                            .append(Log.getStackTraceString(e));
                                    final String errorMessage = errorBuilder.toString();
                                    Log.e(TAG, errorMessage);
                                    throw e;
                                }
                            }
                            
                            // تحديث وقت آخر مزامنة
                            context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
                                    .edit()
                                    .putLong("last_sync_time", System.currentTimeMillis())
                                    .apply();
                            
                            Log.d(TAG, "تمت المزامنة الكاملة بنجاح");
                            handler.post(() -> callback.onSuccess());
                        } catch (Exception e) {
                            StringBuilder errorBuilder = new StringBuilder("خطأ في حفظ المعاملات: ")
                                    .append(e.getMessage())
                                    .append("\nStack trace: ")
                                    .append(Log.getStackTraceString(e));
                            final String errorMessage = errorBuilder.toString();
                            Log.e(TAG, errorMessage);
                            handler.post(() -> callback.onError(errorMessage));
                        }
                    });
                } else {
                    StringBuilder errorBuilder = new StringBuilder("فشل في جلب المعاملات: ")
                            .append(response.code());
                    try {
                        if (response.errorBody() != null) {
                            errorBuilder.append("\n").append(response.errorBody().string());
                        }
                    } catch (IOException e) {
                        errorBuilder.append("\nخطأ في قراءة رسالة الخطأ: ").append(e.getMessage());
                    }
                    final String errorMessage = errorBuilder.toString();
                    Log.e(TAG, errorMessage);
                    handler.post(() -> callback.onError(errorMessage));
                }
            }

            @Override
            public void onFailure(Call<List<Transaction>> call, Throwable t) {
                StringBuilder errorBuilder = new StringBuilder("خطأ في الاتصال: ")
                        .append(t.getMessage())
                        .append("\nStack trace: ")
                        .append(Log.getStackTraceString(t));
                final String errorMessage = errorBuilder.toString();
                Log.e(TAG, errorMessage);
                handler.post(() -> callback.onError(errorMessage));
            }
        });
    }

    private void proceedWithSync(DataCallback callback) {
        Log.d(TAG, "بدء مزامنة التغييرات...");

        // جلب آخر وقت مزامنة
        long lastSyncTime = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
                .getLong("last_sync_time", 0);

        String token = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .getString("token", null);

        // جلب التغييرات من السيرفر
        apiService.getChanges("Bearer " + token, lastSyncTime).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> changes = response.body();
                    
                    executor.execute(() -> {
                        try {
                            // معالجة الحسابات المحدثة
                            List<Account> accounts = (List<Account>) changes.get("accounts");
                            if (accounts != null) {
                                for (Account account : accounts) {
                                    if (account.getServerId() > 0) {
                                        Account existingAccount = accountDao.getAccountByServerIdSync(account.getServerId());
                                        if (existingAccount != null) {
                                            accountDao.update(account);
                                            Log.d(TAG, "تم تحديث حساب: " + account.getServerId());
                                        } else {
                                            accountDao.insert(account);
                                            Log.d(TAG, "تم إضافة حساب جديد: " + account.getServerId());
                                        }
                                    } else {
                                        accountDao.insert(account);
                                        Log.d(TAG, "تم إضافة حساب جديد بدون server_id");
                                    }
                                }
                            }

                            // معالجة المعاملات المحدثة
                            List<Transaction> transactions = (List<Transaction>) changes.get("transactions");
                            if (transactions != null) {
                                for (Transaction transaction : transactions) {
                                    if (transaction.getServerId() > 0) {
                                        Transaction existingTransaction = transactionDao.getTransactionByServerIdSync(transaction.getServerId());
                                        if (existingTransaction != null) {
                                            transactionDao.update(transaction);
                                            Log.d(TAG, "تم تحديث معاملة: " + transaction.getServerId());
                                        } else {
                                            transactionDao.insert(transaction);
                                            Log.d(TAG, "تم إضافة معاملة جديدة: " + transaction.getServerId());
                                        }
                                    } else {
                                        transactionDao.insert(transaction);
                                        Log.d(TAG, "تم إضافة معاملة جديدة بدون server_id");
                                    }
                                }
                            }

                            // تحديث وقت آخر مزامنة
                            context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
                                    .edit()
                                    .putLong("last_sync_time", System.currentTimeMillis())
                                    .apply();

                            // مزامنة العمليات المعلقة إذا كانت موجودة
                            if (pendingOperationDao.getPendingOperationsCount() > 0) {
                                syncPendingOperations(new DataCallback() {
                                    @Override
                                    public void onSuccess() {
                                        Log.d(TAG, "تمت مزامنة التغييرات بنجاح");
                                        handler.post(() -> callback.onSuccess());
                                    }

                                    @Override
                                    public void onError(String error) {
                                        Log.e(TAG, "خطأ في مزامنة العمليات المعلقة: " + error);
                                        handler.post(() -> callback.onError(error));
                                    }
                                });
                            } else {
                                Log.d(TAG, "تمت مزامنة التغييرات بنجاح");
                                handler.post(() -> callback.onSuccess());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "خطأ في معالجة التغييرات: " + e.getMessage());
                            handler.post(() -> callback.onError("خطأ في معالجة التغييرات"));
                        }
                    });
                } else {
                    Log.e(TAG, "فشل في جلب التغييرات: " + response.code());
                    handler.post(() -> callback.onError("فشل في جلب التغييرات"));
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "خطأ في الاتصال: " + t.getMessage());
                handler.post(() -> callback.onError("خطأ في الاتصال"));
            }
        });
    }

    public void updateTransaction(Transaction transaction, DataCallback callback) {
        if (!isNetworkAvailable()) {
            // حفظ العملية في قائمة العمليات المعلقة
            executor.execute(() -> {
                try {
                    String transactionJson = gson.toJson(transaction);
                    PendingOperation operation = new PendingOperation("UPDATE", transaction.getId(), transactionJson);
                    pendingOperationDao.insert(operation);
                    transactionDao.update(transaction);
                    Log.d(TAG, "تم حفظ عملية التحديث في قائمة العمليات المعلقة");
                    handler.post(() -> callback.onSuccess());
                } catch (Exception e) {
                    Log.e(TAG, "خطأ في حفظ العملية المعلقة: " + e.getMessage());
                    handler.post(() -> callback.onError("خطأ في حفظ العملية المعلقة: " + e.getMessage()));
                }
            });
            return;
        }

        String token = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .getString("token", null);

        if (token == null) {
            handler.post(() -> callback.onError("يرجى تسجيل الدخول أولاً"));
            return;
        }

        apiService.updateTransaction("Bearer " + token, transaction.getId(), transaction)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            // تحديث القيد في قاعدة البيانات المحلية
                            executor.execute(() -> {
                                try {
                                    transactionDao.update(transaction);
                                    Log.d(TAG, "تم تحديث القيد بنجاح");
                                    handler.post(() -> callback.onSuccess());
                                } catch (Exception e) {
                                    Log.e(TAG, "خطأ في تحديث القيد: " + e.getMessage());
                                    handler.post(() -> callback.onError("خطأ في تحديث القيد: " + e.getMessage()));
                                }
                            });
                        } else {
                            handler.post(() -> callback.onError("فشل في تحديث القيد"));
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        handler.post(() -> callback.onError("خطأ في الاتصال: " + t.getMessage()));
                    }
                });
    }

    public void deleteTransaction(long transactionId, DataCallback callback) {
        if (!isNetworkAvailable()) {
            handler.post(() -> callback.onError("يرجى الاتصال بالإنترنت لحذف المعاملة"));
            return;
        }

        String token = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .getString("token", null);

        if (token == null) {
            handler.post(() -> callback.onError("يرجى تسجيل الدخول أولاً"));
            return;
        }

        // التحقق من وجود المعاملة في قاعدة البيانات المحلية
        executor.execute(() -> {
            try {
                Transaction transaction = transactionDao.getTransactionById(transactionId).getValue();
                if (transaction == null) {
                    handler.post(() -> callback.onError("المعاملة غير موجودة"));
                    return;
                }

                // التحقق من وجود server_id
                if (transaction.getServerId() <= 0) {
                    handler.post(() -> callback.onError("لا يمكن حذف المعاملة لأنها غير مزامنة مع السيرفر"));
                    return;
                }

                // إرسال طلب الحذف إلى السيرفر باستخدام server_id
                apiService.deleteTransaction("Bearer " + token, transaction.getServerId())
                        .enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                if (response.isSuccessful()) {
                                    // حذف القيد من قاعدة البيانات المحلية
                                    executor.execute(() -> {
                                        try {
                                            transactionDao.delete(transaction);
                                            Log.d(TAG, "تم حذف القيد بنجاح");
                                            handler.post(() -> callback.onSuccess());
                                        } catch (Exception e) {
                                            Log.e(TAG, "خطأ في حذف القيد: " + e.getMessage());
                                            handler.post(() -> callback.onError("خطأ في حذف القيد: " + e.getMessage()));
                                        }
                                    });
                                } else {
                                    handler.post(() -> callback.onError("فشل في حذف القيد من الخادم"));
                                }
                            }

                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {
                                handler.post(() -> callback.onError("خطأ في الاتصال: " + t.getMessage()));
                            }
                        });
            } catch (Exception e) {
                Log.e(TAG, "خطأ في التحقق من المعاملة: " + e.getMessage());
                handler.post(() -> callback.onError("خطأ في التحقق من المعاملة: " + e.getMessage()));
            }
        });
    }

    public void syncPendingOperations(DataCallback callback) {
        if (!isNetworkAvailable()) {
            handler.post(() -> callback.onError("لا يوجد اتصال بالإنترنت"));
            return;
        }

        executor.execute(() -> {
            try {
                List<PendingOperation> operations = pendingOperationDao.getAllPendingOperations().getValue();
                if (operations == null) {
                    operations = new ArrayList<>();
                }
                for (PendingOperation operation : operations) {
                    if (operation.getOperationType().equals("UPDATE")) {
                        Transaction transaction = gson.fromJson(operation.getTransactionData(), Transaction.class);
                        // التحقق مما إذا كان القيد لا يزال موجوداً محلياً
                        Transaction localTransaction = transactionDao.getTransactionById(transaction.getId()).getValue();
                        if (localTransaction == null) {
                            // إذا كان القيد غير موجود، نحذف العملية المعلقة
                            pendingOperationDao.delete(operation);
                            Log.d(TAG, "تم تجاهل عملية التحديث لأن القيد غير موجود محلياً");
                            continue;
                        }
                        updateTransaction(transaction, new DataCallback() {
                            @Override
                            public void onSuccess() {
                                pendingOperationDao.delete(operation);
                            }

                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "فشل في مزامنة عملية التحديث: " + error);
                            }
                        });
                    } else if (operation.getOperationType().equals("DELETE")) {
                        // التحقق مما إذا كان القيد لا يزال موجوداً محلياً
                        Transaction localTransaction = transactionDao.getTransactionById(operation.getTransactionId()).getValue();
                        if (localTransaction == null) {
                            // إذا كان القيد غير موجود، نحذف العملية المعلقة
                            pendingOperationDao.delete(operation);
                            Log.d(TAG, "تم تجاهل عملية الحذف لأن القيد غير موجود محلياً");
                            continue;
                        }
                        deleteTransaction(operation.getTransactionId(), new DataCallback() {
                            @Override
                            public void onSuccess() {
                                pendingOperationDao.delete(operation);
                            }

                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "فشل في مزامنة عملية الحذف: " + error);
                            }
                        });
                    }
                }
                handler.post(() -> callback.onSuccess());
            } catch (Exception e) {
                Log.e(TAG, "خطأ في مزامنة العمليات المعلقة: " + e.getMessage());
                handler.post(() -> callback.onError("خطأ في مزامنة العمليات المعلقة: " + e.getMessage()));
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
} 
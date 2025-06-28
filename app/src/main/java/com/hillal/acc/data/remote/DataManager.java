package com.hillal.acc.data.remote;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.hillal.acc.data.model.Account;
import com.hillal.acc.data.model.Transaction;
import com.hillal.acc.data.room.AccountDao;
import com.hillal.acc.data.room.TransactionDao;
import com.google.gson.Gson;
import com.hillal.acc.data.model.PendingOperation;
import com.hillal.acc.data.room.PendingOperationDao;
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
import org.json.JSONObject;
import com.hillal.acc.data.model.ServerAppUpdateInfo;
import java.util.HashSet;
import java.util.Set;
import com.hillal.acc.data.entities.Cashbox;
import com.hillal.acc.data.dao.CashboxDao;
import com.hillal.acc.data.room.AppDatabase;

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
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 2000; // 2 seconds
    private Set<Long> serverTransactionIds = new HashSet<>();

    public interface DataCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface ApiCallback {
        void onSuccess(ServerAppUpdateInfo updateInfo);
        void onError(String error);
    }

    public DataManager(Context context, AccountDao accountDao, TransactionDao transactionDao, PendingOperationDao pendingOperationDao) {
        this.context = context;
        this.apiService = RetrofitClient.getInstance().getApiService();
        this.accountDao = accountDao;
        this.transactionDao = transactionDao;
        this.pendingOperationDao = pendingOperationDao;
        this.handler = new Handler(Looper.getMainLooper());
        this.executor = Executors.newSingleThreadExecutor();
        this.gson = new Gson();
    }

    private String getCurrentToken() {
        return context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .getString("token", null);
    }

    public void updateUserDetails(JSONObject userDetails, ApiCallback callback) {
        executor.execute(() -> {
            if (!isNetworkAvailable()) {
                handler.post(() -> callback.onError("لا يوجد اتصال بالإنترنت"));
                return;
            }

            String token = getCurrentToken();
            if (token == null) {
                handler.post(() -> callback.onError("لا يوجد توكن مصادقة"));
                return;
            }

            checkAndRefreshToken(new DataCallback() {
                @Override
                public void onSuccess() {
                    String currentToken = getCurrentToken();
                    if (currentToken == null) {
                        Log.e(TAG, "فشل في الحصول على التوكن بعد التحديث");
                        handler.post(() -> callback.onError("فشل في الحصول على التوكن بعد التحديث"));
                        return;
                    }

                    // تحويل JSONObject إلى JsonObject
                    com.google.gson.JsonObject gsonUserDetails = gson.fromJson(userDetails.toString(), com.google.gson.JsonObject.class);

                    Log.d(TAG, "Making API call to update user details...");
                    apiService.updateUserDetails("Bearer " + currentToken, gsonUserDetails).enqueue(new Callback<Map<String, String>>() {
                        @Override
                        public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                Log.d(TAG, "User details updated successfully on server.");
                                handler.post(() -> callback.onSuccess(null));  // تمرير null لأن هذه العملية لا تحتاج إلى ServerAppUpdateInfo
                            } else {
                                String errorMessage = "خطأ في تحديث بيانات المستخدم: " + response.code();
                                try {
                                    if (response.errorBody() != null) {
                                        errorMessage += "\n" + response.errorBody().string();
                                    }
                                } catch (IOException e) {
                                    Log.e(TAG, "Error reading error body for user details update", e);
                                }
                                final String finalErrorMessage = errorMessage;
                                handler.post(() -> callback.onError(finalErrorMessage));
                            }
                        }

                        @Override
                        public void onFailure(Call<Map<String, String>> call, Throwable t) {
                            handler.post(() -> callback.onError("فشل الاتصال بالخادم أثناء تحديث بيانات المستخدم: " + t.getMessage()));
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    handler.post(() -> callback.onError("فشل تجديد التوكن قبل تحديث بيانات المستخدم: " + error));
                }
            });
        });
    }

    public void checkForUpdates(String currentVersion, ApiCallback callback) {
        Log.d(TAG, "Starting update check...");
        executor.execute(() -> {
            if (!isNetworkAvailable()) {
                Log.e(TAG, "No network connection available");
                handler.post(() -> callback.onError("لا يوجد اتصال بالإنترنت"));
                return;
            }

            String token = getCurrentToken();
            if (token == null) {
                Log.e(TAG, "No authentication token available");
                handler.post(() -> callback.onError("لا يوجد توكن مصادقة"));
                return;
            }

            Log.d(TAG, "Checking and refreshing token...");
            checkAndRefreshToken(new DataCallback() {
                @Override
                public void onSuccess() {
                    String currentToken = getCurrentToken();
                    if (currentToken == null) {
                        Log.e(TAG, "Failed to get token after refresh");
                        handler.post(() -> callback.onError("فشل في الحصول على التوكن بعد التحديث"));
                        return;
                    }

                    Log.d(TAG, "Making API call to check for updates with version: " + currentVersion + " ...");
                    apiService.checkForUpdates("Bearer " + currentToken, currentVersion).enqueue(new Callback<ServerAppUpdateInfo>() {
                        @Override
                        public void onResponse(Call<ServerAppUpdateInfo> call, Response<ServerAppUpdateInfo> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                ServerAppUpdateInfo updateInfo = response.body();
                                Log.d(TAG, "Update check successful, version: " + updateInfo.getVersion());
                                Log.d(TAG, "Download URL from server: " + updateInfo.getDownloadUrl());
                                Log.d(TAG, "Description: " + updateInfo.getDescription());
                                Log.d(TAG, "Force update: " + updateInfo.isForceUpdate());
                                
                                // طباعة البيانات الخام من الاستجابة
                                try {
                                    String rawResponse = new Gson().toJson(response.body());
                                    Log.d(TAG, "Raw server response: " + rawResponse);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error printing raw response", e);
                                }
                                
                                handler.post(() -> callback.onSuccess(updateInfo));
                            } else {
                                String errorMessage = "خطأ في التحقق من التحديثات: " + response.code();
                                try {
                                    if (response.errorBody() != null) {
                                        String errorBody = response.errorBody().string();
                                        Log.e(TAG, "Error response body: " + errorBody);
                                        errorMessage += "\n" + errorBody;
                                    }
                                } catch (IOException e) {
                                    Log.e(TAG, "Error reading error body for update check", e);
                                }
                                final String finalErrorMessage = errorMessage;
                                Log.e(TAG, finalErrorMessage);
                                handler.post(() -> callback.onError(finalErrorMessage));
                            }
                        }

                        @Override
                        public void onFailure(Call<ServerAppUpdateInfo> call, Throwable t) {
                            Log.e(TAG, "Network error while checking for updates", t);
                            handler.post(() -> callback.onError("فشل الاتصال بالخادم أثناء التحقق من التحديثات: " + t.getMessage()));
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Token refresh failed: " + error);
                    handler.post(() -> callback.onError("فشل تجديد التوكن قبل التحقق من التحديثات: " + error));
                }
            });
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void checkAndRefreshToken(DataCallback callback) {
        if (!isNetworkAvailable()) {
            Log.e(TAG, "No network connection available for token refresh");
            callback.onError("لا يوجد اتصال بالإنترنت");
            return;
        }

        String currentToken = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .getString("token", null);
        
        if (currentToken == null) {
            Log.e(TAG, "No token available for refresh");
            callback.onError("لا يوجد توكن للتجديد");
            return;
        }

        Log.d(TAG, "Attempting to refresh token...");
        // إزالة "Bearer " إذا كان موجوداً
        if (currentToken.startsWith("Bearer ")) {
            currentToken = currentToken.substring(7);
        }

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
                    
                    context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                            .edit()
                            .putString("token", newToken)
                            .putLong(TOKEN_EXPIRY_KEY, newExpiryTime)
                            .apply();
                    
                    Log.d(TAG, "Token refreshed successfully");
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
                    Log.e(TAG, "Failed to refresh token: " + errorMessage + ", Response code: " + response.code());
                    callback.onError("فشل في تجديد التوكن: " + errorMessage);
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Log.e(TAG, "Network error while refreshing token", t);
                callback.onError("خطأ في تجديد التوكن: " + t.getMessage());
            }
        });
    }

    private void retryFetchAccounts(String token, int retryCount, DataCallback callback) {
        if (retryCount >= MAX_RETRY_ATTEMPTS) {
            Log.e(TAG, "Max retry attempts reached for fetching accounts");
            handler.post(() -> callback.onError("فشل الاتصال بالخادم بعد عدة محاولات. يرجى المحاولة لاحقاً"));
            return;
        }

        Log.d(TAG, "Retrying to fetch accounts. Attempt: " + (retryCount + 1));
        handler.postDelayed(() -> {
            if (!isNetworkAvailable()) {
                handler.post(() -> callback.onError("لا يوجد اتصال بالإنترنت"));
                return;
            }
            fetchAccountsFromServer(token, retryCount + 1, callback);
        }, RETRY_DELAY_MS);
    }

    private void fetchAccountsFromServer(String token, int retryCount, DataCallback callback) {
        Log.d(TAG, "Fetching accounts from server... Attempt: " + (retryCount + 1));
        apiService.getAccounts("Bearer " + token).enqueue(new Callback<List<Account>>() {
            @Override
            public void onResponse(Call<List<Account>> call, Response<List<Account>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Account> accounts = response.body();
                    Log.d(TAG, "Received " + accounts.size() + " accounts from server");
                    executor.execute(() -> {
                        try {
                            // تحديث وإضافة الحسابات
                            for (Account account : accounts) {
                                try {
                                    account.setLastSyncTime(System.currentTimeMillis());
                                    account.setSyncStatus(2); // SYNCED
                                    Log.d(TAG, "Processing account: " + account.getName() +
                                          " (ID: " + account.getServerId() +
                                          ", Phone: " + account.getPhoneNumber() + ")");
                                    Account existingAccount = accountDao.getAccountByServerIdSync(account.getServerId());
                                    if (existingAccount != null) {
                                        account.setId(existingAccount.getId());
                                        account.setBalance(existingAccount.getBalance());
                                        account.setCurrency(existingAccount.getCurrency());
                                        accountDao.update(account);
                                        Log.d(TAG, "Updated existing account: " + account.getServerId());
                                    } else {
                                        accountDao.insert(account);
                                        Log.d(TAG, "Added new account: " + account.getServerId());
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error processing account: " + account.getServerId(), e);
                                }
                            }
                            Log.d(TAG, "All accounts processed successfully");
                            fetchTransactionsPaged(token, retryCount, new DataCallback() {
                                @Override
                                public void onSuccess() {
                                    // بعد جلب المعاملات، جلب الصناديق
                                    fetchCashboxesFromServer(token, 0, callback);
                                }
                                @Override
                                public void onError(String error) {
                                    // حتى لو فشل جلب المعاملات، حاول جلب الصناديق
                                    fetchCashboxesFromServer(token, 0, callback);
                                }
                            });
                        } catch (Exception e) {
                            Log.e(TAG, "Error saving accounts", e);
                            handler.post(() -> callback.onError("خطأ في حفظ البيانات المحلية"));
                        }
                    });
                } else {
                    String errorMessage = "خطأ في الاتصال بالخادم: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            errorMessage += "\n" + response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Log.e(TAG, errorMessage);
                    retryFetchAccounts(token, retryCount, callback);
                }
            }
            @Override
            public void onFailure(Call<List<Account>> call, Throwable t) {
                Log.e(TAG, "Network error while fetching accounts", t);
                retryFetchAccounts(token, retryCount, callback);
            }
        });
    }

    private void retryFetchTransactions(String token, int limit, int offset, int retryCount, DataCallback callback) {
        if (retryCount >= MAX_RETRY_ATTEMPTS) {
            Log.e(TAG, "Max retry attempts reached for fetching transactions");
            handler.post(() -> callback.onError("فشل الاتصال بالخادم بعد عدة محاولات. يرجى المحاولة لاحقاً"));
            return;
        }

        Log.d(TAG, "Retrying to fetch transactions. Attempt: " + (retryCount + 1));
        handler.postDelayed(() -> {
            if (!isNetworkAvailable()) {
                handler.post(() -> callback.onError("لا يوجد اتصال بالإنترنت"));
                return;
            }
            fetchBatch(token, limit, offset, retryCount + 1, callback);
        }, RETRY_DELAY_MS);
    }

    private void fetchTransactionsPaged(String token, int retryCount, DataCallback callback) {
        int limit = 100;
        int offset = 0;
        serverTransactionIds.clear(); // تأكد من تفريغ القائمة قبل البدء
        fetchBatch(token, limit, offset, retryCount, callback);
    }

    private void fetchBatch(String token, int limit, int offset, int retryCount, DataCallback callback) {
        apiService.getTransactions("Bearer " + token, limit, offset).enqueue(new Callback<List<Transaction>>() {
            @Override
            public void onResponse(Call<List<Transaction>> call, Response<List<Transaction>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Transaction> transactions = response.body();
                    Log.d(TAG, "Received batch of " + transactions.size() + " transactions from server (offset: " + offset + ")");
                    executor.execute(() -> {
                        try {
                            // أضف كل server_id من هذه الدفعة إلى المتغير الخارجي
                            for (Transaction transaction : transactions) {
                                if (transaction.getServerId() > 0) {
                                    serverTransactionIds.add(transaction.getServerId());
                                }
                            }
                            for (Transaction transaction : transactions) {
                                try {
                                    transaction.setLastSyncTime(System.currentTimeMillis());
                                    transaction.setSyncStatus(2);
                                    Transaction existingTransaction = transactionDao.getTransactionByServerIdSync(transaction.getServerId());
                                    if (existingTransaction != null) {
                                        transaction.setId(existingTransaction.getId());
                                        transaction.setAmount(existingTransaction.getAmount());
                                        transaction.setCurrency(existingTransaction.getCurrency());
                                        transactionDao.update(transaction);
                                        Log.d(TAG, "Updated existing transaction: " + transaction.getServerId());
                                    } else {
                                        transactionDao.insert(transaction);
                                        Log.d(TAG, "Added new transaction: " + transaction.getServerId());
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error processing transaction: " + transaction.getServerId(), e);
                                }
                            }
                            if (transactions.size() == limit) {
                                fetchBatch(token, limit, offset + limit, retryCount, callback);
                            } else {
                                Log.d(TAG, "All transactions processed successfully (batched)");
                                try {
                                    // جلب كل المعاملات المحلية
                                    List<Transaction> localTransactions = transactionDao.getAllTransactionsSync();
                                    // حذف المعاملات التي كانت مزامنة مع الخادم وحُذفت منه
                                    for (Transaction t : localTransactions) {
                                        if (t.getServerId() > 0 && !serverTransactionIds.contains(t.getServerId())) {
                                            transactionDao.delete(t);
                                            Log.d(TAG, "Deleted local transaction not found on server: " + t.getServerId());
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error deleting local transactions not found on server", e);
                                }
                                handler.post(callback::onSuccess);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error saving transactions", e);
                            handler.post(() -> callback.onError("خطأ في حفظ البيانات المحلية"));
                        }
                    });
                } else {
                    String errorMessage = "خطأ في الاتصال بالخادم: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            errorMessage += "\n" + response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Log.e(TAG, errorMessage);
                    retryFetchTransactions(token, limit, offset, retryCount, callback);
                }
            }

            @Override
            public void onFailure(Call<List<Transaction>> call, Throwable t) {
                Log.e(TAG, "Network error while fetching transactions", t);
                retryFetchTransactions(token, limit, offset, retryCount, callback);
            }
        });
    }

    public void fetchDataFromServer(DataCallback callback) {
        String token = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .getString("token", null);

        if (token == null) {
            Log.e(TAG, "Token is null");
            callback.onError("يرجى تسجيل الدخول أولاً");
            return;
        }

        if (!isNetworkAvailable()) {
            Log.e(TAG, "No network connection");
            callback.onError("لا يوجد اتصال بالإنترنت");
            return;
        }

        fetchAccountsFromServer(token, 0, callback);
    }

    public void shutdown() {
        executor.shutdown();
    }

    private void fetchCashboxesFromServer(String token, int retryCount, DataCallback callback) {
        Log.d(TAG, "Fetching cashboxes from server... Attempt: " + (retryCount + 1));
        ApiService api = RetrofitClient.getApiService();
        AppDatabase db = AppDatabase.getInstance(context);
        CashboxDao cashboxDao = db.cashboxDao();
        api.getCashboxes("Bearer " + token).enqueue(new Callback<List<Cashbox>>() {
            @Override
            public void onResponse(Call<List<Cashbox>> call, Response<List<Cashbox>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Cashbox> cashboxes = response.body();
                    Log.d(TAG, "Received " + cashboxes.size() + " cashboxes from server");
                    executor.execute(() -> {
                        try {
                            cashboxDao.deleteAll();
                            for (Cashbox cashbox : cashboxes) {
                                cashboxDao.insert(cashbox);
                                Log.d("SYNC_CASHBOX", "Saved cashbox: id=" + cashbox.id + ", name=" + cashbox.name);
                            }
                            Log.d(TAG, "All cashboxes processed successfully");
                            handler.post(callback::onSuccess);
                        } catch (Exception e) {
                            Log.e(TAG, "Error saving cashboxes", e);
                            handler.post(() -> callback.onError("خطأ في حفظ بيانات الصناديق المحلية"));
                        }
                    });
                } else {
                    String errorMessage = "خطأ في الاتصال بالخادم: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            errorMessage += "\n" + response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Log.e(TAG, errorMessage);
                    final String finalErrorMessage = errorMessage;
                    handler.post(() -> callback.onError(finalErrorMessage));
                }
            }
            @Override
            public void onFailure(Call<List<Cashbox>> call, Throwable t) {
                Log.e(TAG, "Network error while fetching cashboxes", t);
                handler.post(() -> callback.onError("فشل الاتصال بالخادم أثناء جلب الصناديق: " + t.getMessage()));
            }
        });
    }

    public void performInitialSyncIfNeeded() {
        SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        boolean initialSyncDone = prefs.getBoolean("initial_sync_done", false);
        if (!initialSyncDone) {
            fetchDataFromServer(new DataCallback() {
                @Override
                public void onSuccess() {
                    prefs.edit().putBoolean("initial_sync_done", true).apply();
                }
                @Override
                public void onError(String error) {
                    // يمكنك إعادة المحاولة أو إعلام المستخدم
                }
            });
        }
    }
} 
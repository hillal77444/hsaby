package com.hillal.hhhhhhh.data.sync;

import android.content.Context;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.content.SharedPreferences;

import com.hillal.hhhhhhh.data.remote.ApiService;
import com.hillal.hhhhhhh.data.remote.RetrofitClient;
import com.hillal.hhhhhhh.data.remote.DataManager;
import com.hillal.hhhhhhh.data.room.AccountDao;
import com.hillal.hhhhhhh.data.room.TransactionDao;
import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.model.Transaction;

import java.util.List;
import java.util.ArrayList;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.google.gson.Gson;
import java.util.concurrent.TimeUnit;

public class SyncManager {
    private static final String TAG = "SyncManager";
    private static final int BATCH_SIZE = 10; // عدد العمليات في كل دفعة
    private static final int MAX_RETRIES = 3; // عدد المحاولات القصوى
    private static final long RETRY_DELAY_MS = 5000; // وقت الانتظار بين المحاولات

    private final Context context;
    private final ApiService apiService;
    private final AccountDao accountDao;
    private final TransactionDao transactionDao;
    private final Handler handler;
    private final ExecutorService executor;
    private boolean isSyncing = false;
    private int currentRetryCount = 0;

    public SyncManager(Context context, AccountDao accountDao, TransactionDao transactionDao) {
        this.context = context;
        this.apiService = RetrofitClient.getInstance().getApiService();
        this.accountDao = accountDao;
        this.transactionDao = transactionDao;
        this.handler = new Handler(Looper.getMainLooper());
        this.executor = Executors.newSingleThreadExecutor();
    }

    public interface SyncCallback {
        void onSuccess();
        void onError(String error);
        void onProgress(int progress, int total);
    }

    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                if (capabilities != null) {
                    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                           capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                }
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error checking network availability: " + e.getMessage());
            return false;
        }
    }

    private String getAuthToken() {
        return context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .getString("token", null);
    }

    public void syncPendingItems(SyncCallback callback) {
        if (!isNetworkAvailable()) {
            callback.onError("لا يوجد اتصال بالإنترنت");
            return;
        }

        if (isSyncing) {
            callback.onError("جاري تنفيذ عملية مزامنة أخرى، يرجى الانتظار");
            return;
        }

        String token = getAuthToken();
        if (token == null) {
            callback.onError("يرجى تسجيل الدخول أولاً");
            return;
        }

        isSyncing = true;
        currentRetryCount = 0;
        executor.execute(() -> {
            try {
                List<Account> pendingAccounts = accountDao.getAccountsByServerId(-1);
                List<Transaction> pendingTransactions = transactionDao.getTransactionsByServerId(-1);

                if (pendingAccounts.isEmpty() && pendingTransactions.isEmpty()) {
                    isSyncing = false;
                    handler.post(() -> callback.onSuccess());
                    return;
                }

                List<List<Account>> accountBatches = splitIntoBatches(pendingAccounts, BATCH_SIZE);
                List<List<Transaction>> transactionBatches = splitIntoBatches(pendingTransactions, BATCH_SIZE);

                int totalBatches = accountBatches.size() + transactionBatches.size();
                int completedBatches = 0;

                // مزامنة الحسابات أولاً
                syncAccountBatchesSequentially(accountBatches, token, new SyncCallback() {
                    @Override
                    public void onSuccess() {
                        // بعد اكتمال مزامنة الحسابات، نبدأ بمزامنة المعاملات
                        syncTransactionBatchesSequentially(transactionBatches, token, new SyncCallback() {
                            @Override
                            public void onSuccess() {
                                isSyncing = false;
                                handler.post(() -> callback.onSuccess());
                            }

                            @Override
                            public void onError(String error) {
                                handleSyncError(error, callback);
                            }

                            @Override
                            public void onProgress(int progress, int total) {
                                completedBatches = accountBatches.size() + progress;
                                handler.post(() -> callback.onProgress(completedBatches, totalBatches));
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        handleSyncError(error, callback);
                    }

                    @Override
                    public void onProgress(int progress, int total) {
                        handler.post(() -> callback.onProgress(progress, totalBatches));
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error in syncPendingItems: " + e.getMessage());
                isSyncing = false;
                handler.post(() -> callback.onError("خطأ في المزامنة: " + e.getMessage()));
            }
        });
    }

    private void syncAccountBatchesSequentially(List<List<Account>> batches, String token, SyncCallback callback) {
        if (batches.isEmpty()) {
            callback.onSuccess();
            return;
        }

        List<Account> currentBatch = batches.get(0);
        List<List<Account>> remainingBatches = batches.subList(1, batches.size());

        syncAccountBatch(currentBatch, token, new SyncCallback() {
            @Override
            public void onSuccess() {
                // بعد نجاح المزامنة، ننتقل للدفعة التالية
                syncAccountBatchesSequentially(remainingBatches, token, callback);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }

            @Override
            public void onProgress(int progress, int total) {
                callback.onProgress(progress, total);
            }
        });
    }

    private void syncTransactionBatchesSequentially(List<List<Transaction>> batches, String token, SyncCallback callback) {
        if (batches.isEmpty()) {
            callback.onSuccess();
            return;
        }

        List<Transaction> currentBatch = batches.get(0);
        List<List<Transaction>> remainingBatches = batches.subList(1, batches.size());

        syncTransactionBatch(currentBatch, token, new SyncCallback() {
            @Override
            public void onSuccess() {
                // بعد نجاح المزامنة، ننتقل للدفعة التالية
                syncTransactionBatchesSequentially(remainingBatches, token, callback);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }

            @Override
            public void onProgress(int progress, int total) {
                callback.onProgress(progress, total);
            }
        });
    }

    private <T> List<List<T>> splitIntoBatches(List<T> items, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < items.size(); i += batchSize) {
            batches.add(items.subList(i, Math.min(i + batchSize, items.size())));
        }
        return batches;
    }

    private void syncAccountBatch(List<Account> accounts, String token, SyncCallback callback) {
        ApiService.SyncRequest syncRequest = new ApiService.SyncRequest(accounts, new ArrayList<>());
        apiService.syncData("Bearer " + token, syncRequest).enqueue(new Callback<ApiService.SyncResponse>() {
            @Override
            public void onResponse(Call<ApiService.SyncResponse> call, Response<ApiService.SyncResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiService.SyncResponse syncResponse = response.body();
                    handleSyncResponse(syncResponse, callback);
                } else {
                    callback.onError("فشلت مزامنة الحسابات");
                }
            }

            @Override
            public void onFailure(Call<ApiService.SyncResponse> call, Throwable t) {
                callback.onError("خطأ في الاتصال: " + t.getMessage());
            }
        });
    }

    private void syncTransactionBatch(List<Transaction> transactions, String token, SyncCallback callback) {
        ApiService.SyncRequest syncRequest = new ApiService.SyncRequest(new ArrayList<>(), transactions);
        apiService.syncData("Bearer " + token, syncRequest).enqueue(new Callback<ApiService.SyncResponse>() {
            @Override
            public void onResponse(Call<ApiService.SyncResponse> call, Response<ApiService.SyncResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiService.SyncResponse syncResponse = response.body();
                    handleSyncResponse(syncResponse, callback);
                } else {
                    callback.onError("فشلت مزامنة المعاملات");
                }
            }

            @Override
            public void onFailure(Call<ApiService.SyncResponse> call, Throwable t) {
                callback.onError("خطأ في الاتصال: " + t.getMessage());
            }
        });
    }

    private void handleSyncError(String error, SyncCallback callback) {
        if (currentRetryCount < MAX_RETRIES) {
            currentRetryCount++;
            handler.postDelayed(() -> {
                Log.d(TAG, "Retrying sync operation. Attempt " + currentRetryCount + " of " + MAX_RETRIES);
                syncPendingItems(callback);
            }, RETRY_DELAY_MS);
        } else {
            isSyncing = false;
            handler.post(() -> callback.onError("فشلت المزامنة بعد " + MAX_RETRIES + " محاولات: " + error));
        }
    }

    private void handleSyncResponse(ApiService.SyncResponse syncResponse, SyncCallback callback) {
        try {
            if (syncResponse != null) {
                // تحديث معرفات السيرفر للحسابات
                for (Account account : syncResponse.getAccounts()) {
                    if (account.getServerId() > 0) {
                        accountDao.update(account);
                    }
                }

                // تحديث معرفات السيرفر للمعاملات
                for (Transaction transaction : syncResponse.getTransactions()) {
                    if (transaction.getServerId() > 0) {
                        transactionDao.update(transaction);
                    }
                }
                callback.onSuccess();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling sync response: " + e.getMessage());
            callback.onError("خطأ في معالجة استجابة المزامنة: " + e.getMessage());
        }
    }

    // الحفاظ على performFullSync كما هي بدون تغيير
    public void performFullSync(SyncCallback callback) {
        if (!isNetworkAvailable()) {
            callback.onError("لا يوجد اتصال بالإنترنت");
            return;
        }

        if (isSyncing) {
            callback.onError("جاري تنفيذ عملية مزامنة أخرى، يرجى الانتظار");
            return;
        }

        String token = getAuthToken();
        if (token == null) {
            callback.onError("يرجى تسجيل الدخول أولاً");
            return;
        }

        isSyncing = true;
        executor.execute(() -> {
            try {
                Log.d(TAG, "بدء المزامنة الكاملة...");
                
                // حذف البيانات المحلية
                accountDao.deleteAllAccounts();
                transactionDao.deleteAllTransactions();
                Log.d(TAG, "تم حذف البيانات المحلية");

                // جلب البيانات من السيرفر
                DataManager dataManager = new DataManager(
                    context,
                    accountDao,
                    transactionDao,
                    null
                );

                dataManager.fetchDataFromServer(new DataManager.DataCallback() {
                    @Override
                    public void onSuccess() {
                        isSyncing = false;
                        handler.post(() -> callback.onSuccess());
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "فشلت المزامنة الكاملة: " + error);
                        isSyncing = false;
                        handler.post(() -> callback.onError(error));
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "خطأ في المزامنة الكاملة: " + e.getMessage());
                isSyncing = false;
                handler.post(() -> callback.onError("خطأ في المزامنة الكاملة: " + e.getMessage()));
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
} 
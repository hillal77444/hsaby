package com.hillal.hhhhhhh.data.sync;

import android.content.Context;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

import com.hillal.hhhhhhh.data.remote.ApiService;
import com.hillal.hhhhhhh.data.remote.RetrofitClient;
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

public class SyncManager {
    private static final String TAG = "SyncManager";
    private final Context context;
    private final ApiService apiService;
    private final AccountDao accountDao;
    private final TransactionDao transactionDao;
    private final Handler handler;
    private final ExecutorService executor;
    private boolean isSyncing = false;

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
        void onProgress(int current, int total);
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

    public void onDashboardEntered(SyncCallback callback) {
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
                // جلب الحسابات التي تحتاج مزامنة
                List<Account> pendingAccounts = accountDao.getAccountsByServerId(-1);
                if (!pendingAccounts.isEmpty()) {
                    syncAccountsSequentially(pendingAccounts, token, new SyncCallback() {
                        @Override
                        public void onSuccess() {
                            // بعد اكتمال مزامنة الحسابات، نبدأ بمزامنة المعاملات
                            syncTransactions(token, callback);
                        }

                        @Override
                        public void onError(String error) {
                            isSyncing = false;
                            handler.post(() -> callback.onError(error));
                        }

                        @Override
                        public void onProgress(int current, int total) {
                            handler.post(() -> callback.onProgress(current, total));
                        }
                    });
                } else {
                    // إذا لم تكن هناك حسابات للمزامنة، نبدأ مباشرة بمزامنة المعاملات
                    syncTransactions(token, callback);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in onDashboardEntered: " + e.getMessage());
                isSyncing = false;
                handler.post(() -> callback.onError("خطأ في المزامنة: " + e.getMessage()));
            }
        });
    }

    private void syncAccountsSequentially(List<Account> accounts, String token, SyncCallback callback) {
        if (accounts.isEmpty()) {
            callback.onSuccess();
            return;
        }

        Account currentAccount = accounts.get(0);
        List<Account> remainingAccounts = accounts.subList(1, accounts.size());

        apiService.syncData("Bearer " + token, new ApiService.SyncRequest(List.of(currentAccount), new ArrayList<>()))
                .enqueue(new Callback<ApiService.SyncResponse>() {
                    @Override
                    public void onResponse(Call<ApiService.SyncResponse> call, Response<ApiService.SyncResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ApiService.SyncResponse syncResponse = response.body();
                            Long serverId = syncResponse.getAccountServerId(currentAccount.getId());
                            if (serverId != null) {
                                currentAccount.setServerId(serverId);
                                accountDao.update(currentAccount);
                            }
                            // المتابعة مع الحساب التالي
                            syncAccountsSequentially(remainingAccounts, token, callback);
                        } else {
                            isSyncing = false;
                            handler.post(() -> callback.onError("فشلت مزامنة الحساب"));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiService.SyncResponse> call, Throwable t) {
                        isSyncing = false;
                        handler.post(() -> callback.onError("خطأ في الاتصال: " + t.getMessage()));
                    }
                });
    }

    private void syncTransactions(String token, SyncCallback callback) {
        List<Transaction> pendingTransactions = transactionDao.getTransactionsByServerId(-1);
        if (pendingTransactions.isEmpty()) {
            isSyncing = false;
            handler.post(() -> callback.onSuccess());
            return;
        }

        syncTransactionsSequentially(pendingTransactions, token, callback);
    }

    private void syncTransactionsSequentially(List<Transaction> transactions, String token, SyncCallback callback) {
        if (transactions.isEmpty()) {
            isSyncing = false;
            handler.post(() -> callback.onSuccess());
            return;
        }

        Transaction currentTransaction = transactions.get(0);
        List<Transaction> remainingTransactions = transactions.subList(1, transactions.size());

        apiService.syncData("Bearer " + token, new ApiService.SyncRequest(new ArrayList<>(), List.of(currentTransaction)))
                .enqueue(new Callback<ApiService.SyncResponse>() {
                    @Override
                    public void onResponse(Call<ApiService.SyncResponse> call, Response<ApiService.SyncResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ApiService.SyncResponse syncResponse = response.body();
                            Long serverId = syncResponse.getTransactionServerId(currentTransaction.getId());
                            if (serverId != null) {
                                currentTransaction.setServerId(serverId);
                                transactionDao.update(currentTransaction);
                            }
                            // المتابعة مع المعاملة التالية
                            syncTransactionsSequentially(remainingTransactions, token, callback);
                        } else {
                            isSyncing = false;
                            handler.post(() -> callback.onError("فشلت مزامنة المعاملة"));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiService.SyncResponse> call, Throwable t) {
                        isSyncing = false;
                        handler.post(() -> callback.onError("خطأ في الاتصال: " + t.getMessage()));
                    }
                });
    }

    public void shutdown() {
        executor.shutdown();
    }
} 
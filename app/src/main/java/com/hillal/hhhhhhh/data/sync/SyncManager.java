package com.hillal.hhhhhhh.data.sync;

import android.content.Context;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;

import com.hillal.hhhhhhh.data.remote.ApiService;
import com.hillal.hhhhhhh.data.remote.RetrofitClient;
import com.hillal.hhhhhhh.data.room.AccountDao;
import com.hillal.hhhhhhh.data.room.TransactionDao;
import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.model.Transaction;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SyncManager {
    private static final String TAG = "SyncManager";
    private final Context context;
    private final ApiService apiService;
    private final AccountDao accountDao;
    private final TransactionDao transactionDao;
    private final Handler handler;
    private boolean isAutoSyncEnabled = true;
    private long lastSyncTime = 0;

    public SyncManager(Context context, AccountDao accountDao, TransactionDao transactionDao) {
        this.context = context;
        this.apiService = RetrofitClient.getInstance().getApiService();
        this.accountDao = accountDao;
        this.transactionDao = transactionDao;
        this.handler = new Handler(Looper.getMainLooper());
        this.lastSyncTime = getLastSyncTime();
        startAutoSync();
    }

    private long getLastSyncTime() {
        return context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
                .getLong("last_sync_time", 0);
    }

    private void updateLastSyncTime() {
        long currentTime = System.currentTimeMillis();
        context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
                .edit()
                .putLong("last_sync_time", currentTime)
                .apply();
        this.lastSyncTime = currentTime;
    }

    public interface SyncCallback {
        void onSuccess();
        void onError(String error);
    }

    private void startAutoSync() {
        if (isAutoSyncEnabled) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    syncData(new SyncCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Auto sync successful");
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Auto sync failed: " + error);
                        }
                    });
                    handler.postDelayed(this, 30 * 60 * 1000);
                }
            }, 30 * 60 * 1000);
        }
    }

    public void syncData(SyncCallback callback) {
        new Thread(() -> {
            try {
                // جلب البيانات الجديدة فقط
                List<Account> newAccounts = accountDao.getAccountsModifiedAfter(lastSyncTime);
                List<Transaction> newTransactions = transactionDao.getTransactionsModifiedAfter(lastSyncTime);
                
                if (newAccounts.isEmpty() && newTransactions.isEmpty()) {
                    Log.d(TAG, "No new data to sync");
                    callback.onSuccess();
                    return;
                }

                Log.d(TAG, "Syncing " + newAccounts.size() + " accounts and " + 
                          newTransactions.size() + " transactions");

                // إرسال البيانات الجديدة إلى السيرفر
                apiService.syncData(new SyncRequest(newAccounts, newTransactions))
                    .enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                updateLastSyncTime();
                                callback.onSuccess();
                            } else {
                                callback.onError("فشل المزامنة");
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Log.e(TAG, "Sync error: " + t.getMessage());
                            callback.onError("خطأ في الاتصال");
                        }
                    });
            } catch (Exception e) {
                Log.e(TAG, "Error during sync: " + e.getMessage());
                callback.onError("خطأ في المزامنة");
            }
        }).start();
    }

    public void enableAutoSync(boolean enable) {
        isAutoSyncEnabled = enable;
        context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("auto_sync", enable)
                .apply();
        
        if (enable) {
            startAutoSync();
        } else {
            handler.removeCallbacksAndMessages(null);
        }
    }

    public boolean isAutoSyncEnabled() {
        return context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
                .getBoolean("auto_sync", true);
    }

    public void setSyncInterval(int minutes) {
        // تعيين فاصل المزامنة
        context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
                .edit()
                .putInt("sync_interval", minutes)
                .apply();
    }

    public int getSyncInterval() {
        return context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
                .getInt("sync_interval", 30); // القيمة الافتراضية 30 دقيقة
    }

    private static class SyncRequest {
        private List<Account> accounts;
        private List<Transaction> transactions;
        
        public SyncRequest(List<Account> accounts, List<Transaction> transactions) {
            this.accounts = accounts;
            this.transactions = transactions;
        }
    }
} 
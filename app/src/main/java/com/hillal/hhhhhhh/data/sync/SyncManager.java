package com.hillal.hhhhhhh.data.sync;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.hillal.hhhhhhh.data.remote.DataManager;
import com.hillal.hhhhhhh.data.room.AccountDao;
import com.hillal.hhhhhhh.data.room.TransactionDao;
import com.hillal.hhhhhhh.data.room.PendingOperationDao;

public class SyncManager {
    private static final String TAG = "SyncManager";
    private final Context context;
    private final DataManager dataManager;
    private final Handler handler;
    private final AccountDao accountDao;
    private final TransactionDao transactionDao;
    private final PendingOperationDao pendingOperationDao;

    public SyncManager(Context context, DataManager dataManager, AccountDao accountDao, 
                      TransactionDao transactionDao, PendingOperationDao pendingOperationDao) {
        this.context = context;
        this.dataManager = dataManager;
        this.accountDao = accountDao;
        this.transactionDao = transactionDao;
        this.pendingOperationDao = pendingOperationDao;
        this.handler = new Handler(Looper.getMainLooper());
    }

    public interface SyncCallback {
        void onSuccess();
        void onError(String error);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void performFullSync(SyncCallback callback) {
        if (!isNetworkAvailable()) {
            Log.e(TAG, "No network connection");
            handler.post(() -> callback.onError("No internet connection"));
            return;
        }

        String token = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .getString("token", null);

        if (token == null) {
            Log.e(TAG, "User not authenticated");
            handler.post(() -> callback.onError("User not authenticated"));
            return;
        }

        dataManager.fetchDataFromServer(new DataManager.DataCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Full sync completed successfully");
                handler.post(() -> callback.onSuccess());
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error during full sync: " + error);
                handler.post(() -> callback.onError(error));
            }
        });
    }
} 
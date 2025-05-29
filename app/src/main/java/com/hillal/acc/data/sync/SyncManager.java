package com.hillal.acc.data.sync;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.hillal.acc.data.remote.DataManager;
import com.hillal.acc.data.room.AccountDao;
import com.hillal.acc.data.room.TransactionDao;
import com.hillal.acc.data.room.PendingOperationDao;

public class SyncManager {
    private static final String TAG = "SyncManager";
    private final Context context;
    private final DataManager dataManager;
    private final AccountDao accountDao;
    private final TransactionDao transactionDao;
    private final PendingOperationDao pendingOperationDao;
    private final Handler mainHandler;

    public SyncManager(Context context, DataManager dataManager, AccountDao accountDao, 
                      TransactionDao transactionDao, PendingOperationDao pendingOperationDao) {
        this.context = context;
        this.dataManager = dataManager;
        this.accountDao = accountDao;
        this.transactionDao = transactionDao;
        this.pendingOperationDao = pendingOperationDao;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void performFullSync(SyncCallback callback) {
        if (!isNetworkAvailable()) {
            mainHandler.post(() -> callback.onError("لا يوجد اتصال بالإنترنت"));
            return;
        }

        if (!isUserAuthenticated()) {
            mainHandler.post(() -> callback.onError("المستخدم غير مسجل الدخول"));
            return;
        }

        dataManager.fetchDataFromServer(new DataManager.DataCallback() {
            @Override
            public void onSuccess() {
                mainHandler.post(() -> callback.onSuccess());
            }

            @Override
            public void onError(String error) {
                mainHandler.post(() -> callback.onError(error));
            }
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private boolean isUserAuthenticated() {
        String token = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .getString("token", null);
        return token != null;
    }

    public interface SyncCallback {
        void onSuccess();
        void onError(String error);
    }
} 
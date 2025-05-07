package com.hillal.hhhhhhh.data.sync;

import android.content.Context;
import android.util.Log;

import com.hillal.hhhhhhh.data.remote.ApiService;
import com.hillal.hhhhhhh.data.remote.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SyncManager {
    private static final String TAG = "SyncManager";
    private final Context context;
    private final ApiService apiService;

    public SyncManager(Context context) {
        this.context = context;
        this.apiService = RetrofitClient.getInstance().getApiService();
    }

    public interface SyncCallback {
        void onSuccess();
        void onError(String error);
    }

    public void syncData(SyncCallback callback) {
        // مزامنة البيانات مع الخادم
        apiService.syncData().enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
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
    }

    public void enableAutoSync(boolean enable) {
        // تفعيل/تعطيل المزامنة التلقائية
        context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("auto_sync", enable)
                .apply();
    }

    public boolean isAutoSyncEnabled() {
        return context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
                .getBoolean("auto_sync", false);
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
} 
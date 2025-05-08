package com.hillal.hhhhhhh.data.sync;

import android.content.Context;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.widget.Toast;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

import com.hillal.hhhhhhh.data.remote.ApiService;
import com.hillal.hhhhhhh.data.remote.RetrofitClient;
import com.hillal.hhhhhhh.data.remote.DataManager;
import com.hillal.hhhhhhh.data.room.AccountDao;
import com.hillal.hhhhhhh.data.room.TransactionDao;
import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.model.Transaction;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.google.gson.Gson;

public class SyncManager {
    private static final String TAG = "SyncManager";
    private final Context context;
    private final ApiService apiService;
    private final AccountDao accountDao;
    private final TransactionDao transactionDao;
    private final Handler handler;
    private boolean isAutoSyncEnabled = true;
    private long lastSyncTime = 0;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final int SYNC_INTERVAL = 5 * 60 * 1000; // 5 دقائق
    private static final int MAX_RETRY_COUNT = 3;
    private int currentRetryCount = 0;

    public SyncManager(Context context, AccountDao accountDao, TransactionDao transactionDao) {
        this.context = context;
        this.apiService = RetrofitClient.getInstance().getApiService();
        this.accountDao = accountDao;
        this.transactionDao = transactionDao;
        this.handler = new Handler(Looper.getMainLooper());
        this.lastSyncTime = getLastSyncTime();
        
        // تفعيل المزامنة التلقائية بشكل افتراضي
        this.isAutoSyncEnabled = true;
        context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("auto_sync", true)
                .apply();
        
        // بدء المزامنة التلقائية
        startAutoSync();
        // تنفيذ مزامنة فورية
        performInitialSync();
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
            Log.d(TAG, "Starting auto sync scheduler...");
            handler.removeCallbacksAndMessages(null); // إزالة أي مهام سابقة
            
            Runnable syncRunnable = new Runnable() {
                @Override
                public void run() {
                    if (isNetworkAvailable()) {
                        Log.d(TAG, "Executing auto sync...");
                        // جلب البيانات من السيرفر أولاً
                        DataManager dataManager = new DataManager(
                            context,
                            accountDao,
                            transactionDao,
                            null
                        );

                        dataManager.fetchDataFromServer(new DataManager.DataCallback() {
                            @Override
                            public void onSuccess() {
                                // بعد جلب البيانات، نقوم بمزامنة التغييرات المحلية
                                syncData(new SyncCallback() {
                                    @Override
                                    public void onSuccess() {
                                        Log.d(TAG, "Auto sync successful");
                                        updateLastSyncTime();
                                        currentRetryCount = 0;
                                        // جدولة المزامنة التالية
                                        handler.postDelayed(this, SYNC_INTERVAL);
                                    }

                                    @Override
                                    public void onError(String error) {
                                        Log.e(TAG, "Auto sync failed: " + error);
                                        handleSyncFailure();
                                    }
                                });
                            }

                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "Auto fetch failed: " + error);
                                handleSyncFailure();
                            }
                        });
                    } else {
                        Log.d(TAG, "No network available, retrying in 1 minute...");
                        handler.postDelayed(this, 60 * 1000); // محاولة بعد دقيقة
                    }
                }
            };

            // بدء المزامنة التلقائية
            handler.post(syncRunnable);
        }
    }

    private void handleSyncFailure() {
        currentRetryCount++;
        if (currentRetryCount < MAX_RETRY_COUNT) {
            Log.d(TAG, "Retrying sync in 1 minute... (Attempt " + currentRetryCount + " of " + MAX_RETRY_COUNT + ")");
            handler.postDelayed(() -> {
                if (isNetworkAvailable()) {
                    startAutoSync();
                }
            }, 60 * 1000); // محاولة إعادة المزامنة بعد دقيقة
        } else {
            Log.e(TAG, "Max retry attempts reached. Will try again in next sync interval.");
            currentRetryCount = 0;
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            return capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        }
        return false;
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("خطأ المزامنة", text);
        clipboard.setPrimaryClip(clip);
        handler.post(() -> Toast.makeText(context, "تم نسخ رسالة الخطأ إلى الحافظة", Toast.LENGTH_SHORT).show());
    }

    public void syncData(SyncCallback callback) {
        String token = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .getString("token", null);

        if (token == null) {
            callback.onError("يرجى تسجيل الدخول أولاً");
            return;
        }

        if (!isNetworkAvailable()) {
            callback.onError("لا يوجد اتصال بالإنترنت");
            return;
        }

        executor.execute(() -> {
            try {
                // جلب الحسابات الجديدة
                List<Account> newAccounts = accountDao.getNewAccounts();
                Log.d(TAG, "تم العثور على " + newAccounts.size() + " حساب جديد");

                // جلب المعاملات المعدلة منذ آخر مزامنة
                long lastSyncTime = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
                        .getLong("last_sync_time", 0);
                List<Transaction> modifiedTransactions = transactionDao.getModifiedTransactions(lastSyncTime);
                Log.d(TAG, "تم العثور على " + modifiedTransactions.size() + " معاملة معدلة");

                if (newAccounts.isEmpty() && modifiedTransactions.isEmpty()) {
                    Log.d(TAG, "لا توجد بيانات جديدة للمزامنة");
                    // حتى لو لم تكن هناك بيانات جديدة، نقوم بتحديث وقت آخر مزامنة
                    updateLastSyncTime();
                    handler.post(() -> callback.onSuccess());
                    return;
                }

                // إنشاء طلب المزامنة
                ApiService.SyncRequest syncRequest = new ApiService.SyncRequest(newAccounts, modifiedTransactions);
                
                // تحويل طلب المزامنة إلى JSON للنسخ
                String syncRequestJson = new Gson().toJson(syncRequest);

                // إرسال البيانات إلى السيرفر
                Response<Void> response = apiService.syncData("Bearer " + token, syncRequest).execute();
                
                if (response.isSuccessful()) {
                    // تحديث معرفات السيرفر للحسابات الجديدة
                    for (Account account : newAccounts) {
                        account.setServerId(account.getId());
                        accountDao.update(account);
                    }
                    
                    // تحديث معرفات السيرفر للمعاملات الجديدة
                    for (Transaction transaction : modifiedTransactions) {
                        transaction.setServerId(transaction.getId());
                        transactionDao.update(transaction);
                    }
                    
                    // تحديث وقت آخر مزامنة
                    context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
                            .edit()
                            .putLong("last_sync_time", System.currentTimeMillis())
                            .apply();
                    
                    Log.d(TAG, "تمت المزامنة بنجاح");
                    handler.post(() -> callback.onSuccess());
                } else {
                    String errorBody = response.errorBody() != null ? response.errorBody().string() : "خطأ غير معروف";
                    Log.e(TAG, "فشلت المزامنة: " + errorBody);
                    
                    // نسخ بيانات طلب المزامنة إلى الحافظة
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Sync Request Data", 
                        "Sync Request JSON:\n" + syncRequestJson + 
                        "\n\nError Response:\n" + errorBody);
                    clipboard.setPrimaryClip(clip);
                    
                    handler.post(() -> {
                        Toast.makeText(context, "تم نسخ بيانات المزامنة إلى الحافظة", Toast.LENGTH_LONG).show();
                        callback.onError("فشلت المزامنة: " + errorBody);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "خطأ في المزامنة: " + e.getMessage());
                handler.post(() -> callback.onError("خطأ في المزامنة: " + e.getMessage()));
            }
        });
    }

    public void enableAutoSync(boolean enable) {
        Log.d(TAG, "Setting auto sync to: " + enable);
        isAutoSyncEnabled = enable;
        context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("auto_sync", enable)
                .apply();
        
        if (enable) {
            // بدء المزامنة التلقائية
            startAutoSync();
            // تنفيذ مزامنة فورية
            performInitialSync();
        } else {
            // إيقاف المزامنة التلقائية
            handler.removeCallbacksAndMessages(null);
            Log.d(TAG, "Auto sync disabled");
        }
    }

    private void performInitialSync() {
        if (!isNetworkAvailable()) {
            Log.d(TAG, "No network available, skipping initial sync");
            return;
        }

        DataManager dataManager = new DataManager(
            context,
            accountDao,
            transactionDao,
            null
        );
        dataManager.fetchDataFromServer(new DataManager.DataCallback() {
            @Override
            public void onSuccess() {
                syncData(new SyncCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Initial sync successful");
                        updateLastSyncTime();
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Initial sync failed: " + error);
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Initial fetch failed: " + error);
            }
        });
    }

    public boolean isAutoSyncEnabled() {
        // دائماً نرجع true لأن المزامنة التلقائية مفعلة بشكل افتراضي
        return true;
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
                .getInt("sync_interval", 5); // القيمة الافتراضية 30 دقيقة
    }

    // دالة جديدة لتفعيل المزامنة عند دخول لوحة التحكم
    public void onDashboardEntered() {
        if (isNetworkAvailable()) {
            Log.d(TAG, "Dashboard entered, performing sync...");
            performInitialSync();
        }
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
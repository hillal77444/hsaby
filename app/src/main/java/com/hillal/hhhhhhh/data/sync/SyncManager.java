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
import java.util.ArrayList;

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
    private static final String SYNC_TAG = "sync_task";
    private Runnable currentSyncTask;

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

    public void startAutoSync() {
        if (!isAutoSyncEnabled) {
            Log.d(TAG, "Auto sync is disabled");
            return;
        }

        Log.d(TAG, "Starting auto sync scheduler...");
        
        // إزالة المهام السابقة فقط إذا كانت هناك مزامنة جديدة
        if (currentSyncTask != null) {
            handler.removeCallbacks(currentSyncTask);
        }

        currentSyncTask = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Executing auto sync...");
                if (!isNetworkAvailable()) {
                    Log.d(TAG, "No network available, retrying in 1 minute");
                    handler.postDelayed(this, 60000); // Retry after 1 minute
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
                        Log.d(TAG, "Data fetched successfully, syncing...");
                        syncData(new SyncCallback() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "Auto sync successful");
                                updateLastSyncTime();
                                currentRetryCount = 0;
                                // جدولة المزامنة التالية
                                handler.postDelayed(currentSyncTask, SYNC_INTERVAL);
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
                        Log.e(TAG, "Error fetching data: " + error);
                        handleSyncFailure();
                    }
                });
            }
        };

        handler.post(currentSyncTask);
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
                // جلب الحسابات الجديدة والمعدلة
                List<Account> newAccounts = accountDao.getNewAccounts();
                List<Account> modifiedAccounts = accountDao.getModifiedAccounts(lastSyncTime);
                Log.d(TAG, "تم العثور على " + newAccounts.size() + " حساب جديد و " + modifiedAccounts.size() + " حساب معدل");

                // جلب المعاملات الجديدة والمعدلة
                List<Transaction> newTransactions = transactionDao.getNewTransactions();
                List<Transaction> modifiedTransactions = transactionDao.getModifiedTransactions(lastSyncTime);
                Log.d(TAG, "تم العثور على " + newTransactions.size() + " معاملة جديدة و " + modifiedTransactions.size() + " معاملة معدلة");

                // دمج الحسابات الجديدة والمعدلة
                List<Account> allAccounts = new ArrayList<>();
                allAccounts.addAll(newAccounts);
                allAccounts.addAll(modifiedAccounts);

                // دمج المعاملات الجديدة والمعدلة
                List<Transaction> allTransactions = new ArrayList<>();
                allTransactions.addAll(newTransactions);
                allTransactions.addAll(modifiedTransactions);

                if (allAccounts.isEmpty() && allTransactions.isEmpty()) {
                    Log.d(TAG, "لا توجد بيانات جديدة للمزامنة");
                    updateLastSyncTime();
                    handler.post(() -> callback.onSuccess());
                    return;
                }

                // إنشاء طلب المزامنة
                ApiService.SyncRequest syncRequest = new ApiService.SyncRequest(allAccounts, allTransactions);
                
                // تحويل طلب المزامنة إلى JSON للنسخ
                String syncRequestJson = new Gson().toJson(syncRequest);
                Log.d(TAG, "بيانات المزامنة: " + syncRequestJson);

                // إرسال البيانات إلى السيرفر
                Response<ApiService.SyncResponse> response = apiService.syncData("Bearer " + token, syncRequest).execute();
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiService.SyncResponse syncResponse = response.body();
                    
                    // تحديث معرفات السيرفر للحسابات الجديدة
                    for (Account account : newAccounts) {
                        Long serverId = syncResponse.getAccountServerId(account.getId());
                        if (serverId != null) {
                            account.setServerId(serverId);
                            accountDao.update(account);
                            Log.d(TAG, "تم تحديث معرف السيرفر للحساب الجديد: " + account.getName());
                        }
                    }
                    
                    // تحديث معرفات السيرفر للمعاملات الجديدة
                    for (Transaction transaction : newTransactions) {
                        Long serverId = syncResponse.getTransactionServerId(transaction.getId());
                        if (serverId != null) {
                            transaction.setServerId(serverId);
                            transactionDao.update(transaction);
                            Log.d(TAG, "تم تحديث معرف السيرفر للمعاملة الجديدة: " + transaction.getId());
                        }
                    }

                    // تحديث حالة المزامنة للحسابات المعدلة
                    for (Account account : modifiedAccounts) {
                        account.setLastSyncTime(System.currentTimeMillis());
                        accountDao.update(account);
                        Log.d(TAG, "تم تحديث حالة المزامنة للحساب المعدل: " + account.getName());
                    }

                    // تحديث حالة المزامنة للمعاملات المعدلة
                    for (Transaction transaction : modifiedTransactions) {
                        transaction.setLastSyncTime(System.currentTimeMillis());
                        transactionDao.update(transaction);
                        Log.d(TAG, "تم تحديث حالة المزامنة للمعاملة المعدلة: " + transaction.getDescription());
                    }
                    
                    // تحديث وقت آخر مزامنة
                    updateLastSyncTime();
                    
                    Log.d(TAG, "تمت المزامنة بنجاح");
                    handler.post(() -> {
                        Toast.makeText(context, "تمت المزامنة بنجاح", Toast.LENGTH_SHORT).show();
                        callback.onSuccess();
                    });
                } else {
                    String errorBody = response.errorBody() != null ? response.errorBody().string() : "خطأ غير معروف";
                    Log.e(TAG, "فشلت المزامنة: " + errorBody);
                    
                    // نسخ بيانات طلب المزامنة إلى الحافظة
                    copyToClipboard("Sync Request JSON:\n" + syncRequestJson + "\n\nError Response:\n" + errorBody);
                    
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
            if (currentSyncTask != null) {
                handler.removeCallbacks(currentSyncTask);
                currentSyncTask = null;
            }
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
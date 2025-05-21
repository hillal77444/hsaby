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
import android.net.NetworkInfo;
import android.content.SharedPreferences;

import com.hillal.hhhhhhh.data.remote.ApiService;
import com.hillal.hhhhhhh.data.remote.RetrofitClient;
import com.hillal.hhhhhhh.data.remote.DataManager;
import com.hillal.hhhhhhh.data.room.AccountDao;
import com.hillal.hhhhhhh.data.room.TransactionDao;
import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.model.Transaction;
import com.hillal.hhhhhhh.data.model.User;

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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.io.IOException;
import androidx.lifecycle.LifecycleOwner;
import java.util.Date;

public class SyncManager {
    private static final String TAG = "SyncManager";
    private final Context context;
    private final ApiService apiService;
    private final AccountDao accountDao;
    private final TransactionDao transactionDao;
    private final Handler handler;
    private boolean isAutoSyncEnabled = false;
    private long lastSyncTime = 0;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final long SYNC_INTERVAL = 5 * 60 * 1000; // 5 دقائق
    private static final int MAX_RETRY_COUNT = 3;
    private int currentRetryCount = 0;
    private static final String SYNC_TAG = "sync_task";
    private Runnable currentSyncTask;

    // إضافة ثوابت لحالة المزامنة
    private static final int SYNC_STATUS_PENDING = 0;
    private static final int SYNC_STATUS_SYNCING = 1;
    private static final int SYNC_STATUS_SYNCED = 2;
    private static final int SYNC_STATUS_FAILED = 3;

    // إضافة جدول لتتبع عمليات المزامنة
    private final Map<String, Long> syncInProgress = new ConcurrentHashMap<>();

    // إضافة متغيرات لتخزين البيانات الحالية
    private List<Account> currentNewAccounts;
    private List<Account> currentModifiedAccounts;
    private List<Transaction> currentNewTransactions;
    private List<Transaction> currentModifiedTransactions;

    // إضافة قفل للمزامنة
    private final Object syncLock = new Object();
    private boolean isSyncing = false;

    private static final int MAX_OFFLINE_RETRY_COUNT = 3;
    private static final long OFFLINE_RETRY_INTERVAL = 5 * 60 * 1000; // 5 دقائق
    private int offlineRetryCount = 0;
    private long lastOfflineRetryTime = 0;

    private static final int BATCH_SIZE = 50; // عدد العناصر في كل دفعة

    private Handler syncHandler = new Handler(Looper.getMainLooper());
    private Runnable periodicSyncRunnable;

    private static final String PENDING_SYNC_PREFS = "pending_sync_prefs";
    private static final String PENDING_SYNC_DATA = "pending_sync_data";
    private static final String PENDING_SYNC_TIME = "pending_sync_time";

    public SyncManager(Context context, AccountDao accountDao, TransactionDao transactionDao) {
        this.context = context;
        this.apiService = RetrofitClient.getInstance().getApiService();
        this.accountDao = accountDao;
        this.transactionDao = transactionDao;
        this.handler = new Handler(Looper.getMainLooper());
        this.lastSyncTime = getLastSyncTime();
        
        // تعطيل المزامنة التلقائية بشكل افتراضي
        this.isAutoSyncEnabled = false;
        context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("auto_sync", false)
                .apply();
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
            if (capabilities != null) {
                boolean hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                boolean hasValidConnection = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                return hasInternet && hasValidConnection;
            }
        }
        return false;
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("خطأ المزامنة", text);
        clipboard.setPrimaryClip(clip);
        handler.post(() -> Toast.makeText(context, "تم نسخ رسالة الخطأ إلى الحافظة", Toast.LENGTH_SHORT).show());
    }

    private static class SyncSession {
        private final List<Account> newAccounts;
        private final List<Account> modifiedAccounts;
        private final List<Transaction> newTransactions;
        private final List<Transaction> modifiedTransactions;
        private final long startTime;

        public SyncSession(List<Account> newAccounts, List<Account> modifiedAccounts,
                         List<Transaction> newTransactions, List<Transaction> modifiedTransactions) {
            this.newAccounts = newAccounts;
            this.modifiedAccounts = modifiedAccounts;
            this.newTransactions = newTransactions;
            this.modifiedTransactions = modifiedTransactions;
            this.startTime = System.currentTimeMillis();
        }

        public List<Account> getNewAccounts() {
            return newAccounts;
        }

        public List<Account> getModifiedAccounts() {
            return modifiedAccounts;
        }

        public List<Transaction> getNewTransactions() {
            return newTransactions;
        }

        public List<Transaction> getModifiedTransactions() {
            return modifiedTransactions;
        }

        public long getStartTime() {
            return startTime;
        }
    }

    public void syncData(SyncCallback callback) {
        String token = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .getString("token", null);

        if (token == null) {
            callback.onError("يرجى تسجيل الدخول أولاً");
            return;
        }

        if (!isNetworkAvailable()) {
            handleOfflineSync(callback);
            return;
        }

        // التحقق من حالة المزامنة
        synchronized (syncLock) {
            if (isSyncing) {
                callback.onError("جاري تنفيذ عملية مزامنة أخرى، يرجى الانتظار");
                return;
            }
            isSyncing = true;
        }

        executor.execute(() -> {
            try {
                // إنشاء جلسة مزامنة جديدة
                SyncSession session = new SyncSession(
                    accountDao.getNewAccounts(),
                    accountDao.getModifiedAccounts(lastSyncTime),
                    transactionDao.getNewTransactions(),
                    transactionDao.getModifiedTransactions(lastSyncTime)
                );

                // التحقق من العناصر التي قيد المزامنة
                List<Account> filteredNewAccounts = filterSyncingItems(session.getNewAccounts());
                List<Account> filteredModifiedAccounts = filterSyncingItems(session.getModifiedAccounts());
                List<Transaction> filteredNewTransactions = filterSyncingItems(session.getNewTransactions());
                List<Transaction> filteredModifiedTransactions = filterSyncingItems(session.getModifiedTransactions());

                // تحديث حالة المزامنة للعناصر
                for (Account account : filteredNewAccounts) {
                    account.setSyncStatus(SYNC_STATUS_SYNCING);
                    accountDao.update(account);
                    syncInProgress.put(getItemKey(account), System.currentTimeMillis());
                }
                for (Account account : filteredModifiedAccounts) {
                    account.setSyncStatus(SYNC_STATUS_SYNCING);
                    accountDao.update(account);
                    syncInProgress.put(getItemKey(account), System.currentTimeMillis());
                }
                for (Transaction transaction : filteredNewTransactions) {
                    transaction.setSyncStatus(SYNC_STATUS_SYNCING);
                    transactionDao.update(transaction);
                    syncInProgress.put(getItemKey(transaction), System.currentTimeMillis());
                }
                for (Transaction transaction : filteredModifiedTransactions) {
                    transaction.setSyncStatus(SYNC_STATUS_SYNCING);
                    transactionDao.update(transaction);
                    syncInProgress.put(getItemKey(transaction), System.currentTimeMillis());
                }

                // دمج الحسابات الجديدة والمعدلة
                List<Account> allAccounts = new ArrayList<>();
                allAccounts.addAll(filteredNewAccounts);
                allAccounts.addAll(filteredModifiedAccounts);

                // دمج المعاملات الجديدة والمعدلة
                List<Transaction> allTransactions = new ArrayList<>();
                allTransactions.addAll(filteredNewTransactions);
                allTransactions.addAll(filteredModifiedTransactions);

                if (allAccounts.isEmpty() && allTransactions.isEmpty()) {
                    Log.d(TAG, "لا توجد بيانات جديدة للمزامنة");
                    updateLastSyncTime();
                    synchronized (syncLock) {
                        isSyncing = false;
                    }
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
                
                if (response.isSuccessful()) {
                    ApiService.SyncResponse syncResponse = response.body();
                    if (syncResponse == null) {
                        updateLastSyncTime();
                        synchronized (syncLock) {
                            isSyncing = false;
                        }
                        handler.post(() -> {
                            Toast.makeText(context, "تمت المزامنة بنجاح", Toast.LENGTH_SHORT).show();
                            callback.onSuccess();
                        });
                        return;
                    }
                    
                    // تحديث معرفات السيرفر للحسابات الجديدة
                    for (Account account : filteredNewAccounts) {
                        Long serverId = syncResponse.getAccountServerId(account.getId());
                        if (serverId != null) {
                            account.setServerId(serverId);
                            account.setSyncStatus(SYNC_STATUS_SYNCED);
                            accountDao.update(account);
                            syncInProgress.remove(getItemKey(account));
                        }
                    }
                    
                    // تحديث معرفات السيرفر للمعاملات الجديدة
                    for (Transaction transaction : filteredNewTransactions) {
                        Long serverId = syncResponse.getTransactionServerId(transaction.getId());
                        if (serverId != null) {
                            transaction.setServerId(serverId);
                            transaction.setSyncStatus(SYNC_STATUS_SYNCED);
                            transactionDao.update(transaction);
                            syncInProgress.remove(getItemKey(transaction));
                        }
                    }

                    // تحديث حالة المزامنة للحسابات المعدلة
                    for (Account account : filteredModifiedAccounts) {
                        account.setLastSyncTime(System.currentTimeMillis());
                        account.setSyncStatus(SYNC_STATUS_SYNCED);
                        accountDao.update(account);
                        syncInProgress.remove(getItemKey(account));
                    }

                    // تحديث حالة المزامنة للمعاملات المعدلة
                    for (Transaction transaction : filteredModifiedTransactions) {
                        transaction.setLastSyncTime(System.currentTimeMillis());
                        transaction.setSyncStatus(SYNC_STATUS_SYNCED);
                        transactionDao.update(transaction);
                        syncInProgress.remove(getItemKey(transaction));
                    }
                    
                    updateLastSyncTime();
                    synchronized (syncLock) {
                        isSyncing = false;
                    }
                    handler.post(() -> {
                        Toast.makeText(context, "تمت المزامنة بنجاح", Toast.LENGTH_SHORT).show();
                        callback.onSuccess();
                    });
                } else {
                    String errorBody = response.errorBody() != null ? response.errorBody().string() : "خطأ غير معروف";
                    Log.e(TAG, "فشلت المزامنة: " + errorBody);
                    
                    // نسخ بيانات طلب المزامنة إلى الحافظة
                    copyToClipboard("Sync Request JSON:\n" + syncRequestJson + "\n\nError Response:\n" + errorBody);
                    
                    // تحديث حالة المزامنة في حالة الفشل
                    updateSyncStatusOnFailure(filteredNewAccounts, filteredModifiedAccounts, 
                        filteredNewTransactions, filteredModifiedTransactions);
                    
                    synchronized (syncLock) {
                        isSyncing = false;
                    }
                    handler.post(() -> {
                        Toast.makeText(context, "تم نسخ بيانات المزامنة إلى الحافظة", Toast.LENGTH_LONG).show();
                        callback.onError("فشلت المزامنة: " + errorBody);
                    });
                }
            } catch (Exception e) {
                // تحديث حالة المزامنة في حالة الفشل
                synchronized (syncLock) {
                    isSyncing = false;
                }
                Log.e(TAG, "خطأ في المزامنة: " + e.getMessage());
                handler.post(() -> callback.onError("خطأ في المزامنة: " + e.getMessage()));
            }
        });
    }

    private void handleOfflineSync(SyncCallback callback) {
        long currentTime = System.currentTimeMillis();
        
        // التحقق من عدد محاولات إعادة المحاولة
        if (offlineRetryCount >= MAX_OFFLINE_RETRY_COUNT) {
            // إعادة تعيين العداد بعد فترة
            if (currentTime - lastOfflineRetryTime > OFFLINE_RETRY_INTERVAL) {
                offlineRetryCount = 0;
            } else {
                callback.onError("لا يوجد اتصال بالإنترنت. سيتم إعادة المحاولة تلقائياً عند توفر الاتصال.");
                return;
            }
        }

        // تحديث حالة العناصر إلى PENDING
        executor.execute(() -> {
            try {
                List<Account> newAccounts = accountDao.getNewAccounts();
                List<Account> modifiedAccounts = accountDao.getModifiedAccounts(lastSyncTime);
                List<Transaction> newTransactions = transactionDao.getNewTransactions();
                List<Transaction> modifiedTransactions = transactionDao.getModifiedTransactions(lastSyncTime);

                // تحديث حالة العناصر إلى PENDING
                for (Account account : newAccounts) {
                    account.setSyncStatus(SYNC_STATUS_PENDING);
                    accountDao.update(account);
                }
                for (Account account : modifiedAccounts) {
                    account.setSyncStatus(SYNC_STATUS_PENDING);
                    accountDao.update(account);
                }
                for (Transaction transaction : newTransactions) {
                    transaction.setSyncStatus(SYNC_STATUS_PENDING);
                    transactionDao.update(transaction);
                }
                for (Transaction transaction : modifiedTransactions) {
                    transaction.setSyncStatus(SYNC_STATUS_PENDING);
                    transactionDao.update(transaction);
                }

                // تخزين وقت آخر محاولة
                lastOfflineRetryTime = currentTime;
                offlineRetryCount++;

                // إعادة جدولة المزامنة
                handler.postDelayed(() -> {
                    if (isNetworkAvailable()) {
                        syncData(callback);
                    } else {
                        callback.onError("لا يوجد اتصال بالإنترنت. سيتم إعادة المحاولة تلقائياً عند توفر الاتصال.");
                    }
                }, OFFLINE_RETRY_INTERVAL);

            } catch (Exception e) {
                Log.e(TAG, "خطأ في المزامنة في وضع عدم الاتصال: " + e.getMessage());
                handler.post(() -> callback.onError("خطأ في المزامنة في وضع عدم الاتصال: " + e.getMessage()));
            }
        });
    }

    private <T> List<T> filterSyncingItems(List<T> items) {
        return items.stream()
                .filter(item -> {
                    String key = getItemKey(item);
                    Long syncStartTime = syncInProgress.get(key);
                    if (syncStartTime == null) {
                        return true;
                    }
                    // إذا كانت المزامنة تستغرق أكثر من 5 دقائق، نعتبرها فاشلة
                    if (System.currentTimeMillis() - syncStartTime > 5 * 60 * 1000) {
                        syncInProgress.remove(key);
                        return true;
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    private <T> String getItemKey(T item) {
        if (item instanceof Account) {
            return "account_" + ((Account) item).getId();
        } else if (item instanceof Transaction) {
            return "transaction_" + ((Transaction) item).getId();
        }
        return "";
    }

    private void updateSyncStatusOnFailure(List<Account> newAccounts, List<Account> modifiedAccounts,
                                         List<Transaction> newTransactions, List<Transaction> modifiedTransactions) {
        if (newAccounts != null) {
            for (Account account : newAccounts) {
                account.setSyncStatus(SYNC_STATUS_FAILED);
                accountDao.update(account);
                syncInProgress.remove(getItemKey(account));
            }
        }
        if (modifiedAccounts != null) {
            for (Account account : modifiedAccounts) {
                account.setSyncStatus(SYNC_STATUS_FAILED);
                accountDao.update(account);
                syncInProgress.remove(getItemKey(account));
            }
        }
        if (newTransactions != null) {
            for (Transaction transaction : newTransactions) {
                transaction.setSyncStatus(SYNC_STATUS_FAILED);
                transactionDao.update(transaction);
                syncInProgress.remove(getItemKey(transaction));
            }
        }
        if (modifiedTransactions != null) {
            for (Transaction transaction : modifiedTransactions) {
                transaction.setSyncStatus(SYNC_STATUS_FAILED);
                transactionDao.update(transaction);
                syncInProgress.remove(getItemKey(transaction));
            }
        }
    }

    public void enableAutoSync(boolean enable) {
        Log.d(TAG, "Setting auto sync to: " + enable);
        isAutoSyncEnabled = enable;
        context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("auto_sync", enable)
                .apply();
        
        if (enable) {
            // بدء المزامنة الدورية
            startPeriodicSync();
        } else {
            // إيقاف المزامنة الدورية
            stopPeriodicSync();
            Log.d(TAG, "Auto sync disabled");
        }
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
                .getInt("sync_interval", 5); // القيمة الافتراضية 30 دقيقة
    }

    // دالة جديدة لتفعيل المزامنة عند دخول لوحة التحكم
    public void onDashboardEntered() {
        if (!isNetworkAvailable()) {
            Log.d(TAG, "لا يوجد اتصال بالإنترنت للتحقق من التغييرات");
            return;
        }

        // محاولة إعادة المزامنات الفاشلة أولاً
        retryPendingSync();

        // التحقق من وقت آخر مزامنة
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSyncTime < SYNC_INTERVAL) {
            Log.d(TAG, "تمت المزامنة مؤخراً، جاري تخطي هذه المزامنة");
            return;
        }

        syncChanges(new SyncCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "تم التحقق من التغييرات بنجاح عند دخول لوحة التحكم");
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "فشل التحقق من التغييرات عند دخول لوحة التحكم: " + error);
            }
        });
    }

    private static class SyncRequest {
        private List<Account> accounts;
        private List<Transaction> transactions;
        
        public SyncRequest(List<Account> accounts, List<Transaction> transactions) {
            this.accounts = accounts;
            this.transactions = transactions;
        }
    }

    private Runnable syncRunnable = new Runnable() {
        @Override
        public void run() {
            if (isNetworkAvailable()) {
                Log.d(TAG, "Executing periodic sync...");
                
                // أولاً: إرسال التغييرات المحلية إلى السيرفر
                syncChanges(new SyncCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Local changes synced successfully");
                        
                        // ثانياً: جلب التغييرات من السيرفر
                        receiveChanges(new SyncCallback() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "Server changes received successfully");
                                // جدولة المزامنة التالية
                                handler.postDelayed(syncRunnable, SYNC_INTERVAL);
                            }

                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "Failed to receive server changes: " + error);
                                // إعادة المحاولة بعد فترة
                                handler.postDelayed(syncRunnable, SYNC_INTERVAL);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Failed to sync local changes: " + error);
                        // إعادة المحاولة بعد فترة
                        handler.postDelayed(syncRunnable, SYNC_INTERVAL);
                    }
                });
            } else {
                Log.d(TAG, "No network available, retrying in 30 seconds...");
                handler.postDelayed(syncRunnable, SYNC_INTERVAL);
            }
        }
    };

    public void syncChanges(SyncCallback callback) {
        if (!isNetworkAvailable()) {
            callback.onError("لا يوجد اتصال بالإنترنت");
            return;
        }

        if (isSyncing) {
            callback.onError("جاري تنفيذ عملية مزامنة أخرى، يرجى الانتظار");
            return;
        }

        String token = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .getString("token", null);

        if (token == null) {
            callback.onError("يرجى تسجيل الدخول أولاً");
            return;
        }

        isSyncing = true;
        executor.execute(() -> {
            try {
                Log.d(TAG, "بدء مزامنة التغييرات...");
                
                // 1. إرسال التغييرات المحلية إلى السيرفر
                List<Account> modifiedAccounts = accountDao.getModifiedAccounts(getLastSyncTime());
                List<Transaction> newTransactions = transactionDao.getNewTransactions();
                List<Transaction> modifiedTransactions = transactionDao.getModifiedTransactions(getLastSyncTime());

                // التحقق من وجود تغييرات
                if (!modifiedAccounts.isEmpty() || !newTransactions.isEmpty() || !modifiedTransactions.isEmpty()) {
                    Log.d(TAG, String.format("إرسال %d حساب معدل، %d معاملة جديدة، %d معاملة معدلة",
                        modifiedAccounts.size(), newTransactions.size(), modifiedTransactions.size()));

                    Map<String, Object> changes = new HashMap<>();
                    changes.put("accounts", modifiedAccounts);
                    changes.put("new_transactions", newTransactions);
                    changes.put("modified_transactions", modifiedTransactions);

                    // إرسال التغييرات إلى السيرفر
                    apiService.syncChanges("Bearer " + token, changes).enqueue(new Callback<Map<String, Object>>() {
                        @Override
                        public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                Map<String, Object> responseData = response.body();
                                
                                // التحقق من نجاح المزامنة
                                boolean syncSuccess = true;
                                
                                // تحديث معرفات السيرفر للمعاملات الجديدة
                                if (responseData.containsKey("new_transaction_ids")) {
                                    Map<Long, Long> newTransactionIds = (Map<Long, Long>) responseData.get("new_transaction_ids");
                                    for (Transaction transaction : newTransactions) {
                                        Long serverId = newTransactionIds.get(transaction.getId());
                                        if (serverId != null) {
                                            transaction.setServerId(serverId);
                                            transaction.setLastSyncTime(System.currentTimeMillis());
                                            transactionDao.update(transaction);
                                            Log.d(TAG, "تم تحديث معرف السيرفر للمعاملة: " + transaction.getId() + " -> " + serverId);
                                        } else {
                                            syncSuccess = false;
                                            Log.e(TAG, "لم يتم استلام معرف سيرفر للمعاملة: " + transaction.getId());
                                        }
                                    }
                                }

                                if (syncSuccess) {
                                    // 2. جلب التغييرات من السيرفر
                                    fetchServerChanges(token, callback);
                                } else {
                                    // حفظ المزامنة الفاشلة للتحميل لاحقاً
                                    savePendingSync(changes);
                                    String error = "فشلت مزامنة بعض التغييرات، سيتم إعادة المحاولة عند توفر الاتصال";
                                    Log.e(TAG, error);
                                    isSyncing = false;
                                    handler.post(() -> callback.onError(error));
                                }
                            } else {
                                // حفظ المزامنة الفاشلة للتحميل لاحقاً
                                savePendingSync(changes);
                                String error = "فشلت مزامنة التغييرات المحلية، سيتم إعادة المحاولة عند توفر الاتصال";
                                Log.e(TAG, error);
                                isSyncing = false;
                                handler.post(() -> callback.onError(error));
                            }
                        }

                        @Override
                        public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                            // حفظ المزامنة الفاشلة للتحميل لاحقاً
                            savePendingSync(changes);
                            String error = "خطأ في الاتصال: " + t.getMessage() + "، سيتم إعادة المحاولة عند توفر الاتصال";
                            Log.e(TAG, error);
                            isSyncing = false;
                            handler.post(() -> callback.onError(error));
                        }
                    });
                } else {
                    // إذا لم تكن هناك تغييرات محلية، جلب التغييرات من السيرفر مباشرة
                    fetchServerChanges(token, callback);
                }
            } catch (Exception e) {
                Log.e(TAG, "خطأ في مزامنة التغييرات: " + e.getMessage());
                isSyncing = false;
                handler.post(() -> callback.onError("خطأ في مزامنة التغييرات: " + e.getMessage()));
            }
        });
    }

    private void fetchServerChanges(String token, SyncCallback callback) {
        long lastSyncTime = getLastSyncTime();
        Log.d(TAG, "جلب التغييرات من السيرفر منذ: " + lastSyncTime);

        apiService.getChanges("Bearer " + token, lastSyncTime).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> changes = response.body();
                    executor.execute(() -> {
                        try {
                            // تحديث الحسابات
                            List<Map<String, Object>> accounts = (List<Map<String, Object>>) changes.get("accounts");
                            if (accounts != null && !accounts.isEmpty()) {
                                Log.d(TAG, "تم استلام " + accounts.size() + " حساب معدل من السيرفر");
                                for (Map<String, Object> accountData : accounts) {
                                    String accountNumber = (String) accountData.get("account_number");
                                    if (accountNumber == null) continue;

                                    Account existingAccount = accountDao.getAccountByNumberSync(accountNumber);
                                    if (existingAccount != null) {
                                        // تحديث الحساب الموجود
                                        existingAccount.setName((String) accountData.get("account_name"));
                                        existingAccount.setPhoneNumber((String) accountData.get("phone_number"));
                                        existingAccount.setBalance(((Number) accountData.get("balance")).doubleValue());
                                        existingAccount.setCurrency((String) accountData.get("currency"));
                                        existingAccount.setNotes((String) accountData.get("notes"));
                                        existingAccount.setWhatsappEnabled((Boolean) accountData.get("whatsapp_enabled"));
                                        existingAccount.setIsDebtor((Boolean) accountData.get("is_debtor"));
                                        existingAccount.setLastSyncTime(System.currentTimeMillis());
                                        accountDao.update(existingAccount);
                                    }
                                }
                            }

                            // تحديث المعاملات
                            List<Map<String, Object>> transactions = (List<Map<String, Object>>) changes.get("transactions");
                            if (transactions != null && !transactions.isEmpty()) {
                                Log.d(TAG, "تم استلام " + transactions.size() + " معاملة من السيرفر");
                                for (Map<String, Object> transactionData : transactions) {
                                    long serverId = ((Number) transactionData.get("id")).longValue();
                                    
                                    Transaction existingTransaction = transactionDao.getTransactionByServerIdSync(serverId);
                                    if (existingTransaction != null) {
                                        // تحديث المعاملة الموجودة
                                        existingTransaction.setAmount(((Number) transactionData.get("amount")).doubleValue());
                                        existingTransaction.setType((String) transactionData.get("type"));
                                        existingTransaction.setDescription((String) transactionData.get("description"));
                                        existingTransaction.setNotes((String) transactionData.get("notes"));
                                        existingTransaction.setTransactionDate(((Number) transactionData.get("date")).longValue());
                                        existingTransaction.setCurrency((String) transactionData.get("currency"));
                                        existingTransaction.setWhatsappEnabled((Boolean) transactionData.get("whatsapp_enabled"));
                                        existingTransaction.setLastSyncTime(System.currentTimeMillis());
                                        transactionDao.update(existingTransaction);
                                    } else {
                                        // إضافة معاملة جديدة
                                        Transaction transaction = new Transaction();
                                        transaction.setServerId(serverId);
                                        transaction.setAmount(((Number) transactionData.get("amount")).doubleValue());
                                        transaction.setType((String) transactionData.get("type"));
                                        transaction.setDescription((String) transactionData.get("description"));
                                        transaction.setNotes((String) transactionData.get("notes"));
                                        transaction.setTransactionDate(((Number) transactionData.get("date")).longValue());
                                        transaction.setCurrency((String) transactionData.get("currency"));
                                        transaction.setWhatsappEnabled((Boolean) transactionData.get("whatsapp_enabled"));
                                        transaction.setAccountId(((Number) transactionData.get("account_id")).longValue());
                                        transaction.setUserId(((Number) transactionData.get("user_id")).longValue());
                                        transaction.setLastSyncTime(System.currentTimeMillis());
                                        transactionDao.insert(transaction);
                                    }
                                }
                            }

                            updateLastSyncTime();
                            Log.d(TAG, "تم تحديث البيانات المحلية بنجاح");
                            isSyncing = false;
                            handler.post(() -> callback.onSuccess());
                        } catch (Exception e) {
                            Log.e(TAG, "خطأ في تحديث البيانات المحلية: " + e.getMessage());
                            isSyncing = false;
                            handler.post(() -> callback.onError("خطأ في تحديث البيانات المحلية: " + e.getMessage()));
                        }
                    });
                } else {
                    String error = "فشل جلب التغييرات من السيرفر";
                    Log.e(TAG, error);
                    isSyncing = false;
                    handler.post(() -> callback.onError(error));
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                String error = "خطأ في الاتصال: " + t.getMessage();
                Log.e(TAG, error);
                isSyncing = false;
                handler.post(() -> callback.onError(error));
            }
        });
    }

    public void receiveChanges(SyncCallback callback) {
        if (!isNetworkAvailable()) {
            callback.onError("No internet connection");
            return;
        }

        String token = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .getString("token", null);

        if (token == null) {
            callback.onError("User not authenticated");
            return;
        }

        long currentUserId = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .getLong("user_id", -1);

        if (currentUserId == -1) {
            callback.onError("User ID not found");
            return;
        }

        long lastSyncTime = getLastSyncTime();
        Log.d(TAG, "Fetching changes since: " + lastSyncTime + " for user: " + currentUserId);

        apiService.getChanges("Bearer " + token, lastSyncTime).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> changes = response.body();
                    executor.execute(() -> {
                        try {
                            // تحديث الحسابات
                            List<Map<String, Object>> accounts = (List<Map<String, Object>>) changes.get("accounts");
                            if (accounts != null && !accounts.isEmpty()) {
                                Log.d(TAG, "Received " + accounts.size() + " account changes");
                                for (Map<String, Object> accountData : accounts) {
                                    // التحقق من أن الحساب ينتمي للمستخدم الحالي
                                    Long accountUserId = ((Number) accountData.get("user_id")).longValue();
                                    if (accountUserId != currentUserId) {
                                        Log.d(TAG, "Skipping account not belonging to current user");
                                        continue;
                                    }

                                    String accountNumber = (String) accountData.get("account_number");
                                    if (accountNumber == null) {
                                        Log.e(TAG, "Account number is null, skipping account");
                                        continue;
                                    }

                                    // البحث عن الحساب في قاعدة البيانات المحلية
                                    Account existingAccount = accountDao.getAccountByNumberSync(accountNumber);
                                    if (existingAccount != null) {
                                        // تحديث الحساب الموجود
                                        existingAccount.setName((String) accountData.get("account_name"));
                                        existingAccount.setPhoneNumber((String) accountData.get("phone_number"));
                                        existingAccount.setBalance(((Number) accountData.get("balance")).doubleValue());
                                        existingAccount.setCurrency((String) accountData.get("currency"));
                                        existingAccount.setNotes((String) accountData.get("notes"));
                                        existingAccount.setWhatsappEnabled((Boolean) accountData.get("whatsapp_enabled"));
                                        existingAccount.setIsDebtor((Boolean) accountData.get("is_debtor"));
                                        existingAccount.setServerId(((Number) accountData.get("id")).longValue());
                                        existingAccount.setLastSyncTime(System.currentTimeMillis());
                                        existingAccount.setSyncStatus(SYNC_STATUS_SYNCED);
                                        accountDao.update(existingAccount);
                                        Log.d(TAG, "Updated account: " + accountNumber);
                                    } else {
                                        // إضافة حساب جديد
                                        Account account = new Account();
                                        account.setAccountNumber(accountNumber);
                                        account.setName((String) accountData.get("account_name"));
                                        account.setPhoneNumber((String) accountData.get("phone_number"));
                                        account.setBalance(((Number) accountData.get("balance")).doubleValue());
                                        account.setCurrency((String) accountData.get("currency"));
                                        account.setNotes((String) accountData.get("notes"));
                                        account.setWhatsappEnabled((Boolean) accountData.get("whatsapp_enabled"));
                                        account.setIsDebtor((Boolean) accountData.get("is_debtor"));
                                        account.setServerId(((Number) accountData.get("id")).longValue());
                                        account.setUserId(currentUserId);
                                        account.setLastSyncTime(System.currentTimeMillis());
                                        account.setSyncStatus(SYNC_STATUS_SYNCED);
                                        accountDao.insert(account);
                                        Log.d(TAG, "Added new account: " + accountNumber);
                                    }
                                }
                            }

                            // تحديث المعاملات
                            List<Map<String, Object>> transactions = (List<Map<String, Object>>) changes.get("transactions");
                            if (transactions != null && !transactions.isEmpty()) {
                                Log.d(TAG, "Received " + transactions.size() + " transaction changes");
                                for (Map<String, Object> transactionData : transactions) {
                                    // التحقق من أن المعاملة تنتمي للمستخدم الحالي
                                    Long transactionUserId = ((Number) transactionData.get("user_id")).longValue();
                                    if (transactionUserId != currentUserId) {
                                        Log.d(TAG, "Skipping transaction not belonging to current user");
                                        continue;
                                    }

                                    long serverId = ((Number) transactionData.get("id")).longValue();
                                    
                                    // البحث عن المعاملة في قاعدة البيانات المحلية
                                    Transaction existingTransaction = transactionDao.getTransactionByServerIdSync(serverId);
                                    if (existingTransaction != null) {
                                        // تحديث المعاملة الموجودة
                                        existingTransaction.setAmount(((Number) transactionData.get("amount")).doubleValue());
                                        existingTransaction.setType((String) transactionData.get("type"));
                                        existingTransaction.setDescription((String) transactionData.get("description"));
                                        existingTransaction.setNotes((String) transactionData.get("notes"));
                                        existingTransaction.setTransactionDate(((Number) transactionData.get("date")).longValue());
                                        existingTransaction.setCurrency((String) transactionData.get("currency"));
                                        existingTransaction.setWhatsappEnabled((Boolean) transactionData.get("whatsapp_enabled"));
                                        existingTransaction.setAccountId(((Number) transactionData.get("account_id")).longValue());
                                        existingTransaction.setLastSyncTime(System.currentTimeMillis());
                                        existingTransaction.setSyncStatus(SYNC_STATUS_SYNCED);
                                        transactionDao.update(existingTransaction);
                                        Log.d(TAG, "Updated transaction: " + serverId);
                                    } else {
                                        // إضافة معاملة جديدة
                                        Transaction transaction = new Transaction();
                                        transaction.setServerId(serverId);
                                        transaction.setAmount(((Number) transactionData.get("amount")).doubleValue());
                                        transaction.setType((String) transactionData.get("type"));
                                        transaction.setDescription((String) transactionData.get("description"));
                                        transaction.setNotes((String) transactionData.get("notes"));
                                        transaction.setTransactionDate(((Number) transactionData.get("date")).longValue());
                                        transaction.setCurrency((String) transactionData.get("currency"));
                                        transaction.setWhatsappEnabled((Boolean) transactionData.get("whatsapp_enabled"));
                                        transaction.setAccountId(((Number) transactionData.get("account_id")).longValue());
                                        transaction.setUserId(currentUserId);
                                        transaction.setLastSyncTime(System.currentTimeMillis());
                                        transaction.setSyncStatus(SYNC_STATUS_SYNCED);
                                        transactionDao.insert(transaction);
                                        Log.d(TAG, "Added new transaction: " + serverId);
                                    }
                                }
                            }

                            // التعامل مع المعاملات المحذوفة
                            List<Long> deletedTransactionIds = (List<Long>) changes.get("deleted_transactions");
                            if (deletedTransactionIds != null && !deletedTransactionIds.isEmpty()) {
                                Log.d(TAG, "Received " + deletedTransactionIds.size() + " deleted transactions");
                                for (Long serverId : deletedTransactionIds) {
                                    Transaction transaction = transactionDao.getTransactionByServerIdSync(serverId);
                                    if (transaction != null && transaction.getUserId() == currentUserId) {
                                        transactionDao.delete(transaction);
                                        Log.d(TAG, "Deleted transaction with server ID: " + serverId);
                                    }
                                }
                            }

                            saveLastSyncTime(System.currentTimeMillis());
                            Log.d(TAG, "Successfully updated local data");
                            handler.post(() -> callback.onSuccess());
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating local data: " + e.getMessage());
                            handler.post(() -> callback.onError("Error updating local data: " + e.getMessage()));
                        }
                    });
                } else {
                    String errorBody;
                    try {
                        errorBody = response.errorBody() != null ? 
                            response.errorBody().string() : "Unknown error";
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading error body: " + e.getMessage());
                        errorBody = "Error reading response";
                    }
                    final String finalErrorBody = errorBody;
                    Log.e(TAG, "Failed to receive changes: " + finalErrorBody);
                    callback.onError("Failed to receive changes: " + finalErrorBody);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                final String errorBody = t.getMessage();
                handler.post(() -> callback.onError("Failed to receive changes: " + errorBody));
            }
        });
    }

    private void saveLastSyncTime(long time) {
        SharedPreferences prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE);
        prefs.edit().putLong("last_sync_time", time).apply();
    }

    // إضافة دالة لحذف القيد
    public void deleteTransaction(long transactionId, SyncCallback callback) {
        if (!isNetworkAvailable()) {
            callback.onError("No internet connection");
            return;
        }

        String token = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .getString("token", null);

        if (token == null) {
            callback.onError("User not authenticated");
            return;
        }

        executor.execute(() -> {
            try {
                // الحصول على القيد من قاعدة البيانات المحلية
                transactionDao.getTransactionById(transactionId).observe((LifecycleOwner) context, transaction -> {
                    if (transaction != null) {
                        // إذا كان القيد له معرف سيرفر، نقوم بحذفه من السيرفر
                        if (transaction.getServerId() > 0) {
                            apiService.deleteTransaction("Bearer " + token, transaction.getServerId()).enqueue(new Callback<Void>() {
                                @Override
                                public void onResponse(Call<Void> call, Response<Void> response) {
                                    if (response.isSuccessful()) {
                                        // حذف القيد من قاعدة البيانات المحلية
                                        executor.execute(() -> {
                                            transactionDao.delete(transaction);
                                            Log.d(TAG, "Transaction deleted successfully from server and local database");
                                            handler.post(() -> callback.onSuccess());
                                        });
                                    } else {
                                        String errorBody;
                                        try {
                                            errorBody = response.errorBody() != null ? 
                                                response.errorBody().string() : "Unknown error";
                                        } catch (IOException e) {
                                            Log.e(TAG, "Error reading error body: " + e.getMessage());
                                            errorBody = "Error reading response";
                                        }
                                        final String finalErrorBody = errorBody;
                                        Log.e(TAG, "Failed to delete transaction from server: " + finalErrorBody);
                                        handler.post(() -> callback.onError("Failed to delete transaction: " + finalErrorBody));
                                    }
                                }

                                @Override
                                public void onFailure(Call<Void> call, Throwable t) {
                                    final String errorBody = t.getMessage();
                                    handler.post(() -> callback.onError("Failed to delete transaction: " + errorBody));
                                }
                            });
                        } else {
                            // إذا لم يكن للقيد معرف سيرفر، نقوم بحذفه محلياً فقط
                            transactionDao.delete(transaction);
                            Log.d(TAG, "Transaction deleted from local database only");
                            handler.post(() -> callback.onSuccess());
                        }
                    } else {
                        handler.post(() -> callback.onError("Transaction not found"));
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error deleting transaction: " + e.getMessage());
                handler.post(() -> callback.onError("Error deleting transaction: " + e.getMessage()));
            }
        });
    }

    private List<Account> getModifiedAccounts(long lastSyncTime) {
        return accountDao.getModifiedAccounts(lastSyncTime);
    }

    private List<Transaction> getModifiedTransactions(long lastSyncTime) {
        return transactionDao.getModifiedTransactions(lastSyncTime);
    }

    public void performFullSync(SyncCallback callback) {
        if (!isNetworkAvailable()) {
            callback.onError("لا يوجد اتصال بالإنترنت");
            return;
        }

        if (isSyncing) {
            callback.onError("جاري تنفيذ عملية مزامنة أخرى، يرجى الانتظار");
            return;
        }

        String token = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .getString("token", null);

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
                        updateLastSyncTime();
                        Log.d(TAG, "تمت المزامنة الكاملة بنجاح");
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

    private void performInitialSync() {
        if (!isNetworkAvailable()) {
            Log.d(TAG, "No network available for initial sync");
            return;
        }

        String token = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .getString("token", null);

        if (token == null) {
            Log.d(TAG, "No token available for initial sync");
            return;
        }

        performFullSync(new SyncCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Initial sync completed successfully");
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Initial sync failed: " + error);
            }
        });
    }

    public void startPeriodicSync() {
        if (periodicSyncRunnable != null) {
            syncHandler.removeCallbacks(periodicSyncRunnable);
        }

        periodicSyncRunnable = new Runnable() {
            @Override
            public void run() {
                if (isNetworkAvailable()) {
                    // محاولة إعادة المزامنات الفاشلة أولاً
                    retryPendingSync();
                    
                    // ثم المزامنة العادية
                    syncChanges(new SyncCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "تمت المزامنة الدورية بنجاح");
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "فشلت المزامنة الدورية: " + error);
                        }
                    });
                }
                // جدولة المزامنة التالية
                syncHandler.postDelayed(this, SYNC_INTERVAL);
            }
        };

        // بدء المزامنة الدورية
        syncHandler.post(periodicSyncRunnable);
    }

    public void stopPeriodicSync() {
        if (periodicSyncRunnable != null) {
            syncHandler.removeCallbacks(periodicSyncRunnable);
            periodicSyncRunnable = null;
        }
    }

    private void savePendingSync(Map<String, Object> changes) {
        SharedPreferences prefs = context.getSharedPreferences(PENDING_SYNC_PREFS, Context.MODE_PRIVATE);
        String changesJson = new Gson().toJson(changes);
        prefs.edit()
            .putString(PENDING_SYNC_DATA, changesJson)
            .putLong(PENDING_SYNC_TIME, System.currentTimeMillis())
            .apply();
        Log.d(TAG, "تم حفظ بيانات المزامنة الفاشلة للتحميل لاحقاً");
    }

    private void clearPendingSync() {
        SharedPreferences prefs = context.getSharedPreferences(PENDING_SYNC_PREFS, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        Log.d(TAG, "تم مسح بيانات المزامنة الفاشلة بعد نجاح المزامنة");
    }

    private void retryPendingSync() {
        SharedPreferences prefs = context.getSharedPreferences(PENDING_SYNC_PREFS, Context.MODE_PRIVATE);
        String pendingData = prefs.getString(PENDING_SYNC_DATA, null);
        long pendingTime = prefs.getLong(PENDING_SYNC_TIME, 0);

        if (pendingData != null && pendingTime > 0) {
            Log.d(TAG, "تم العثور على مزامنة فاشلة من: " + new Date(pendingTime));
            try {
                Map<String, Object> changes = new Gson().fromJson(pendingData, Map.class);
                syncChanges(new SyncCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "تمت إعادة مزامنة البيانات الفاشلة بنجاح");
                        clearPendingSync();
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "فشلت إعادة مزامنة البيانات الفاشلة: " + error);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "خطأ في تحليل بيانات المزامنة الفاشلة: " + e.getMessage());
                clearPendingSync();
            }
        }
    }
} 
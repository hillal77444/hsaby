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
import android.net.Network;
import android.net.NetworkRequest;

import com.hillal.hhhhhhh.data.remote.ApiService;
import com.hillal.hhhhhhh.data.remote.RetrofitClient;
import com.hillal.hhhhhhh.data.remote.DataManager;
import com.hillal.hhhhhhh.data.room.AccountDao;
import com.hillal.hhhhhhh.data.room.TransactionDao;
import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.model.Transaction;
import com.hillal.hhhhhhh.data.model.User;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.IOException;
import androidx.lifecycle.LifecycleOwner;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

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

    // إضافة ثوابت جديدة
    private static final int MAX_SYNC_RETRY_COUNT = 3;
    private static final long SYNC_RETRY_DELAY = 5000; // 5 ثواني
    private static final long SYNC_TIMEOUT = 30000; // 30 ثانية

    // إضافة متغيرات جديدة
    private final Map<String, SyncSession> activeSyncSessions = new ConcurrentHashMap<>();
    private final Map<Long, Long> lastTransactionSync = new ConcurrentHashMap<>();
    private final Map<Long, Long> lastAccountSync = new ConcurrentHashMap<>();

    // إضافة ثوابت جديدة للمزامنة في وضع عدم الاتصال
    private static final String OFFLINE_QUEUE_PREFS = "offline_sync_queue";
    private static final String OFFLINE_QUEUE_DATA = "offline_queue_data";
    private static final int MAX_QUEUE_SIZE = 1000;
    private static final long MIN_RETRY_INTERVAL = 60000; // دقيقة واحدة
    private static final long MAX_RETRY_INTERVAL = 3600000; // ساعة واحدة

    // إضافة المتغيرات المفقودة
    private final AtomicLong lastRetryTime = new AtomicLong(0);
    private final AtomicInteger retryCount = new AtomicInteger(0);

    // تعديل تعريف offlineQueue
    private final Queue<SyncRequest> offlineQueue = new ConcurrentLinkedQueue<>();

    // إضافة كلاس جديد للتعامل مع التعارضات
    private static class ConflictResolver {
        private final TransactionDao transactionDao;
        private final AccountDao accountDao;

        public ConflictResolver(TransactionDao transactionDao, AccountDao accountDao) {
            this.transactionDao = transactionDao;
            this.accountDao = accountDao;
        }

        public Transaction resolveTransactionConflict(Transaction local, Transaction server) {
            if (local.getLastSyncTime() > server.getLastSyncTime()) {
                return local;
            } else if (server.getLastSyncTime() > local.getLastSyncTime()) {
                return server;
            } else {
                return local;
            }
        }

        public Account resolveAccountConflict(Account local, Account server) {
            if (local.getLastSyncTime() > server.getLastSyncTime()) {
                return local;
            } else if (server.getLastSyncTime() > local.getLastSyncTime()) {
                return server;
            } else {
                return local;
            }
        }
    }

    // تعديل SyncRequest
    private static class SyncRequest {
        private final String id;
        private final Map<String, Object> data;
        private final long timestamp;
        private int retryCount;

        public SyncRequest(Map<String, Object> data) {
            this.id = UUID.randomUUID().toString();
            this.data = data;
            this.timestamp = System.currentTimeMillis();
            this.retryCount = 0;
        }
    }

    public SyncManager(Context context, AccountDao accountDao, TransactionDao transactionDao) {
        this.context = context;
        this.apiService = RetrofitClient.getInstance().getApiService();
        this.accountDao = accountDao;
        this.transactionDao = transactionDao;
        this.handler = new Handler(Looper.getMainLooper());
        this.lastSyncTime = getLastSyncTime();
        
        // تحميل قائمة انتظار المزامنة
        loadOfflineQueue();
        
        // بدء مراقبة الاتصال
        startNetworkMonitoring();
        
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
        private final String id;
        private final List<Account> newAccounts;
        private final List<Account> modifiedAccounts;
        private final List<Transaction> newTransactions;
        private final List<Transaction> modifiedTransactions;
        private final AtomicInteger completedBatches = new AtomicInteger(0);
        private final AtomicInteger failedBatches = new AtomicInteger(0);
        private final AtomicInteger totalBatches = new AtomicInteger(0);

        public SyncSession(String id, List<Account> newAccounts, List<Account> modifiedAccounts,
                          List<Transaction> newTransactions, List<Transaction> modifiedTransactions) {
            this.id = id;
            this.newAccounts = newAccounts;
            this.modifiedAccounts = modifiedAccounts;
            this.newTransactions = newTransactions;
            this.modifiedTransactions = modifiedTransactions;
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

        public void incrementCompletedBatches() {
            completedBatches.incrementAndGet();
        }

        public void incrementFailedBatches() {
            failedBatches.incrementAndGet();
        }

        public void setTotalBatches(int total) {
            totalBatches.set(total);
        }

        public boolean waitForCompletion(long timeout) {
            long startTime = System.currentTimeMillis();
            while (completedBatches.get() + failedBatches.get() < totalBatches.get()) {
                if (System.currentTimeMillis() - startTime > timeout) {
                    return false;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    return false;
                }
            }
            return failedBatches.get() == 0;
        }
    }

    public void syncData(SyncCallback callback) {
        String token = getToken();
        if (token == null) {
            callback.onError("يرجى تسجيل الدخول أولاً");
            return;
        }

        if (!isNetworkAvailable()) {
            handleOfflineSync(callback);
            return;
        }

        synchronized (syncLock) {
            if (isSyncing) {
                callback.onError("جاري تنفيذ عملية مزامنة أخرى، يرجى الانتظار");
                return;
            }
            isSyncing = true;
        }

        executor.execute(() -> {
            try {
                String syncId = UUID.randomUUID().toString();
                SyncSession session = new SyncSession(
                    syncId,
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

    // تعديل handleOfflineSync
    private void handleOfflineSync(SyncCallback callback) {
        Log.d(TAG, "لا يوجد اتصال بالإنترنت، جاري حفظ التغييرات للتحميل لاحقاً");
        
        try {
            // الحصول على التغييرات الحالية
            List<Transaction> newTransactions = transactionDao.getNewTransactions();
            List<Transaction> modifiedTransactions = transactionDao.getModifiedTransactions(lastSyncTime);

            if (newTransactions.isEmpty() && modifiedTransactions.isEmpty()) {
                callback.onSuccess();
                return;
            }

            // إنشاء طلب مزامنة
            Map<String, Object> syncData = new HashMap<>();
            if (!newTransactions.isEmpty()) {
                syncData.put("new_transactions", newTransactions);
            }
            if (!modifiedTransactions.isEmpty()) {
                syncData.put("modified_transactions", modifiedTransactions);
            }

            // إضافة الطلب إلى قائمة الانتظار
            SyncRequest request = new SyncRequest(syncData);
            if (offlineQueue.size() < MAX_QUEUE_SIZE) {
                offlineQueue.offer(request);
                saveOfflineQueue();
                
                // تحديث حالة المعاملات
                for (Transaction transaction : newTransactions) {
                    transaction.setSyncStatus(SYNC_STATUS_PENDING);
                    transactionDao.update(transaction);
                }
                for (Transaction transaction : modifiedTransactions) {
                    transaction.setSyncStatus(SYNC_STATUS_PENDING);
                    transactionDao.update(transaction);
                }

                // بدء مراقبة الاتصال
                startNetworkMonitoring();

                callback.onSuccess();
            } else {
                callback.onError("قائمة انتظار المزامنة ممتلئة");
            }
        } catch (Exception e) {
            Log.e(TAG, "خطأ في حفظ المزامنة في وضع عدم الاتصال: " + e.getMessage());
            callback.onError("خطأ في حفظ المزامنة في وضع عدم الاتصال");
        }
    }

    // تعديل saveOfflineQueue
    private void saveOfflineQueue() {
        try {
            List<Map<String, Object>> queueData = new ArrayList<>();
            for (SyncRequest request : offlineQueue) {
                Map<String, Object> requestData = new HashMap<>();
                requestData.put("id", request.id);
                requestData.put("data", request.data);
                requestData.put("timestamp", request.timestamp);
                requestData.put("retryCount", request.retryCount);
                queueData.add(requestData);
            }

            String queueJson = new Gson().toJson(queueData);
            context.getSharedPreferences(OFFLINE_QUEUE_PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putString(OFFLINE_QUEUE_DATA, queueJson)
                    .apply();
        } catch (Exception e) {
            Log.e(TAG, "خطأ في حفظ قائمة انتظار المزامنة: " + e.getMessage());
        }
    }

    // تعديل loadOfflineQueue
    private void loadOfflineQueue() {
        try {
            String queueJson = context.getSharedPreferences(OFFLINE_QUEUE_PREFS, Context.MODE_PRIVATE)
                    .getString(OFFLINE_QUEUE_DATA, null);
            
            if (queueJson != null) {
                Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> queueData = new Gson().fromJson(queueJson, type);
                
                offlineQueue.clear();
                for (Map<String, Object> requestData : queueData) {
                    SyncRequest request = new SyncRequest((Map<String, Object>) requestData.get("data"));
                    request.retryCount = ((Number) requestData.get("retryCount")).intValue();
                    offlineQueue.offer(request);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "خطأ في تحميل قائمة انتظار المزامنة: " + e.getMessage());
        }
    }

    private void startNetworkMonitoring() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkRequest.Builder builder = new NetworkRequest.Builder();
            connectivityManager.registerNetworkCallback(builder.build(), new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    Log.d(TAG, "تم اكتشاف اتصال بالإنترنت، جاري محاولة المزامنة");
                    processOfflineQueue();
                }
            });
        }
    }

    private void processOfflineQueue() {
        if (!isNetworkAvailable() || offlineQueue.isEmpty()) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastRetryTime.get() < getRetryInterval()) {
            return;
        }

        SyncRequest request = offlineQueue.peek();
        if (request == null) {
            return;
        }

        if (request.retryCount >= MAX_SYNC_RETRY_COUNT) {
            Log.d(TAG, "تم تجاوز الحد الأقصى لمحاولات المزامنة، جاري إزالة الطلب");
            offlineQueue.poll();
            saveOfflineQueue();
            return;
        }

        String token = getToken();
        if (token == null) {
            return;
        }

        apiService.syncChanges(token, request.data).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "تمت مزامنة البيانات المخزنة بنجاح");
                    offlineQueue.poll();
                    saveOfflineQueue();
                    lastRetryTime.set(currentTime);
                    retryCount.set(0);
                    
                    // معالجة الطلب التالي
                    handler.postDelayed(() -> processOfflineQueue(), 1000);
                } else {
                    handleSyncFailure(request);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                handleSyncFailure(request);
            }
        });
    }

    private void handleSyncFailure(SyncRequest request) {
        request.retryCount++;
        lastRetryTime.set(System.currentTimeMillis());
        retryCount.incrementAndGet();
        
        if (request.retryCount >= MAX_SYNC_RETRY_COUNT) {
            Log.d(TAG, "فشلت المزامنة بعد " + MAX_SYNC_RETRY_COUNT + " محاولات");
            offlineQueue.poll();
            saveOfflineQueue();
        }
        
        // إعادة المحاولة بعد فترة
        handler.postDelayed(() -> processOfflineQueue(), getRetryInterval());
    }

    private long getRetryInterval() {
        // زيادة الفاصل الزمني مع كل محاولة فاشلة
        return Math.min(MIN_RETRY_INTERVAL * (1 << retryCount.get()), MAX_RETRY_INTERVAL);
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
            handleOfflineSync(callback);
            return;
        }

        String syncId = UUID.randomUUID().toString();
        SyncSession session = new SyncSession(
            syncId,
            accountDao.getNewAccounts(),
            accountDao.getModifiedAccounts(lastSyncTime),
            transactionDao.getNewTransactions(),
            transactionDao.getModifiedTransactions(lastSyncTime)
        );
        
        synchronized (syncLock) {
            if (isSyncing) {
                Log.d(TAG, "جاري تنفيذ مزامنة أخرى، سيتم إضافة هذه المزامنة إلى قائمة الانتظار");
                callback.onError("جاري تنفيذ مزامنة أخرى");
                return;
            }
            isSyncing = true;
            activeSyncSessions.put(syncId, session);
        }

        try {
            String token = getToken();
            if (token == null) {
                callback.onError("لم يتم العثور على رمز المصادقة");
                return;
            }

            // الحصول على المعاملات الجديدة والمعدلة
            List<Transaction> newTransactions = transactionDao.getNewTransactions();
            List<Transaction> modifiedTransactions = transactionDao.getModifiedTransactions(lastSyncTime);

            // تصفية المعاملات المكررة والمزامنة مؤخراً
            newTransactions = filterTransactions(newTransactions);
            modifiedTransactions = filterTransactions(modifiedTransactions);

            // تجميع المعاملات في دفعات
            List<List<Transaction>> newTransactionBatches = splitIntoBatches(newTransactions, BATCH_SIZE);
            List<List<Transaction>> modifiedTransactionBatches = splitIntoBatches(modifiedTransactions, BATCH_SIZE);

            // إرسال كل دفعة على حدة
            for (List<Transaction> batch : newTransactionBatches) {
                sendTransactionBatch(batch, true, token, session);
            }

            for (List<Transaction> batch : modifiedTransactionBatches) {
                sendTransactionBatch(batch, false, token, session);
            }

            // انتظار اكتمال جميع الدفعات
            if (session.waitForCompletion(SYNC_TIMEOUT)) {
                updateLastSyncTime();
                isSyncing = false;
                activeSyncSessions.remove(syncId);
                callback.onSuccess();
            } else {
                throw new TimeoutException("انتهت مهلة المزامنة");
            }

        } catch (Exception e) {
            Log.e(TAG, "خطأ في المزامنة: " + e.getMessage());
            isSyncing = false;
            activeSyncSessions.remove(syncId);
            callback.onError("خطأ في المزامنة: " + e.getMessage());
        }
    }

    private List<Transaction> filterTransactions(List<Transaction> transactions) {
        return transactions.stream()
            .filter(t -> {
                Long lastSync = lastTransactionSync.get(t.getId());
                return lastSync == null || 
                       (System.currentTimeMillis() - lastSync) > SYNC_RETRY_DELAY;
            })
            .collect(Collectors.toList());
    }

    private List<List<Transaction>> splitIntoBatches(List<Transaction> items, int batchSize) {
        List<List<Transaction>> batches = new ArrayList<>();
        for (int i = 0; i < items.size(); i += batchSize) {
            batches.add(items.subList(i, Math.min(i + batchSize, items.size())));
        }
        return batches;
    }

    private void sendTransactionBatch(List<Transaction> batch, boolean isNew, String token, SyncSession session) {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put(isNew ? "new_transactions" : "modified_transactions", batch);

        apiService.syncChanges(token, requestData).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> responseData = response.body();
                    handleSyncResponse(responseData, batch, session);
                } else {
                    handleSyncError(batch, session);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                handleSyncError(batch, session);
            }
        });
    }

    private void handleSyncResponse(Map<String, Object> responseData, List<Transaction> batch, SyncSession session) {
        if (responseData.containsKey("transactionIdMap")) {
            List<Map<String, Object>> idMap = (List<Map<String, Object>>) responseData.get("transactionIdMap");
            for (Map<String, Object> mapping : idMap) {
                long localId = ((Number) mapping.get("localId")).longValue();
                long serverId = ((Number) mapping.get("serverId")).longValue();
                
                LiveData<Transaction> transactionLiveData = transactionDao.getTransactionById(localId);
                transactionLiveData.observeForever(new Observer<Transaction>() {
                    @Override
                    public void onChanged(Transaction transaction) {
                        if (transaction != null) {
                            transaction.setServerId(serverId);
                            transaction.setSyncStatus(SYNC_STATUS_SYNCED);
                            transaction.setLastSyncTime(System.currentTimeMillis());
                            transactionDao.update(transaction);
                            lastTransactionSync.put(localId, System.currentTimeMillis());
                        }
                        transactionLiveData.removeObserver(this);
                    }
                });
            }
        }
        session.incrementCompletedBatches();
    }

    private void handleSyncError(List<Transaction> batch, SyncSession session) {
        for (Transaction transaction : batch) {
            transaction.setSyncStatus(SYNC_STATUS_FAILED);
            transactionDao.update(transaction);
        }
        session.incrementFailedBatches();
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
        Log.d(TAG, "جلب التغييرات من السيرفر منذ: " + lastSyncTime + " للمستخدم: " + currentUserId);

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
                                    // التحقق من أن الحساب ينتمي للمستخدم الحالي
                                    Long accountUserId = ((Number) accountData.get("user_id")).longValue();
                                    if (accountUserId != currentUserId) {
                                        Log.d(TAG, "تخطي حساب لا ينتمي للمستخدم الحالي");
                                        continue;
                                    }

                                    String accountNumber = (String) accountData.get("account_number");
                                    if (accountNumber == null) {
                                        Log.e(TAG, "رقم الحساب فارغ، تم تخطي الحساب");
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
                                        existingAccount.setLastSyncTime(System.currentTimeMillis());
                                        existingAccount.setSyncStatus(SYNC_STATUS_SYNCED);
                                        accountDao.update(existingAccount);
                                        Log.d(TAG, "تم تحديث الحساب: " + accountNumber);
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
                                        Log.d(TAG, "تم إضافة حساب جديد: " + accountNumber);
                                    }
                                }
                            }

                            // تحديث المعاملات
                            List<Map<String, Object>> transactions = (List<Map<String, Object>>) changes.get("transactions");
                            if (transactions != null && !transactions.isEmpty()) {
                                Log.d(TAG, "تم استلام " + transactions.size() + " معاملة من السيرفر");
                                for (Map<String, Object> transactionData : transactions) {
                                    // التحقق من أن المعاملة تنتمي للمستخدم الحالي
                                    Long transactionUserId = ((Number) transactionData.get("user_id")).longValue();
                                    if (transactionUserId != currentUserId) {
                                        Log.d(TAG, "تخطي معاملة لا تنتمي للمستخدم الحالي");
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
                                        Log.d(TAG, "تم تحديث المعاملة: " + serverId);
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
                                        Log.d(TAG, "تم إضافة معاملة جديدة: " + serverId);
                                    }
                                }
                            }

                            // التعامل مع المعاملات المحذوفة
                            List<Long> deletedTransactionIds = (List<Long>) changes.get("deleted_transactions");
                            if (deletedTransactionIds != null && !deletedTransactionIds.isEmpty()) {
                                Log.d(TAG, "تم استلام " + deletedTransactionIds.size() + " معاملة محذوفة");
                                for (Long serverId : deletedTransactionIds) {
                                    Transaction transaction = transactionDao.getTransactionByServerIdSync(serverId);
                                    if (transaction != null && transaction.getUserId() == currentUserId) {
                                        transactionDao.delete(transaction);
                                        Log.d(TAG, "تم حذف المعاملة: " + serverId);
                                    }
                                }
                            }

                            saveLastSyncTime(System.currentTimeMillis());
                            Log.d(TAG, "تم تحديث البيانات المحلية بنجاح");
                            handler.post(() -> callback.onSuccess());
                        } catch (Exception e) {
                            Log.e(TAG, "خطأ في تحديث البيانات المحلية: " + e.getMessage());
                            handler.post(() -> callback.onError("خطأ في تحديث البيانات المحلية: " + e.getMessage()));
                        }
                    });
                } else {
                    String errorBody;
                    try {
                        errorBody = response.errorBody() != null ? 
                            response.errorBody().string() : "خطأ غير معروف";
                    } catch (IOException e) {
                        Log.e(TAG, "خطأ في قراءة رسالة الخطأ: " + e.getMessage());
                        errorBody = "خطأ في قراءة الرد";
                    }
                    final String finalErrorBody = errorBody;
                    Log.e(TAG, "فشل في جلب التغييرات: " + finalErrorBody);
                    callback.onError("فشل في جلب التغييرات: " + finalErrorBody);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                final String errorBody = t.getMessage();
                handler.post(() -> callback.onError("فشل في جلب التغييرات: " + errorBody));
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

    private String getToken() {
        return context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .getString("token", null);
    }
} 
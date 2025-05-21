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
import java.util.UUID;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.HashSet;
import java.util.Set;

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
    private static final long SYNC_INTERVAL = 15 * 60 * 1000; // 15 دقيقة
    private static final int MAX_RETRY_COUNT = 2;
    private int currentRetryCount = 0;
    private static final String SYNC_TAG = "sync_task";
    private Runnable currentSyncTask;

    // ثوابت معرفات السيرفر
    private static final long PENDING_SERVER_ID = -1;      // في انتظار المزامنة
    private static final long SYNCING_SERVER_ID = -2;      // قيد المزامنة
    private static final long FAILED_SERVER_ID = -3;       // فشلت المزامنة

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
    private static final long BATCH_DELAY = 1000; // تأخير 1 ثانية بين كل دفعة

    private Handler syncHandler = new Handler(Looper.getMainLooper());
    private Runnable periodicSyncRunnable;

    private static final String PENDING_SYNC_PREFS = "pending_sync_prefs";
    private static final String PENDING_SYNC_DATA = "pending_sync_data";
    private static final String PENDING_SYNC_TIME = "pending_sync_time";

    private boolean isSyncInProgress = false;
    private long lastSuccessfulSync = 0;

    // تحسين آلية القفل
    private final Map<String, SyncLock> syncLocks = new ConcurrentHashMap<>();
    private static final long LOCK_TIMEOUT = 5 * 60 * 1000; // 5 دقائق

    // تحسينات جديدة
    private static final int MAX_BATCH_SIZE = 50;
    private static final int MIN_BATCH_SIZE = 10;
    private static final long MAX_SYNC_DURATION = 30 * 60 * 1000; // 30 دقيقة
    private static final int MAX_CONCURRENT_SYNC = 3;
    private final Map<String, SyncSession> activeSyncSessions = new ConcurrentHashMap<>();
    private final Queue<SyncRequest> syncQueue = new ConcurrentLinkedQueue<>();
    private int currentConcurrentSync = 0;

    // إضافة ثوابت حالة المزامنة
    private static final int SYNC_STATUS_SYNCED = 1;
    private static final int SYNC_STATUS_PENDING = 0;
    private static final int SYNC_STATUS_FAILED = -1;

    private int calculateOptimalBatchSize(int totalItems) {
        // حساب حجم الدفعة الأمثل بناءً على عدد العناصر
        if (totalItems <= MIN_BATCH_SIZE) {
            return totalItems;
        }
        if (totalItems >= MAX_BATCH_SIZE * 2) {
            return MAX_BATCH_SIZE;
        }
        return Math.max(MIN_BATCH_SIZE, totalItems / 2);
    }

    private boolean canStartNewSync() {
        return currentConcurrentSync < MAX_CONCURRENT_SYNC;
    }

    private void processSyncQueue() {
        if (!canStartNewSync() || syncQueue.isEmpty()) {
            return;
        }

        SyncRequest request = syncQueue.poll();
        if (request != null) {
            currentConcurrentSync++;
            syncBatch(request.accounts, request.transactions, new SyncCallback() {
                @Override
                public void onSuccess() {
                    currentConcurrentSync--;
                    processSyncQueue();
                }

                @Override
                public void onError(String error) {
                    if (request.retryCount < MAX_RETRY_COUNT) {
                        request.retryCount++;
                        syncQueue.offer(request);
                    }
                    currentConcurrentSync--;
                    processSyncQueue();
                }
            }, 0, 1);
        }
    }

    private void validateSyncData(List<Account> accounts, List<Transaction> transactions) {
        // التحقق من تكامل البيانات
        for (Account account : accounts) {
            if (account.getServerId() < 0 && account.getBalance() < 0) {
                Log.w(TAG, "Invalid account balance for account: " + account.getId());
            }
        }

        for (Transaction transaction : transactions) {
            if (transaction.getServerId() < 0 && transaction.getAmount() == 0) {
                Log.w(TAG, "Invalid transaction amount for transaction: " + transaction.getId());
            }
        }
    }

    private void handleSyncConflict(Account localAccount, Account serverAccount) {
        // تعديل طريقة التحقق من التعارضات باستخدام lastSyncTime
        if (localAccount.getLastSyncTime() > serverAccount.getLastSyncTime()) {
            // البيانات المحلية أحدث
            return;
        }
        // تحديث البيانات المحلية بالبيانات من السيرفر
        localAccount.setBalance(serverAccount.getBalance());
        localAccount.setLastSyncTime(serverAccount.getLastSyncTime());
        accountDao.update(localAccount);
    }

    private void syncBatch(List<Account> accounts, List<Transaction> transactions, 
                         SyncCallback callback, int batchIndex, int totalBatches) {
        if (accounts.isEmpty() && transactions.isEmpty()) {
            if (batchIndex >= totalBatches - 1) {
                handler.post(() -> callback.onSuccess());
            }
            return;
        }

        // التحقق من تكامل البيانات
        validateSyncData(accounts, transactions);

        // محاولة الحصول على التوكن
        String token = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .getString("token", null);

        // إذا لم يكن هناك توكن، نحاول تجديده
        if (token == null) {
            String username = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                    .getString("username", null);
            String password = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                    .getString("password", null);

            if (username != null && password != null) {
                // محاولة تسجيل الدخول تلقائياً
                apiService.login(username, password).enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Map<String, Object> loginResponse = response.body();
                            String newToken = (String) loginResponse.get("token");
                            if (newToken != null) {
                                // حفظ التوكن الجديد
                                context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                                        .edit()
                                        .putString("token", newToken)
                                        .apply();
                                
                                // متابعة المزامنة مع التوكن الجديد
                                syncBatch(accounts, transactions, callback, batchIndex, totalBatches);
                                return;
                            }
                        }
                        // إذا فشل تجديد التوكن
                        handler.post(() -> callback.onError("فشل تجديد جلسة الدخول، يرجى تسجيل الدخول مرة أخرى"));
                    }

                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                        handler.post(() -> callback.onError("فشل الاتصال بالسيرفر، يرجى المحاولة مرة أخرى"));
                    }
                });
                return;
            } else {
                handler.post(() -> callback.onError("يرجى تسجيل الدخول أولاً"));
                return;
            }
        }

        // إنشاء معرف جلسة فريد
        String sessionId = UUID.randomUUID().toString();
        SyncSession session = new SyncSession();
        activeSyncSessions.put(sessionId, session);

        // التحقق من القفل لكل عنصر
        List<Account> unlockedAccounts = new ArrayList<>();
        List<Transaction> unlockedTransactions = new ArrayList<>();

        for (Account account : accounts) {
            String itemKey = getItemKey(account);
            if (acquireLock(itemKey, sessionId)) {
                unlockedAccounts.add(account);
            } else {
                Log.d(TAG, "Account is locked: " + account.getId());
            }
        }

        for (Transaction transaction : transactions) {
            String itemKey = getItemKey(transaction);
            if (acquireLock(itemKey, sessionId)) {
                unlockedTransactions.add(transaction);
            } else {
                Log.d(TAG, "Transaction is locked: " + transaction.getId());
            }
        }

        if (unlockedAccounts.isEmpty() && unlockedTransactions.isEmpty()) {
            handler.post(() -> callback.onError("جميع العناصر قيد المزامنة حالياً"));
            return;
        }

        // إنشاء طلب المزامنة للدفعة الحالية
        ApiService.SyncRequest syncRequest = new ApiService.SyncRequest(unlockedAccounts, unlockedTransactions);
        
        executor.execute(() -> {
            try {
                Response<ApiService.SyncResponse> response = apiService.syncData("Bearer " + token, syncRequest).execute();
                
                if (response.isSuccessful()) {
                    ApiService.SyncResponse syncResponse = response.body();
                    if (syncResponse != null) {
                        // تحديث معرفات السيرفر للحسابات
                        for (Account account : unlockedAccounts) {
                            String itemKey = getItemKey(account);
                            if (!session.isItemProcessed(itemKey)) {
                                Long serverId = syncResponse.getAccountServerId(account.getId());
                                if (serverId != null) {
                                    // تحديث معرف السيرفر في قاعدة البيانات المحلية
                                    try {
                                        // تحديث في قاعدة البيانات المحلية
                                        account.setServerId(serverId);
                                        account.setLastSyncTime(System.currentTimeMillis());
                                        account.setSyncStatus(SYNC_STATUS_SYNCED);
                                        accountDao.update(account);

                                        // التحقق من نجاح التحديث
                                        Account updatedAccount = accountDao.getAccountById(account.getId());
                                        if (updatedAccount != null && updatedAccount.getServerId() == serverId) {
                                            Log.d(TAG, "Successfully updated account server_id: " + serverId);
                                            releaseLock(itemKey);
                                            session.addProcessedItem(itemKey);
                                        } else {
                                            Log.e(TAG, "Failed to update account server_id in local database");
                                            // إعادة المحاولة
                                            account.setServerId(-1);
                                            account.setSyncStatus(SYNC_STATUS_FAILED);
                                            accountDao.update(account);
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error updating account server_id: " + e.getMessage());
                                        // إعادة المحاولة
                                        account.setServerId(-1);
                                        account.setSyncStatus(SYNC_STATUS_FAILED);
                                        accountDao.update(account);
                                    }
                                }
                            }
                        }
                        
                        // تحديث معرفات السيرفر للمعاملات
                        for (Transaction transaction : unlockedTransactions) {
                            String itemKey = getItemKey(transaction);
                            if (!session.isItemProcessed(itemKey)) {
                                Long serverId = syncResponse.getTransactionServerId(transaction.getId());
                                if (serverId != null) {
                                    // تحديث معرف السيرفر في قاعدة البيانات المحلية
                                    try {
                                        // تحديث في قاعدة البيانات المحلية
                                        transaction.setServerId(serverId);
                                        transaction.setLastSyncTime(System.currentTimeMillis());
                                        transaction.setSyncStatus(SYNC_STATUS_SYNCED);
                                        transactionDao.update(transaction);

                                        // التحقق من نجاح التحديث
                                        Transaction updatedTransaction = transactionDao.getTransactionById(transaction.getId());
                                        if (updatedTransaction != null && updatedTransaction.getServerId() == serverId) {
                                            Log.d(TAG, "Successfully updated transaction server_id: " + serverId);
                                            releaseLock(itemKey);
                                            session.addProcessedItem(itemKey);
                                        } else {
                                            Log.e(TAG, "Failed to update transaction server_id in local database");
                                            // إعادة المحاولة
                                            transaction.setServerId(-1);
                                            transaction.setSyncStatus(SYNC_STATUS_FAILED);
                                            transactionDao.update(transaction);
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error updating transaction server_id: " + e.getMessage());
                                        // إعادة المحاولة
                                        transaction.setServerId(-1);
                                        transaction.setSyncStatus(SYNC_STATUS_FAILED);
                                        transactionDao.update(transaction);
                                    }
                                }
                            }
                        }
                    }

                    // جدولة الدفعة التالية
                    if (batchIndex < totalBatches - 1) {
                        handler.postDelayed(() -> {
                            List<Account> nextAccounts = getNextBatch(accountDao.getNewAccounts(), batchIndex + 1);
                            List<Transaction> nextTransactions = getNextBatch(transactionDao.getNewTransactions(), batchIndex + 1);
                            
                            syncBatch(nextAccounts, nextTransactions, callback, batchIndex + 1, totalBatches);
                        }, BATCH_DELAY);
                    } else {
                        // انتهت جميع الدفعات بنجاح
                        updateLastSyncTime();
                        activeSyncSessions.remove(sessionId);
                        handler.post(() -> callback.onSuccess());
                    }
                } else {
                    // في حالة الفشل، نحفظ الدفعة للتحميل لاحقاً
                    Map<String, Object> batchData = new HashMap<>();
                    batchData.put("accounts", unlockedAccounts);
                    batchData.put("transactions", unlockedTransactions);
                    savePendingSync(batchData);
                    
                    String errorBody = response.errorBody() != null ? response.errorBody().string() : "خطأ غير معروف";
                    Log.e(TAG, "فشلت مزامنة الدفعة " + (batchIndex + 1) + ": " + errorBody);
                    
                    // نتابع مع الدفعة التالية
                    if (batchIndex < totalBatches - 1) {
                        handler.postDelayed(() -> {
                            List<Account> nextAccounts = getNextBatch(accountDao.getNewAccounts(), batchIndex + 1);
                            List<Transaction> nextTransactions = getNextBatch(transactionDao.getNewTransactions(), batchIndex + 1);
                            
                            syncBatch(nextAccounts, nextTransactions, callback, batchIndex + 1, totalBatches);
                        }, BATCH_DELAY);
                    } else {
                        activeSyncSessions.remove(sessionId);
                        handler.post(() -> callback.onError("فشلت مزامنة بعض الدفعات، سيتم إعادة المحاولة لاحقاً"));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "خطأ في مزامنة الدفعة " + (batchIndex + 1) + ": " + e.getMessage());
                
                // نحفظ الدفعة للتحميل لاحقاً
                Map<String, Object> batchData = new HashMap<>();
                batchData.put("accounts", unlockedAccounts);
                batchData.put("transactions", unlockedTransactions);
                savePendingSync(batchData);
                
                // نتابع مع الدفعة التالية
                if (batchIndex < totalBatches - 1) {
                    handler.postDelayed(() -> {
                        List<Account> nextAccounts = getNextBatch(accountDao.getNewAccounts(), batchIndex + 1);
                        List<Transaction> nextTransactions = getNextBatch(transactionDao.getNewTransactions(), batchIndex + 1);
                        
                        syncBatch(nextAccounts, nextTransactions, callback, batchIndex + 1, totalBatches);
                    }, BATCH_DELAY);
                } else {
                    activeSyncSessions.remove(sessionId);
                    handler.post(() -> callback.onError("فشلت مزامنة بعض الدفعات، سيتم إعادة المحاولة لاحقاً"));
                }
            }
        });
    }

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
        if (!isAutoSyncEnabled || isSyncInProgress) {
            Log.d(TAG, "Auto sync is disabled or already in progress");
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSuccessfulSync < SYNC_INTERVAL) {
            Log.d(TAG, "Skipping sync - too soon since last successful sync");
            return;
        }

        isSyncInProgress = true;
        Log.d(TAG, "Starting auto sync...");

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

        // تحديث حالة المزامنة عند الانتهاء
        isSyncInProgress = false;
        lastSuccessfulSync = System.currentTimeMillis();
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

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("خطأ المزامنة", text);
        clipboard.setPrimaryClip(clip);
        handler.post(() -> Toast.makeText(context, "تم نسخ رسالة الخطأ إلى الحافظة", Toast.LENGTH_SHORT).show());
    }

    private <T> List<T> getNextBatch(List<T> items, int batchIndex) {
        int startIndex = batchIndex * BATCH_SIZE;
        int endIndex = Math.min(startIndex + BATCH_SIZE, items.size());
        
        if (startIndex >= items.size()) {
            return new ArrayList<>();
        }
        
        return items.subList(startIndex, endIndex);
    }

    public void syncData(SyncCallback callback) {
        if (isSyncInProgress) {
            Log.d(TAG, "Sync already in progress, skipping");
            if (callback != null) {
                callback.onError("Sync already in progress");
            }
            return;
        }

        isSyncInProgress = true;

        if (!isNetworkAvailable()) {
            handleOfflineSync(callback);
            return;
        }

        executor.execute(() -> {
            try {
                // الحصول على جميع العناصر غير المتزامنة
                List<Account> allNewAccounts = accountDao.getNewAccounts();
                List<Transaction> allNewTransactions = transactionDao.getNewTransactions();

                // حساب عدد الدفعات المطلوبة
                int totalItems = allNewAccounts.size() + allNewTransactions.size();
                int totalBatches = (int) Math.ceil((double) totalItems / BATCH_SIZE);

                if (totalItems == 0) {
                    Log.d(TAG, "لا توجد بيانات جديدة للمزامنة");
                    isSyncInProgress = false;
                    handler.post(() -> callback.onSuccess());
                    return;
                }

                // بدء المزامنة مع الدفعة الأولى
                List<Account> firstBatchAccounts = getNextBatch(allNewAccounts, 0);
                List<Transaction> firstBatchTransactions = getNextBatch(allNewTransactions, 0);

                syncBatch(firstBatchAccounts, firstBatchTransactions, callback, 0, totalBatches);

            } catch (Exception e) {
                Log.e(TAG, "خطأ في بدء المزامنة: " + e.getMessage());
                isSyncInProgress = false;
                handler.post(() -> callback.onError("خطأ في بدء المزامنة: " + e.getMessage()));
            }
        });
    }

    private void handleOfflineSync(SyncCallback callback) {
        if (!isNetworkAvailable()) {
            long currentTime = System.currentTimeMillis();
            
            if (offlineRetryCount >= MAX_RETRY_COUNT) {
                if (currentTime - lastOfflineRetryTime > OFFLINE_RETRY_INTERVAL) {
                    offlineRetryCount = 0;
                } else {
                    callback.onError("لا يوجد اتصال بالإنترنت. سيتم إعادة المحاولة تلقائياً عند توفر الاتصال.");
                    return;
                }
            }

            executor.execute(() -> {
                try {
                    // حفظ البيانات الحالية للتحميل لاحقاً
                    List<Account> newAccounts = accountDao.getNewAccounts();
                    List<Account> modifiedAccounts = accountDao.getModifiedAccounts(lastSyncTime);
                    List<Transaction> newTransactions = transactionDao.getNewTransactions();
                    List<Transaction> modifiedTransactions = transactionDao.getModifiedTransactions(lastSyncTime);

                    Map<String, Object> pendingData = new HashMap<>();
                    pendingData.put("new_accounts", newAccounts);
                    pendingData.put("modified_accounts", modifiedAccounts);
                    pendingData.put("new_transactions", newTransactions);
                    pendingData.put("modified_transactions", modifiedTransactions);

                    savePendingSync(pendingData);

                    lastOfflineRetryTime = currentTime;
                    offlineRetryCount++;

                    // جدولة إعادة المحاولة
                    handler.postDelayed(() -> {
                        if (isNetworkAvailable()) {
                            retryPendingSync();
                        }
                    }, OFFLINE_RETRY_INTERVAL);

                } catch (Exception e) {
                    Log.e(TAG, "Error in offline sync: " + e.getMessage());
                    handler.post(() -> callback.onError("خطأ في المزامنة في وضع عدم الاتصال"));
                }
            });
        }
    }

    private <T> List<T> filterSyncingItems(List<T> items) {
        return items.stream()
                .filter(item -> {
                    if (item instanceof Account) {
                        return ((Account) item).getServerId() < 0;
                    } else if (item instanceof Transaction) {
                        return ((Transaction) item).getServerId() < 0;
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

    private boolean acquireLock(String itemKey, String sessionId) {
        SyncLock existingLock = syncLocks.get(itemKey);
        if (existingLock != null) {
            if (existingLock.isExpired()) {
                syncLocks.remove(itemKey);
            } else {
                return false; // العنصر مقفل حالياً
            }
        }
        syncLocks.put(itemKey, new SyncLock(sessionId));
        return true;
    }

    private void releaseLock(String itemKey) {
        syncLocks.remove(itemKey);
    }

    private boolean isItemLocked(String itemKey) {
        SyncLock lock = syncLocks.get(itemKey);
        if (lock == null) return false;
        if (lock.isExpired()) {
            syncLocks.remove(itemKey);
            return false;
        }
        return true;
    }

    private <T> List<T> filterLockedItems(List<T> items) {
        return items.stream()
                .filter(item -> !isItemLocked(getItemKey(item)))
                .collect(Collectors.toList());
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
        try {
            SharedPreferences prefs = context.getSharedPreferences(PENDING_SYNC_PREFS, Context.MODE_PRIVATE);
            String changesJson = new Gson().toJson(changes);
            prefs.edit()
                .putString(PENDING_SYNC_DATA, changesJson)
                .putLong(PENDING_SYNC_TIME, System.currentTimeMillis())
                .apply();
            Log.d(TAG, "تم حفظ بيانات المزامنة الفاشلة للتحميل لاحقاً");
        } catch (Exception e) {
            Log.e(TAG, "Error saving pending sync: " + e.getMessage());
        }
    }

    private void retryPendingSync() {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PENDING_SYNC_PREFS, Context.MODE_PRIVATE);
            String pendingData = prefs.getString(PENDING_SYNC_DATA, null);
            long pendingTime = prefs.getLong(PENDING_SYNC_TIME, 0);

            if (pendingData != null && pendingTime > 0) {
                Log.d(TAG, "تم العثور على مزامنة فاشلة من: " + new Date(pendingTime));
                
                Map<String, Object> changes = new Gson().fromJson(pendingData, Map.class);
                if (changes != null && !changes.isEmpty()) {
                    syncData(new SyncCallback() {
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
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in retry pending sync: " + e.getMessage());
            clearPendingSync();
        }
    }

    private void clearPendingSync() {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PENDING_SYNC_PREFS, Context.MODE_PRIVATE);
            prefs.edit().clear().apply();
            Log.d(TAG, "تم مسح بيانات المزامنة الفاشلة");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing pending sync: " + e.getMessage());
        }
    }

    // إضافة تعريفات الفئات المفقودة
    private static class SyncSession {
        private final Set<String> processedItems = new HashSet<>();
        private int retryCount = 0;

        public void addProcessedItem(String itemKey) {
            processedItems.add(itemKey);
        }

        public boolean isItemProcessed(String itemKey) {
            return processedItems.contains(itemKey);
        }
    }

    private static class SyncRequest {
        private final List<Account> accounts;
        private final List<Transaction> transactions;
        private int retryCount = 0;

        public SyncRequest(List<Account> accounts, List<Transaction> transactions) {
            this.accounts = accounts;
            this.transactions = transactions;
        }
    }

    private static class SyncLock {
        private final long lockTime;
        private final String sessionId;
        private int retryCount;

        public SyncLock(String sessionId) {
            this.lockTime = System.currentTimeMillis();
            this.sessionId = sessionId;
            this.retryCount = 0;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - lockTime > LOCK_TIMEOUT;
        }
    }
} 
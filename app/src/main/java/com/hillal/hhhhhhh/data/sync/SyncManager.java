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
import com.hillal.hhhhhhh.data.room.AppDatabase;
import com.hillal.hhhhhhh.App;

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
    private static final int SYNC_INTERVAL = 30 * 1000; // 30 ثانية
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

    private final AppDatabase database;

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

        this.database = ((App) context.getApplicationContext()).getDatabase();
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

                // جدولة محاولة إعادة المزامنة
                handler.postDelayed(() -> {
                    if (isNetworkAvailable()) {
                        syncData(callback);
                    } else {
                        callback.onError("لا يوجد اتصال بالإنترنت. سيتم إعادة المحاولة تلقائياً عند توفر الاتصال.");
                    }
                }, OFFLINE_RETRY_INTERVAL);

            } catch (Exception e) {
                Log.e(TAG, "خطأ في معالجة المزامنة بدون إنترنت: " + e.getMessage());
                callback.onError("حدث خطأ أثناء محاولة المزامنة بدون إنترنت");
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

    private Runnable syncRunnable = new Runnable() {
        @Override
        public void run() {
            if (isNetworkAvailable()) {
                Log.d(TAG, "Executing periodic sync...");
                receiveChanges(new SyncCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Periodic sync successful");
                        handler.postDelayed(syncRunnable, SYNC_INTERVAL);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Periodic sync failed: " + error);
                        handler.postDelayed(syncRunnable, SYNC_INTERVAL);
                    }
                });
            } else {
                Log.d(TAG, "No network available, retrying in 30 seconds...");
                handler.postDelayed(syncRunnable, SYNC_INTERVAL);
            }
        }
    };

    public void startPeriodicSync() {
        Log.d(TAG, "Starting periodic sync...");
        handler.postDelayed(syncRunnable, SYNC_INTERVAL);
    }

    public void stopPeriodicSync() {
        Log.d(TAG, "Stopping periodic sync...");
        handler.removeCallbacks(syncRunnable);
    }

    public void syncChanges(SyncCallback callback) {
        if (!isNetworkAvailable()) {
            callback.onError("No internet connection");
            return;
        }

        // Get local changes
        List<Transaction> localTransactions = database.transactionDao().getAllTransactionsSync();
        List<Account> localAccounts = database.accountDao().getAllAccountsSync();
        List<User> localUsers = database.userDao().getAllUsers();

        // ... rest of the code ...
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

        long lastSyncTime = getLastSyncTime();
        Log.d(TAG, "Fetching changes since: " + lastSyncTime);

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
                                    Account account = new Account();
                                    account.setServerId(((Number) accountData.get("id")).longValue());
                                    account.setName((String) accountData.get("name"));
                                    account.setPhoneNumber((String) accountData.get("phone_number"));
                                    account.setBalance(((Number) accountData.get("balance")).doubleValue());
                                    account.setCurrency((String) accountData.get("currency"));
                                    account.setNotes((String) accountData.get("notes"));
                                    account.setWhatsappEnabled((Boolean) accountData.get("whatsapp_enabled"));
                                    account.setLastSyncTime(System.currentTimeMillis());
                                    account.setSyncStatus(2); // SYNCED
                                    database.accountDao().insert(account);
                                }
                            }

                            // تحديث المعاملات
                            List<Map<String, Object>> transactions = (List<Map<String, Object>>) changes.get("transactions");
                            if (transactions != null && !transactions.isEmpty()) {
                                Log.d(TAG, "Received " + transactions.size() + " transaction changes");
                                for (Map<String, Object> transactionData : transactions) {
                                    Transaction transaction = new Transaction();
                                    transaction.setServerId(((Number) transactionData.get("id")).longValue());
                                    transaction.setAccountId(((Number) transactionData.get("account_id")).longValue());
                                    transaction.setAmount(((Number) transactionData.get("amount")).doubleValue());
                                    transaction.setType((String) transactionData.get("type"));
                                    transaction.setDescription((String) transactionData.get("description"));
                                    transaction.setCurrency((String) transactionData.get("currency"));
                                    transaction.setDate(((Number) transactionData.get("date")).longValue());
                                    transaction.setNotes((String) transactionData.get("notes"));
                                    transaction.setWhatsappEnabled((Boolean) transactionData.get("whatsapp_enabled"));
                                    transaction.setLastSyncTime(System.currentTimeMillis());
                                    transaction.setSyncStatus(2); // SYNCED
                                    database.transactionDao().insert(transaction);
                                }
                            }

                            saveLastSyncTime(System.currentTimeMillis());
                            handler.post(() -> callback.onSuccess());
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating local data: " + e.getMessage());
                            handler.post(() -> callback.onError("Error updating local data: " + e.getMessage()));
                        }
                    });
                } else {
                    String errorBody = "Unknown error";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading error body: " + e.getMessage());
                    }
                    Log.e(TAG, "Failed to receive changes: " + errorBody);
                    callback.onError("Failed to receive changes: " + errorBody);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage());
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    private void saveLastSyncTime(long time) {
        SharedPreferences prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE);
        prefs.edit().putLong("last_sync_time", time).apply();
    }
} 
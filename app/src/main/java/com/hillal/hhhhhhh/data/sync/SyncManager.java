package com.hillal.hhhhhhh.data.sync;

import android.content.Context;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.widget.Toast;

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
import org.json.JSONArray;
import org.json.JSONObject;

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

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("خطأ المزامنة", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, "تم نسخ رسالة الخطأ إلى الحافظة", Toast.LENGTH_SHORT).show();
    }

    public void syncData(SyncCallback callback) {
        new Thread(() -> {
            try {
                // جلب التوكن
                String token = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                        .getString("token", null);
                
                if (token == null) {
                    String error = "يرجى تسجيل الدخول أولاً";
                    copyToClipboard(error);
                    callback.onError(error);
                    return;
                }

                // جلب البيانات الجديدة فقط
                List<Account> newAccounts = accountDao.getAccountsModifiedAfter(lastSyncTime);
                List<Transaction> newTransactions = transactionDao.getTransactionsModifiedAfter(lastSyncTime);
                
                if (newAccounts.isEmpty() && newTransactions.isEmpty()) {
                    Log.d(TAG, "No new data to sync");
                    callback.onSuccess();
                    return;
                }

                String syncDetails = String.format("جاري مزامنة %d حساب و %d معاملة", 
                    newAccounts.size(), newTransactions.size());
                Log.d(TAG, syncDetails);

                // تحويل البيانات إلى JSON
                JSONObject syncData = new JSONObject();
                JSONArray accountsArray = new JSONArray();
                JSONArray transactionsArray = new JSONArray();

                // تحويل الحسابات
                for (Account account : newAccounts) {
                    JSONObject accountJson = new JSONObject();
                    accountJson.put("account_number", account.getAccountNumber());
                    accountJson.put("account_name", account.getName());
                    accountJson.put("balance", account.getBalance());
                    accountsArray.put(accountJson);
                }

                // تحويل المعاملات
                for (Transaction transaction : newTransactions) {
                    JSONObject transactionJson = new JSONObject();
                    transactionJson.put("id", transaction.getId());
                    String dateStr = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                        .format(new java.util.Date(transaction.getDate()));
                    transactionJson.put("date", dateStr);
                    transactionJson.put("amount", transaction.getAmount());
                    transactionJson.put("description", transaction.getDescription());
                    transactionJson.put("account_id", transaction.getAccountId());
                    transactionsArray.put(transactionJson);
                }

                syncData.put("accounts", accountsArray);
                syncData.put("transactions", transactionsArray);

                Log.d(TAG, "Sync data: " + syncData.toString());

                // إرسال البيانات الجديدة إلى السيرفر
                ApiService.SyncRequest syncRequest = new ApiService.SyncRequest(newAccounts, newTransactions);
                apiService.syncData("Bearer " + token, syncRequest)
                    .enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                updateLastSyncTime();
                                String successMessage = String.format("تمت مزامنة %d حساب و %d معاملة بنجاح", 
                                    newAccounts.size(), newTransactions.size());
                                callback.onSuccess();
                            } else {
                                String errorMessage;
                                try {
                                    if (response.errorBody() != null) {
                                        String errorBody = response.errorBody().string();
                                        Log.d(TAG, "Error response body: " + errorBody);
                                        // محاولة تحليل رسالة الخطأ من JSON
                                        if (errorBody.contains("error")) {
                                            String rawError = errorBody.split("\"error\":\"")[1].split("\"")[0];
                                            // تحويل النص من Unicode إلى نص عربي
                                            errorMessage = java.net.URLDecoder.decode(rawError, "UTF-8");
                                        } else {
                                            errorMessage = String.format("فشل المزامنة (رمز الخطأ: %d)", response.code());
                                        }
                                    } else {
                                        errorMessage = String.format("فشل المزامنة (رمز الخطأ: %d)", response.code());
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing error response", e);
                                    errorMessage = String.format("فشل المزامنة (رمز الخطأ: %d)", response.code());
                                }
                                Log.e(TAG, "Sync failed: " + errorMessage);
                                copyToClipboard(errorMessage);
                                callback.onError(errorMessage);
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Log.e(TAG, "Sync error: " + t.getMessage());
                            String errorMessage;
                            if (t instanceof java.net.UnknownHostException) {
                                errorMessage = "لا يمكن الوصول إلى الخادم. يرجى التحقق من اتصال الإنترنت";
                            } else if (t instanceof java.net.SocketTimeoutException) {
                                errorMessage = "انتهت مهلة الاتصال بالخادم. يرجى المحاولة مرة أخرى";
                            } else {
                                errorMessage = "خطأ في الاتصال: " + t.getMessage();
                            }
                            copyToClipboard(errorMessage);
                            callback.onError(errorMessage);
                        }
                    });
            } catch (Exception e) {
                Log.e(TAG, "Error during sync: " + e.getMessage());
                String errorMessage = "خطأ في المزامنة: " + e.getMessage();
                copyToClipboard(errorMessage);
                callback.onError(errorMessage);
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
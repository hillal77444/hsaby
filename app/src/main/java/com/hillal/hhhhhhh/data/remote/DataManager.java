package com.hillal.hhhhhhh.data.remote;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.model.Transaction;
import com.hillal.hhhhhhh.data.room.AccountDao;
import com.hillal.hhhhhhh.data.room.TransactionDao;
import com.google.gson.Gson;
import com.hillal.hhhhhhh.data.model.PendingOperation;
import com.hillal.hhhhhhh.data.room.PendingOperationDao;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkCapabilities;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class DataManager {
    private static final String TAG = "DataManager";
    private final Context context;
    private final ApiService apiService;
    private final AccountDao accountDao;
    private final TransactionDao transactionDao;
    private final PendingOperationDao pendingOperationDao;
    private final Handler handler;
    private final ExecutorService executor;
    private final Gson gson;
    private static final long TOKEN_REFRESH_THRESHOLD = 3600000; // ساعة واحدة قبل انتهاء الصلاحية
    private static final String TOKEN_EXPIRY_KEY = "token_expiry_time";
    private static final long INVALID_SERVER_ID = 0;
    private static final long NEW_TRANSACTION_SERVER_ID = -System.currentTimeMillis();
    private static final int MAX_SYNC_ATTEMPTS = 3;
    private static final long SYNC_TIMEOUT = 5 * 60 * 1000; // 5 دقائق
    private final Map<Long, Integer> syncAttempts = new ConcurrentHashMap<>();
    private final Map<Long, Long> lastSyncTimes = new ConcurrentHashMap<>();

    public DataManager(Context context, AccountDao accountDao, TransactionDao transactionDao, PendingOperationDao pendingOperationDao) {
        this.context = context;
        this.apiService = RetrofitClient.getInstance().getApiService();
        this.accountDao = accountDao;
        this.transactionDao = transactionDao;
        this.pendingOperationDao = pendingOperationDao;
        this.handler = new Handler(Looper.getMainLooper());
        this.executor = Executors.newSingleThreadExecutor();
        this.gson = new Gson();
    }

    public interface DataCallback {
        void onSuccess();
        void onError(String error);
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

    private String getAuthToken() {
        return context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .getString("token", null);
    }

    private void checkAndRefreshToken(DataCallback callback) {
        if (!isNetworkAvailable()) {
            callback.onSuccess();
            return;
        }

        String currentToken = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .getString("token", null);
        
        if (currentToken == null) {
            callback.onError("لا يوجد توكن للتجديد");
            return;
        }

        // إزالة "Bearer " إذا كان موجوداً
        if (currentToken.startsWith("Bearer ")) {
            currentToken = currentToken.substring(7);
        }

        // محاولة تجديد التوكن مباشرة
        apiService.refreshToken("Bearer " + currentToken).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String newToken = response.body().get("token");
                    if (newToken == null) {
                        Log.e(TAG, "Token is null in response");
                        callback.onError("Token is null in response");
                        return;
                    }
                    
                    long newExpiryTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000); // 24 ساعة
                    
                    // حفظ التوكن الجديد ووقت انتهاء الصلاحية
                    context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                            .edit()
                            .putString("token", newToken)
                            .putLong(TOKEN_EXPIRY_KEY, newExpiryTime)
                            .apply();
                    
                    Log.d(TAG, "تم تجديد التوكن بنجاح");
                    callback.onSuccess();
                } else {
                    String errorMessage = "Unknown error";
                    try {
                        if (response.errorBody() != null) {
                            errorMessage = response.errorBody().string();
                        }
                    } catch (IOException e) {
                        errorMessage = "Error reading response";
                    }
                    Log.e(TAG, "فشل في تجديد التوكن: " + errorMessage + ", Response code: " + response.code());
                    callback.onError("فشل في تجديد التوكن: " + errorMessage);
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Log.e(TAG, "خطأ في تجديد التوكن: " + t.getMessage());
                callback.onError("خطأ في تجديد التوكن: " + t.getMessage());
            }
        });
    }

    public void fetchDataFromServer(DataCallback callback) {
        if (!isNetworkAvailable()) {
            callback.onError("لا يوجد اتصال بالإنترنت");
            return;
        }

        String token = getAuthToken();
        if (token == null) {
            callback.onError("يرجى تسجيل الدخول أولاً");
            return;
        }

        executor.execute(() -> {
            try {
                // جلب الحسابات
                apiService.getAccounts("Bearer " + token).enqueue(new Callback<List<Account>>() {
                    @Override
                    public void onResponse(Call<List<Account>> call, Response<List<Account>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Account> accounts = response.body();
                            // حفظ الحسابات في قاعدة البيانات المحلية
                            executor.execute(() -> {
                                for (Account account : accounts) {
                                    accountDao.insert(account);
                                }
                                // بعد حفظ الحسابات، نبدأ بجلب المعاملات
                                fetchTransactions(token, callback);
                            });
                        } else {
                            handler.post(() -> callback.onError("فشل جلب الحسابات"));
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Account>> call, Throwable t) {
                        handler.post(() -> callback.onError("خطأ في الاتصال: " + t.getMessage()));
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error in fetchDataFromServer: " + e.getMessage());
                handler.post(() -> callback.onError("خطأ في جلب البيانات: " + e.getMessage()));
            }
        });
    }

    private boolean isValidServerId(Long serverId) {
        return serverId != null && serverId != INVALID_SERVER_ID;
    }

    private boolean canSyncTransaction(Transaction transaction) {
        if (transaction == null) return false;
        
        Long serverId = transaction.getServerId();
        if (!isValidServerId(serverId)) {
            // تعيين server_id سالب للمعاملة الجديدة
            transaction.setServerId(NEW_TRANSACTION_SERVER_ID);
            transactionDao.update(transaction);
            return true;
        }

        // التحقق من عدد محاولات المزامنة
        int attempts = syncAttempts.getOrDefault(serverId, 0);
        if (attempts >= MAX_SYNC_ATTEMPTS) {
            Log.w(TAG, "تم تجاوز الحد الأقصى لمحاولات المزامنة للمعاملة: " + serverId);
            return false;
        }

        // التحقق من وقت آخر مزامنة
        long lastSync = lastSyncTimes.getOrDefault(serverId, 0L);
        if (System.currentTimeMillis() - lastSync < SYNC_TIMEOUT) {
            Log.w(TAG, "المعاملة قيد المزامنة حالياً: " + serverId);
            return false;
        }

        return true;
    }

    private void fetchTransactions(String token, DataCallback callback) {
        apiService.getTransactions("Bearer " + token).enqueue(new Callback<List<Transaction>>() {
            @Override
            public void onResponse(Call<List<Transaction>> call, Response<List<Transaction>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Transaction> transactions = response.body();
                    // حفظ المعاملات في قاعدة البيانات المحلية
                    executor.execute(() -> {
                        for (Transaction transaction : transactions) {
                            transactionDao.insert(transaction);
                        }
                        handler.post(() -> callback.onSuccess());
                    });
                } else {
                    handler.post(() -> callback.onError("فشل جلب المعاملات"));
                }
            }

            @Override
            public void onFailure(Call<List<Transaction>> call, Throwable t) {
                handler.post(() -> callback.onError("خطأ في الاتصال: " + t.getMessage()));
            }
        });
    }

    public void updateAccount(Account account, DataCallback callback) {
        if (!isNetworkAvailable()) {
            // حفظ التغييرات محلياً فقط
            executor.execute(() -> {
                accountDao.update(account);
                handler.post(() -> callback.onSuccess());
            });
            return;
        }

        String token = getAuthToken();
        if (token == null) {
            callback.onError("يرجى تسجيل الدخول أولاً");
            return;
        }

        apiService.updateAccount("Bearer " + token, account).enqueue(new Callback<Account>() {
            @Override
            public void onResponse(Call<Account> call, Response<Account> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Account updatedAccount = response.body();
                    executor.execute(() -> {
                        accountDao.update(updatedAccount);
                        handler.post(() -> callback.onSuccess());
                    });
                } else {
                    // حفظ التغييرات محلياً فقط
                    executor.execute(() -> {
                        accountDao.update(account);
                        handler.post(() -> callback.onSuccess());
                    });
                }
            }

            @Override
            public void onFailure(Call<Account> call, Throwable t) {
                // حفظ التغييرات محلياً فقط
                executor.execute(() -> {
                    accountDao.update(account);
                    handler.post(() -> callback.onSuccess());
                });
            }
        });
    }

    public void updateTransaction(Transaction transaction, DataCallback callback) {
        if (!isNetworkAvailable()) {
            // حفظ التغييرات محلياً فقط
            executor.execute(() -> {
                transactionDao.update(transaction);
                handler.post(() -> callback.onSuccess());
            });
            return;
        }

        String token = getAuthToken();
        if (token == null) {
            callback.onError("يرجى تسجيل الدخول أولاً");
            return;
        }

        apiService.updateTransaction("Bearer " + token, transaction).enqueue(new Callback<Transaction>() {
            @Override
            public void onResponse(Call<Transaction> call, Response<Transaction> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Transaction updatedTransaction = response.body();
                    executor.execute(() -> {
                        transactionDao.update(updatedTransaction);
                        handler.post(() -> callback.onSuccess());
                    });
                } else {
                    // حفظ التغييرات محلياً فقط
                    executor.execute(() -> {
                        transactionDao.update(transaction);
                        handler.post(() -> callback.onSuccess());
                    });
                }
            }

            @Override
            public void onFailure(Call<Transaction> call, Throwable t) {
                // حفظ التغييرات محلياً فقط
                executor.execute(() -> {
                    transactionDao.update(transaction);
                    handler.post(() -> callback.onSuccess());
                });
            }
        });
    }

    public void deleteAccount(Account account, DataCallback callback) {
        if (!isNetworkAvailable()) {
            // حفظ التغييرات محلياً فقط
            executor.execute(() -> {
                accountDao.delete(account);
                handler.post(() -> callback.onSuccess());
            });
            return;
        }

        String token = getAuthToken();
        if (token == null) {
            callback.onError("يرجى تسجيل الدخول أولاً");
            return;
        }

        apiService.deleteAccount("Bearer " + token, account.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    executor.execute(() -> {
                        accountDao.delete(account);
                        handler.post(() -> callback.onSuccess());
                    });
                } else {
                    // حفظ التغييرات محلياً فقط
                    executor.execute(() -> {
                        accountDao.delete(account);
                        handler.post(() -> callback.onSuccess());
                    });
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // حفظ التغييرات محلياً فقط
                executor.execute(() -> {
                    accountDao.delete(account);
                    handler.post(() -> callback.onSuccess());
                });
            }
        });
    }

    public void deleteTransaction(Transaction transaction, DataCallback callback) {
        if (!isNetworkAvailable()) {
            // حفظ التغييرات محلياً فقط
            executor.execute(() -> {
                transactionDao.delete(transaction);
                handler.post(() -> callback.onSuccess());
            });
            return;
        }

        String token = getAuthToken();
        if (token == null) {
            callback.onError("يرجى تسجيل الدخول أولاً");
            return;
        }

        apiService.deleteTransaction("Bearer " + token, transaction.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    executor.execute(() -> {
                        transactionDao.delete(transaction);
                        handler.post(() -> callback.onSuccess());
                    });
                } else {
                    // حفظ التغييرات محلياً فقط
                    executor.execute(() -> {
                        transactionDao.delete(transaction);
                        handler.post(() -> callback.onSuccess());
                    });
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // حفظ التغييرات محلياً فقط
                executor.execute(() -> {
                    transactionDao.delete(transaction);
                    handler.post(() -> callback.onSuccess());
                });
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
} 0
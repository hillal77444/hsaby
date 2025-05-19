package com.hillal.hhhhhhh.data.remote;

import android.content.Context;
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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

    public DataManager(Context context, AccountDao accountDao, TransactionDao transactionDao, PendingOperationDao pendingOperationDao) {
        this.context = context;
        this.apiService = RetrofitClient.getApiService();
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
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
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

        // محاولة تجديد التوكن مباشرة
        apiService.refreshToken("Bearer " + currentToken).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String newToken = response.body().get("token");
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
                    Log.e(TAG, "فشل في تجديد التوكن: " + response.code());
                    callback.onError("فشل في تجديد التوكن");
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Log.e(TAG, "خطأ في تجديد التوكن: " + t.getMessage());
                callback.onError("خطأ في تجديد التوكن: " + t.getMessage());
            }
        });
    }

    public void fetchDataFromServer(DataCallback callback, boolean isFullSync) {
        String token = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .getString("token", null);

        if (token == null) {
            handler.post(() -> callback.onError("يرجى تسجيل الدخول أولاً"));
            return;
        }

        // التحقق من صلاحية التوكن وتجديده إذا لزم الأمر
        checkAndRefreshToken(new DataCallback() {
            @Override
            public void onSuccess() {
                if (isFullSync) {
                    // حذف جميع البيانات المحلية في حالة المزامنة الكاملة
                    executor.execute(() -> {
                        try {
                            accountDao.deleteAll();
                            transactionDao.deleteAll();
                            Log.d(TAG, "تم حذف جميع البيانات المحلية");
                            proceedWithFullSync(token, callback);
                        } catch (Exception e) {
                            Log.e(TAG, "خطأ في حذف البيانات المحلية: " + e.getMessage());
                            handler.post(() -> callback.onError("خطأ في حذف البيانات المحلية"));
                        }
                    });
                } else {
                    // مزامنة التغييرات فقط
                    proceedWithSync(callback);
                }
            }

            @Override
            public void onError(String error) {
                handler.post(() -> callback.onError(error));
            }
        });
    }

    private void proceedWithFullSync(String token, DataCallback callback) {
        Log.d(TAG, "بدء المزامنة الكاملة...");

        // جلب جميع الحسابات
        apiService.getAccounts("Bearer " + token).enqueue(new Callback<List<Account>>() {
            @Override
            public void onResponse(Call<List<Account>> call, Response<List<Account>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Account> accounts = response.body();
                    Log.d(TAG, "تم جلب " + accounts.size() + " حساب من السيرفر");
                    
                    executor.execute(() -> {
                        try {
                            // إضافة جميع الحسابات
                            for (Account account : accounts) {
                                accountDao.insert(account);
                                Log.d(TAG, "تم إضافة حساب: " + account.getServerId());
                            }
                            
                            // جلب جميع المعاملات
                            fetchAllTransactions(token, callback);
                        } catch (Exception e) {
                            Log.e(TAG, "خطأ في حفظ الحسابات: " + e.getMessage());
                            handler.post(() -> callback.onError("خطأ في حفظ الحسابات"));
                        }
                    });
                } else {
                    Log.e(TAG, "فشل في جلب الحسابات: " + response.code());
                    handler.post(() -> callback.onError("فشل في جلب الحسابات"));
                }
            }

            @Override
            public void onFailure(Call<List<Account>> call, Throwable t) {
                Log.e(TAG, "خطأ في الاتصال: " + t.getMessage());
                handler.post(() -> callback.onError("خطأ في الاتصال"));
            }
        });
    }

    private void fetchAllTransactions(String token, DataCallback callback) {
        Log.d(TAG, "جلب جميع المعاملات...");
        
        apiService.getTransactions("Bearer " + token).enqueue(new Callback<List<Transaction>>() {
            @Override
            public void onResponse(Call<List<Transaction>> call, Response<List<Transaction>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Transaction> transactions = response.body();
                    Log.d(TAG, "تم جلب " + transactions.size() + " معاملة من السيرفر");
                    
                    executor.execute(() -> {
                        try {
                            // إضافة جميع المعاملات
                            for (Transaction transaction : transactions) {
                                transactionDao.insert(transaction);
                                Log.d(TAG, "تم إضافة معاملة: " + transaction.getServerId());
                            }
                            
                            // تحديث وقت آخر مزامنة
                            context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
                                    .edit()
                                    .putLong("last_sync_time", System.currentTimeMillis())
                                    .apply();
                            
                            Log.d(TAG, "تمت المزامنة الكاملة بنجاح");
                            handler.post(() -> callback.onSuccess());
                        } catch (Exception e) {
                            Log.e(TAG, "خطأ في حفظ المعاملات: " + e.getMessage());
                            handler.post(() -> callback.onError("خطأ في حفظ المعاملات"));
                        }
                    });
                } else {
                    Log.e(TAG, "فشل في جلب المعاملات: " + response.code());
                    handler.post(() -> callback.onError("فشل في جلب المعاملات"));
                }
            }

            @Override
            public void onFailure(Call<List<Transaction>> call, Throwable t) {
                Log.e(TAG, "خطأ في الاتصال: " + t.getMessage());
                handler.post(() -> callback.onError("خطأ في الاتصال"));
            }
        });
    }

    private void proceedWithSync(DataCallback callback) {
        Log.d(TAG, "بدء مزامنة التغييرات...");

        // جلب آخر وقت مزامنة
        long lastSyncTime = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
                .getLong("last_sync_time", 0);

        String token = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .getString("token", null);

        // جلب التغييرات من السيرفر
        apiService.getChanges("Bearer " + token, lastSyncTime).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> changes = response.body();
                    
                    executor.execute(() -> {
                        try {
                            // معالجة الحسابات المحدثة
                            List<Account> accounts = (List<Account>) changes.get("accounts");
                            if (accounts != null) {
                                for (Account account : accounts) {
                                    if (account.getServerId() > 0) {
                                        Account existingAccount = accountDao.getAccountByServerId(account.getServerId());
                                        if (existingAccount != null) {
                                            accountDao.update(account);
                                            Log.d(TAG, "تم تحديث حساب: " + account.getServerId());
                                        } else {
                                            accountDao.insert(account);
                                            Log.d(TAG, "تم إضافة حساب جديد: " + account.getServerId());
                                        }
                                    } else {
                                        accountDao.insert(account);
                                        Log.d(TAG, "تم إضافة حساب جديد بدون server_id");
                                    }
                                }
                            }

                            // معالجة المعاملات المحدثة
                            List<Transaction> transactions = (List<Transaction>) changes.get("transactions");
                            if (transactions != null) {
                                for (Transaction transaction : transactions) {
                                    if (transaction.getServerId() > 0) {
                                        Transaction existingTransaction = transactionDao.getTransactionByServerId(transaction.getServerId());
                                        if (existingTransaction != null) {
                                            transactionDao.update(transaction);
                                            Log.d(TAG, "تم تحديث معاملة: " + transaction.getServerId());
                                        } else {
                                            transactionDao.insert(transaction);
                                            Log.d(TAG, "تم إضافة معاملة جديدة: " + transaction.getServerId());
                                        }
                                    } else {
                                        transactionDao.insert(transaction);
                                        Log.d(TAG, "تم إضافة معاملة جديدة بدون server_id");
                                    }
                                }
                            }

                            // تحديث وقت آخر مزامنة
                            context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
                                    .edit()
                                    .putLong("last_sync_time", System.currentTimeMillis())
                                    .apply();

                            // مزامنة العمليات المعلقة إذا كانت موجودة
                            if (pendingOperationDao.getPendingOperationsCount() > 0) {
                                syncPendingOperations(new DataCallback() {
                                    @Override
                                    public void onSuccess() {
                                        Log.d(TAG, "تمت مزامنة التغييرات بنجاح");
                                        handler.post(() -> callback.onSuccess());
                                    }

                                    @Override
                                    public void onError(String error) {
                                        Log.e(TAG, "خطأ في مزامنة العمليات المعلقة: " + error);
                                        handler.post(() -> callback.onError(error));
                                    }
                                });
                            } else {
                                Log.d(TAG, "تمت مزامنة التغييرات بنجاح");
                                handler.post(() -> callback.onSuccess());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "خطأ في معالجة التغييرات: " + e.getMessage());
                            handler.post(() -> callback.onError("خطأ في معالجة التغييرات"));
                        }
                    });
                } else {
                    Log.e(TAG, "فشل في جلب التغييرات: " + response.code());
                    handler.post(() -> callback.onError("فشل في جلب التغييرات"));
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "خطأ في الاتصال: " + t.getMessage());
                handler.post(() -> callback.onError("خطأ في الاتصال"));
            }
        });
    }

    public void updateTransaction(Transaction transaction, DataCallback callback) {
        if (!isNetworkAvailable()) {
            // حفظ العملية في قائمة العمليات المعلقة
            executor.execute(() -> {
                try {
                    String transactionJson = gson.toJson(transaction);
                    PendingOperation operation = new PendingOperation("UPDATE", transaction.getId(), transactionJson);
                    pendingOperationDao.insert(operation);
                    transactionDao.update(transaction);
                    Log.d(TAG, "تم حفظ عملية التحديث في قائمة العمليات المعلقة");
                    handler.post(() -> callback.onSuccess());
                } catch (Exception e) {
                    Log.e(TAG, "خطأ في حفظ العملية المعلقة: " + e.getMessage());
                    handler.post(() -> callback.onError("خطأ في حفظ العملية المعلقة: " + e.getMessage()));
                }
            });
            return;
        }

        String token = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .getString("token", null);

        if (token == null) {
            handler.post(() -> callback.onError("يرجى تسجيل الدخول أولاً"));
            return;
        }

        apiService.updateTransaction("Bearer " + token, transaction.getId(), transaction)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            // تحديث القيد في قاعدة البيانات المحلية
                            executor.execute(() -> {
                                try {
                                    transactionDao.update(transaction);
                                    Log.d(TAG, "تم تحديث القيد بنجاح");
                                    handler.post(() -> callback.onSuccess());
                                } catch (Exception e) {
                                    Log.e(TAG, "خطأ في تحديث القيد: " + e.getMessage());
                                    handler.post(() -> callback.onError("خطأ في تحديث القيد: " + e.getMessage()));
                                }
                            });
                        } else {
                            handler.post(() -> callback.onError("فشل في تحديث القيد"));
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        handler.post(() -> callback.onError("خطأ في الاتصال: " + t.getMessage()));
                    }
                });
    }

    public void deleteTransaction(long transactionId, DataCallback callback) {
        if (!isNetworkAvailable()) {
            handler.post(() -> callback.onError("يرجى الاتصال بالإنترنت لحذف المعاملة"));
            return;
        }

        String token = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .getString("token", null);

        if (token == null) {
            handler.post(() -> callback.onError("يرجى تسجيل الدخول أولاً"));
            return;
        }

        // التحقق من وجود المعاملة في قاعدة البيانات المحلية
        executor.execute(() -> {
            try {
                Transaction transaction = transactionDao.getTransactionById(transactionId).getValue();
                if (transaction == null) {
                    handler.post(() -> callback.onError("المعاملة غير موجودة"));
                    return;
                }

                // التحقق من وجود server_id
                if (transaction.getServerId() <= 0) {
                    handler.post(() -> callback.onError("لا يمكن حذف المعاملة لأنها غير مزامنة مع السيرفر"));
                    return;
                }

                // إرسال طلب الحذف إلى السيرفر باستخدام server_id
                apiService.deleteTransaction("Bearer " + token, transaction.getServerId())
                        .enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                if (response.isSuccessful()) {
                                    // حذف القيد من قاعدة البيانات المحلية
                                    executor.execute(() -> {
                                        try {
                                            transactionDao.delete(transaction);
                                            Log.d(TAG, "تم حذف القيد بنجاح");
                                            handler.post(() -> callback.onSuccess());
                                        } catch (Exception e) {
                                            Log.e(TAG, "خطأ في حذف القيد: " + e.getMessage());
                                            handler.post(() -> callback.onError("خطأ في حذف القيد: " + e.getMessage()));
                                        }
                                    });
                                } else {
                                    handler.post(() -> callback.onError("فشل في حذف القيد من الخادم"));
                                }
                            }

                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {
                                handler.post(() -> callback.onError("خطأ في الاتصال: " + t.getMessage()));
                            }
                        });
            } catch (Exception e) {
                Log.e(TAG, "خطأ في التحقق من المعاملة: " + e.getMessage());
                handler.post(() -> callback.onError("خطأ في التحقق من المعاملة: " + e.getMessage()));
            }
        });
    }

    public void syncPendingOperations(DataCallback callback) {
        if (!isNetworkAvailable()) {
            handler.post(() -> callback.onError("لا يوجد اتصال بالإنترنت"));
            return;
        }

        executor.execute(() -> {
            try {
                List<PendingOperation> operations = pendingOperationDao.getAllPendingOperations();
                for (PendingOperation operation : operations) {
                    if (operation.getOperationType().equals("UPDATE")) {
                        Transaction transaction = gson.fromJson(operation.getTransactionData(), Transaction.class);
                        // التحقق مما إذا كان القيد لا يزال موجوداً محلياً
                        Transaction localTransaction = transactionDao.getTransactionById(transaction.getId()).getValue();
                        if (localTransaction == null) {
                            // إذا كان القيد غير موجود، نحذف العملية المعلقة
                            pendingOperationDao.delete(operation);
                            Log.d(TAG, "تم تجاهل عملية التحديث لأن القيد غير موجود محلياً");
                            continue;
                        }
                        updateTransaction(transaction, new DataCallback() {
                            @Override
                            public void onSuccess() {
                                pendingOperationDao.delete(operation);
                            }

                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "فشل في مزامنة عملية التحديث: " + error);
                            }
                        });
                    } else if (operation.getOperationType().equals("DELETE")) {
                        // التحقق مما إذا كان القيد لا يزال موجوداً محلياً
                        Transaction localTransaction = transactionDao.getTransactionById(operation.getTransactionId()).getValue();
                        if (localTransaction == null) {
                            // إذا كان القيد غير موجود، نحذف العملية المعلقة
                            pendingOperationDao.delete(operation);
                            Log.d(TAG, "تم تجاهل عملية الحذف لأن القيد غير موجود محلياً");
                            continue;
                        }
                        deleteTransaction(operation.getTransactionId(), new DataCallback() {
                            @Override
                            public void onSuccess() {
                                pendingOperationDao.delete(operation);
                            }

                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "فشل في مزامنة عملية الحذف: " + error);
                            }
                        });
                    }
                }
                handler.post(() -> callback.onSuccess());
            } catch (Exception e) {
                Log.e(TAG, "خطأ في مزامنة العمليات المعلقة: " + e.getMessage());
                handler.post(() -> callback.onError("خطأ في مزامنة العمليات المعلقة: " + e.getMessage()));
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
} 
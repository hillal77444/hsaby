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

    public void fetchDataFromServer(DataCallback callback) {
        String token = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .getString("token", null);

        if (token == null) {
            handler.post(() -> callback.onError("يرجى تسجيل الدخول أولاً"));
            return;
        }

        // جلب الحسابات
        apiService.getAccounts("Bearer " + token).enqueue(new Callback<List<Account>>() {
            @Override
            public void onResponse(Call<List<Account>> call, Response<List<Account>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Account> accounts = response.body();
                    // حفظ الحسابات في قاعدة البيانات المحلية على خيط منفصل
                    executor.execute(() -> {
                        try {
                            accountDao.insertAll(accounts);
                            Log.d(TAG, "تم جلب " + accounts.size() + " حساب بنجاح");
                            // جلب المعاملات
                            fetchTransactions(token, callback);
                        } catch (Exception e) {
                            Log.e(TAG, "خطأ في حفظ الحسابات: " + e.getMessage());
                            handler.post(() -> callback.onError("خطأ في حفظ الحسابات: " + e.getMessage()));
                        }
                    });
                } else {
                    handler.post(() -> callback.onError("فشل في جلب الحسابات"));
                }
            }

            @Override
            public void onFailure(Call<List<Account>> call, Throwable t) {
                handler.post(() -> callback.onError("خطأ في الاتصال: " + t.getMessage()));
            }
        });
    }

    private void fetchTransactions(String token, DataCallback callback) {
        apiService.getTransactions("Bearer " + token).enqueue(new Callback<List<Transaction>>() {
            @Override
            public void onResponse(Call<List<Transaction>> call, Response<List<Transaction>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Transaction> transactions = response.body();
                    // حفظ المعاملات في قاعدة البيانات المحلية على خيط منفصل
                    executor.execute(() -> {
                        try {
                            for (Transaction serverTransaction : transactions) {
                                // البحث عن المعاملة باستخدام server_id
                                Transaction existingTransaction = transactionDao.getTransactionByServerIdSync(serverTransaction.getServerId());
                                
                                if (existingTransaction == null) {
                                    // إذا لم تكن المعاملة موجودة، نقوم بإضافتها كمعاملة جديدة
                                    Transaction newTransaction = new Transaction();
                                    newTransaction.setServerId(serverTransaction.getServerId());
                                    newTransaction.setUserId(serverTransaction.getUserId());
                                    newTransaction.setAccountId(serverTransaction.getAccountId());
                                    newTransaction.setAmount(serverTransaction.getAmount());
                                    newTransaction.setType(serverTransaction.getType());
                                    newTransaction.setDescription(serverTransaction.getDescription());
                                    newTransaction.setNotes(serverTransaction.getNotes());
                                    newTransaction.setCurrency(serverTransaction.getCurrency());
                                    newTransaction.setTransactionDate(serverTransaction.getTransactionDate());
                                    newTransaction.setCreatedAt(serverTransaction.getCreatedAt());
                                    newTransaction.setUpdatedAt(serverTransaction.getUpdatedAt());
                                    newTransaction.setLastSyncTime(System.currentTimeMillis());
                                    newTransaction.setSyncStatus(2); // SYNCED
                                    
                                    transactionDao.insert(newTransaction);
                                    Log.d(TAG, "تمت إضافة معاملة جديدة من السيرفر: " + serverTransaction.getServerId());
                                } else {
                                    // إذا كانت المعاملة موجودة، نقوم بتحديثها
                                    existingTransaction.setUserId(serverTransaction.getUserId());
                                    existingTransaction.setAccountId(serverTransaction.getAccountId());
                                    existingTransaction.setAmount(serverTransaction.getAmount());
                                    existingTransaction.setType(serverTransaction.getType());
                                    existingTransaction.setDescription(serverTransaction.getDescription());
                                    existingTransaction.setNotes(serverTransaction.getNotes());
                                    existingTransaction.setCurrency(serverTransaction.getCurrency());
                                    existingTransaction.setTransactionDate(serverTransaction.getTransactionDate());
                                    existingTransaction.setCreatedAt(serverTransaction.getCreatedAt());
                                    existingTransaction.setUpdatedAt(serverTransaction.getUpdatedAt());
                                    existingTransaction.setLastSyncTime(System.currentTimeMillis());
                                    existingTransaction.setSyncStatus(2); // SYNCED
                                    
                                    transactionDao.update(existingTransaction);
                                    Log.d(TAG, "تم تحديث معاملة موجودة: " + serverTransaction.getServerId());
                                }
                            }
                            Log.d(TAG, "تم جلب " + transactions.size() + " معاملة بنجاح");
                            handler.post(() -> callback.onSuccess());
                        } catch (Exception e) {
                            Log.e(TAG, "خطأ في حفظ المعاملات: " + e.getMessage());
                            handler.post(() -> callback.onError("خطأ في حفظ المعاملات: " + e.getMessage()));
                        }
                    });
                } else {
                    handler.post(() -> callback.onError("فشل في جلب المعاملات"));
                }
            }

            @Override
            public void onFailure(Call<List<Transaction>> call, Throwable t) {
                handler.post(() -> callback.onError("خطأ في الاتصال: " + t.getMessage()));
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
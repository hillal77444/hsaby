package com.hillal.acc.data.sync;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import android.app.AlertDialog;
import android.util.Log;

import com.hillal.acc.data.room.AppDatabase;
import com.hillal.acc.data.room.AccountDao;
import com.hillal.acc.data.room.TransactionDao;
import com.hillal.acc.data.model.Account;
import com.hillal.acc.data.model.Transaction;
import com.hillal.acc.data.remote.ApiService;
import com.hillal.acc.data.remote.RetrofitClient;
import com.hillal.acc.data.remote.ApiService.SyncRequest;
import com.hillal.acc.data.remote.ApiService.SyncResponse;
import com.hillal.acc.data.preferences.UserPreferences;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.IOException;
import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MigrationManager {
    private final AppDatabase database;
    private final AccountDao accountDao;
    private final TransactionDao transactionDao;
    private final ApiService apiService;
    private final Context context;
    private final UserPreferences userPreferences;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private int migratedAccountsCount = 0;
    private int migratedTransactionsCount = 0;

    public MigrationManager(Context context) {
        this.context = context;
        this.database = AppDatabase.getInstance(context);
        this.accountDao = database.accountDao();
        this.transactionDao = database.transactionDao();
        this.apiService = RetrofitClient.getApiService();
        this.userPreferences = new UserPreferences(context);
    }

    public void migrateLocalData() {
        executor.execute(() -> {
            if (!isNetworkAvailable()) {
                new Handler(Looper.getMainLooper()).post(() -> 
                    Toast.makeText(context, "لا يوجد اتصال بالإنترنت", Toast.LENGTH_SHORT).show());
                return;
            }

            String token = getAuthToken();
            if (token == null) {
                return;
            }

            migratedAccountsCount = 0;
            migratedTransactionsCount = 0;

            List<Account> accountsToMigrate = accountDao.getNewOrModifiedAccounts();
            List<Transaction> transactionsToMigrate = transactionDao.getNewOrModifiedTransactions();

            if (accountsToMigrate.isEmpty() && transactionsToMigrate.isEmpty()) {
                return;
            }

            Log.d("MigrationManager", "Starting migration with " + accountsToMigrate.size() + " accounts and " + 
                transactionsToMigrate.size() + " transactions");

            // أولاً: مزامنة الحسابات
            SyncRequest accountRequest = new SyncRequest(accountsToMigrate, null);
            apiService.syncData("Bearer " + token, accountRequest).enqueue(new Callback<SyncResponse>() {
                @Override
                public void onResponse(Call<SyncResponse> call, Response<SyncResponse> response) {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            SyncResponse syncResponse = response.body();
                            
                            executor.execute(() -> {
                                try {
                                    // تحديث الحسابات
                                    for (Account account : accountsToMigrate) {
                                        Long serverId = syncResponse.getAccountServerId(account.getId());
                                        
                                        if (serverId != null && serverId > 0) {
                                            account.setServerId(serverId);
                                            account.setSyncStatus(2); 
                                            accountDao.update(account);
                                            migratedAccountsCount++;
                                        }
                                    }

                                    // بعد تحديث الحسابات، نقوم بتحديث معرفات الحسابات في المعاملات
                                    for (Transaction transaction : transactionsToMigrate) {
                                        Account relatedAccount = accountDao.getAccountByIdSync(transaction.getAccountId());
                                        if (relatedAccount != null && relatedAccount.getServerId() > 0) {
                                            // نحتفظ بمعرف الحساب المحلي كما هو
                                            // نستخدم معرف الخادم فقط عند إرسال البيانات للخادم
                                            Log.d("MigrationManager", "المعاملة " + transaction.getId() + 
                                                " معرف الحساب المحلي: " + transaction.getAccountId() + 
                                                " معرف الحساب على الخادم: " + relatedAccount.getServerId());
                                        }
                                    }

                                    // ثم نقوم بمزامنة المعاملات
                                    if (!transactionsToMigrate.isEmpty()) {
                                        // إضافة الحسابات المرتبطة بالمعاملات التي لم تتم مزامنتها بعد
                                        List<Account> relatedAccounts = new ArrayList<>();
                                        List<Transaction> transactionsForServer = new ArrayList<>();
                                        
                                        // تحضير المعاملات للإرسال للخادم
                                        for (Transaction transaction : transactionsToMigrate) {
                                            Account account = accountDao.getAccountByIdSync(transaction.getAccountId());
                                            if (account != null && account.getServerId() > 0) {
                                                // نضيف الحساب إذا لم تتم مزامنته بعد
                                                if (account.getSyncStatus() != 2 && !relatedAccounts.contains(account)) {
                                                    relatedAccounts.add(account);
                                                }
                                                
                                                // إنشاء نسخة من المعاملة للإرسال للخادم
                                                Transaction serverTransaction = new Transaction();
                                                serverTransaction.setId(transaction.getId());
                                                serverTransaction.setServerId(transaction.getServerId());
                                                serverTransaction.setAccountId(account.getServerId());
                                                serverTransaction.setAmount(transaction.getAmount());
                                                serverTransaction.setType(transaction.getType());
                                                serverTransaction.setDescription(transaction.getDescription());
                                                serverTransaction.setNotes(transaction.getNotes());
                                                serverTransaction.setCurrency(transaction.getCurrency());
                                                serverTransaction.setTransactionDate(transaction.getTransactionDate());
                                                serverTransaction.setCreatedAt(transaction.getCreatedAt());
                                                serverTransaction.setUpdatedAt(transaction.getUpdatedAt());
                                                serverTransaction.setModified(transaction.isModified());
                                                serverTransaction.setWhatsappEnabled(transaction.isWhatsappEnabled());
                                                serverTransaction.setSyncStatus(transaction.getSyncStatus());
                                                serverTransaction.setCashboxId(transaction.getCashboxId());
                                                
                                                Log.d("MigrationManager", "Creating server transaction: ID=" + transaction.getId() + 
                                                    ", CashboxID=" + transaction.getCashboxId());
                                                transactionsForServer.add(serverTransaction);
                                            }
                                        }
                                        
                                        SyncRequest transactionRequest = new SyncRequest(relatedAccounts, transactionsForServer);
                                        apiService.syncData("Bearer " + token, transactionRequest).enqueue(new Callback<SyncResponse>() {
                                            @Override
                                            public void onResponse(Call<SyncResponse> call, Response<SyncResponse> response) {
                                                try {
                                                    if (response.isSuccessful() && response.body() != null) {
                                                        SyncResponse transactionResponse = response.body();
                                                        
                                                        executor.execute(() -> {
                                                            try {
                                                                // تحديث المعاملات
                                                                for (Transaction transaction : transactionsToMigrate) {
                                                                    Long serverId = transactionResponse.getTransactionServerId(transaction.getId());
                                                                    
                                                                    if (serverId != null && serverId > 0) {
                                                                        try {
                                                                            // طباعة معلومات المعاملة قبل التحديث
                                                                            Log.d("MigrationManager", "تحديث المعاملة: " + transaction.getId() + 
                                                                                " معرف الحساب المحلي: " + transaction.getAccountId() + 
                                                                                " معرف الخادم الجديد: " + serverId);

                                                                            // تحديث معرف الخادم وحالة المزامنة فقط
                                                                            transaction.setServerId(serverId);
                                                                            transaction.setSyncStatus(2);
                                                                            
                                                                            // تحديث قاعدة البيانات المحلية
                                                                            database.runInTransaction(() -> {
                                                                                try {
                                                                                    transactionDao.update(transaction);
                                                                                    migratedTransactionsCount++;
                                                                                    Log.d("MigrationManager", "تم تحديث المعاملة بنجاح: " + transaction.getId());
                                                                                } catch (Exception e) {
                                                                                    Log.e("MigrationManager", "فشل في تحديث المعاملة في قاعدة البيانات: " + transaction.getId() + 
                                                                                        " الخطأ: " + e.getMessage());
                                                                                    throw e;
                                                                                }
                                                                            });
                                                                        } catch (Exception e) {
                                                                            Log.e("MigrationManager", "فشل في تحديث المعاملة: " + transaction.getId() + 
                                                                                " الخطأ: " + e.getMessage());
                                                                            new Handler(Looper.getMainLooper()).post(() -> 
                                                                                Toast.makeText(context, "حدث خطأ أثناء تحديث قاعدة البيانات المحلية", Toast.LENGTH_LONG).show());
                                                                        }
                                                                    }
                                                                }

                                                                new Handler(Looper.getMainLooper()).post(() -> {
                                                                    if (migratedAccountsCount > 0 || migratedTransactionsCount > 0) {
                                                                        String summary = String.format("تم ترحيل %d حساب و %d معاملة بنجاح", 
                                                                            migratedAccountsCount, migratedTransactionsCount);
                                                                        Toast.makeText(context, summary, Toast.LENGTH_LONG).show();
                                                                    } else {
                                                                        Toast.makeText(context, "لم يتم تحديث أي بيانات", Toast.LENGTH_LONG).show();
                                                                    }
                                                                });
                                                            } catch (Exception e) {
                                                                Log.e("MigrationManager", "Error processing transaction response", e);
                                                                new Handler(Looper.getMainLooper()).post(() -> 
                                                                    Toast.makeText(context, "حدث خطأ أثناء معالجة المعاملات", Toast.LENGTH_LONG).show());
                                                            }
                                                        });
                                                    } else {
                                                        handleErrorResponse(response);
                                                    }
                                                } catch (Exception e) {
                                                    handleUnexpectedError(e);
                                                }
                                            }

                                            @Override
                                            public void onFailure(Call<SyncResponse> call, Throwable t) {
                                                handleNetworkError(t);
                                            }
                                        });
                                    } else {
                                        // إذا لم تكن هناك معاملات، نعرض رسالة النجاح للحسابات فقط
                                        new Handler(Looper.getMainLooper()).post(() -> {
                                            if (migratedAccountsCount > 0) {
                                                String summary = String.format("تم ترحيل %d حساب بنجاح", migratedAccountsCount);
                                                Toast.makeText(context, summary, Toast.LENGTH_LONG).show();
                                            } else {
                                                Toast.makeText(context, "لم يتم تحديث أي بيانات", Toast.LENGTH_LONG).show();
                                            }
                                        });
                                    }
                                } catch (Exception e) {
                                    Log.e("MigrationManager", "Error processing account response", e);
                                    new Handler(Looper.getMainLooper()).post(() -> 
                                        Toast.makeText(context, "حدث خطأ أثناء معالجة الحسابات", Toast.LENGTH_LONG).show());
                                }
                            });
                        } else {
                            handleErrorResponse(response);
                        }
                    } catch (Exception e) {
                        handleUnexpectedError(e);
                    }
                }

                @Override
                public void onFailure(Call<SyncResponse> call, Throwable t) {
                    handleNetworkError(t);
                }
            });
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private String getAuthToken() {
        String token = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .getString("token", null);
        if (token == null || token.isEmpty()) {
            new Handler(Looper.getMainLooper()).post(() -> 
                Toast.makeText(context, "يرجى تسجيل الدخول أولاً", Toast.LENGTH_SHORT).show());
            return null;
        }
        return token;
    }

    private void handleErrorResponse(Response<SyncResponse> response) {
        String errorMessage = "فشل في الاتصال بالخادم";
        if (response.code() == 401) {
            errorMessage = "انتهت صلاحية الجلسة، يرجى تسجيل الدخول مرة أخرى";
        } else if (response.code() == 403) {
            errorMessage = "ليس لديك صلاحية للوصول إلى هذه البيانات";
        } else if (response.code() >= 500) {
            errorMessage = "الخادم غير متاح حالياً، يرجى المحاولة لاحقاً";
        }

        final String finalErrorMessage = errorMessage;
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(context, finalErrorMessage, Toast.LENGTH_LONG).show();
        });
    }

    private void handleUnexpectedError(Exception e) {
        Log.e("MigrationManager", "Unexpected error", e);
        new Handler(Looper.getMainLooper()).post(() -> 
            Toast.makeText(context, "حدث خطأ غير متوقع", Toast.LENGTH_LONG).show());
    }

    private void handleNetworkError(Throwable t) {
        Log.e("MigrationManager", "Network error", t);
        String errorMessage = "فشل في الاتصال بالخادم";
        if (t instanceof java.net.UnknownHostException) {
            errorMessage = "لا يمكن الوصول إلى الخادم، يرجى التحقق من اتصال الإنترنت";
        } else if (t instanceof java.net.SocketTimeoutException) {
            errorMessage = "انتهت مهلة الاتصال بالخادم، يرجى المحاولة مرة أخرى";
        }
        
        final String finalErrorMessage = errorMessage;
        new Handler(Looper.getMainLooper()).post(() -> 
            Toast.makeText(context, finalErrorMessage, Toast.LENGTH_LONG).show());
    }
} 
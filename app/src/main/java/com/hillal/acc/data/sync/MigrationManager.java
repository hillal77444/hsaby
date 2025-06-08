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
import java.util.Map;
import java.util.HashMap;
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
                runOnUiThread(() ->
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
                runOnUiThread(() ->
                        Toast.makeText(context, "لا توجد بيانات بحاجة للمزامنة", Toast.LENGTH_SHORT).show());
                return;
            }

            if (!accountsToMigrate.isEmpty()) {
                // مزامنة الحسابات أولاً ثم مزامنة المعاملات
                migrateAccounts(token, accountsToMigrate, transactionsToMigrate);
            } else if (!transactionsToMigrate.isEmpty()) {
                // إذا لم تكن هناك حسابات بحاجة للمزامنة، مزامنة المعاملات مباشرة
                migrateTransactions(token, transactionsToMigrate);
            }
        });
    }

    private void migrateAccounts(String token, List<Account> accountsToMigrate, List<Transaction> transactionsToMigrate) {
        SyncRequest accountRequest = new SyncRequest(accountsToMigrate, new ArrayList<>());
        apiService.syncData("Bearer " + token, accountRequest).enqueue(new Callback<SyncResponse>() {
            @Override
            public void onResponse(Call<SyncResponse> call, Response<SyncResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SyncResponse syncResponse = response.body();
                    executor.execute(() -> {
                        try {
                            for (Account account : accountsToMigrate) {
                                Long serverId = syncResponse.getAccountServerId(account.getId());
                                if (serverId != null && serverId > 0) {
                                    account.setServerId(serverId);
                                    account.setSyncStatus(2);
                                    accountDao.update(account);
                                    migratedAccountsCount++;
                                }
                            }
                            runOnUiThread(() -> {
                                if (migratedAccountsCount > 0) {
                                    Toast.makeText(context, "تم ترحيل " + migratedAccountsCount + " حساب بنجاح", Toast.LENGTH_SHORT).show();
                                }
                            });
                            // بعد انتهاء مزامنة الحسابات، نبدأ بمزامنة المعاملات إذا وجدت
                            if (transactionsToMigrate != null && !transactionsToMigrate.isEmpty()) {
                                migrateTransactions(token, transactionsToMigrate);
                            }
                        } catch (Exception e) {
                            runOnUiThread(() ->
                                    Toast.makeText(context, "حدث خطأ أثناء تحديث الحسابات", Toast.LENGTH_LONG).show());
                        }
                    });
                } else {
                    handleErrorResponse(response);
                }
            }
            @Override
            public void onFailure(Call<SyncResponse> call, Throwable t) {
                handleNetworkError(t);
            }
        });
    }

    private void migrateTransactions(String token, List<Transaction> transactionsToMigrate) {
        executor.execute(() -> {
            // بناء خريطة ربط بين accountId المحلي وserverId بعد تحديث الحسابات
            List<Account> allAccounts = accountDao.getAllAccountsSync();
            Map<Long, Long> accountIdMap = new HashMap<>();
            for (Account account : allAccounts) {
                // الحل الصحيح: serverId من النوع long ولا يمكن مقارنته مع null
                if (account.getServerId() > 0) {
                    accountIdMap.put(account.getId(), account.getServerId());
                }
            }

            // استبدال accountId في المعاملات بـ serverId قبل الترحيل
            for (Transaction transaction : transactionsToMigrate) {
                Long serverAccountId = accountIdMap.get(transaction.getAccountId());
                if (serverAccountId != null) {
                    transaction.setAccountId(serverAccountId);
                }
            }

            SyncRequest transactionRequest = new SyncRequest(new ArrayList<>(), transactionsToMigrate);
            apiService.syncData("Bearer " + token, transactionRequest).enqueue(new Callback<SyncResponse>() {
                @Override
                public void onResponse(Call<SyncResponse> call, Response<SyncResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        SyncResponse syncResponse = response.body();
                        executor.execute(() -> {
                            try {
                                for (Transaction transaction : transactionsToMigrate) {
                                    Long serverId = syncResponse.getTransactionServerId(transaction.getId());
                                    if (serverId != null && serverId > 0) {
                                        transaction.setServerId(serverId);
                                        transaction.setSyncStatus(2);
                                        transactionDao.update(transaction);
                                        migratedTransactionsCount++;
                                    }
                                }
                                runOnUiThread(() -> {
                                    if (migratedTransactionsCount > 0) {
                                        Toast.makeText(context, "تم ترحيل " + migratedTransactionsCount + " معاملة بنجاح", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(context, "لم يتم تحديث أي معاملات", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } catch (Exception e) {
                                runOnUiThread(() ->
                                        Toast.makeText(context, "حدث خطأ أثناء تحديث المعاملات", Toast.LENGTH_LONG).show());
                            }
                        });
                    } else {
                        handleErrorResponse(response);
                    }
                }

                @Override
                public void onFailure(Call<SyncResponse> call, Throwable t) {
                    handleNetworkError(t);
                }
            });
        });
    }

    private void handleErrorResponse(Response<?> response) {
        String errorMessage = "فشل في الاتصال بالخادم";
        if (response.code() == 401) {
            errorMessage = "انتهت صلاحية الجلسة، يرجى تسجيل الدخول مرة أخرى";
        } else if (response.code() == 403) {
            errorMessage = "ليس لديك صلاحية للوصول إلى هذه البيانات";
        } else if (response.code() >= 500) {
            errorMessage = "الخادم غير متاح حالياً، يرجى المحاولة لاحقاً";
        }
        final String finalErrorMessage = errorMessage;
        runOnUiThread(() -> {
            new AlertDialog.Builder(context)
                    .setTitle("خطأ في المزامنة")
                    .setMessage(finalErrorMessage)
                    .setPositiveButton("حسناً", null)
                    .show();
        });
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
        runOnUiThread(() ->
                Toast.makeText(context, finalErrorMessage, Toast.LENGTH_LONG).show());
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
            runOnUiThread(() ->
                    Toast.makeText(context, "يرجى تسجيل الدخول أولاً", Toast.LENGTH_SHORT).show());
            return null;
        }
        return token;
    }

    private void runOnUiThread(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }
}
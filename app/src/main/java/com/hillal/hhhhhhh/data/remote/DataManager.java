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

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DataManager {
    private static final String TAG = "DataManager";
    private final Context context;
    private final ApiService apiService;
    private final AccountDao accountDao;
    private final TransactionDao transactionDao;
    private final Handler handler;

    public DataManager(Context context, AccountDao accountDao, TransactionDao transactionDao) {
        this.context = context;
        this.apiService = RetrofitClient.getApiService();
        this.accountDao = accountDao;
        this.transactionDao = transactionDao;
        this.handler = new Handler(Looper.getMainLooper());
    }

    public interface DataCallback {
        void onSuccess();
        void onError(String error);
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
                    // حفظ الحسابات في قاعدة البيانات المحلية
                    accountDao.insertAll(accounts);
                    Log.d(TAG, "تم جلب " + accounts.size() + " حساب بنجاح");

                    // جلب المعاملات
                    fetchTransactions(token, callback);
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
                    // حفظ المعاملات في قاعدة البيانات المحلية
                    transactionDao.insertAll(transactions);
                    Log.d(TAG, "تم جلب " + transactions.size() + " معاملة بنجاح");
                    handler.post(() -> callback.onSuccess());
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
} 
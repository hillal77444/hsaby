package com.hillal.acc.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.hillal.acc.data.dao.CashboxDao;
import com.hillal.acc.data.entities.Cashbox;
import com.hillal.acc.data.room.AppDatabase;
import com.hillal.acc.data.remote.ApiService;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CashboxRepository {
    private CashboxDao cashboxDao;
    private ExecutorService executorService;

    public CashboxRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        cashboxDao = db.cashboxDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<Cashbox>> getAllCashboxes() {
        MutableLiveData<List<Cashbox>> data = new MutableLiveData<>();
        executorService.execute(() -> data.postValue(cashboxDao.getAll()));
        return data;
    }

    public void insert(Cashbox cashbox) {
        executorService.execute(() -> cashboxDao.insert(cashbox));
    }

    public void update(Cashbox cashbox) {
        executorService.execute(() -> cashboxDao.update(cashbox));
    }

    public void delete(Cashbox cashbox) {
        executorService.execute(() -> cashboxDao.delete(cashbox));
    }

    public void deleteAll() {
        executorService.execute(() -> cashboxDao.deleteAll());
    }

    public void fetchCashboxesFromApi(String baseUrl, String token, Runnable onDone) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ApiService api = retrofit.create(ApiService.class);
        Call<List<Cashbox>> call = api.getCashboxes(token);
        call.enqueue(new Callback<List<Cashbox>>() {
            @Override
            public void onResponse(Call<List<Cashbox>> call, Response<List<Cashbox>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    executorService.execute(() -> {
                        cashboxDao.deleteAll();
                        for (Cashbox c : response.body()) {
                            cashboxDao.insert(c);
                        }
                        if (onDone != null) onDone.run();
                    });
                } else {
                    if (onDone != null) onDone.run();
                }
            }
            @Override
            public void onFailure(Call<List<Cashbox>> call, Throwable t) {
                if (onDone != null) onDone.run();
            }
        });
    }

    public void addCashboxToServer(String baseUrl, String token, String name, CashboxCallback callback) {
        android.util.Log.d("CashboxRepository", "Adding cashbox to server: name=" + name + ", baseUrl=" + baseUrl);
        
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ApiService api = retrofit.create(ApiService.class);
        ApiService.AddCashboxRequest request = new ApiService.AddCashboxRequest(name);
        Call<Cashbox> call = api.addCashbox(token, request);
        call.enqueue(new Callback<Cashbox>() {
            @Override
            public void onResponse(Call<Cashbox> call, Response<Cashbox> response) {
                android.util.Log.d("CashboxRepository", "Server response: code=" + response.code() + ", success=" + response.isSuccessful());
                
                if (response.isSuccessful() && response.body() != null) {
                    Cashbox serverCashbox = response.body();
                    android.util.Log.d("CashboxRepository", "Received cashbox from server: id=" + serverCashbox.id + ", name=" + serverCashbox.name);
                    
                    executorService.execute(() -> {
                        cashboxDao.insert(serverCashbox);
                        android.util.Log.d("CashboxRepository", "Saved cashbox to local database: id=" + serverCashbox.id + ", name=" + serverCashbox.name);
                        callback.onSuccess(serverCashbox);
                    });
                } else {
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        errorBody = "Error reading response body";
                    }
                    android.util.Log.e("CashboxRepository", "Server error: code=" + response.code() + ", body=" + errorBody);
                    callback.onError("فشل في إضافة الصندوق إلى الخادم: " + response.code());
                }
            }
            @Override
            public void onFailure(Call<Cashbox> call, Throwable t) {
                android.util.Log.e("CashboxRepository", "Network error: " + t.getMessage(), t);
                callback.onError("خطأ في الاتصال: " + t.getMessage());
            }
        });
    }

    public interface CashboxCallback {
        void onSuccess(Cashbox cashbox);
        void onError(String error);
    }
} 
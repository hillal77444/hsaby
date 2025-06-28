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
} 
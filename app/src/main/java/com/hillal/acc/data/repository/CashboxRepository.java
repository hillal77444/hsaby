package com.hillal.acc.data.repository;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.hillal.acc.data.dao.CashboxDao;
import com.hillal.acc.data.entities.Cashbox;
import com.hillal.acc.data.room.AppDatabase;
import com.hillal.acc.data.remote.ApiService;
import com.hillal.acc.data.remote.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CashboxRepository {
    private static final String TAG = "CashboxRepository";
    private CashboxDao cashboxDao;
    private ExecutorService executorService;
    private final ApiService apiService;

    public CashboxRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        cashboxDao = db.cashboxDao();
        executorService = Executors.newSingleThreadExecutor();
        // استخدام RetrofitClient الموحد
        apiService = RetrofitClient.getInstance().getApiService();
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

    public void fetchCashboxesFromApi(String token, Runnable onDone) {
        Log.d(TAG, "Fetching cashboxes from server...");
        Call<List<Cashbox>> call = apiService.getCashboxes(token);
        call.enqueue(new Callback<List<Cashbox>>() {
            @Override
            public void onResponse(Call<List<Cashbox>> call, Response<List<Cashbox>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Cashbox> cashboxes = response.body();
                    Log.d(TAG, "Received " + cashboxes.size() + " cashboxes from server");
                    executorService.execute(() -> {
                        try {
                            cashboxDao.deleteAll();
                            for (Cashbox c : cashboxes) {
                                cashboxDao.insert(c);
                                Log.d(TAG, "Saved cashbox: id=" + c.id + ", name=" + c.name);
                            }
                            Log.d(TAG, "All cashboxes processed successfully");
                        } catch (Exception e) {
                            Log.e(TAG, "Error saving cashboxes to database", e);
                        }
                        if (onDone != null) onDone.run();
                    });
                } else {
                    String errorMessage = "خطأ في جلب الصناديق: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            errorMessage += "\n" + response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Log.e(TAG, errorMessage);
                    if (onDone != null) onDone.run();
                }
            }
            @Override
            public void onFailure(Call<List<Cashbox>> call, Throwable t) {
                Log.e(TAG, "Network error while fetching cashboxes", t);
                if (onDone != null) onDone.run();
            }
        });
    }

    public void addCashboxToServer(String token, String name, CashboxCallback callback) {
        Log.d(TAG, "Adding cashbox to server - name: " + name);
        
        // التحقق من صحة البيانات
        if (name == null || name.trim().isEmpty()) {
            callback.onError("اسم الصندوق مطلوب");
            return;
        }

        // إنشاء طلب إضافة الصندوق
        ApiService.AddCashboxRequest request = new ApiService.AddCashboxRequest(name.trim());
        Call<Cashbox> call = apiService.addCashbox(token, request);
        
        call.enqueue(new Callback<Cashbox>() {
            @Override
            public void onResponse(Call<Cashbox> call, Response<Cashbox> response) {
                Log.d(TAG, "Server response: code=" + response.code());
                
                if (response.isSuccessful() && response.body() != null) {
                    Cashbox serverCashbox = response.body();
                    Log.d(TAG, "Received cashbox from server: id=" + serverCashbox.id + ", name=" + serverCashbox.name);
                    
                    // حفظ الصندوق في قاعدة البيانات المحلية
                    executorService.execute(() -> {
                        try {
                            cashboxDao.insert(serverCashbox);
                            Log.d(TAG, "Saved cashbox to local database: id=" + serverCashbox.id + ", name=" + serverCashbox.name);
                            callback.onSuccess(serverCashbox);
                        } catch (Exception e) {
                            Log.e(TAG, "Error saving cashbox to local database", e);
                            callback.onError("خطأ في حفظ الصندوق محلياً: " + e.getMessage());
                        }
                    });
                } else {
                    String errorMessage = "فشل في إضافة الصندوق إلى الخادم";
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Server error body: " + errorBody);
                            errorMessage += " (كود: " + response.code() + ")";
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading response body", e);
                        errorMessage += " (كود: " + response.code() + ")";
                    }
                    
                    Log.e(TAG, "Server error: " + errorMessage);
                    Log.e(TAG, "Response headers: " + response.headers());
                    callback.onError(errorMessage);
                }
            }
            
            @Override
            public void onFailure(Call<Cashbox> call, Throwable t) {
                Log.e(TAG, "Network error while adding cashbox", t);
                Log.e(TAG, "Call details: " + call.request().url());
                Log.e(TAG, "Request headers: " + call.request().headers());
                
                String errorMessage = "خطأ في الاتصال بالخادم";
                if (t.getMessage() != null) {
                    errorMessage += ": " + t.getMessage();
                }
                callback.onError(errorMessage);
            }
        });
    }

    public interface CashboxCallback {
        void onSuccess(Cashbox cashbox);
        void onError(String error);
    }
} 
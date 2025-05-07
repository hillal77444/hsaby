package com.hillal.hhhhhhh.data.repository;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import com.hillal.hhhhhhh.data.model.User;
import com.hillal.hhhhhhh.data.remote.ApiService;
import com.hillal.hhhhhhh.data.remote.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {
    private static final String TAG = "AuthRepository";
    private static final String PREF_NAME = "auth_prefs";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PHONE = "phone";

    private final ApiService apiService;
    private final SharedPreferences preferences;

    public interface AuthCallback {
        void onSuccess(User user);
        void onError(String error);
    }

    public AuthRepository(Application application) {
        apiService = RetrofitClient.getInstance().getApiService();
        preferences = application.getSharedPreferences(PREF_NAME, Application.MODE_PRIVATE);
    }

    public void login(String phone, String password, AuthCallback callback) {
        apiService.login(new ApiService.LoginRequest(phone, password)).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    saveUserData(user);
                    callback.onSuccess(user);
                } else {
                    String errorMessage = "فشل تسجيل الدخول: " + response.code();
                    if (response.errorBody() != null) {
                        try {
                            errorMessage += " - " + response.errorBody().string();
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body", e);
                        }
                    }
                    callback.onError(errorMessage);
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Log.e(TAG, "Login error: " + t.getMessage(), t);
                String errorMessage = "خطأ في الاتصال: " + t.getMessage();
                if (t instanceof java.net.UnknownHostException) {
                    errorMessage = "لا يمكن الوصول إلى الخادم. يرجى التحقق من اتصال الإنترنت";
                } else if (t instanceof java.net.SocketTimeoutException) {
                    errorMessage = "انتهت مهلة الاتصال بالخادم. يرجى المحاولة مرة أخرى";
                }
                callback.onError(errorMessage);
            }
        });
    }

    public void register(String username, String phone, String password, AuthCallback callback) {
        apiService.register(new ApiService.RegisterRequest(username, phone, password)).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    saveUserData(user);
                    callback.onSuccess(user);
                } else {
                    String errorMessage = "فشل التسجيل: " + response.code();
                    if (response.errorBody() != null) {
                        try {
                            errorMessage += " - " + response.errorBody().string();
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body", e);
                        }
                    }
                    callback.onError(errorMessage);
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Log.e(TAG, "Registration error: " + t.getMessage(), t);
                String errorMessage = "خطأ في الاتصال: " + t.getMessage();
                if (t instanceof java.net.UnknownHostException) {
                    errorMessage = "لا يمكن الوصول إلى الخادم. يرجى التحقق من اتصال الإنترنت";
                } else if (t instanceof java.net.SocketTimeoutException) {
                    errorMessage = "انتهت مهلة الاتصال بالخادم. يرجى المحاولة مرة أخرى";
                }
                callback.onError(errorMessage);
            }
        });
    }

    public void logout() {
        preferences.edit().clear().apply();
    }

    private void saveUserData(User user) {
        preferences.edit()
                .putString(KEY_TOKEN, user.getToken())
                .putLong(KEY_USER_ID, user.getId())
                .putString(KEY_USERNAME, user.getUsername())
                .putString(KEY_PHONE, user.getPhone())
                .apply();
    }

    public String getToken() {
        return preferences.getString(KEY_TOKEN, null);
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    public User getCurrentUser() {
        if (!isLoggedIn()) {
            return null;
        }

        return new User(
                preferences.getLong(KEY_USER_ID, -1),
                preferences.getString(KEY_USERNAME, ""),
                preferences.getString(KEY_PHONE, ""),
                preferences.getString(KEY_TOKEN, "")
        );
    }
} 
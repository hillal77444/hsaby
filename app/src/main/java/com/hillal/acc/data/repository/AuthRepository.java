package com.hillal.acc.data.repository;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import com.hillal.acc.data.model.User;
import com.hillal.acc.data.remote.ApiService;
import com.hillal.acc.data.remote.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.IOException;

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
        Log.d(TAG, "Attempting to login with phone: " + phone);
        apiService.login(new ApiService.LoginRequest(phone, password)).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    saveUserData(user);
                    callback.onSuccess(user);
                } else {
                    String errorMessage;
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            Log.d(TAG, "Error response from server: " + errorBody);
                            org.json.JSONObject jsonObject = new org.json.JSONObject(errorBody);
                            if (jsonObject.has("error")) {
                                errorMessage = jsonObject.getString("error");
                            } else {
                                errorMessage = "حدث خطأ أثناء تسجيل الدخول";
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body", e);
                            errorMessage = "حدث خطأ أثناء تسجيل الدخول";
                        }
                    } else {
                        errorMessage = "حدث خطأ أثناء تسجيل الدخول";
                    }
                    Log.e(TAG, "Login failed: " + errorMessage);
                    callback.onError(errorMessage);
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Log.e(TAG, "Login error: " + t.getMessage(), t);
                String errorMessage;
                if (t instanceof java.net.UnknownHostException) {
                    errorMessage = "لا يمكن الوصول إلى الخادم. يرجى التحقق من اتصال الإنترنت";
                    Log.e(TAG, "Server is unreachable. Check if the server is running and accessible.");
                } else if (t instanceof java.net.SocketTimeoutException) {
                    errorMessage = "انتهت مهلة الاتصال بالخادم. يرجى المحاولة مرة أخرى";
                    Log.e(TAG, "Connection timeout. Server might be overloaded or network is slow.");
                } else if (t instanceof IOException) {
                    errorMessage = "خطأ في الاتصال بالخادم: " + t.getMessage();
                    Log.e(TAG, "IO Exception during connection", t);
                } else {
                    errorMessage = "خطأ غير متوقع: " + t.getMessage();
                    Log.e(TAG, "Unexpected error during login", t);
                }
                callback.onError(errorMessage);
            }
        });
    }

    public void register(String username, String phone, String password, AuthCallback callback) {
        Log.d(TAG, "Attempting to register with username: " + username + ", phone: " + phone);
        apiService.register(new ApiService.RegisterRequest(username, phone, password)).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    saveUserData(user);
                    callback.onSuccess(user);
                } else {
                    String errorMessage = null;
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            Log.d(TAG, "Error response from server: " + errorBody);
                            org.json.JSONObject jsonObject = new org.json.JSONObject(errorBody);
                            if (jsonObject.has("error")) {
                                errorMessage = jsonObject.getString("error");
                            } else {
                                errorMessage = errorBody;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body", e);
                            errorMessage = "حدث خطأ أثناء إنشاء الحساب";
                        }
                    } else {
                        errorMessage = "حدث خطأ أثناء إنشاء الحساب";
                    }
                    Log.e(TAG, "Registration failed: " + errorMessage);
                    callback.onError(errorMessage);
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Log.e(TAG, "Registration error: " + t.getMessage(), t);
                String errorMessage;
                if (t instanceof java.net.UnknownHostException) {
                    errorMessage = "لا يمكن الوصول إلى الخادم. يرجى التحقق من اتصال الإنترنت";
                    Log.e(TAG, "Server is unreachable. Check if the server is running and accessible.");
                } else if (t instanceof java.net.SocketTimeoutException) {
                    errorMessage = "انتهت مهلة الاتصال بالخادم. يرجى المحاولة مرة أخرى";
                    Log.e(TAG, "Connection timeout. Server might be overloaded or network is slow.");
                } else if (t instanceof IOException) {
                    errorMessage = "خطأ في الاتصال بالخادم: " + t.getMessage();
                    Log.e(TAG, "IO Exception during connection", t);
                } else {
                    errorMessage = "خطأ غير متوقع: " + t.getMessage();
                    Log.e(TAG, "Unexpected error during registration", t);
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

    public void updateUserNameOnServer(String newName, AuthCallback callback) {
        String token = getToken();
        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("username", newName);

        apiService.updateUserName("Bearer " + token, body).enqueue(new retrofit2.Callback<User>() {
            @Override
            public void onResponse(retrofit2.Call<User> call, retrofit2.Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    saveUserData(response.body());
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("فشل تحديث الاسم في السيرفر");
                }
            }
            @Override
            public void onFailure(retrofit2.Call<User> call, Throwable t) {
                callback.onError("خطأ في الاتصال بالسيرفر");
            }
        });
    }
} 
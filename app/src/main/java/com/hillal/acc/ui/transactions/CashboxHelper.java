package com.hillal.acc.ui.transactions;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import com.hillal.acc.data.entities.Cashbox;
import com.hillal.acc.data.repository.CashboxRepository;
import com.hillal.acc.viewmodel.CashboxViewModel;

/**
 * Utility class for handling cashbox operations
 */
public class CashboxHelper {
    private static final String TAG = "CashboxHelper";

    /**
     * Check if network is available
     */
    public static boolean isNetworkAvailable(Context context) {
        if (context == null) return false;
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * Get authentication token from shared preferences
     */
    public static String getAuthToken(Context context) {
        if (context == null) return null;
        return context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .getString("token", null);
    }

    /**
     * Add cashbox to server with proper validation and error handling
     */
    public static void addCashboxToServer(Context context, CashboxViewModel cashboxViewModel, 
                                        String name, CashboxCallback callback) {
        Log.d(TAG, "Adding cashbox: " + name);

        // Validate context
        if (context == null) {
            callback.onError("خطأ في السياق");
            return;
        }

        // Validate name
        if (name == null || name.trim().isEmpty()) {
            callback.onError("اسم الصندوق مطلوب");
            return;
        }

        // Check network connectivity
        if (!isNetworkAvailable(context)) {
            Log.w(TAG, "No network connection available");
            callback.onError("لا يوجد اتصال بالإنترنت. يرجى التحقق من الاتصال والمحاولة مرة أخرى");
            return;
        }

        // Get authentication token
        String token = getAuthToken(context);
        if (token == null) {
            Log.w(TAG, "No authentication token found");
            callback.onError("يرجى تسجيل الدخول أولاً");
            return;
        }

        Log.d(TAG, "Token found, proceeding with cashbox addition");

        // Add cashbox to server
        cashboxViewModel.addCashboxToServer("Bearer " + token, name.trim(), 
            new CashboxRepository.CashboxCallback() {
                @Override
                public void onSuccess(Cashbox cashbox) {
                    Log.d(TAG, "Cashbox added successfully: id=" + cashbox.id + ", name=" + cashbox.name);
                    callback.onSuccess(cashbox);
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Error adding cashbox: " + error);
                    callback.onError(error);
                }
            });
    }

    /**
     * Show success message
     */
    public static void showSuccessMessage(Context context, String message) {
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show error message
     */
    public static void showErrorMessage(Context context, String error) {
        if (context != null) {
            Toast.makeText(context, "خطأ في إضافة الصندوق: " + error, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Callback interface for cashbox operations
     */
    public interface CashboxCallback {
        void onSuccess(Cashbox cashbox);
        void onError(String error);
    }
} 
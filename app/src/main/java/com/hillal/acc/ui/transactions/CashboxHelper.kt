package com.hillal.acc.ui.transactions

import android.content.Context
import android.net.ConnectivityManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.hillal.acc.data.entities.Cashbox
import com.hillal.acc.data.repository.CashboxRepository
import com.hillal.acc.viewmodel.CashboxViewModel

/**
 * Utility class for handling cashbox operations
 */
object CashboxHelper {
    private const val TAG = "CashboxHelper"
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Check if network is available
     */
    @JvmStatic
    fun isNetworkAvailable(context: Context?): Boolean {
        if (context == null) return false
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.getActiveNetworkInfo()
        return activeNetworkInfo != null && activeNetworkInfo.isConnected()
    }

    /**
     * Get authentication token from shared preferences
     */
    fun getAuthToken(context: Context?): String? {
        if (context == null) return null
        return context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            .getString("token", null)
    }

    /**
     * Add cashbox to server with proper validation and error handling
     */
    @JvmStatic
    fun addCashboxToServer(
        context: Context?, cashboxViewModel: CashboxViewModel,
        name: String?, callback: CashboxCallback
    ) {
        Log.d(TAG, "Adding cashbox: " + name)

        // Validate context
        if (context == null) {
            callback.onError("خطأ في السياق")
            return
        }

        // Validate name
        if (name == null || name.trim { it <= ' ' }.isEmpty()) {
            callback.onError("اسم الصندوق مطلوب")
            return
        }

        // Check network connectivity
        if (!isNetworkAvailable(context)) {
            Log.w(TAG, "No network connection available")
            callback.onError("لا يوجد اتصال بالإنترنت. يرجى التحقق من الاتصال والمحاولة مرة أخرى")
            return
        }

        // Get authentication token
        val token = getAuthToken(context)
        if (token == null) {
            Log.w(TAG, "No authentication token found")
            callback.onError("يرجى تسجيل الدخول أولاً")
            return
        }

        Log.d(TAG, "Token found, proceeding with cashbox addition")

        // Add cashbox to server
        cashboxViewModel.addCashboxToServer(
            "Bearer " + token, name.trim { it <= ' ' },
            object : CashboxRepository.CashboxCallback {
                override fun onSuccess(cashbox: Cashbox) {
                    Log.d(
                        TAG,
                        "Cashbox added successfully: id=" + cashbox.id + ", name=" + cashbox.name
                    )
                    mainHandler.post(Runnable { callback.onSuccess(cashbox) })
                }

                override fun onError(error: String?) {
                    Log.e(TAG, "Error adding cashbox: " + error)
                    mainHandler.post(Runnable { callback.onError(error) })
                }
            })
    }

    /**
     * Show success message
     */
    @JvmStatic
    fun showSuccessMessage(context: Context?, message: String?) {
        if (context != null) {
            mainHandler.post(Runnable {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            })
        }
    }

    /**
     * Show error message
     */
    @JvmStatic
    fun showErrorMessage(context: Context?, error: String?) {
        if (context != null) {
            mainHandler.post(Runnable {
                Toast.makeText(
                    context,
                    "خطأ في إضافة الصندوق: " + error,
                    Toast.LENGTH_LONG
                ).show()
            })
        }
    }

    /**
     * Callback interface for cashbox operations
     */
    interface CashboxCallback {
        fun onSuccess(cashbox: Cashbox?)
        fun onError(error: String?)
    }
}
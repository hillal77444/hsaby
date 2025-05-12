package com.accounting.app

import android.content.Context
import android.webkit.JavascriptInterface
import android.widget.Toast
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
import java.nio.charset.StandardCharsets
import com.google.gson.Gson

class WebAppInterface(private val context: MainActivity, private val dbHelper: DatabaseHelper) {
    private val TAG = "WebAppInterface"
    private val gson = Gson()

    @JavascriptInterface
    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    @JavascriptInterface
    fun onPageLoaded() {
        // يمكن إضافة أي كود مطلوب عند تحميل الصفحة
    }

    @JavascriptInterface
    fun isOnline(): Boolean {
        return context.isOnline()
    }

    @JavascriptInterface
    fun refreshContent() {
        context.refreshContent()
    }

    @JavascriptInterface
    fun login(data: String) {
        try {
            val jsonData = JSONObject(data)
            val username = jsonData.getString("username")
            val phone = jsonData.getString("phone")
            val password = jsonData.getString("password")
            val rememberMe = jsonData.optBoolean("remember_me", false)

            // التحقق من صحة بيانات تسجيل الدخول
            if (dbHelper.verifyLogin(username, password)) {
                // حفظ حالة تسجيل الدخول إذا تم اختيار "تذكرني"
                if (rememberMe) {
                    context.saveLoginState(username, phone)
                }
                showToast("تم تسجيل الدخول بنجاح")
            } else {
                showToast("بيانات تسجيل الدخول غير صحيحة")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during login: ${e.message}")
            showToast("حدث خطأ أثناء تسجيل الدخول")
        }
    }

    @JavascriptInterface
    fun logout() {
        try {
            // مسح حالة تسجيل الدخول
            context.clearLoginState()
            showToast("تم تسجيل الخروج بنجاح")
        } catch (e: Exception) {
            Log.e(TAG, "Error during logout: ${e.message}")
            showToast("حدث خطأ أثناء تسجيل الخروج")
        }
    }

    @JavascriptInterface
    fun updateProfile(data: String) {
        try {
            val jsonData = JSONObject(data)
            val username = jsonData.getString("username")
            val phone = jsonData.getString("phone")

            // حفظ البيانات محلياً
            dbHelper.updateProfile(username, phone)

            // تحديث بيانات تسجيل الدخول المخزنة
            context.saveLoginState(username, phone)

            // إرسال البيانات للسيرفر إذا كان متصلاً
            if (context.isOnline) {
                // TODO: إضافة كود إرسال البيانات للسيرفر
            }

            showToast("تم تحديث الملف الشخصي بنجاح")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating profile: ${e.message}")
            showToast("حدث خطأ أثناء تحديث الملف الشخصي")
        }
    }

    @JavascriptInterface
    fun changePassword(data: String) {
        try {
            val jsonData = JSONObject(data)
            val currentPassword = jsonData.getString("current_password")
            val newPassword = jsonData.getString("new_password")

            // التحقق من كلمة المرور الحالية
            if (!dbHelper.verifyPassword(currentPassword)) {
                showToast("كلمة المرور الحالية غير صحيحة")
                return
            }

            // تحديث كلمة المرور محلياً
            dbHelper.updatePassword(newPassword)

            // إرسال التغيير للسيرفر إذا كان متصلاً
            if (context.isOnline) {
                // TODO: إضافة كود إرسال كلمة المرور الجديدة للسيرفر
            }

            showToast("تم تغيير كلمة المرور بنجاح")
        } catch (e: Exception) {
            Log.e(TAG, "Error changing password: ${e.message}")
            showToast("حدث خطأ أثناء تغيير كلمة المرور")
        }
    }

    @JavascriptInterface
    fun saveAppSettings(data: String) {
        try {
            val jsonData = JSONObject(data)
            val darkMode = jsonData.getBoolean("dark_mode")
            val notifications = jsonData.getBoolean("notifications")
            val syncInterval = jsonData.getInt("sync_interval")

            // حفظ الإعدادات محلياً
            dbHelper.saveAppSettings(darkMode, notifications, syncInterval)

            // إرسال الإعدادات للسيرفر إذا كان متصلاً
            if (context.isOnline) {
                // TODO: إضافة كود إرسال الإعدادات للسيرفر
            }

            showToast("تم حفظ الإعدادات بنجاح")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving app settings: ${e.message}")
            showToast("حدث خطأ أثناء حفظ الإعدادات")
        }
    }

    @JavascriptInterface
    fun clearCache() {
        try {
            // مسح الذاكرة المؤقتة
            context.cacheDir.deleteRecursively()
            showToast("تم مسح الذاكرة المؤقتة بنجاح")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache: ${e.message}")
            showToast("حدث خطأ أثناء مسح الذاكرة المؤقتة")
        }
    }

    @JavascriptInterface
    fun deleteAccount() {
        try {
            // حذف البيانات المحلية
            dbHelper.deleteAllData()
            // مسح حالة تسجيل الدخول
            context.clearLoginState()

            // حذف الحساب من السيرفر إذا كان متصلاً
            if (context.isOnline) {
                // TODO: إضافة كود حذف الحساب من السيرفر
            }

            showToast("تم حذف الحساب بنجاح")
            // TODO: إعادة توجيه المستخدم لصفحة تسجيل الدخول
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting account: ${e.message}")
            showToast("حدث خطأ أثناء حذف الحساب")
        }
    }

    @JavascriptInterface
    fun loadSettings(): String {
        try {
            val settings = dbHelper.getAppSettings()
            return settings.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading settings: ${e.message}")
            return "{}"
        }
    }

    @JavascriptInterface
    fun syncData() {
        try {
            if (context.isOnline) {
                // مزامنة البيانات مع السيرفر
                // TODO: إضافة كود المزامنة
                showToast("تمت المزامنة بنجاح")
            } else {
                showToast("لا يوجد اتصال بالإنترنت")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing data: ${e.message}")
            showToast("حدث خطأ أثناء المزامنة")
        }
    }

    // وظائف الحسابات
    @JavascriptInterface
    fun addAccount(data: String): String {
        try {
            val jsonData = JSONObject(data)
            val accountNumber = jsonData.getString("account_number")
            val accountName = jsonData.getString("account_name")
            val balance = jsonData.getDouble("balance")
            val phoneNumber = jsonData.optString("phone_number")
            val isDebtor = jsonData.optBoolean("is_debtor", false)
            val notes = jsonData.optString("notes")

            val accountId = dbHelper.addAccount(accountNumber, accountName, balance, phoneNumber, isDebtor, notes)
            return JSONObject().apply {
                put("success", true)
                put("account_id", accountId)
            }.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error adding account: ${e.message}")
            return JSONObject().apply {
                put("success", false)
                put("error", "حدث خطأ أثناء إضافة الحساب")
            }.toString()
        }
    }

    @JavascriptInterface
    fun updateAccount(data: String): String {
        try {
            val jsonData = JSONObject(data)
            val accountId = jsonData.getLong("id")
            val accountNumber = jsonData.getString("account_number")
            val accountName = jsonData.getString("account_name")
            val balance = jsonData.getDouble("balance")
            val phoneNumber = jsonData.optString("phone_number")
            val isDebtor = jsonData.optBoolean("is_debtor", false)
            val notes = jsonData.optString("notes")

            dbHelper.updateAccount(accountId, accountNumber, accountName, balance, phoneNumber, isDebtor, notes)
            return JSONObject().apply {
                put("success", true)
            }.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating account: ${e.message}")
            return JSONObject().apply {
                put("success", false)
                put("error", "حدث خطأ أثناء تحديث الحساب")
            }.toString()
        }
    }

    @JavascriptInterface
    fun deleteAccount(accountId: Long): String {
        try {
            dbHelper.deleteAccount(accountId)
            return JSONObject().apply {
                put("success", true)
            }.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting account: ${e.message}")
            return JSONObject().apply {
                put("success", false)
                put("error", "حدث خطأ أثناء حذف الحساب")
            }.toString()
        }
    }

    @JavascriptInterface
    fun getAccount(accountId: Long): String {
        try {
            val account = dbHelper.getAccount(accountId)
            return if (account != null) {
                JSONObject().apply {
                    put("success", true)
                    put("account", account)
                }.toString()
            } else {
                JSONObject().apply {
                    put("success", false)
                    put("error", "الحساب غير موجود")
                }.toString()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting account: ${e.message}")
            return JSONObject().apply {
                put("success", false)
                put("error", "حدث خطأ أثناء جلب بيانات الحساب")
            }.toString()
        }
    }

    @JavascriptInterface
    fun getAllAccounts(): String {
        try {
            val accounts = dbHelper.getAllAccounts()
            return JSONObject().apply {
                put("success", true)
                put("accounts", accounts)
            }.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all accounts: ${e.message}")
            return JSONObject().apply {
                put("success", false)
                put("error", "حدث خطأ أثناء جلب قائمة الحسابات")
            }.toString()
        }
    }

    // وظائف المعاملات
    @JavascriptInterface
    fun addTransaction(data: String): String {
        try {
            val jsonData = JSONObject(data)
            val accountId = jsonData.getLong("account_id")
            val date = jsonData.getLong("date")
            val amount = jsonData.getDouble("amount")
            val description = jsonData.getString("description")
            val type = jsonData.getString("type")
            val currency = jsonData.getString("currency")
            val notes = jsonData.optString("notes")

            val transactionId = dbHelper.addTransaction(accountId, date, amount, description, type, currency, notes)
            return JSONObject().apply {
                put("success", true)
                put("transaction_id", transactionId)
            }.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error adding transaction: ${e.message}")
            return JSONObject().apply {
                put("success", false)
                put("error", "حدث خطأ أثناء إضافة المعاملة")
            }.toString()
        }
    }

    @JavascriptInterface
    fun updateTransaction(data: String): String {
        try {
            val jsonData = JSONObject(data)
            val transactionId = jsonData.getLong("id")
            val date = jsonData.getLong("date")
            val amount = jsonData.getDouble("amount")
            val description = jsonData.getString("description")
            val type = jsonData.getString("type")
            val currency = jsonData.getString("currency")
            val notes = jsonData.optString("notes")

            dbHelper.updateTransaction(transactionId, date, amount, description, type, currency, notes)
            return JSONObject().apply {
                put("success", true)
            }.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating transaction: ${e.message}")
            return JSONObject().apply {
                put("success", false)
                put("error", "حدث خطأ أثناء تحديث المعاملة")
            }.toString()
        }
    }

    @JavascriptInterface
    fun deleteTransaction(transactionId: Long): String {
        try {
            dbHelper.deleteTransaction(transactionId)
            return JSONObject().apply {
                put("success", true)
            }.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting transaction: ${e.message}")
            return JSONObject().apply {
                put("success", false)
                put("error", "حدث خطأ أثناء حذف المعاملة")
            }.toString()
        }
    }

    @JavascriptInterface
    fun getTransaction(transactionId: Long): String {
        try {
            val transaction = dbHelper.getTransaction(transactionId)
            return if (transaction != null) {
                JSONObject().apply {
                    put("success", true)
                    put("transaction", transaction)
                }.toString()
            } else {
                JSONObject().apply {
                    put("success", false)
                    put("error", "المعاملة غير موجودة")
                }.toString()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting transaction: ${e.message}")
            return JSONObject().apply {
                put("success", false)
                put("error", "حدث خطأ أثناء جلب بيانات المعاملة")
            }.toString()
        }
    }

    @JavascriptInterface
    fun getAccountTransactions(accountId: Long): String {
        try {
            val transactions = dbHelper.getAccountTransactions(accountId)
            return JSONObject().apply {
                put("success", true)
                put("transactions", transactions)
            }.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting account transactions: ${e.message}")
            return JSONObject().apply {
                put("success", false)
                put("error", "حدث خطأ أثناء جلب معاملات الحساب")
            }.toString()
        }
    }

    @JavascriptInterface
    fun updateUI(data: String) {
        try {
            val jsonData = JSONObject(data)
            val uiVersion = jsonData.getInt("ui_version")
            val cssUrl = jsonData.getString("css_url")
            val jsUrl = jsonData.getString("js_url")
            val htmlUrl = jsonData.getString("html_url")

            // حفظ إصدار واجهة المستخدم الحالي
            dbHelper.saveUIVersion(uiVersion)

            // تحميل وتطبيق التحديثات
            if (context.isOnline) {
                // تحميل ملفات CSS و JavaScript
                downloadAndSaveFile(cssUrl, "styles.css")
                downloadAndSaveFile(jsUrl, "app.js")
                downloadAndSaveFile(htmlUrl, "index.html")

                // تحديث واجهة المستخدم
                context.refreshContent()
                showToast("تم تحديث واجهة التطبيق بنجاح")
            } else {
                showToast("لا يوجد اتصال بالإنترنت")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating UI: ${e.message}")
            showToast("حدث خطأ أثناء تحديث واجهة التطبيق")
        }
    }

    @JavascriptInterface
    fun getUIVersion(): String {
        try {
            val version = dbHelper.getUIVersion()
            return JSONObject().apply {
                put("success", true)
                put("version", version)
            }.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting UI version: ${e.message}")
            return JSONObject().apply {
                put("success", false)
                put("error", "حدث خطأ أثناء جلب إصدار واجهة المستخدم")
            }.toString()
        }
    }

    private fun downloadAndSaveFile(url: String, fileName: String) {
        try {
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.connect()

            val inputStream = connection.inputStream
            val file = File(context.filesDir, fileName)
            val outputStream = FileOutputStream(file)

            inputStream.copyTo(outputStream)
            outputStream.close()
            inputStream.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading file $fileName: ${e.message}")
            throw e
        }
    }

    @JavascriptInterface
    fun clearUICache() {
        try {
            // حذف ملفات CSS و JavaScript المخزنة
            File(context.filesDir, "styles.css").delete()
            File(context.filesDir, "app.js").delete()
            File(context.filesDir, "index.html").delete()

            // إعادة تحميل الصفحة
            context.refreshContent()
            showToast("تم مسح ذاكرة واجهة المستخدم بنجاح")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing UI cache: ${e.message}")
            showToast("حدث خطأ أثناء مسح ذاكرة واجهة المستخدم")
        }
    }
} 
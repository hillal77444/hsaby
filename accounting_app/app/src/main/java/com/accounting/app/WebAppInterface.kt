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
import org.json.JSONArray
import com.accounting.app.api.ApiClient
import com.accounting.app.models.SyncResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.content.SharedPreferences
import com.accounting.app.models.LoginRequest

class WebAppInterface(private val context: MainActivity, private val dbHelper: DatabaseHelper, private val sharedPreferences: SharedPreferences) {
    private val TAG = "WebAppInterface"
    private val gson = Gson()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    companion object {
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_USERNAME = "username"
    }

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
    fun login(username: String, password: String) {
        coroutineScope.launch {
            try {
                val response = ApiClient.getApiService().login(LoginRequest(username, password))
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse != null && loginResponse.success) {
                        // حفظ حالة تسجيل الدخول
                        sharedPreferences.edit().apply {
                            putBoolean(KEY_IS_LOGGED_IN, true)
                            putString(KEY_USERNAME, username)
                            apply()
                        }
                        Toast.makeText(context, "تم تسجيل الدخول بنجاح", Toast.LENGTH_SHORT).show()
                        context.loadDashboard()
                    } else {
                        Toast.makeText(context, "فشل تسجيل الدخول: ${loginResponse?.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "فشل تسجيل الدخول: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ في الاتصال: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @JavascriptInterface
    fun register(username: String, phone: String, password: String): Boolean {
        val passwordHash = dbHelper.hashPassword(password)
        return dbHelper.addUser(username, phone, passwordHash)
    }

    @JavascriptInterface
    fun logout(): String {
        return try {
            context.clearLoginState()
            gson.toJson(mapOf(
                "success" to true,
                "message" to "تم تسجيل الخروج بنجاح"
            ))
        } catch (e: Exception) {
            gson.toJson(mapOf(
                "success" to false,
                "message" to "حدث خطأ أثناء تسجيل الخروج"
            ))
        }
    }

    @JavascriptInterface
    fun getAccount(accountId: Long): String {
        val account = dbHelper.getAccount(accountId)
        return account?.toString() ?: "{}"
    }

    @JavascriptInterface
    fun getAllAccounts(): String {
        return dbHelper.getAllAccounts().toString()
    }

    @JavascriptInterface
    fun addAccount(accountJson: String): Boolean {
        val account = JSONObject(accountJson)
        val newAccount = Account(
            accountNumber = account.getString("account_number"),
            accountName = account.getString("account_name"),
            balance = account.getDouble("balance"),
            phoneNumber = if (account.has("phone_number")) account.getString("phone_number") else null,
            isDebtor = account.getBoolean("is_debtor"),
            notes = if (account.has("notes")) account.getString("notes") else null
        )
        return dbHelper.addAccount(newAccount)
    }

    @JavascriptInterface
    fun updateAccount(accountJson: String): Boolean {
        val account = JSONObject(accountJson)
        val updatedAccount = Account(
            id = account.getLong("id"),
            accountNumber = account.getString("account_number"),
            accountName = account.getString("account_name"),
            balance = account.getDouble("balance"),
            phoneNumber = if (account.has("phone_number")) account.getString("phone_number") else null,
            isDebtor = account.getBoolean("is_debtor"),
            notes = if (account.has("notes")) account.getString("notes") else null
        )
        return dbHelper.updateAccount(updatedAccount)
    }

    @JavascriptInterface
    fun deleteAccount(accountId: Long): Boolean {
        return dbHelper.deleteAccount(accountId)
    }

    @JavascriptInterface
    fun getTransactionsForAccount(accountId: Long): String {
        val transactions = dbHelper.getTransactionsForAccount(accountId)
        val jsonArray = JSONArray()
        transactions.forEach { transaction ->
            val json = JSONObject().apply {
                put("id", transaction.id)
                put("date", transaction.date)
                put("amount", transaction.amount)
                put("description", transaction.description)
                put("type", transaction.type)
                put("currency", transaction.currency)
                put("notes", transaction.notes)
                put("account_id", transaction.accountId)
            }
            jsonArray.put(json)
        }
        return jsonArray.toString()
    }

    @JavascriptInterface
    fun addTransaction(transactionJson: String): Boolean {
        val transaction = JSONObject(transactionJson)
        val newTransaction = Transaction(
            date = transaction.getLong("date"),
            amount = transaction.getDouble("amount"),
            description = transaction.getString("description"),
            type = transaction.getString("type"),
            currency = transaction.getString("currency"),
            notes = if (transaction.has("notes")) transaction.getString("notes") else null,
            accountId = transaction.getLong("account_id")
        )
        return dbHelper.addTransaction(newTransaction)
    }

    @JavascriptInterface
    fun updateTransaction(transactionJson: String): Boolean {
        val transaction = JSONObject(transactionJson)
        val updatedTransaction = Transaction(
            id = transaction.getLong("id"),
            date = transaction.getLong("date"),
            amount = transaction.getDouble("amount"),
            description = transaction.getString("description"),
            type = transaction.getString("type"),
            currency = transaction.getString("currency"),
            notes = if (transaction.has("notes")) transaction.getString("notes") else null,
            accountId = transaction.getLong("account_id")
        )
        return dbHelper.updateTransaction(updatedTransaction)
    }

    @JavascriptInterface
    fun deleteTransaction(transactionId: Long): Boolean {
        return dbHelper.deleteTransaction(transactionId)
    }

    @JavascriptInterface
    fun getSettings(): String {
        val settings = dbHelper.getAllSettings()
        val json = JSONObject()
        settings.forEach { (key, value) ->
            json.put(key, value)
        }
        return json.toString()
    }

    @JavascriptInterface
    fun updateSettings(settingsJson: String): Boolean {
        val json = JSONObject(settingsJson)
        val settings = mutableMapOf<String, String>()
        json.keys().forEach { key ->
            settings[key] = json.getString(key)
        }
        return dbHelper.updateSettings(settings)
    }

    @JavascriptInterface
    fun syncData(): Boolean {
        return try {
            val syncData = dbHelper.syncData()
            val token = context.getAuthToken() ?: return false
            
            coroutineScope.launch {
                try {
                    val response = ApiClient.getApiService().syncData("Bearer $token", syncData)
                    if (response.isSuccessful) {
                        response.body()?.let { apiResponse ->
                            if (apiResponse.success) {
                                apiResponse.data?.let { syncResponse ->
                                    context.updateLocalData(syncResponse)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing data: ${e.message}")
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing sync data: ${e.message}")
            false
        }
    }

    @JavascriptInterface
    fun loadAccountsPage() {
        context.loadAccountsPage()
    }

    @JavascriptInterface
    fun loadEntriesPage() {
        context.loadEntriesPage()
    }

    @JavascriptInterface
    fun loadReportsPage() {
        context.loadReportsPage()
    }

    @JavascriptInterface
    fun getFilteredEntries(accountId: Long?, dateFrom: Long?, dateTo: Long?, currency: String?): String {
        val entries = dbHelper.getFilteredEntries(accountId, dateFrom, dateTo, currency)
        return Gson().toJson(entries)
    }
} 
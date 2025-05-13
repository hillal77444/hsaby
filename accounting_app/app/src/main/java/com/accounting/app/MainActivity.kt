package com.accounting.app

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import android.webkit.CookieManager
import android.net.ConnectivityManager
import android.content.Context
import android.net.NetworkCapabilities
import android.content.SharedPreferences
import android.Manifest
import android.os.Build
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.lifecycle.lifecycleScope
import com.accounting.app.api.ApiService
import com.accounting.app.api.ApiClient
import com.accounting.app.models.*
import java.io.File
import kotlinx.coroutines.launch
import android.widget.Toast
import android.database.sqlite.SQLiteDatabase
import android.content.ContentValues
import android.util.Log
import org.json.JSONObject
import org.json.JSONArray
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var dbHelper: DatabaseHelper
    private var isFirstLoad = true
    private var lastSyncTime: Long = 0
    private val SYNC_INTERVAL = 5 * 60 * 1000 // 5 minutes
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var apiService: ApiService
    private var authToken: String? = null

    companion object {
        private const val TAG = "MainActivity"
        private const val PREFS_NAME = "AccountingAppPrefs"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_USERNAME = "username"
        private const val KEY_PHONE = "phone"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize database
        dbHelper = DatabaseHelper(this)

        // Initialize WebView and SwipeRefreshLayout
        webView = findViewById(R.id.webView)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        // تهيئة SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // تهيئة API Service
        apiService = ApiClient.getApiService(this)

        setupWebView()
        setupSwipeRefresh()
        setupBackPress()
        checkPermissions()
        loadContent()
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                swipeRefreshLayout.isRefreshing = false
                if (isFirstLoad) {
                    isFirstLoad = false
                    injectJavaScript()
                }
            }
        }

        webView.addJavascriptInterface(WebAppInterface(this, dbHelper), "Android")
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            refreshContent()
        }
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    fun refreshContent() {
        if (isOnline()) {
            webView.reload()
        } else {
            loadLocalContent()
        }
    }

    fun isOnline(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
        return actNw.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun loadContent() {
        if (isLoggedIn()) {
            // المستخدم مسجل دخوله
            if (isOnline()) {
                loadRemoteContent()
            } else {
                loadLocalContent()
            }
        } else {
            // المستخدم غير مسجل دخوله
            if (isOnline()) {
                webView.loadUrl("http://212.224.88.122:5007/login")
            } else {
                webView.loadUrl("file:///android_asset/login.html")
            }
        }
    }

    private fun loadRemoteContent() {
        webView.loadUrl("http://212.224.88.122:5007/dashboard")
    }

    private fun loadLocalContent() {
        val localFile = File(filesDir, "dashboard.html")
        if (localFile.exists()) {
            webView.loadUrl("file://${localFile.absolutePath}")
        } else {
            webView.loadUrl("file:///android_asset/dashboard.html")
        }
    }

    private fun injectJavaScript() {
        val js = """
            window.addEventListener('load', function() {
                if (typeof Android !== 'undefined') {
                    Android.onPageLoaded();
                }
            });
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.INTERNET,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                1
            )
        }
    }

    // وظائف حفظ تسجيل الدخول
    fun saveLoginState(username: String, phone: String) {
        sharedPreferences.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USERNAME, username)
            putString(KEY_PHONE, phone)
            apply()
        }
    }

    fun clearLoginState() {
        sharedPreferences.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_USERNAME)
            remove(KEY_PHONE)
            apply()
        }
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getStoredUsername(): String {
        return sharedPreferences.getString(KEY_USERNAME, "") ?: ""
    }

    fun getStoredPhone(): String {
        return sharedPreferences.getString(KEY_PHONE, "") ?: ""
    }

    private fun login(username: String, password: String) {
        lifecycleScope.launch {
            try {
                val response = apiService.login(LoginRequest(username, password))
                if (response.isSuccessful && response.body()?.success == true) {
                    authToken = response.body()?.data?.token
                    // حفظ التوكن في SharedPreferences
                    saveAuthToken(authToken!!)
                    // تحديث واجهة المستخدم
                    updateUIAfterLogin()
                } else {
                    showError("فشل تسجيل الدخول")
                }
            } catch (e: Exception) {
                showError("خطأ في الاتصال بالسيرفر")
            }
        }
    }

    private fun register(username: String, phone: String, password: String) {
        lifecycleScope.launch {
            try {
                val user = com.accounting.app.models.User(
                    username = username,
                    phone = phone,
                    passwordHash = password
                )
                val response = apiService.register(user)
                if (response.isSuccessful && response.body()?.success == true) {
                    showSuccess("تم التسجيل بنجاح")
                } else {
                    showError("فشل التسجيل")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error registering: ${e.message}")
                showError("خطأ في الاتصال بالسيرفر")
            }
        }
    }

    private fun syncData() {
        lifecycleScope.launch {
            try {
                val token = getAuthToken() ?: return@launch
                val db = DatabaseHelper(this@MainActivity)
                
                // تحويل JSONArray إلى List<Account>
                val accountsArray = db.getAllAccounts()
                val accounts = mutableListOf<com.accounting.app.models.Account>()
                for (i in 0 until accountsArray.length()) {
                    val jsonAccount = accountsArray.getJSONObject(i)
                    accounts.add(jsonToAccount(jsonAccount))
                }
                
                val transactions = db.getAllTransactions().map { transaction ->
                    com.accounting.app.models.Transaction(
                        id = transaction.id,
                        date = transaction.date,
                        amount = transaction.amount,
                        description = transaction.description,
                        type = transaction.type,
                        currency = transaction.currency,
                        notes = transaction.notes,
                        accountId = transaction.accountId
                    )
                }
                
                val syncData = SyncData(
                    accounts = accounts,
                    transactions = transactions,
                    lastSyncTimestamp = db.getLastSyncTimestamp()
                )
                
                val response = apiService.syncData("Bearer $token", syncData)
                if (response.isSuccessful && response.body()?.success == true) {
                    // تحديث البيانات المحلية
                    response.body()?.data?.let { syncResponse ->
                        updateLocalData(syncResponse)
                    }
                    showSuccess("تمت المزامنة بنجاح")
                } else {
                    showError("فشلت المزامنة")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing data: ${e.message}")
                showError("خطأ في الاتصال بالسيرفر")
            }
        }
    }

    private fun jsonToAccount(json: JSONObject): com.accounting.app.models.Account {
        return com.accounting.app.models.Account(
            id = json.getLong("id"),
            accountNumber = json.getString("account_number"),
            accountName = json.getString("account_name"),
            balance = json.getDouble("balance"),
            phoneNumber = if (json.has("phone_number")) json.getString("phone_number") else null,
            isDebtor = json.getBoolean("is_debtor"),
            notes = if (json.has("notes")) json.getString("notes") else null,
            createdAt = json.getLong("created_at"),
            updatedAt = json.getLong("updated_at")
        )
    }

    private fun updateLocalData(syncResponse: SyncResponse) {
        try {
            val db = DatabaseHelper(this)
            
            // تحديث الحسابات
            syncResponse.accounts.forEach { account ->
                val values = ContentValues().apply {
                    put("account_number", account.accountNumber)
                    put("account_name", account.accountName)
                    put("balance", account.balance)
                    put("phone_number", account.phoneNumber)
                    put("is_debtor", if (account.isDebtor) 1 else 0)
                    put("notes", account.notes)
                    put("created_at", account.createdAt)
                    put("updated_at", account.updatedAt)
                }
                
                db.writableDatabase.insertWithOnConflict(
                    "accounts",
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            
            // تحديث المعاملات
            syncResponse.transactions.forEach { transaction ->
                val values = ContentValues().apply {
                    put("date", transaction.date)
                    put("amount", transaction.amount)
                    put("description", transaction.description)
                    put("type", transaction.type)
                    put("currency", transaction.currency)
                    put("notes", transaction.notes)
                    put("account_id", transaction.accountId)
                }
                
                db.writableDatabase.insertWithOnConflict(
                    "transactions",
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            
            // تحديث timestamp المزامنة
            db.updateLastSyncTimestamp(syncResponse.syncTimestamp)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating local data: ${e.message}")
        }
    }

    private fun saveAuthToken(token: String) {
        getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("auth_token", token)
            .apply()
    }

    private fun getAuthToken(): String? {
        return getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            .getString("auth_token", null)
    }

    private fun clearAuthToken() {
        getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            .edit()
            .remove("auth_token")
            .apply()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun updateUIAfterLogin() {
        // تحديث واجهة المستخدم بعد تسجيل الدخول
        webView.loadUrl("http://212.224.88.122:5007/dashboard")
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }
} 
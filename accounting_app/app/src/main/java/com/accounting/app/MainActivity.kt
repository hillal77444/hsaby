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
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var dbHelper: DatabaseHelper
    private var isFirstLoad = true
    private var lastSyncTime: Long = 0
    private val SYNC_INTERVAL = 5 * 60 * 1000 // 5 minutes
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
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

        // Initialize WebView
        webView = findViewById(R.id.webView)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        // تهيئة SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        setupWebView()
        setupSwipeRefresh()
        checkPermissions()
        loadContent()
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
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
        if (isOnline()) {
            loadRemoteContent()
        } else {
            loadLocalContent()
        }
    }

    private fun loadRemoteContent() {
        webView.loadUrl("https://accounting.example.com")
    }

    private fun loadLocalContent() {
        val localFile = File(filesDir, "index.html")
        if (localFile.exists()) {
            webView.loadUrl("file://${localFile.absolutePath}")
        } else {
            webView.loadUrl("file:///android_asset/index.html")
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
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
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

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }
} 
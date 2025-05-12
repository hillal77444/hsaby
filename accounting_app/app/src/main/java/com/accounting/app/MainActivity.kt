package com.accounting.app

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.net.Uri
import android.content.Intent
import android.os.Environment
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewAssetLoader.AssetsPathHandler
import androidx.webkit.WebViewAssetLoader.ResourcesPathHandler
import androidx.webkit.WebViewAssetLoader.InternalStoragePathHandler
import java.io.File
import android.util.Log
import android.widget.Toast
import android.net.ConnectivityManager
import android.content.Context
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ProgressBar
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import android.content.SharedPreferences

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private var isOnline = false
    private val handler = Handler(Looper.getMainLooper())
    private val syncInterval = 15 * 60 * 1000 // 15 minutes
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

        // تهيئة SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // تهيئة WebView
        webView = findViewById(R.id.webView)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        progressBar = findViewById(R.id.progressBar)

        setupWebView()
        setupSwipeRefresh()
        checkConnectivity()
        startSyncService()
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
            databaseEnabled = true
            setGeolocationEnabled(true)
            mediaPlaybackRequiresUserGesture = false
        }

        // إعداد معالجات المسارات
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", AssetsPathHandler(this))
            .addPathHandler("/res/", ResourcesPathHandler(this))
            .addPathHandler("/internal/", InternalStoragePathHandler(this, "internal"))
            .build()

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false

                // التحقق من حالة تسجيل الدخول
                if (isLoggedIn()) {
                    // إرسال بيانات المستخدم المخزنة إلى JavaScript
                    val userData = """
                        {
                            "username": "${getStoredUsername()}",
                            "phone": "${getStoredPhone()}"
                        }
                    """.trimIndent()
                    webView.evaluateJavascript(
                        "window.dispatchEvent(new CustomEvent('userData', { detail: $userData }));",
                        null
                    )
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                // معالجة اختيار الملفات
                return true
            }
        }

        // إضافة واجهة JavaScript
        webView.addJavascriptInterface(WebAppInterface(this), "Android")

        // تحميل الصفحة الرئيسية
        loadInitialPage()
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            refreshContent()
        }
    }

    private fun loadInitialPage() {
        progressBar.visibility = View.VISIBLE
        if (isOnline) {
            webView.loadUrl("http://your-server-url/")
        } else {
            webView.loadUrl("file:///android_asset/offline/index.html")
        }
    }

    private fun refreshContent() {
        if (isOnline) {
            webView.reload()
        } else {
            Toast.makeText(this, "لا يوجد اتصال بالإنترنت", Toast.LENGTH_SHORT).show()
            swipeRefresh.isRefreshing = false
        }
    }

    private fun checkConnectivity() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        
        isOnline = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        
        if (isOnline) {
            syncData()
        }
    }

    private fun startSyncService() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (isOnline) {
                    syncData()
                }
                handler.postDelayed(this, syncInterval.toLong())
            }
        }, syncInterval.toLong())
    }

    private fun syncData() {
        // تنفيذ المزامنة مع السيرفر
        // TODO: إضافة كود المزامنة
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
        handler.removeCallbacksAndMessages(null)
    }
} 
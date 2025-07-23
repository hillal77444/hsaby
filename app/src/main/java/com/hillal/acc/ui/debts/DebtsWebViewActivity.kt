package com.hillal.acc.ui.debts

import android.app.ProgressDialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.hillal.acc.data.preferences.UserPreferences

class DebtsWebViewActivity : AppCompatActivity() {
    private var progressDialog: ProgressDialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val phone = UserPreferences(this).phoneNumber ?: UserPreferences(this).getPhoneNumber()
        val webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.builtInZoomControls = true
        webView.settings.displayZoomControls = false
        webView.settings.setSupportZoom(true)
        progressDialog = ProgressDialog(this).apply {
            setMessage("جاري التحميل...")
            setCancelable(false)
            show()
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                progressDialog?.dismiss()
            }
            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                progressDialog?.dismiss()
            }
        }
        webView.webChromeClient = WebChromeClient()
        if (isInternetAvailable(this)) {
            webView.loadUrl("https://malyp.com/api/account_summary/$phone")
        } else {
            val errorHtml = """
                <html>
                <body style='text-align:center;direction:rtl;padding-top:40px;'>
                    <h2>لا يوجد اتصال بالإنترنت</h2>
                    <p>يرجى التحقق من اتصالك وحاول مرة أخرى.</p>
                </body>
                </html>
            """
            webView.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null)
            progressDialog?.dismiss()
        }
        setContentView(webView)
    }

    override fun onDestroy() {
        progressDialog?.dismiss()
        super.onDestroy()
    }

    private fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }
    }
} 
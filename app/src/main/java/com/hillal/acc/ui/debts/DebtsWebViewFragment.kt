package com.hillal.acc.ui.debts

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.hillal.acc.data.preferences.UserPreferences

class DebtsWebViewFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val context = requireContext()
        val phone = UserPreferences(context).phoneNumber ?: UserPreferences(context).getPhoneNumber()
        val webView = WebView(context)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()

        if (isInternetAvailable(context)) {
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
        }
        return webView
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
package com.hillal.acc.ui.report

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.hillal.acc.databinding.FragmentAccountReportBinding
import com.hillal.acc.models.AccountReport
import com.hillal.acc.network.ApiService
import com.hillal.acc.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.text.NumberFormat
import java.util.Collections
import java.util.Locale

class AccountReportFragment : Fragment() {
    private var binding: FragmentAccountReportBinding? = null
    private var apiService: ApiService? = null
    private var numberFormat: NumberFormat? = null
    private var accountId = 0
    private var currency: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (getArguments() != null) {
            accountId = getArguments()!!.getInt("accountId")
            currency = getArguments()!!.getString("currency")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAccountReportBinding.inflate(inflater, container, false)
        return binding!!.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupNumberFormat()
        setupApiService()
        setupWebView()
        loadAccountReport()

        // ضبط insets للجذر لرفع المحتوى مع الكيبورد وأزرار النظام
        ViewCompat.setOnApplyWindowInsetsListener(
            binding!!.getRoot(),
            OnApplyWindowInsetsListener { v: View?, insets: WindowInsetsCompat? ->
                var bottom = insets!!.getInsets(WindowInsetsCompat.Type.ime()).bottom
                if (bottom == 0) {
                    bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
                }
                v!!.setPadding(
                    v.getPaddingLeft(),
                    v.getPaddingTop(),
                    v.getPaddingRight(),
                    bottom
                )
                insets
            })
    }

    private fun setupNumberFormat() {
        numberFormat = NumberFormat.getNumberInstance(Locale.US)
        numberFormat!!.setMinimumFractionDigits(0)
        numberFormat!!.setMaximumFractionDigits(2)
    }

    private fun setupApiService() {
        apiService = RetrofitClient.getInstance().getApiService()
    }

    private fun setupWebView() {
        if (binding != null && binding!!.reportWebView != null) {
            Log.d("AccountReport", "Setting up WebView")
            binding!!.reportWebView.getSettings().setJavaScriptEnabled(true)
            binding!!.reportWebView.getSettings().setDomStorageEnabled(true)
            binding!!.reportWebView.getSettings().setLoadWithOverviewMode(true)
            binding!!.reportWebView.getSettings().setUseWideViewPort(true)
            binding!!.reportWebView.getSettings().setBuiltInZoomControls(true)
            binding!!.reportWebView.getSettings().setDisplayZoomControls(false)
            binding!!.reportWebView.setInitialScale(1)


            // إضافة padding في أسفل WebView
            binding!!.reportWebView.setPadding(0, 0, 0, 80)


            // إضافة WebViewClient للتعامل مع الأخطاء
            binding!!.reportWebView.setWebViewClient(object : WebViewClient() {
                override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                    Log.e("AccountReport", "WebView error: " + description)
                    showError("خطأ في تحميل التقرير: " + description)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    Log.d("AccountReport", "WebView page loaded successfully")
                }
            })

            Log.d("AccountReport", "WebView setup completed")
        } else {
            Log.e("AccountReport", "WebView or binding is null during setup")
        }
    }

    private fun loadAccountReport() {
        if (accountId <= 0 || currency == null) {
            showError("بيانات الحساب غير صحيحة")
            return
        }

        Log.d(
            "AccountReport",
            "Starting to load account report for ID: " + accountId + ", Currency: " + currency
        )
        binding!!.progressBar.setVisibility(View.VISIBLE)

        try {
            // تجربة الـ endpoint الأول
            val call = apiService!!.getAccountDetails(accountId, currency)
            val requestUrl = call.request().url().toString()
            Log.d("AccountReport", "Making request to URL: " + requestUrl)

            call.enqueue(object : Callback<AccountReport?> {
                override fun onResponse(
                    call: Call<AccountReport?>,
                    response: Response<AccountReport?>
                ) {
                    binding!!.progressBar.setVisibility(View.GONE)
                    Log.d("AccountReport", "Received response with code: " + response.code())

                    if (response.isSuccessful()) {
                        try {
                            val responseBody =
                                if (response.body() != null) response.body().toString() else "null"
                            Log.d("AccountReport", "Raw response body: " + responseBody)

                            val report = response.body()
                            if (report == null) {
                                Log.e("AccountReport", "Response body is null")
                                // تجربة الـ endpoint الثاني
                                tryAlternativeEndpoint()
                                return
                            }

                            // التحقق من البيانات المستلمة
                            if (report.getTransactions() == null) {
                                Log.e("AccountReport", "Transactions list is null")
                                showError("لا توجد بيانات المعاملات")
                                return
                            }

                            // التحقق من البيانات الأساسية
                            if (report.getAccountId() <= 0) {
                                Log.e("AccountReport", "Invalid account ID in response")
                                showError("بيانات الحساب غير صحيحة")
                                return
                            }

                            if (report.getCurrency() == null || report.getCurrency().isEmpty()) {
                                Log.e("AccountReport", "Currency is null or empty")
                                showError("العملة غير محددة")
                                return
                            }

                            // تحديث العرض في الـ UI thread
                            if (getActivity() != null) {
                                Log.d("AccountReport", "Updating UI with report data")
                                getActivity()!!.runOnUiThread(Runnable {
                                    try {
                                        updateReportView(report)
                                    } catch (e: Exception) {
                                        Log.e("AccountReport", "Error updating report view", e)
                                        showError("خطأ في تحديث العرض: " + e.message)
                                    }
                                })
                            } else {
                                Log.e("AccountReport", "Activity is null")
                                showError("خطأ في تحديث واجهة المستخدم")
                            }
                        } catch (e: Exception) {
                            Log.e("AccountReport", "Error processing response", e)
                            showError("خطأ في معالجة البيانات: " + e.message)
                        }
                    } else {
                        var errorMessage = "فشل في تحميل البيانات"
                        try {
                            if (response.errorBody() != null) {
                                val errorBody = response.errorBody()!!.string()
                                Log.e("AccountReport", "Error response body: " + errorBody)
                                errorMessage += ": " + errorBody
                            } else {
                                Log.e("AccountReport", "Error response code: " + response.code())
                                errorMessage += " (رمز الخطأ: " + response.code() + ")"
                            }
                        } catch (e: IOException) {
                            Log.e("AccountReport", "Error reading error body", e)
                            errorMessage += " (رمز الخطأ: " + response.code() + ")"
                        }
                        showError(errorMessage)
                    }
                }

                override fun onFailure(call: Call<AccountReport?>, t: Throwable) {
                    Log.e("AccountReport", "Network request failed", t)
                    binding!!.progressBar.setVisibility(View.GONE)
                    showError("حدث خطأ في الاتصال: " + t.message)
                }
            })
        } catch (e: Exception) {
            Log.e("AccountReport", "Error making request", e)
            binding!!.progressBar.setVisibility(View.GONE)
            showError("خطأ في إرسال الطلب: " + e.message)
        }
    }

    private fun tryAlternativeEndpoint() {
        Log.d("AccountReport", "Trying alternative endpoint")
        try {
            val call = apiService!!.getAccountReport(accountId, currency)
            val requestUrl = call.request().url().toString()
            Log.d("AccountReport", "Making request to alternative URL: " + requestUrl)

            call.enqueue(object : Callback<AccountReport?> {
                override fun onResponse(
                    call: Call<AccountReport?>,
                    response: Response<AccountReport?>
                ) {
                    if (response.isSuccessful()) {
                        try {
                            val responseBody =
                                if (response.body() != null) response.body().toString() else "null"
                            Log.d(
                                "AccountReport",
                                "Raw response body from alternative endpoint: " + responseBody
                            )

                            val report = response.body()
                            if (report == null) {
                                Log.e("AccountReport", "Alternative endpoint response body is null")
                                showError("لم يتم استلام أي بيانات من الخادم")
                                return
                            }

                            if (getActivity() != null) {
                                getActivity()!!.runOnUiThread(Runnable {
                                    try {
                                        updateReportView(report)
                                    } catch (e: Exception) {
                                        Log.e(
                                            "AccountReport",
                                            "Error updating report view from alternative endpoint",
                                            e
                                        )
                                        showError("خطأ في تحديث العرض: " + e.message)
                                    }
                                })
                            }
                        } catch (e: Exception) {
                            Log.e(
                                "AccountReport",
                                "Error processing alternative endpoint response",
                                e
                            )
                            showError("خطأ في معالجة البيانات: " + e.message)
                        }
                    } else {
                        var errorMessage = "فشل في تحميل البيانات من الخادم البديل"
                        try {
                            if (response.errorBody() != null) {
                                val errorBody = response.errorBody()!!.string()
                                Log.e(
                                    "AccountReport",
                                    "Alternative endpoint error response body: " + errorBody
                                )
                                errorMessage += ": " + errorBody
                            } else {
                                Log.e(
                                    "AccountReport",
                                    "Alternative endpoint error response code: " + response.code()
                                )
                                errorMessage += " (رمز الخطأ: " + response.code() + ")"
                            }
                        } catch (e: IOException) {
                            Log.e(
                                "AccountReport",
                                "Error reading alternative endpoint error body",
                                e
                            )
                            errorMessage += " (رمز الخطأ: " + response.code() + ")"
                        }
                        showError(errorMessage)
                    }
                }

                override fun onFailure(call: Call<AccountReport?>, t: Throwable) {
                    Log.e("AccountReport", "Alternative endpoint network request failed", t)
                    showError("حدث خطأ في الاتصال بالخادم البديل: " + t.message)
                }
            })
        } catch (e: Exception) {
            Log.e("AccountReport", "Error making alternative endpoint request", e)
            showError("خطأ في إرسال الطلب للخادم البديل: " + e.message)
        }
    }

    private fun updateReportView(report: AccountReport?) {
        if (report == null) {
            Log.e("AccountReport", "Report is null in updateReportView")
            showError("التقرير فارغ")
            return
        }

        if (binding == null || binding!!.reportWebView == null) {
            Log.e("AccountReport", "WebView or binding is null")
            showError("خطأ في تهيئة واجهة المستخدم")
            return
        }

        try {
            Log.d("AccountReport", "Updating report view with data: " + report.toString())

            // التحقق من البيانات قبل عرضها
            if (report.getAccountId() <= 0) {
                Log.e("AccountReport", "Invalid account ID in report")
                showError("بيانات الحساب غير صحيحة")
                return
            }

            val html = StringBuilder()
            html.append("<!DOCTYPE html>")
            html.append("<html dir='rtl' lang='ar'>")
            html.append("<head>")
            html.append("<meta charset='UTF-8'>")
            html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>")
            html.append("<style>")
            html.append("body { font-family: 'Cairo', Arial, sans-serif; margin: 0; padding: 16px; padding-bottom: 80px; background: #f5f5f5; }")
            html.append(".header { background: linear-gradient(135deg, #1976d2, #2196f3); color: white; padding: 20px; border-radius: 10px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }")
            html.append(".header h2 { margin: 0; font-size: 24px; text-align: center; }")
            html.append(".summary-table { width: 100%; background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1); margin-bottom: 20px; }")
            html.append(".summary-table th { background: #1976d2; color: white; padding: 15px; text-align: center; font-weight: bold; font-size: 18px; }")
            html.append(".info-row, .balance-row { border-bottom: 1px solid #e0e0e0; }")
            html.append(".info-cell, .balance-cell { padding: 12px; text-align: center; vertical-align: middle; }")
            html.append(".info-label, .balance-label { color: #666; font-size: 14px; margin-bottom: 4px; }")
            html.append(".info-value { color: #333; font-size: 16px; font-weight: 500; }")
            html.append(".balance { color: #1976d2; font-weight: bold; font-size: 18px; }")
            html.append(".debits { color: #d32f2f; font-weight: bold; font-size: 18px; }")
            html.append(".credits { color: #388e3c; font-weight: bold; font-size: 18px; }")
            html.append("table { width: 100%; border-collapse: collapse; background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }")
            html.append("th { background: #1976d2; color: white; padding: 12px; text-align: center; font-weight: bold; }")
            html.append("td { padding: 12px; text-align: center; border-bottom: 1px solid #eee; }")
            html.append("tr:nth-child(even) { background: #f8f9fa; }")
            html.append("tr:hover { background: #e3f2fd; }")
            html.append(".credit { color: #4caf50; }")
            html.append(".debit { color: #f44336; }")
            html.append(".balance { font-weight: bold; }")
            html.append("@media print {")
            html.append("  .summary-table { box-shadow: none; border: 1px solid #ddd; }")
            html.append("  .summary-table th { background: #f5f5f5 !important; color: #333 !important; }")
            html.append("}")
            html.append("</style>")
            html.append("</head>")
            html.append("<body>")

            // معلومات الحساب
            html.append("<div class='header'>")
            html.append("<h2>تقرير الحساب</h2>")
            html.append("</div>")

            // جدول ملخص الحساب
            html.append("<table class='summary-table'>")
            html.append("<tr>")
            html.append("<th colspan='3'>معلومات الحساب</th>")
            html.append("</tr>")
            html.append("<tr class='info-row'>")
            html.append("<td class='info-cell'>")
            html.append("<div class='info-label'>اسم التاجر</div>")
            html.append("<div class='info-value'>")
                .append(if (report.getUserName() != null) report.getUserName() else "-")
                .append("</div>")
            html.append("</td>")
            html.append("<td class='info-cell'>")
            html.append("<div class='info-label'>اسم الحساب</div>")
            html.append("<div class='info-value'>")
                .append(if (report.getAccountName() != null) report.getAccountName() else "-")
                .append("</div>")
            html.append("</td>")
            html.append("<td class='info-cell'>")
            html.append("<div class='info-label'>العملة</div>")
            html.append("<div class='info-value'>")
                .append(if (report.getCurrency() != null) report.getCurrency() else "-")
                .append("</div>")
            html.append("</td>")
            html.append("</tr>")
            html.append("<tr class='balance-row'>")
            html.append("<td class='balance-cell'>")
            html.append("<div class='balance-label'>إجمالي الدائن</div>")
            html.append("<div class='credits'>")
                .append(numberFormat!!.format(report.getTotalCredits())).append("</div>")
            html.append("</td>")
            html.append("<td class='balance-cell'>")
            html.append("<div class='balance-label'>إجمالي المدين</div>")
            html.append("<div class='debits'>")
                .append(numberFormat!!.format(report.getTotalDebits())).append("</div>")
            html.append("</td>")
            html.append("<td class='balance-cell'>")
            html.append("<div class='balance-label'>الرصيد الحالي</div>")
            html.append("<div class='balance'>").append(numberFormat!!.format(report.getBalance()))
                .append("</div>")
            html.append("</td>")
            html.append("</tr>")
            html.append("</table>")

            // جدول المعاملات
            html.append("<table>")
            html.append("<tr>")
            html.append("<th>التاريخ</th>")
            html.append("<th>لك</th>")
            html.append("<th>عليك</th>")
            html.append("<th>الوصف</th>")
            html.append("<th>الرصيد</th>")
            html.append("</tr>")

            if (report.getTransactions() != null && !report.getTransactions().isEmpty()) {
                // ترتيب المعاملات من الأقدم إلى الأحدث
                val sortedTransactions: MutableList<AccountReport.Transaction?> =
                    ArrayList<AccountReport.Transaction?>(report.getTransactions())
                Collections.sort<AccountReport.Transaction?>(
                    sortedTransactions,
                    Comparator { t1: AccountReport.Transaction?, t2: AccountReport.Transaction? ->
                        val dateTime1 = if (t1!!.getDate() != null) t1.getDate() else ""
                        val dateTime2 = if (t2!!.getDate() != null) t2.getDate() else ""
                        dateTime1.compareTo(dateTime2)
                    })

                var runningBalance = 0.0
                for (tx in sortedTransactions) {
                    if (tx != null) {
                        // حساب الرصيد التراكمي
                        if (tx.getType() != null) {
                            if (tx.getType() == "credit") {
                                runningBalance += tx.getAmount()
                            } else {
                                runningBalance -= tx.getAmount()
                            }
                        }

                        html.append("<tr>")
                        // تنسيق التاريخ (إزالة الوقت)
                        val date: String? =
                            if (tx.getDate() != null) tx.getDate().split(" ".toRegex())
                                .dropLastWhile { it.isEmpty() }.toTypedArray()[0] else "-"
                        html.append("<td>").append(date).append("</td>")


                        // عرض المبلغ في العمود المناسب
                        if (tx.getType() != null && tx.getType() == "credit") {
                            html.append("<td class='credit'>")
                                .append(numberFormat!!.format(tx.getAmount())).append("</td>")
                            html.append("<td>-</td>")
                        } else {
                            html.append("<td>-</td>")
                            html.append("<td class='debit'>")
                                .append(numberFormat!!.format(tx.getAmount())).append("</td>")
                        }

                        html.append("<td>")
                            .append(if (tx.getDescription() != null) tx.getDescription() else "-")
                            .append("</td>")
                        html.append("<td class='balance'>")
                            .append(numberFormat!!.format(runningBalance)).append("</td>")
                        html.append("</tr>")
                    }
                }
            } else {
                html.append("<tr><td colspan='5' style='text-align: center;'>لا توجد معاملات</td></tr>")
            }

            html.append("</table></body></html>")

            val htmlContent = html.toString()
            Log.d("AccountReport", "Generated HTML content length: " + htmlContent.length)

            binding!!.reportWebView.loadDataWithBaseURL(
                null,
                htmlContent,
                "text/html; charset=UTF-8",
                "UTF-8",
                null
            )
            Log.d("AccountReport", "HTML content loaded into WebView")
        } catch (e: Exception) {
            Log.e("AccountReport", "Error updating report view", e)
            showError("خطأ في عرض التقرير: " + e.message)
        }
    }

    private fun showError(message: String?) {
        try {
            // طباعة الخطأ في السجل
            Log.e("AccountReport", "Error: " + message)


            // عرض رسالة الخطأ في Toast
            if (getContext() != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("AccountReport", "Error showing error message", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        fun newInstance(accountId: Int, currency: String?): AccountReportFragment {
            val fragment = AccountReportFragment()
            val args = Bundle()
            args.putInt("accountId", accountId)
            args.putString("currency", currency)
            fragment.setArguments(args)
            return fragment
        }
    }
}
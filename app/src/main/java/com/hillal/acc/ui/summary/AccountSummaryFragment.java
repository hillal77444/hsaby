package com.hillal.acc.ui.summary;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.core.view.ViewCompat;

import com.hillal.acc.R;
import com.hillal.acc.databinding.FragmentAccountSummaryBinding;
import com.hillal.acc.models.AccountSummary;
import com.hillal.acc.models.AccountSummaryResponse;
import com.hillal.acc.models.CurrencySummary;
import com.hillal.acc.network.ApiService;
import com.hillal.acc.network.RetrofitClient;
import com.hillal.acc.data.preferences.UserPreferences;
import com.hillal.acc.ui.report.AccountReportFragment;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountSummaryFragment extends Fragment {
    private FragmentAccountSummaryBinding binding;
    private ApiService apiService;
    private NumberFormat numberFormat;
    private float scale = 1.0f;
    private ScaleGestureDetector scaleGestureDetector;
    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 2.0f;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAccountSummaryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupNumberFormat();
        setupApiService();
        setupWebView();
        loadAccountSummary();
        
        // ضبط insets للجذر لرفع المحتوى مع الكيبورد وأزرار النظام
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int bottom = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime()).bottom;
            if (bottom == 0) {
                bottom = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()).bottom;
            }
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottom);
            return insets;
        });
    }

    private void setupNumberFormat() {
        numberFormat = NumberFormat.getNumberInstance(Locale.US);
        numberFormat.setMinimumFractionDigits(0);
        numberFormat.setMaximumFractionDigits(2);
    }

    private void setupApiService() {
        apiService = RetrofitClient.getInstance().getApiService();
    }

    private void setupWebView() {
        // إعدادات WebView الأساسية
        binding.detailsWebView.getSettings().setJavaScriptEnabled(true);
        binding.detailsWebView.getSettings().setDomStorageEnabled(true);
        binding.detailsWebView.getSettings().setSupportZoom(true);
        binding.detailsWebView.getSettings().setBuiltInZoomControls(true);
        binding.detailsWebView.getSettings().setDisplayZoomControls(false);
        binding.detailsWebView.getSettings().setLoadWithOverviewMode(true);
        binding.detailsWebView.getSettings().setUseWideViewPort(true);
        
        // تفعيل التفاعل مع المحتوى
        binding.detailsWebView.setClickable(true);
        binding.detailsWebView.setFocusable(true);
        binding.detailsWebView.setFocusableInTouchMode(true);
        
        // إضافة واجهة JavaScript
        binding.detailsWebView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void showAccountReport(int accountId, String currency) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        try {
                            // طباعة معلومات التصحيح
                            Log.d("AccountSummary", "Opening report for account: " + accountId + ", currency: " + currency);
                            
                            AccountReportFragment fragment = AccountReportFragment.newInstance(accountId, currency);
                            getActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.nav_host_fragment_content_main, fragment)
                                .addToBackStack(null)
                                .commit();
                        } catch (Exception e) {
                            Log.e("AccountSummary", "Error opening report", e);
                            showError("خطأ في فتح التقرير: " + e.getMessage(), "");
                        }
                    });
                }
            }
        }, "Android");

        // إضافة WebViewClient للتحكم في التفاعل
        binding.detailsWebView.setWebViewClient(new android.webkit.WebViewClient() {
            @Override
            public void onPageFinished(android.webkit.WebView view, String url) {
                super.onPageFinished(view, url);
                // تفعيل التفاعل مع المحتوى بعد تحميل الصفحة
                view.evaluateJavascript(
                    "document.body.style.webkitTouchCallout = 'none';" +
                    "document.body.style.webkitUserSelect = 'none';" +
                    "document.body.style.webkitTapHighlightColor = 'transparent';",
                    null
                );
            }
        });
    }

    private void loadAccountSummary() {
        String phoneNumber = getPhoneNumber();
        
        if (phoneNumber == null) {
            binding.progressBar.setVisibility(View.GONE);
            showError("لم يتم العثور على رقم الهاتف", "");
            return;
        }
        
        binding.progressBar.setVisibility(View.VISIBLE);
        updateTitle(phoneNumber);

        try {
            apiService.getAccountSummary(phoneNumber).enqueue(new Callback<AccountSummaryResponse>() {
                @Override
                public void onResponse(@NonNull Call<AccountSummaryResponse> call, @NonNull Response<AccountSummaryResponse> response) {
                    binding.progressBar.setVisibility(View.GONE);
                    
                    if (response.isSuccessful()) {
                        try {
                            AccountSummaryResponse summaryResponse = response.body();
                            if (summaryResponse == null) {
                                showError("لم يتم استلام أي بيانات من الخادم", "");
                                return;
                            }

                            // التحقق من البيانات المستلمة
                            if (summaryResponse.getCurrencySummary() == null || summaryResponse.getCurrencySummary().isEmpty()) {
                                showError("لا توجد بيانات ملخص العملات", "");
                                return;
                            }

                            if (summaryResponse.getAccounts() == null || summaryResponse.getAccounts().isEmpty()) {
                                showError("لا توجد بيانات الحسابات", "");
                                return;
                            }

                            // تحديث الجداول في الـ UI thread
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    try {
                                        updateSummaryTable(summaryResponse.getCurrencySummary());
                                        updateDetailsTable(summaryResponse.getAccounts());
                                    } catch (Exception e) {
                                        showError("خطأ في تحديث الجداول", "");
                                    }
                                });
                            }
                        } catch (Exception e) {
                            showError("خطأ في معالجة البيانات", "");
                        }
                    } else {
                        String errorMessage = "فشل في تحميل البيانات";
                        try {
                            if (response.errorBody() != null) {
                                String errorBody = response.errorBody().string();
                                Log.e("AccountSummary", "Error response body: " + errorBody);
                                errorMessage += ": " + errorBody;
                            } else {
                                Log.e("AccountSummary", "Error response code: " + response.code());
                                errorMessage += " (رمز الخطأ: " + response.code() + ")";
                            }
                        } catch (IOException e) {
                            Log.e("AccountSummary", "Error reading error body", e);
                            errorMessage += " (رمز الخطأ: " + response.code() + ")";
                        }
                        showError(errorMessage, "");
                    }
                }

                @Override
                public void onFailure(@NonNull Call<AccountSummaryResponse> call, @NonNull Throwable t) {
                    binding.progressBar.setVisibility(View.GONE);
                    showError(t.getMessage(), "");
                }
            });
        } catch (Exception e) {
            binding.progressBar.setVisibility(View.GONE);
            showError(e.getMessage(), "");
        }
    }

    private void updateTitle(String phoneNumber) {
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            binding.summaryTitleTextView.setText("ملخص الحسابات لرقم: " + phoneNumber);
        } else {
            binding.summaryTitleTextView.setText("ملخص الحسابات");
        }
    }

    private void updateSummaryTable(List<CurrencySummary> summaries) {
        try {
            StringBuilder html = new StringBuilder();
            html.append("<html><head><style>");
            html.append("body { font-family: Arial, sans-serif; margin: 0; padding: 0; }");
            html.append("table { width: 100%; border-collapse: collapse; }");
            html.append("th, td { padding: 8px; text-align: center; border: 1px solid #ddd; }");
            html.append("th { background-color: #f5f5f5; font-weight: bold; }");
            html.append("tr:nth-child(even) { background-color: #f9f9f9; }");
            html.append("</style></head><body>");
            
            html.append("<table>");
            // إضافة رأس الجدول
            html.append("<tr>");
            html.append("<th>الدائن</th>");
            html.append("<th>المدين</th>");
            html.append("<th>الرصيد</th>");
            html.append("<th>العملة</th>");
            html.append("</tr>");

            // إضافة البيانات
            if (summaries != null && !summaries.isEmpty()) {
                for (CurrencySummary summary : summaries) {
                    if (summary != null) {
                        html.append("<tr>");
                        html.append("<td>").append(numberFormat.format(summary.getTotalCredits())).append("</td>");
                        html.append("<td>").append(numberFormat.format(summary.getTotalDebits())).append("</td>");
                        html.append("<td>").append(numberFormat.format(summary.getTotalBalance())).append("</td>");
                        html.append("<td>").append(summary.getCurrency() != null ? summary.getCurrency() : "-").append("</td>");
                        html.append("</tr>");
                    }
                }
            } else {
                html.append("<tr><td colspan='4' style='text-align: center;'>لا توجد بيانات</td></tr>");
            }
            
            html.append("</table></body></html>");
            
            binding.summaryWebView.loadDataWithBaseURL(null, html.toString(), "text/html", "UTF-8", null);
        } catch (Exception e) {
            showError("خطأ في عرض ملخص العملات: " + e.getMessage(), "");
        }
    }

    private void updateDetailsTable(List<AccountSummary> accounts) {
        try {
            StringBuilder html = new StringBuilder();
            html.append("<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'><style>");
            html.append("body { font-family: Arial, sans-serif; margin: 0; padding: 0; -webkit-touch-callout: none; -webkit-user-select: none; }");
            html.append("table { width: 100%; border-collapse: collapse; }");
            html.append("th, td { padding: 8px; text-align: center; border: 1px solid #ddd; }");
            html.append("th { background-color: #f5f5f5; font-weight: bold; }");
            html.append("tr:nth-child(even) { background-color: #f9f9f9; }");
            html.append(".report-btn { background-color: #4CAF50; color: white; padding: 6px 12px; border: none; border-radius: 4px; cursor: pointer; -webkit-tap-highlight-color: transparent; }");
            html.append(".report-btn:active { background-color: #45a049; }");
            html.append("</style></head><body>");
            
            html.append("<script>");
            html.append("function showReport(accountId, currency) {");
            html.append("    if (window.Android && typeof window.Android.showAccountReport === 'function') {");
            html.append("        window.Android.showAccountReport(accountId, currency);");
            html.append("    } else {");
            html.append("        alert('خطأ في فتح التقرير');");
            html.append("    }");
            html.append("}");
            html.append("</script>");
            
            html.append("<table>");
            html.append("<tr>");
            html.append("<th>تقرير</th>");
            html.append("<th>الرصيد</th>");
            html.append("<th>عليك</th>");
            html.append("<th>لك</th>");
            html.append("<th>العملة</th>");
            html.append("<th>التاجر</th>");
            html.append("</tr>");

            if (accounts != null && !accounts.isEmpty()) {
                for (AccountSummary account : accounts) {
                    if (account != null) {
                        html.append("<tr>");
                        html.append("<td><button class='report-btn' onclick='showReport(")
                            .append(account.getUserId())
                            .append(", \"")
                            .append(account.getCurrency() != null ? account.getCurrency() : "-")
                            .append("\")'>تقرير</button></td>");
                        html.append("<td>").append(numberFormat.format(account.getBalance())).append("</td>");
                        html.append("<td>").append(numberFormat.format(account.getTotalDebits())).append("</td>");
                        html.append("<td>").append(numberFormat.format(account.getTotalCredits())).append("</td>");
                        html.append("<td>").append(account.getCurrency() != null ? account.getCurrency() : "-").append("</td>");
                        html.append("<td>").append(account.getUserName() != null ? account.getUserName() : "-").append("</td>");
                        html.append("</tr>");
                    }
                }
            } else {
                html.append("<tr><td colspan='6' style='text-align: center;'>لا توجد بيانات</td></tr>");
            }
            
            html.append("</table></body></html>");
            
            binding.detailsWebView.loadDataWithBaseURL(null, html.toString(), "text/html", "UTF-8", null);
        } catch (Exception e) {
            showError("خطأ في عرض تفاصيل الحسابات: " + e.getMessage(), "");
        }
    }

    private void showError(String message, String responseData) {
        try {
            // طباعة رسالة الخطأ في السجل
            Log.e("AccountSummary", "Error: " + message);
            
            // تحويل رسالة الخطأ إلى رسالة آمنة للمستخدم
            String userMessage;
            if (message.contains("Network is unreachable") || 
                message.contains("Unable to resolve host") || 
                message.contains("Failed to connect") ||
                message.contains("Connection refused")) {
                userMessage = "يرجى التحقق من اتصال الإنترنت والمحاولة مرة أخرى";
            } else if (message.contains("timeout") || message.contains("timed out")) {
                userMessage = "انتهت مهلة الاتصال، يرجى المحاولة مرة أخرى";
            } else if (message.contains("null") || message.contains("empty")) {
                userMessage = "لم يتم العثور على البيانات المطلوبة";
            } else if (message.contains("phone number") || message.contains("رقم الهاتف")) {
                userMessage = "يرجى تسجيل الدخول مرة أخرى";
            } else {
                userMessage = "حدث خطأ، يرجى المحاولة مرة أخرى";
            }
            
            // عرض رسالة Toast بسيطة للمستخدم
            if (getContext() != null) {
                Toast.makeText(getContext(), userMessage, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e("AccountSummary", "خطأ في عرض رسالة الخطأ", e);
        }
    }

    private String getAppVersion() {
        try {
            return getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "غير معروف";
        }
    }

    private boolean isNetworkAvailable() {
        try {
            android.net.ConnectivityManager connectivityManager = (android.net.ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    private String getNetworkType() {
        try {
            android.net.ConnectivityManager connectivityManager = (android.net.ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetworkInfo != null) {
                if (activeNetworkInfo.getType() == android.net.ConnectivityManager.TYPE_WIFI) {
                    return "WiFi";
                } else if (activeNetworkInfo.getType() == android.net.ConnectivityManager.TYPE_MOBILE) {
                    return "بيانات الجوال";
                }
            }
            return "غير معروف";
        } catch (Exception e) {
            return "غير معروف";
        }
    }

    private String getSignalStrength() {
        try {
            android.telephony.TelephonyManager telephonyManager = (android.telephony.TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
            android.telephony.SignalStrength signalStrength = telephonyManager.getSignalStrength();
            if (signalStrength != null) {
                return signalStrength.getLevel() + "/4";
            }
            return "غير معروف";
        } catch (Exception e) {
            return "غير معروف";
        }
    }

    private String getPhoneNumber() {
        try {
            UserPreferences userPreferences = new UserPreferences(requireContext());
            String phoneNumber = userPreferences.getPhoneNumber();
            
            // التحقق من صحة الرقم
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                showError("لم يتم العثور على رقم الهاتف", "");
                return null;
            }
            
            // إزالة المسافات فقط
            phoneNumber = phoneNumber.trim();
            
            return phoneNumber;
        } catch (Exception e) {
            showError("حدث خطأ في جلب رقم الهاتف", "");
            return null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 
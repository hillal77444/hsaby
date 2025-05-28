package com.hillal.hhhhhhh.ui.summary;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.databinding.FragmentAccountSummaryBinding;
import com.hillal.hhhhhhh.models.AccountSummary;
import com.hillal.hhhhhhh.models.AccountSummaryResponse;
import com.hillal.hhhhhhh.models.CurrencySummary;
import com.hillal.hhhhhhh.network.ApiService;
import com.hillal.hhhhhhh.network.RetrofitClient;
import com.hillal.hhhhhhh.data.preferences.UserPreferences;

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
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupNumberFormat();
        setupApiService();
        setupScaleGestureDetector();
        
        // إضافة واجهة JavaScript
        binding.detailsWebView.addJavascriptInterface(this, "Android");
        
        loadAccountSummary();
    }

    private void setupNumberFormat() {
        numberFormat = NumberFormat.getNumberInstance(Locale.US);
        numberFormat.setMinimumFractionDigits(0);
        numberFormat.setMaximumFractionDigits(2);
    }

    private void setupApiService() {
        apiService = RetrofitClient.getInstance().getApiService();
    }

    private void setupScaleGestureDetector() {
        scaleGestureDetector = new ScaleGestureDetector(requireContext(), new ScaleListener());
        
        // إضافة معالج اللمس للـ WebView
        binding.summaryWebView.setOnTouchListener((v, event) -> {
            scaleGestureDetector.onTouchEvent(event);
            return true;
        });
        
        binding.detailsWebView.setOnTouchListener((v, event) -> {
            scaleGestureDetector.onTouchEvent(event);
            return true;
        });

        // تفعيل التكبير والتصغير في WebView
        binding.summaryWebView.getSettings().setBuiltInZoomControls(true);
        binding.summaryWebView.getSettings().setDisplayZoomControls(false);
        binding.detailsWebView.getSettings().setBuiltInZoomControls(true);
        binding.detailsWebView.getSettings().setDisplayZoomControls(false);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            try {
                scale *= detector.getScaleFactor();
                // تحديد حدود التكبير والتصغير
                scale = Math.max(MIN_SCALE, Math.min(scale, MAX_SCALE));
                
                // تطبيق التكبير على WebView
                binding.summaryWebView.setScaleX(scale);
                binding.summaryWebView.setScaleY(scale);
                binding.detailsWebView.setScaleX(scale);
                binding.detailsWebView.setScaleY(scale);
                
                return true;
            } catch (Exception e) {
                Log.e("AccountSummary", "خطأ في التكبير والتصغير", e);
                return false;
            }
        }
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
                                        showError("خطأ في تحديث الجداول: " + e.getMessage(), "");
                                    }
                                });
                            }
                        } catch (Exception e) {
                            showError("خطأ في معالجة البيانات: " + e.getMessage(), "");
                        }
                    } else {
                        String errorMessage = "فشل في تحميل البيانات";
                        try {
                            if (response.errorBody() != null) {
                                errorMessage += ": " + response.errorBody().string();
                            } else {
                                errorMessage += " (رمز الخطأ: " + response.code() + ")";
                            }
                        } catch (IOException e) {
                            errorMessage += " (رمز الخطأ: " + response.code() + ")";
                        }
                        showError(errorMessage, "");
                    }
                }

                @Override
                public void onFailure(@NonNull Call<AccountSummaryResponse> call, @NonNull Throwable t) {
                    binding.progressBar.setVisibility(View.GONE);
                    showError("حدث خطأ في الاتصال: " + t.getMessage(), "");
                }
            });
        } catch (Exception e) {
            binding.progressBar.setVisibility(View.GONE);
            showError("خطأ في إرسال الطلب: " + e.getMessage(), "");
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
            html.append("<html><head><style>");
            html.append("body { font-family: Arial, sans-serif; margin: 0; padding: 0; }");
            html.append("table { width: 100%; border-collapse: collapse; }");
            html.append("th, td { padding: 8px; text-align: center; border: 1px solid #ddd; }");
            html.append("th { background-color: #f5f5f5; font-weight: bold; }");
            html.append("tr:nth-child(even) { background-color: #f9f9f9; }");
            html.append(".report-btn { background-color: #4CAF50; color: white; padding: 6px 12px; border: none; border-radius: 4px; cursor: pointer; }");
            html.append(".report-btn:hover { background-color: #45a049; }");
            html.append("</style></head><body>");
            
            html.append("<table>");
            // إضافة رأس الجدول
            html.append("<tr>");
            html.append("<th>تقرير</th>");
            html.append("<th>الرصيد</th>");
            html.append("<th>عليك</th>");
            html.append("<th>لك</th>");
            html.append("<th>العملة</th>");
            html.append("<th>الاسم</th>");
            html.append("</tr>");

            // إضافة البيانات
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
            
            html.append("</table>");
            
            // إضافة JavaScript للتعامل مع النقر على زر التقرير
            html.append("<script>");
            html.append("function showReport(accountId, currency) {");
            html.append("    window.Android.showAccountReport(accountId, currency);");
            html.append("}");
            html.append("</script>");
            
            html.append("</body></html>");
            
            binding.detailsWebView.loadDataWithBaseURL(null, html.toString(), "text/html", "UTF-8", null);
        } catch (Exception e) {
            showError("خطأ في عرض تفاصيل الحسابات: " + e.getMessage(), "");
        }
    }

    @JavascriptInterface
    public void showAccountReport(int accountId, String currency) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                try {
                    // فتح صفحة التقرير
                    AccountReportFragment fragment = AccountReportFragment.newInstance(accountId, currency);
                    getActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.nav_host_fragment_content_main, fragment)
                        .addToBackStack(null)
                        .commit();
                } catch (Exception e) {
                    showError("خطأ في فتح التقرير: " + e.getMessage(), "");
                }
            });
        }
    }

    private void showError(String message, String responseData) {
        try {
            // طباعة رسالة الخطأ في السجل
            Log.e("AccountSummary", "Error: " + message);
            
            // عرض رسالة Toast بسيطة للمستخدم
            if (getContext() != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
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
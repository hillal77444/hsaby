package com.hillal.hhhhhhh.ui.report;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.databinding.FragmentAccountReportBinding;
import com.hillal.hhhhhhh.models.AccountReport;
import com.hillal.hhhhhhh.network.ApiService;
import com.hillal.hhhhhhh.network.RetrofitClient;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountReportFragment extends Fragment {
    private FragmentAccountReportBinding binding;
    private ApiService apiService;
    private NumberFormat numberFormat;
    private int accountId;
    private String currency;

    public AccountReportFragment() {
        // Constructor فارغ مطلوب
    }

    public static AccountReportFragment newInstance(int accountId, String currency) {
        AccountReportFragment fragment = new AccountReportFragment();
        Bundle args = new Bundle();
        args.putInt("accountId", accountId);
        args.putString("currency", currency);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            accountId = getArguments().getInt("accountId");
            currency = getArguments().getString("currency");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAccountReportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupNumberFormat();
        setupApiService();
        setupWebView();
        loadAccountReport();
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
        if (binding != null && binding.reportWebView != null) {
            Log.d("AccountReport", "Setting up WebView");
            binding.reportWebView.getSettings().setJavaScriptEnabled(true);
            binding.reportWebView.getSettings().setDomStorageEnabled(true);
            binding.reportWebView.getSettings().setLoadWithOverviewMode(true);
            binding.reportWebView.getSettings().setUseWideViewPort(true);
            binding.reportWebView.getSettings().setBuiltInZoomControls(true);
            binding.reportWebView.getSettings().setDisplayZoomControls(false);
            binding.reportWebView.setInitialScale(1);
            
            // إضافة WebViewClient للتعامل مع الأخطاء
            binding.reportWebView.setWebViewClient(new android.webkit.WebViewClient() {
                @Override
                public void onReceivedError(android.webkit.WebView view, int errorCode, String description, String failingUrl) {
                    Log.e("AccountReport", "WebView error: " + description);
                    showError("خطأ في تحميل التقرير: " + description);
                }
            });
            
            Log.d("AccountReport", "WebView setup completed");
        } else {
            Log.e("AccountReport", "WebView or binding is null during setup");
        }
    }

    private void loadAccountReport() {
        if (accountId <= 0 || currency == null) {
            showError("بيانات الحساب غير صحيحة");
            return;
        }

        Log.d("AccountReport", "Starting to load account report for ID: " + accountId + ", Currency: " + currency);
        binding.progressBar.setVisibility(View.VISIBLE);

        try {
            Call<AccountReport> call = apiService.getAccountDetails(accountId, currency);
            String requestUrl = call.request().url().toString();
            Log.d("AccountReport", "Making request to URL: " + requestUrl);

            call.enqueue(new Callback<AccountReport>() {
                @Override
                public void onResponse(@NonNull Call<AccountReport> call, @NonNull Response<AccountReport> response) {
                    Log.d("AccountReport", "Received response with code: " + response.code());
                    binding.progressBar.setVisibility(View.GONE);

                    if (response.isSuccessful()) {
                        try {
                            AccountReport report = response.body();
                            Log.d("AccountReport", "Response body: " + (report != null ? report.toString() : "null"));

                            if (report == null) {
                                Log.e("AccountReport", "Response body is null");
                                showError("لم يتم استلام أي بيانات من الخادم");
                                return;
                            }

                            // التحقق من البيانات المستلمة
                            if (report.getUserName() == null || report.getAccountName() == null) {
                                Log.e("AccountReport", "Missing required fields: userName or accountName");
                                showError("بيانات الحساب غير مكتملة");
                                return;
                            }

                            // تحديث العرض في الـ UI thread
                            if (getActivity() != null) {
                                Log.d("AccountReport", "Updating UI with report data");
                                getActivity().runOnUiThread(() -> {
                                    try {
                                        updateReportView(report);
                                    } catch (Exception e) {
                                        Log.e("AccountReport", "Error updating report view", e);
                                        showError("خطأ في تحديث التقرير: " + e.getMessage());
                                    }
                                });
                            } else {
                                Log.e("AccountReport", "Activity is null");
                                showError("خطأ في تحديث واجهة المستخدم");
                            }
                        } catch (Exception e) {
                            Log.e("AccountReport", "Error processing response", e);
                            showError("خطأ في معالجة البيانات: " + e.getMessage());
                        }
                    } else {
                        String errorMessage = "فشل في تحميل البيانات";
                        try {
                            if (response.errorBody() != null) {
                                String errorBody = response.errorBody().string();
                                Log.e("AccountReport", "Error response body: " + errorBody);
                                errorMessage += ": " + errorBody;
                            } else {
                                Log.e("AccountReport", "Error response code: " + response.code());
                                errorMessage += " (رمز الخطأ: " + response.code() + ")";
                            }
                        } catch (IOException e) {
                            Log.e("AccountReport", "Error reading error body", e);
                            errorMessage += " (رمز الخطأ: " + response.code() + ")";
                        }
                        showError(errorMessage);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<AccountReport> call, @NonNull Throwable t) {
                    Log.e("AccountReport", "Network request failed", t);
                    binding.progressBar.setVisibility(View.GONE);
                    showError("حدث خطأ في الاتصال: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e("AccountReport", "Error making request", e);
            binding.progressBar.setVisibility(View.GONE);
            showError("خطأ في إرسال الطلب: " + e.getMessage());
        }
    }

    private void updateReportView(AccountReport report) {
        try {
            if (report == null) {
                Log.e("AccountReport", "Report is null in updateReportView");
                showError("التقرير فارغ");
                return;
            }

            Log.d("AccountReport", "Updating report view with data: " + report.toString());

            StringBuilder html = new StringBuilder();
            html.append("<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'><style>");
            html.append("body { font-family: Arial, sans-serif; margin: 0; padding: 16px; -webkit-touch-callout: none; -webkit-user-select: none; }");
            html.append("h2 { color: #333; text-align: center; margin-bottom: 20px; }");
            html.append("table { width: 100%; border-collapse: collapse; margin-top: 16px; }");
            html.append("th, td { padding: 8px; text-align: center; border: 1px solid #ddd; }");
            html.append("th { background-color: #f5f5f5; font-weight: bold; }");
            html.append("tr:nth-child(even) { background-color: #f9f9f9; }");
            html.append(".summary { margin: 16px 0; padding: 16px; background-color: #f5f5f5; border-radius: 4px; }");
            html.append(".summary p { margin: 8px 0; }");
            html.append(".summary strong { color: #333; }");
            html.append("</style></head><body>");

            // معلومات الحساب
            html.append("<h2>تقرير الحساب</h2>");
            html.append("<div class='summary'>");
            html.append("<p><strong>اسم المستخدم:</strong> ").append(report.getUserName() != null ? report.getUserName() : "-").append("</p>");
            html.append("<p><strong>اسم الحساب:</strong> ").append(report.getAccountName() != null ? report.getAccountName() : "-").append("</p>");
            html.append("<p><strong>العملة:</strong> ").append(report.getCurrency() != null ? report.getCurrency() : "-").append("</p>");
            html.append("<p><strong>الرصيد الحالي:</strong> ").append(numberFormat.format(report.getBalance())).append("</p>");
            html.append("<p><strong>إجمالي المدين:</strong> ").append(numberFormat.format(report.getTotalDebits())).append("</p>");
            html.append("<p><strong>إجمالي الدائن:</strong> ").append(numberFormat.format(report.getTotalCredits())).append("</p>");
            html.append("</div>");

            // جدول المعاملات
            html.append("<h3>المعاملات</h3>");
            html.append("<table>");
            html.append("<tr>");
            html.append("<th>التاريخ</th>");
            html.append("<th>النوع</th>");
            html.append("<th>المبلغ</th>");
            html.append("<th>الوصف</th>");
            html.append("</tr>");

            if (report.getTransactions() != null && !report.getTransactions().isEmpty()) {
                for (AccountReport.Transaction tx : report.getTransactions()) {
                    if (tx != null) {
                        html.append("<tr>");
                        html.append("<td>").append(tx.getDate() != null ? tx.getDate() : "-").append("</td>");
                        html.append("<td>").append(tx.getType() != null ? (tx.getType().equals("credit") ? "دائن" : "مدين") : "-").append("</td>");
                        html.append("<td>").append(numberFormat.format(tx.getAmount())).append("</td>");
                        html.append("<td>").append(tx.getDescription() != null ? tx.getDescription() : "-").append("</td>");
                        html.append("</tr>");
                    }
                }
            } else {
                html.append("<tr><td colspan='4' style='text-align: center;'>لا توجد معاملات</td></tr>");
            }

            html.append("</table></body></html>");

            String htmlContent = html.toString();
            Log.d("AccountReport", "Generated HTML content length: " + htmlContent.length());

            if (binding != null && binding.reportWebView != null) {
                Log.d("AccountReport", "Loading HTML content into WebView");
                binding.reportWebView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
            } else {
                Log.e("AccountReport", "WebView is null or not initialized");
                showError("خطأ في تهيئة WebView");
            }
        } catch (Exception e) {
            Log.e("AccountReport", "Error updating report view", e);
            showError("خطأ في عرض التقرير: " + e.getMessage());
        }
    }

    private void showError(String message) {
        try {
            // طباعة الخطأ في السجل
            Log.e("AccountReport", "Error: " + message);
            
            // عرض رسالة الخطأ في Toast
            if (getContext() != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e("AccountReport", "Error showing error message", e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 
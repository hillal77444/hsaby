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

                @Override
                public void onPageFinished(android.webkit.WebView view, String url) {
                    Log.d("AccountReport", "WebView page loaded successfully");
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
            // تجربة الـ endpoint الأول
            Call<AccountReport> call = apiService.getAccountDetails(accountId, currency);
            String requestUrl = call.request().url().toString();
            Log.d("AccountReport", "Making request to URL: " + requestUrl);

            call.enqueue(new Callback<AccountReport>() {
                @Override
                public void onResponse(@NonNull Call<AccountReport> call, @NonNull Response<AccountReport> response) {
                    binding.progressBar.setVisibility(View.GONE);
                    Log.d("AccountReport", "Received response with code: " + response.code());

                    if (response.isSuccessful()) {
                        try {
                            String responseBody = response.body() != null ? response.body().toString() : "null";
                            Log.d("AccountReport", "Raw response body: " + responseBody);

                            AccountReport report = response.body();
                            if (report == null) {
                                Log.e("AccountReport", "Response body is null");
                                // تجربة الـ endpoint الثاني
                                tryAlternativeEndpoint();
                                return;
                            }

                            // التحقق من البيانات المستلمة
                            if (report.getTransactions() == null) {
                                Log.e("AccountReport", "Transactions list is null");
                                showError("لا توجد بيانات المعاملات");
                                return;
                            }

                            // التحقق من البيانات الأساسية
                            if (report.getAccountId() <= 0) {
                                Log.e("AccountReport", "Invalid account ID in response");
                                showError("بيانات الحساب غير صحيحة");
                                return;
                            }

                            if (report.getCurrency() == null || report.getCurrency().isEmpty()) {
                                Log.e("AccountReport", "Currency is null or empty");
                                showError("العملة غير محددة");
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
                                        showError("خطأ في تحديث العرض: " + e.getMessage());
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

    private void tryAlternativeEndpoint() {
        Log.d("AccountReport", "Trying alternative endpoint");
        try {
            Call<AccountReport> call = apiService.getAccountReport(accountId, currency);
            String requestUrl = call.request().url().toString();
            Log.d("AccountReport", "Making request to alternative URL: " + requestUrl);

            call.enqueue(new Callback<AccountReport>() {
                @Override
                public void onResponse(@NonNull Call<AccountReport> call, @NonNull Response<AccountReport> response) {
                    if (response.isSuccessful()) {
                        try {
                            String responseBody = response.body() != null ? response.body().toString() : "null";
                            Log.d("AccountReport", "Raw response body from alternative endpoint: " + responseBody);

                            AccountReport report = response.body();
                            if (report == null) {
                                Log.e("AccountReport", "Alternative endpoint response body is null");
                                showError("لم يتم استلام أي بيانات من الخادم");
                                return;
                            }

                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    try {
                                        updateReportView(report);
                                    } catch (Exception e) {
                                        Log.e("AccountReport", "Error updating report view from alternative endpoint", e);
                                        showError("خطأ في تحديث العرض: " + e.getMessage());
                                    }
                                });
                            }
                        } catch (Exception e) {
                            Log.e("AccountReport", "Error processing alternative endpoint response", e);
                            showError("خطأ في معالجة البيانات: " + e.getMessage());
                        }
                    } else {
                        String errorMessage = "فشل في تحميل البيانات من الخادم البديل";
                        try {
                            if (response.errorBody() != null) {
                                String errorBody = response.errorBody().string();
                                Log.e("AccountReport", "Alternative endpoint error response body: " + errorBody);
                                errorMessage += ": " + errorBody;
                            } else {
                                Log.e("AccountReport", "Alternative endpoint error response code: " + response.code());
                                errorMessage += " (رمز الخطأ: " + response.code() + ")";
                            }
                        } catch (IOException e) {
                            Log.e("AccountReport", "Error reading alternative endpoint error body", e);
                            errorMessage += " (رمز الخطأ: " + response.code() + ")";
                        }
                        showError(errorMessage);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<AccountReport> call, @NonNull Throwable t) {
                    Log.e("AccountReport", "Alternative endpoint network request failed", t);
                    showError("حدث خطأ في الاتصال بالخادم البديل: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e("AccountReport", "Error making alternative endpoint request", e);
            showError("خطأ في إرسال الطلب للخادم البديل: " + e.getMessage());
        }
    }

    private void updateReportView(AccountReport report) {
        if (report == null) {
            Log.e("AccountReport", "Report is null in updateReportView");
            showError("التقرير فارغ");
            return;
        }

        if (binding == null || binding.reportWebView == null) {
            Log.e("AccountReport", "WebView or binding is null");
            showError("خطأ في تهيئة واجهة المستخدم");
            return;
        }

        try {
            Log.d("AccountReport", "Updating report view with data: " + report.toString());

            // التحقق من البيانات قبل عرضها
            if (report.getAccountId() <= 0) {
                Log.e("AccountReport", "Invalid account ID in report");
                showError("بيانات الحساب غير صحيحة");
                return;
            }

            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>");
            html.append("<html dir='rtl' lang='ar'>");
            html.append("<head>");
            html.append("<meta charset='UTF-8'>");
            html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>");
            html.append("<style>");
            html.append("body { font-family: 'Cairo', Arial, sans-serif; margin: 0; padding: 16px; background: #f5f5f5; }");
            html.append(".header { background: linear-gradient(135deg, #1976d2, #2196f3); color: white; padding: 20px; border-radius: 10px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }");
            html.append(".header h2 { margin: 0; font-size: 24px; text-align: center; }");
            html.append(".summary { background: white; padding: 20px; border-radius: 10px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }");
            html.append(".summary p { margin: 10px 0; font-size: 16px; }");
            html.append(".summary strong { color: #1976d2; }");
            html.append("table { width: 100%; border-collapse: collapse; background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }");
            html.append("th { background: #1976d2; color: white; padding: 12px; text-align: center; font-weight: bold; }");
            html.append("td { padding: 12px; text-align: center; border-bottom: 1px solid #eee; }");
            html.append("tr:nth-child(even) { background: #f8f9fa; }");
            html.append("tr:hover { background: #e3f2fd; }");
            html.append(".credit { color: #4caf50; }");
            html.append(".debit { color: #f44336; }");
            html.append(".balance { font-weight: bold; }");
            html.append("</style>");
            html.append("</head>");
            html.append("<body>");

            // معلومات الحساب
            html.append("<div class='header'>");
            html.append("<h2>تقرير الحساب</h2>");
            html.append("</div>");

            html.append("<div class='summary'>");
            html.append("<p><strong>اسم المستخدم:</strong> ").append(report.getUserName() != null ? report.getUserName() : "-").append("</p>");
            html.append("<p><strong>اسم الحساب:</strong> ").append(report.getAccountName() != null ? report.getAccountName() : "-").append("</p>");
            html.append("<p><strong>العملة:</strong> ").append(report.getCurrency() != null ? report.getCurrency() : "-").append("</p>");
            html.append("<p><strong>الرصيد الحالي:</strong> ").append(numberFormat.format(report.getBalance())).append("</p>");
            html.append("<p><strong>إجمالي المدين:</strong> ").append(numberFormat.format(report.getTotalDebits())).append("</p>");
            html.append("<p><strong>إجمالي الدائن:</strong> ").append(numberFormat.format(report.getTotalCredits())).append("</p>");
            html.append("</div>");

            // جدول المعاملات
            html.append("<table>");
            html.append("<tr>");
            html.append("<th>التاريخ</th>");
            html.append("<th>لك</th>");
            html.append("<th>عليك</th>");
            html.append("<th>الوصف</th>");
            html.append("<th>الرصيد</th>");
            html.append("</tr>");

            if (report.getTransactions() != null && !report.getTransactions().isEmpty()) {
                double runningBalance = 0;
                for (AccountReport.Transaction tx : report.getTransactions()) {
                    if (tx != null) {
                        // حساب الرصيد التراكمي
                        if (tx.getType() != null) {
                            if (tx.getType().equals("credit")) {
                                runningBalance += tx.getAmount();
                            } else {
                                runningBalance -= tx.getAmount();
                            }
                        }

                        html.append("<tr>");
                        // تنسيق التاريخ (إزالة الوقت)
                        String date = tx.getDate() != null ? tx.getDate().split(" ")[0] : "-";
                        html.append("<td>").append(date).append("</td>");
                        
                        // عرض المبلغ في العمود المناسب
                        if (tx.getType() != null && tx.getType().equals("credit")) {
                            html.append("<td class='credit'>").append(numberFormat.format(tx.getAmount())).append("</td>");
                            html.append("<td>-</td>");
                        } else {
                            html.append("<td>-</td>");
                            html.append("<td class='debit'>").append(numberFormat.format(tx.getAmount())).append("</td>");
                        }
                        
                        html.append("<td>").append(tx.getDescription() != null ? tx.getDescription() : "-").append("</td>");
                        html.append("<td class='balance'>").append(numberFormat.format(runningBalance)).append("</td>");
                        html.append("</tr>");
                    }
                }
            } else {
                html.append("<tr><td colspan='5' style='text-align: center;'>لا توجد معاملات</td></tr>");
            }

            html.append("</table></body></html>");

            String htmlContent = html.toString();
            Log.d("AccountReport", "Generated HTML content length: " + htmlContent.length());

            binding.reportWebView.loadDataWithBaseURL(null, htmlContent, "text/html; charset=UTF-8", "UTF-8", null);
            Log.d("AccountReport", "HTML content loaded into WebView");
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
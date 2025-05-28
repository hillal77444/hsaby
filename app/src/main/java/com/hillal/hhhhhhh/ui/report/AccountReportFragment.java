package com.hillal.hhhhhhh.ui.report;

import android.os.Bundle;
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

    public static AccountReportFragment newInstance(int accountId, String currency) {
        AccountReportFragment fragment = new AccountReportFragment();
        Bundle args = new Bundle();
        args.putInt("accountId", accountId);
        args.putString("currency", currency);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAccountReportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            accountId = getArguments().getInt("accountId");
            currency = getArguments().getString("currency");
        }

        setupNumberFormat();
        setupApiService();
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

    private void loadAccountReport() {
        binding.progressBar.setVisibility(View.VISIBLE);

        apiService.getAccountDetails(accountId, currency).enqueue(new Callback<AccountReport>() {
            @Override
            public void onResponse(@NonNull Call<AccountReport> call, @NonNull Response<AccountReport> response) {
                binding.progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    AccountReport report = response.body();
                    updateReportView(report);
                } else {
                    showError("فشل في تحميل التقرير");
                }
            }

            @Override
            public void onFailure(@NonNull Call<AccountReport> call, @NonNull Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                showError("خطأ في الاتصال: " + t.getMessage());
            }
        });
    }

    private void updateReportView(AccountReport report) {
        StringBuilder html = new StringBuilder();
        html.append("<html><head><style>");
        html.append("body { font-family: Arial, sans-serif; margin: 0; padding: 16px; }");
        html.append("h2 { color: #333; text-align: center; }");
        html.append("table { width: 100%; border-collapse: collapse; margin-top: 16px; }");
        html.append("th, td { padding: 8px; text-align: center; border: 1px solid #ddd; }");
        html.append("th { background-color: #f5f5f5; font-weight: bold; }");
        html.append("tr:nth-child(even) { background-color: #f9f9f9; }");
        html.append(".summary { margin: 16px 0; padding: 16px; background-color: #f5f5f5; border-radius: 4px; }");
        html.append("</style></head><body>");

        // معلومات الحساب
        html.append("<h2>تقرير الحساب</h2>");
        html.append("<div class='summary'>");
        html.append("<p><strong>اسم المستخدم:</strong> ").append(report.getUserName()).append("</p>");
        html.append("<p><strong>اسم الحساب:</strong> ").append(report.getAccountName()).append("</p>");
        html.append("<p><strong>العملة:</strong> ").append(report.getCurrency()).append("</p>");
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

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        for (AccountReport.Transaction tx : report.getTransactions()) {
            html.append("<tr>");
            html.append("<td>").append(tx.getDate()).append("</td>");
            html.append("<td>").append(tx.getType().equals("credit") ? "دائن" : "مدين").append("</td>");
            html.append("<td>").append(numberFormat.format(tx.getAmount())).append("</td>");
            html.append("<td>").append(tx.getDescription()).append("</td>");
            html.append("</tr>");
        }

        html.append("</table></body></html>");

        binding.reportWebView.loadDataWithBaseURL(null, html.toString(), "text/html", "UTF-8", null);
    }

    private void showError(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 
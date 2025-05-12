package com.hillal.hhhhhhh.ui;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.model.Transaction;
import com.hillal.hhhhhhh.viewmodel.AccountStatementViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import org.json.JSONException;
import org.json.JSONObject;

public class AccountStatementActivity extends AppCompatActivity {
    private AccountStatementViewModel viewModel;
    private WebView webView;
    private List<Account> allAccounts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_statement);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("كشف الحساب التفصيلي");
        }

        viewModel = new ViewModelProvider(this).get(AccountStatementViewModel.class);
        setupWebView();
        loadAccounts();
    }

    private void setupWebView() {
        webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new WebAppInterface(), "Android");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // تحميل البيانات الأولية
                loadInitialData();
            }
        });

        // تحميل صفحة HTML الرئيسية
        String htmlContent = getInitialHtmlContent();
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
    }

    private String getInitialHtmlContent() {
        return "<!DOCTYPE html>" +
               "<html dir='rtl'>" +
               "<head>" +
               "<meta charset='UTF-8'>" +
               "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
               "<style>" +
               "body { font-family: Arial, sans-serif; padding: 20px; }" +
               ".controls { background: #f5f5f5; padding: 20px; border-radius: 8px; margin-bottom: 20px; }" +
               "select { width: 100%; padding: 10px; margin: 10px 0; border: 1px solid #ddd; border-radius: 4px; }" +
               ".date-container { display: flex; gap: 10px; margin: 10px 0; }" +
               ".date-input { flex: 1; }" +
               ".date-input input { width: 100%; padding: 10px; border: 1px solid #ddd; border-radius: 4px; }" +
               ".date-input label { display: block; margin-bottom: 5px; color: #666; }" +
               "button { background: #2196F3; color: white; padding: 10px 20px; border: none; border-radius: 4px; cursor: pointer; width: 100%; margin-top: 10px; }" +
               "button:hover { background: #1976D2; }" +
               "#reportContent { margin-top: 20px; }" +
               "</style>" +
               "</head>" +
               "<body>" +
               "<div class='controls'>" +
               "<select id='accountSelect' onchange='Android.onAccountSelected(this.value)'>" +
               "<option value=''>اختر الحساب</option>" +
               "</select>" +
               "<div class='date-container'>" +
               "<div class='date-input'>" +
               "<label>من تاريخ</label>" +
               "<input type='date' id='startDate' onchange='Android.onDateChanged()'>" +
               "</div>" +
               "<div class='date-input'>" +
               "<label>إلى تاريخ</label>" +
               "<input type='date' id='endDate' onchange='Android.onDateChanged()'>" +
               "</div>" +
               "</div>" +
               "<button onclick='Android.showReport()'>عرض التقرير</button>" +
               "</div>" +
               "<div id='reportContent'></div>" +
               "</body>" +
               "</html>";
    }

    private class WebAppInterface {
        @JavascriptInterface
        public void onAccountSelected(String accountId) {
            runOnUiThread(() -> {
                if (accountId != null && !accountId.isEmpty()) {
                    updateReport();
                }
            });
        }

        @JavascriptInterface
        public void onDateChanged() {
            runOnUiThread(() -> AccountStatementActivity.this.updateReport());
        }

        @JavascriptInterface
        public void showReport() {
            runOnUiThread(() -> AccountStatementActivity.this.updateReport());
        }
    }

    private void loadAccounts() {
        viewModel.getAllAccounts().observe(this, accounts -> {
            allAccounts = accounts;
            updateAccountsList();
        });
    }

    private void updateAccountsList() {
        StringBuilder js = new StringBuilder("document.getElementById('accountSelect').innerHTML = '<option value=\"\">اختر الحساب</option>'");
        for (Account account : allAccounts) {
            js.append(" + '<option value=\"").append(account.getId()).append("\">")
              .append(account.getName()).append("</option>'");
        }
        webView.evaluateJavascript(js.toString(), null);
    }

    private void updateReport() {
        // عرض مؤشر التحميل
        webView.evaluateJavascript(
            "document.getElementById('reportContent').innerHTML = '<div style=\"text-align: center; padding: 20px;\">جاري تحميل التقرير...</div>'",
            null
        );

        // الحصول على القيم من WebView
        webView.evaluateJavascript(
            "JSON.stringify({" +
            "accountId: document.getElementById('accountSelect').value," +
            "startDate: document.getElementById('startDate').value," +
            "endDate: document.getElementById('endDate').value" +
            "})",
            value -> {
                try {
                    JSONObject data = new JSONObject(value);
                    String accountId = data.getString("accountId");
                    String startDate = data.getString("startDate");
                    String endDate = data.getString("endDate");

                    if (accountId.isEmpty()) {
                        showError("الرجاء اختيار الحساب");
                        return;
                    }

                    if (startDate.isEmpty() || endDate.isEmpty()) {
                        showError("الرجاء تحديد الفترة الزمنية");
                        return;
                    }

                    try {
                        Date start = parseDate(startDate);
                        Date end = parseDate(endDate);

                        // اجعل endDate نهاية اليوم
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(end);
                        cal.set(Calendar.HOUR_OF_DAY, 23);
                        cal.set(Calendar.MINUTE, 59);
                        cal.set(Calendar.SECOND, 59);
                        cal.set(Calendar.MILLISECOND, 999);
                        Date endOfDay = cal.getTime();

                        if (start.after(endOfDay)) {
                            showError("تاريخ البداية يجب أن يكون قبل تاريخ النهاية");
                            return;
                        }

                        // الحصول على معلومات الحساب
                        Account selectedAccount = null;
                        for (Account acc : allAccounts) {
                            if (acc.getId() == Long.parseLong(accountId)) {
                                selectedAccount = acc;
                                break;
                            }
                        }

                        if (selectedAccount == null) {
                            showError("لم يتم العثور على الحساب المحدد");
                            return;
                        }

                        // عرض معلومات الحساب
                        String accountInfo = String.format(
                            "<div style='margin-bottom: 20px; padding: 15px; background: #f8f9fa; border-radius: 8px;'>" +
                            "<h3 style='margin: 0 0 10px 0;'>%s</h3>" +
                            "<p style='margin: 5px 0;'>الفترة: من %s إلى %s</p>" +
                            "</div>",
                            selectedAccount.getName(),
                            formatDate(start.getTime()),
                            formatDate(end.getTime())
                        );

                        webView.evaluateJavascript(
                            "document.getElementById('reportContent').innerHTML = '" + accountInfo + "'",
                            null
                        );

                        // تحميل العمليات
                        viewModel.getTransactionsForAccountInDateRange(
                            selectedAccount.getId(),
                            start,
                            endOfDay
                        ).observe(this, transactions -> {
                            if (transactions != null && !transactions.isEmpty()) {
                                String reportHtml = generateReportHtml(transactions);
                                webView.evaluateJavascript(
                                    "document.getElementById('reportContent').innerHTML += '" + reportHtml + "'",
                                    null
                                );
                            } else {
                                webView.evaluateJavascript(
                                    "document.getElementById('reportContent').innerHTML += '<p style=\"text-align: center; color: #666; padding: 20px;\">لا توجد عمليات في الفترة المحددة</p>'",
                                    null
                                );
                            }
                        });
                    } catch (Exception e) {
                        showError("خطأ في تنسيق التاريخ");
                    }
                } catch (JSONException e) {
                    showError("خطأ في معالجة البيانات");
                }
            }
        );
    }

    private void loadInitialData() {
        // تعيين التواريخ الافتراضية
        Calendar cal = Calendar.getInstance();
        String toDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(cal.getTime());
        cal.add(Calendar.DATE, -3); // أول أمس
        String fromDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(cal.getTime());
        
        // تعيين التواريخ في حقول الإدخال
        String js = String.format(
            "document.getElementById('startDate').value = '%s';" +
            "document.getElementById('endDate').value = '%s';",
            fromDate, toDate
        );
        webView.evaluateJavascript(js, null);
    }

    private String getSelectedAccountId() {
        final String[] accountId = {""};
        webView.evaluateJavascript(
            "document.getElementById('accountSelect').value",
            value -> {
                if (value != null && !value.equals("null")) {
                    accountId[0] = value.replace("\"", "");
                }
            }
        );
        return accountId[0];
    }

    private String getStartDate() {
        final String[] startDate = {""};
        webView.evaluateJavascript(
            "document.getElementById('startDate').value",
            value -> {
                if (value != null && !value.equals("null")) {
                    startDate[0] = value.replace("\"", "");
                }
            }
        );
        return startDate[0];
    }

    private String getEndDate() {
        final String[] endDate = {""};
        webView.evaluateJavascript(
            "document.getElementById('endDate').value",
            value -> {
                if (value != null && !value.equals("null")) {
                    endDate[0] = value.replace("\"", "");
                }
            }
        );
        return endDate[0];
    }

    private Date parseDate(String dateStr) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr);
        } catch (Exception e) {
            return new Date();
        }
    }

    private String generateReportHtml(List<Transaction> transactions) {
        StringBuilder html = new StringBuilder();
        
        // ترتيب العمليات من الأقدم إلى الأحدث
        Collections.sort(transactions, (a, b) -> Long.compare(a.getDate(), b.getDate()));

        // تجميع العمليات حسب العملة
        Map<String, List<Transaction>> currencyMap = new HashMap<>();
        for (Transaction t : transactions) {
            String currency = t.getCurrency();
            if (!currencyMap.containsKey(currency)) {
                currencyMap.put(currency, new ArrayList<>());
            }
            currencyMap.get(currency).add(t);
        }

        // إنشاء جدول لكل عملة
        for (String currency : currencyMap.keySet()) {
            List<Transaction> currencyTransactions = currencyMap.get(currency);
            
            // حساب الرصيد السابق للعملة
            double previousBalance = 0;
            for (Transaction t : currencyTransactions) {
                if (t.getType().equals("debit")) {
                    previousBalance -= t.getAmount();
                } else {
                    previousBalance += t.getAmount();
                }
            }

            // عنوان العملة
            html.append("<div style='margin-top: 30px;'>");
            html.append("<h3 style='margin: 0 0 15px 0; color: #2196F3;'>العملة: ").append(currency).append("</h3>");
            
            html.append("<table style='width: 100%; border-collapse: collapse; margin-bottom: 20px; background: white; box-shadow: 0 1px 3px rgba(0,0,0,0.1);'>");
            html.append("<tr style='background: #f5f5f5;'>");
            html.append("<th style='border: 1px solid #ddd; padding: 12px; text-align: right;'>التاريخ</th>");
            html.append("<th style='border: 1px solid #ddd; padding: 12px; text-align: right;'>الوصف</th>");
            html.append("<th style='border: 1px solid #ddd; padding: 12px; text-align: right;'>عليه</th>");
            html.append("<th style='border: 1px solid #ddd; padding: 12px; text-align: right;'>له</th>");
            html.append("<th style='border: 1px solid #ddd; padding: 12px; text-align: right;'>الرصيد</th>");
            html.append("</tr>");

            // صف الرصيد السابق
            html.append("<tr>");
            html.append("<td style='border: 1px solid #ddd; padding: 12px; text-align: right;'>").append(formatDate(Calendar.getInstance().getTimeInMillis())).append("</td>");
            html.append("<td style='border: 1px solid #ddd; padding: 12px; text-align: right;'>الرصيد السابق</td>");
            html.append("<td style='border: 1px solid #ddd; padding: 12px; text-align: right;'></td>");
            html.append("<td style='border: 1px solid #ddd; padding: 12px; text-align: right;'></td>");
            html.append("<td style='border: 1px solid #ddd; padding: 12px; text-align: right;'>").append(String.format(Locale.US, "%.2f", previousBalance)).append("</td>");
            html.append("</tr>");

            double runningBalance = previousBalance;
            double totalDebit = 0;
            double totalCredit = 0;

            // عرض العمليات
            for (Transaction t : currencyTransactions) {
                html.append("<tr>");
                html.append("<td style='border: 1px solid #ddd; padding: 12px; text-align: right;'>").append(formatDate(t.getDate())).append("</td>");
                html.append("<td style='border: 1px solid #ddd; padding: 12px; text-align: right;'>").append(t.getDescription()).append("</td>");
                
                if ("debit".equals(t.getType())) {
                    html.append("<td style='border: 1px solid #ddd; padding: 12px; text-align: right;'>").append(String.format(Locale.US, "%.2f", t.getAmount())).append("</td>");
                    html.append("<td style='border: 1px solid #ddd; padding: 12px; text-align: right;'></td>");
                    runningBalance -= t.getAmount();
                    totalDebit += t.getAmount();
                } else {
                    html.append("<td style='border: 1px solid #ddd; padding: 12px; text-align: right;'></td>");
                    html.append("<td style='border: 1px solid #ddd; padding: 12px; text-align: right;'>").append(String.format(Locale.US, "%.2f", t.getAmount())).append("</td>");
                    runningBalance += t.getAmount();
                    totalCredit += t.getAmount();
                }
                
                html.append("<td style='border: 1px solid #ddd; padding: 12px; text-align: right;'>").append(String.format(Locale.US, "%.2f", runningBalance)).append("</td>");
                html.append("</tr>");
            }

            // صف الإجمالي
            html.append("<tr style='background: #f0f0f0; font-weight: bold;'>");
            html.append("<td style='border: 1px solid #ddd; padding: 12px; text-align: right;' colspan='2'>الإجمالي</td>");
            html.append("<td style='border: 1px solid #ddd; padding: 12px; text-align: right;'>").append(String.format(Locale.US, "%.2f", totalDebit)).append("</td>");
            html.append("<td style='border: 1px solid #ddd; padding: 12px; text-align: right;'>").append(String.format(Locale.US, "%.2f", totalCredit)).append("</td>");
            html.append("<td style='border: 1px solid #ddd; padding: 12px; text-align: right;'></td>");
            html.append("</tr>");

            html.append("</table>");
            html.append("</div>");
        }

        return html.toString();
    }

    private String formatDate(long timestamp) {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
            .format(new Date(timestamp));
    }

    private void showError(String message) {
        String errorHtml = String.format(
            "<div style='text-align: center; padding: 20px; color: #dc3545;'>%s</div>",
            message
        );
        webView.evaluateJavascript(
            "document.getElementById('reportContent').innerHTML = '" + errorHtml + "'",
            null
        );
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 
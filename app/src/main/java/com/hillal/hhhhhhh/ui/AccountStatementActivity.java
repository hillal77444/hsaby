package com.hillal.hhhhhhh.ui;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.model.Transaction;
import com.hillal.hhhhhhh.viewmodel.AccountStatementViewModel;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import android.view.MenuItem;
import android.content.SharedPreferences;
import java.net.HttpURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

public class AccountStatementActivity extends AppCompatActivity {
    private WebView webView;
    private AccountStatementViewModel viewModel;
    private List<Account> allAccounts;
    private SimpleDateFormat dateFormat;
    private String selectedAccountId;
    private String startDate;
    private String endDate;
    private static final String PREF_NAME = "AccountStatementPrefs";
    private static final String KEY_HTML_CONTENT = "html_content";
    private static final String SERVER_URL = "http://10.0.2.2:5000/api/html-content";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_statement);

        // إعداد شريط التطبيق
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("كشف الحساب التفصيلي");
        }

        webView = findViewById(R.id.webView);
        viewModel = new ViewModelProvider(this).get(AccountStatementViewModel.class);
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        allAccounts = new ArrayList<>();

        setupWebView();
        loadAccounts();
        loadHtmlContent();
    }

    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.addJavascriptInterface(new WebAppInterface(), "Android");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                loadInitialData();
            }
        });
    }

    private void loadHtmlContent() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String savedHtml = prefs.getString(KEY_HTML_CONTENT, null);
        
        if (savedHtml != null) {
            webView.loadDataWithBaseURL(null, savedHtml, "text/html", "UTF-8", null);
        } else {
            webView.loadDataWithBaseURL(null, generateInitialHtml(), "text/html", "UTF-8", null);
        }
    }

    private void saveHtmlContent(String html) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit().putString(KEY_HTML_CONTENT, html).apply();
    }

    private void fetchUpdatedHtmlFromServer() {
        new Thread(() -> {
            try {
                URL url = new URL(SERVER_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    String newHtml = response.toString();
                    saveHtmlContent(newHtml);
                    
                    runOnUiThread(() -> {
                        webView.evaluateJavascript("updateContent('" + newHtml + "')", null);
                    });
                }
                
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "فشل تحديث المحتوى", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private String generateInitialHtml() {
        return """
            <!DOCTYPE html>
            <html dir="rtl">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        margin: 0;
                        padding: 16px;
                        background-color: #f5f5f5;
                    }
                    .card {
                        background: white;
                        border-radius: 8px;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                        padding: 16px;
                        margin-bottom: 16px;
                    }
                    .form-group {
                        margin-bottom: 16px;
                    }
                    select, input {
                        width: 100%;
                        padding: 12px;
                        border: 1px solid #ddd;
                        border-radius: 8px;
                        margin-top: 8px;
                        font-size: 16px;
                        background-color: #fff;
                        box-shadow: 0 1px 3px rgba(0,0,0,0.1);
                    }
                    select:focus, input:focus {
                        outline: none;
                        border-color: #2196F3;
                        box-shadow: 0 0 0 2px rgba(33,150,243,0.2);
                    }
                    .date-container {
                        display: flex;
                        gap: 16px;
                        margin-bottom: 20px;
                    }
                    .date-container > div {
                        flex: 1;
                    }
                    button {
                        background-color: #2196F3;
                        color: white;
                        border: none;
                        padding: 14px;
                        border-radius: 8px;
                        width: 100%;
                        cursor: pointer;
                        font-size: 16px;
                        font-weight: bold;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                        transition: all 0.3s ease;
                    }
                    button:hover {
                        background-color: #1976D2;
                        box-shadow: 0 4px 8px rgba(0,0,0,0.2);
                    }
                    button:active {
                        transform: translateY(1px);
                    }
                    table {
                        width: 100%;
                        border-collapse: collapse;
                        margin-top: 16px;
                        background-color: white;
                        border-radius: 8px;
                        overflow: hidden;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    }
                    th, td {
                        border: 1px solid #ddd;
                        padding: 12px;
                        text-align: right;
                    }
                    th {
                        background-color: #f5f5f5;
                        font-weight: bold;
                    }
                    .currency-header {
                        background-color: #e3f2fd;
                        padding: 12px;
                        margin-top: 20px;
                        border-radius: 8px;
                        font-weight: bold;
                        color: #1976D2;
                    }
                    label {
                        font-size: 16px;
                        color: #333;
                        font-weight: bold;
                        display: block;
                        margin-bottom: 4px;
                    }
                    .refresh-button {
                        background-color: #4CAF50;
                        color: white;
                        border: none;
                        padding: 10px 20px;
                        border-radius: 8px;
                        cursor: pointer;
                        margin-bottom: 16px;
                        width: 100%;
                        font-size: 16px;
                        font-weight: bold;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        gap: 8px;
                    }
                    .refresh-button:hover {
                        background-color: #45a049;
                    }
                    .refresh-button:active {
                        transform: translateY(1px);
                    }
                </style>
            </head>
            <body>
                <div class="card">
                    <button onclick="refreshContent()" class="refresh-button">
                        <span>🔄</span> تحديث المحتوى
                    </button>
                    <div class="form-group">
                        <label for="accountDropdown">اختر الحساب</label>
                        <select id="accountDropdown" onchange="onAccountSelected()">
                            <option value="">-- اختر الحساب --</option>
                        </select>
                    </div>
                    <div class="date-container">
                        <div class="form-group">
                            <label for="startDateInput">من تاريخ</label>
                            <input type="date" id="startDateInput" onchange="onDateChanged()">
                        </div>
                        <div class="form-group">
                            <label for="endDateInput">إلى تاريخ</label>
                            <input type="date" id="endDateInput" onchange="onDateChanged()">
                        </div>
                    </div>
                    <button onclick="showReport()">عرض التقرير</button>
                </div>
                <div id="reportContainer"></div>

                <script>
                    function onAccountSelected() {
                        const accountId = document.getElementById('accountDropdown').value;
                        if (accountId) {
                            Android.onAccountSelected(accountId);
                        }
                    }

                    function onDateChanged() {
                        Android.onDateChanged();
                    }

                    function showReport() {
                        Android.showReport();
                    }

                    function updateAccounts(accounts) {
                        const dropdown = document.getElementById('accountDropdown');
                        dropdown.innerHTML = '<option value="">-- اختر الحساب --</option>' + accounts;
                    }

                    function updateDates(startDate, endDate) {
                        document.getElementById('startDateInput').value = startDate;
                        document.getElementById('endDateInput').value = endDate;
                    }

                    function updateReport(html) {
                        document.getElementById('reportContainer').innerHTML = html;
                    }

                    function refreshContent() {
                        Android.refreshContent();
                    }

                    function updateContent(newHtml) {
                        const parser = new DOMParser();
                        const newDoc = parser.parseFromString(newHtml, 'text/html');
                        const newContent = newDoc.querySelector('.card').innerHTML;
                        document.querySelector('.card').innerHTML = newContent;
                    }
                </script>
            </body>
            </html>
            """;
    }

    private void loadAccounts() {
        viewModel.getAllAccounts().observe(this, accounts -> {
            if (accounts != null && !accounts.isEmpty()) {
            allAccounts = accounts;
                StringBuilder options = new StringBuilder();
                for (Account account : accounts) {
                    options.append("<option value='").append(account.getId()).append("'>")
                           .append(account.getName())
                           .append("</option>");
                }
                
                String js = String.format("updateAccounts('%s');", options.toString().replace("'", "\\'"));
                webView.evaluateJavascript(js, null);
            } else {
                Toast.makeText(this, "لا توجد حسابات متاحة", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadInitialData() {
        // تعيين تاريخ النهاية (اليوم الحالي)
        Calendar calendar = Calendar.getInstance();
        endDate = dateFormat.format(calendar.getTime());
        
        // تعيين تاريخ البداية (قبل 3 أيام)
        calendar.add(Calendar.DAY_OF_MONTH, -3);
        startDate = dateFormat.format(calendar.getTime());

        String js = String.format("updateDates('%s', '%s');", startDate, endDate);
        webView.evaluateJavascript(js, null);
    }

    private void updateReport() {
        if (selectedAccountId == null || selectedAccountId.isEmpty()) {
            Toast.makeText(this, "الرجاء اختيار حساب", Toast.LENGTH_SHORT).show();
            return;
        }

        if (startDate == null || endDate == null) {
            Toast.makeText(this, "الرجاء تحديد الفترة الزمنية", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Date start = dateFormat.parse(startDate);
            Date end = dateFormat.parse(endDate);

            Calendar cal = Calendar.getInstance();
            cal.setTime(end);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);
            Date endOfDay = cal.getTime();

            if (start.after(endOfDay)) {
                Toast.makeText(this, "تاريخ البداية يجب أن يكون قبل تاريخ النهاية", Toast.LENGTH_SHORT).show();
                return;
            }

            long accountId = Long.parseLong(selectedAccountId);
                    viewModel.getTransactionsForAccountInDateRange(
                accountId,
                start,
                endOfDay
                    ).observe(this, transactions -> {
                String html = generateReportHtml(transactions);
                String js = String.format("updateReport('%s');", html.replace("'", "\\'"));
                webView.evaluateJavascript(js, null);
            });
        } catch (Exception e) {
            Toast.makeText(this, "خطأ في تنسيق التاريخ", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private String generateReportHtml(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            return "<div class='card'>لا توجد معاملات في هذه الفترة</div>";
        }

        Map<String, List<Transaction>> transactionsByCurrency = transactions.stream()
            .collect(Collectors.groupingBy(Transaction::getCurrency));

        StringBuilder html = new StringBuilder();
        
        for (Map.Entry<String, List<Transaction>> entry : transactionsByCurrency.entrySet()) {
            String currency = entry.getKey();
            List<Transaction> currencyTransactions = entry.getValue();
            
            Collections.sort(currencyTransactions, (t1, t2) -> Long.compare(t1.getDate(), t2.getDate()));
            
            double previousBalance = 0;
            long firstTransactionDate = currencyTransactions.get(0).getDate();
            for (Transaction t : currencyTransactions) {
                if (t.getDate() < firstTransactionDate) {
                    if (t.getType().equals("عليه") || t.getType().equalsIgnoreCase("debit")) {
                        previousBalance -= t.getAmount();
                    } else {
                        previousBalance += t.getAmount();
                    }
                }
            }

            html.append("<div class='card'>");
            html.append("<div class='currency-header'>").append(currency).append("</div>");
            html.append("<table>");
            html.append("<tr><th>التاريخ</th><th>الوصف</th><th>عليه</th><th>له</th><th>الرصيد</th></tr>");
            
            html.append("<tr>");
            html.append("<td>").append(dateFormat.format(new Date(firstTransactionDate))).append("</td>");
            html.append("<td>الرصيد السابق</td>");
            html.append("<td></td><td></td>");
            html.append("<td>").append(String.format(Locale.ENGLISH, "%.2f", previousBalance)).append("</td>");
            html.append("</tr>");

            double runningBalance = previousBalance;
            double totalDebit = 0;
            double totalCredit = 0;

            for (Transaction t : currencyTransactions) {
                    html.append("<tr>");
                html.append("<td>").append(dateFormat.format(new Date(t.getDate()))).append("</td>");
                html.append("<td>").append(t.getDescription()).append("</td>");
                
                if (t.getType().equals("عليه") || t.getType().equalsIgnoreCase("debit")) {
                    html.append("<td>").append(String.format(Locale.ENGLISH, "%.2f", t.getAmount())).append("</td>");
                        html.append("<td></td>");
                    totalDebit += t.getAmount();
                    runningBalance -= t.getAmount();
                    } else {
                        html.append("<td></td>");
                    html.append("<td>").append(String.format(Locale.ENGLISH, "%.2f", t.getAmount())).append("</td>");
                    totalCredit += t.getAmount();
                    runningBalance += t.getAmount();
                }
                
                html.append("<td>").append(String.format(Locale.ENGLISH, "%.2f", runningBalance)).append("</td>");
                html.append("</tr>");
            }

            html.append("<tr style='font-weight: bold;'>");
            html.append("<td colspan='2'>المجموع</td>");
            html.append("<td>").append(String.format(Locale.ENGLISH, "%.2f", totalDebit)).append("</td>");
            html.append("<td>").append(String.format(Locale.ENGLISH, "%.2f", totalCredit)).append("</td>");
            html.append("<td>").append(String.format(Locale.ENGLISH, "%.2f", runningBalance)).append("</td>");
            html.append("</tr>");
            
            html.append("</table>");
            html.append("</div>");
        }

        return html.toString();
    }

    private class WebAppInterface {
        @JavascriptInterface
        public void onAccountSelected(String accountId) {
            selectedAccountId = accountId;
            updateReport();
        }

        @JavascriptInterface
        public void onDateChanged() {
            runOnUiThread(() -> {
                webView.evaluateJavascript(
                    "(function() { return { endDate: document.getElementById('endDateInput').value, startDate: document.getElementById('startDateInput').value }; })();",
                    value -> {
                        try {
                            value = value.replace("\\", "");
                            if (value.startsWith("\"") && value.endsWith("\"")) {
                                value = value.substring(1, value.length() - 1);
                            }
                            String[] dates = value.split(",");
                            endDate = dates[0].split(":")[1].replace("\"", "");
                            startDate = dates[1].split(":")[1].replace("\"", "");
                            updateReport();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                );
            });
        }

        @JavascriptInterface
        public void showReport() {
            updateReport();
        }

        @JavascriptInterface
        public void refreshContent() {
            runOnUiThread(() -> {
                fetchUpdatedHtmlFromServer();
            });
        }
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
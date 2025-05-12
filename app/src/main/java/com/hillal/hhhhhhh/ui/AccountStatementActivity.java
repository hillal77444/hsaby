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
import org.json.JSONObject;

public class AccountStatementActivity extends AppCompatActivity {
    private WebView webView;
    private AccountStatementViewModel viewModel;
    private List<Account> allAccounts;
    private SimpleDateFormat dateFormat;
    private String selectedAccountId;
    private String startDate;
    private String endDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_statement);

        webView = findViewById(R.id.webView);
        viewModel = new ViewModelProvider(this).get(AccountStatementViewModel.class);
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        dateFormat.setTimeZone(TimeZone.getDefault());
        allAccounts = new ArrayList<>();

        setupWebView();
        loadAccounts();
    }

    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new WebAppInterface(), "Android");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                loadInitialData();
            }
        });

        String html = generateInitialHtml();
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
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
                        padding: 8px;
                        border: 1px solid #ddd;
                        border-radius: 4px;
                        margin-top: 4px;
                        font-size: 16px;
                    }
                    .date-container {
                        display: flex;
                        gap: 8px;
                    }
                    .date-container > div {
                        flex: 1;
                    }
                    button {
                        background-color: #2196F3;
                        color: white;
                        border: none;
                        padding: 12px;
                        border-radius: 4px;
                        width: 100%;
                        cursor: pointer;
                        font-size: 16px;
                    }
                    button:hover {
                        background-color: #1976D2;
                    }
                    table {
                        width: 100%;
                        border-collapse: collapse;
                        margin-top: 16px;
                    }
                    th, td {
                        border: 1px solid #ddd;
                        padding: 8px;
                        text-align: right;
                    }
                    th {
                        background-color: #f5f5f5;
                    }
                    .currency-header {
                        background-color: #e3f2fd;
                        padding: 8px;
                        margin-top: 16px;
                        border-radius: 4px;
                    }
                    label {
                        font-size: 16px;
                        color: #333;
                    }
                </style>
            </head>
            <body>
                <div class="card">
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
                </script>
            </body>
            </html>
            """;
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
                    "(function() { " +
                    "const startDate = document.getElementById('startDateInput').value; " +
                    "const endDate = document.getElementById('endDateInput').value; " +
                    "return JSON.stringify({startDate: startDate, endDate: endDate}); " +
                    "})();",
                    value -> {
                        try {
                            // Parse the JSON response
                            value = value.replace("\\", "");
                            if (value.startsWith("\"") && value.endsWith("\"")) {
                                value = value.substring(1, value.length() - 1);
                            }
                            
                            // Parse JSON string
                            JSONObject json = new JSONObject(value);
                            startDate = json.getString("startDate");
                            endDate = json.getString("endDate");
                            
                            if (!startDate.isEmpty() && !endDate.isEmpty()) {
                                updateReport();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(AccountStatementActivity.this, 
                                "خطأ في قراءة التاريخ", Toast.LENGTH_SHORT).show();
                        }
                    }
                );
            });
        }

        @JavascriptInterface
        public void showReport() {
            updateReport();
        }
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
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        calendar.add(Calendar.DAY_OF_MONTH, -3);
        startDate = dateFormat.format(calendar.getTime());
        
        calendar = Calendar.getInstance(TimeZone.getDefault());
        endDate = dateFormat.format(calendar.getTime());

        String js = String.format("updateDates('%s', '%s');", startDate, endDate);
        webView.evaluateJavascript(js, null);
    }

    private void updateReport() {
        if (selectedAccountId == null || selectedAccountId.isEmpty()) {
            Toast.makeText(this, "الرجاء اختيار حساب", Toast.LENGTH_SHORT).show();
            return;
        }

        if (startDate == null || startDate.isEmpty() || endDate == null || endDate.isEmpty()) {
            Toast.makeText(this, "الرجاء تحديد الفترة الزمنية", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // تحويل التواريخ من HTML5 date input (yyyy-MM-dd) إلى Date
            Calendar cal = Calendar.getInstance(TimeZone.getDefault());
            
            // تحويل تاريخ البداية
            String[] startParts = startDate.split("-");
            cal.set(Calendar.YEAR, Integer.parseInt(startParts[0]));
            cal.set(Calendar.MONTH, Integer.parseInt(startParts[1]) - 1);
            cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(startParts[2]));
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date start = cal.getTime();

            // تحويل تاريخ النهاية
            String[] endParts = endDate.split("-");
            cal.set(Calendar.YEAR, Integer.parseInt(endParts[0]));
            cal.set(Calendar.MONTH, Integer.parseInt(endParts[1]) - 1);
            cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(endParts[2]));
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);
            Date endOfDay = cal.getTime();

            // تحويل معرف الحساب
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
        } catch (NumberFormatException e) {
            Toast.makeText(this, "خطأ في معرف الحساب", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } catch (Exception e) {
            Toast.makeText(this, "خطأ في تنسيق التاريخ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                    previousBalance += t.getAmount();
                }
            }

            html.append("<div class='card'>");
            html.append("<div class='currency-header'>").append(currency).append("</div>");
            html.append("<table>");
            html.append("<tr><th>التاريخ</th><th>الوصف</th><th>مدين</th><th>دائن</th><th>الرصيد</th></tr>");
            
            // Add previous balance row
            html.append("<tr>");
            html.append("<td>").append(formatDate(new Date(firstTransactionDate))).append("</td>");
            html.append("<td>الرصيد السابق</td>");
            html.append("<td></td><td></td>");
            html.append("<td>").append(String.format(Locale.ENGLISH, "%.2f", previousBalance)).append("</td>");
            html.append("</tr>");

            double runningBalance = previousBalance;
            double totalDebit = 0;
            double totalCredit = 0;

            for (Transaction t : currencyTransactions) {
                html.append("<tr>");
                html.append("<td>").append(formatDate(new Date(t.getDate()))).append("</td>");
                html.append("<td>").append(t.getDescription()).append("</td>");
                
                if (t.getAmount() > 0) {
                    html.append("<td>").append(String.format(Locale.ENGLISH, "%.2f", t.getAmount())).append("</td>");
                    html.append("<td></td>");
                    totalDebit += t.getAmount();
                } else {
                    html.append("<td></td>");
                    html.append("<td>").append(String.format(Locale.ENGLISH, "%.2f", -t.getAmount())).append("</td>");
                    totalCredit += -t.getAmount();
                }
                
                runningBalance += t.getAmount();
                html.append("<td>").append(String.format(Locale.ENGLISH, "%.2f", runningBalance)).append("</td>");
                html.append("</tr>");
            }

            // Add totals row
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

    private String formatDate(Date date) {
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        cal.setTime(date);
        return String.format(Locale.ENGLISH, "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH));
    }
} 
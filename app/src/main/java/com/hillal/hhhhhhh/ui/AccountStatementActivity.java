package com.hillal.hhhhhhh.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.model.Transaction;
import com.hillal.hhhhhhh.viewmodel.AccountStatementViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.text.ParseException;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;

public class AccountStatementActivity extends AppCompatActivity {
    private AccountStatementViewModel viewModel;
    private AutoCompleteTextView accountDropdown;
    private TextInputEditText startDateInput, endDateInput;
    private MaterialButton btnShowReport;
    private WebView webView;
    private Calendar calendar;
    private SimpleDateFormat dateFormat;
    // قائمة الحسابات للاستخدام الداخلي
    private List<Account> allAccounts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_statement);

        // إعداد شريط التطبيق
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("كشف الحساب التفصيلي");
        }

        // تهيئة المتغيرات
        calendar = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // تهيئة ViewModel
        viewModel = new ViewModelProvider(this).get(AccountStatementViewModel.class);

        // تهيئة واجهة المستخدم
        initializeViews();
        setupDatePickers();
        loadAccounts();
        setupWebView();

        // تعيين التواريخ الافتراضية
        setDefaultDates();
    }

    private void initializeViews() {
        accountDropdown = findViewById(R.id.accountDropdown);
        startDateInput = findViewById(R.id.startDateInput);
        endDateInput = findViewById(R.id.endDateInput);
        btnShowReport = findViewById(R.id.btnShowReport);
        webView = findViewById(R.id.webView);

        btnShowReport.setOnClickListener(v -> showReport());
    }

    private void setupDatePickers() {
        startDateInput.setOnClickListener(v -> showDatePicker(startDateInput));
        endDateInput.setOnClickListener(v -> showDatePicker(endDateInput));
    }

    private void showDatePicker(TextInputEditText input) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                input.setText(dateFormat.format(calendar.getTime()));
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void loadAccounts() {
        viewModel.getAllAccounts().observe(this, accounts -> {
            if (accounts == null) return;
            allAccounts = accounts;
            List<String> accountNames = new ArrayList<>();
            for (Account acc : accounts) {
                accountNames.add(acc.getName());
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                accountNames
            );
            accountDropdown.setAdapter(adapter);
            
            // تفعيل البحث الفوري
            accountDropdown.setOnItemClickListener((parent, view, position, id) -> {
                String selectedAccountName = (String) parent.getItemAtPosition(position);
                // يمكنك إضافة أي إجراء إضافي هنا عند اختيار الحساب
            });
            
            // تفعيل البحث أثناء الكتابة
            accountDropdown.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    adapter.getFilter().filter(s);
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        });
    }

    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
    }

    private void showReport() {
        String selectedAccountName = accountDropdown.getText().toString();
        String startDate = startDateInput.getText().toString();
        String endDate = endDateInput.getText().toString();

        if (selectedAccountName.isEmpty()) {
            Toast.makeText(this, "الرجاء اختيار الحساب", Toast.LENGTH_SHORT).show();
            return;
        }

        if (startDate.isEmpty() || endDate.isEmpty()) {
            Toast.makeText(this, "الرجاء تحديد الفترة الزمنية", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Date start = dateFormat.parse(startDate);
            Date end = dateFormat.parse(endDate);

            // اجعل endDate نهاية اليوم
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

            // البحث عن الحساب المحدد
            viewModel.getAllAccounts().observe(this, accounts -> {
                final Account selectedAccount = getSelectedAccount(accounts, selectedAccountName);
                final Date startFinal = start;
                final Date endFinal = endOfDay;

                if (selectedAccount != null) {
                    viewModel.getTransactionsForAccountInDateRange(
                        selectedAccount.getId(),
                        startFinal,
                        endFinal
                    ).observe(this, transactions -> {
                        String htmlContent = generateReportHtml(selectedAccount, startFinal, endFinal, transactions);
                        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
                    });
                } else {
                    Toast.makeText(this, "لم يتم العثور على الحساب المحدد", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (ParseException e) {
            Toast.makeText(this, "خطأ في تنسيق التاريخ", Toast.LENGTH_SHORT).show();
        }
    }

    private String generateReportHtml(Account account, Date startDate, Date endDate, List<Transaction> transactions) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html dir='rtl' lang='ar'>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; }");
        html.append("table { width: 100%; border-collapse: collapse; margin-top: 20px; }");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: right; }");
        html.append("th { background-color: #f5f5f5; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        
        // عنوان التقرير
        html.append("<h2>كشف الحساب التفصيلي</h2>");
        html.append("<p>الحساب: ").append(account.getName()).append("</p>");
        html.append("<p>الفترة: من ").append(dateFormat.format(startDate)).append(" إلى ").append(dateFormat.format(endDate)).append("</p>");

        // افصل المعاملات حسب العملة
        Map<String, List<Transaction>> currencyMap = new HashMap<>();
        for (Transaction transaction : transactions) {
            String currency = transaction.getCurrency();
            if (!currencyMap.containsKey(currency)) {
                currencyMap.put(currency, new ArrayList<>());
            }
            currencyMap.get(currency).add(transaction);
        }

        for (String currency : currencyMap.keySet()) {
            List<Transaction> allCurrencyTransactions = currencyMap.get(currency);
            // رتب العمليات من الأقدم إلى الأحدث
            sortTransactionsByDate(allCurrencyTransactions);
            // احسب الرصيد السابق (كل العمليات قبل startDate)
            double previousBalance = 0;
            for (Transaction t : allCurrencyTransactions) {
                if (t.getDate() < startDate.getTime()) {
                    if (t.getType().equals("عليه") || t.getType().equalsIgnoreCase("debit")) {
                        previousBalance -= t.getAmount();
                    } else {
                        previousBalance += t.getAmount();
                    }
                }
            }
            // احسب إجمالي المدين والدائن خلال الفترة
            double totalDebit = 0;
            double totalCredit = 0;
            for (Transaction t : allCurrencyTransactions) {
                if (t.getDate() >= startDate.getTime() && t.getDate() <= endDate.getTime()) {
                    if (t.getType().equals("عليه") || t.getType().equalsIgnoreCase("debit")) {
                        totalDebit += t.getAmount();
                    } else {
                        totalCredit += t.getAmount();
                    }
                }
            }
            html.append("<h3>العملة: ").append(currency).append("</h3>");
            html.append("<table>");
            html.append("<tr>");
            html.append("<th>التاريخ</th>");
            html.append("<th>الوصف</th>");
            html.append("<th>عليه</th>");
            html.append("<th>له</th>");
            html.append("<th>الرصيد</th>");
            html.append("</tr>");
            // صف الرصيد السابق
            html.append("<tr>");
            html.append("<td>").append(dateFormat.format(startDate)).append("</td>");
            html.append("<td>الرصيد السابق</td>");
            html.append("<td></td><td></td>");
            html.append("<td>").append(String.format(Locale.US, "%.2f", previousBalance)).append("</td>");
            html.append("</tr>");
            // العمليات خلال الفترة
            double runningBalance = previousBalance;
            for (Transaction transaction : allCurrencyTransactions) {
                if (transaction.getDate() >= startDate.getTime() && transaction.getDate() <= endDate.getTime()) {
                    html.append("<tr>");
                    html.append("<td>").append(dateFormat.format(transaction.getDate())).append("</td>");
                    html.append("<td>").append(transaction.getDescription()).append("</td>");
                    if (transaction.getType().equals("عليه") || transaction.getType().equalsIgnoreCase("debit")) {
                        html.append("<td>").append(String.format(Locale.US, "%.2f", transaction.getAmount())).append("</td>");
                        html.append("<td></td>");
                        runningBalance -= transaction.getAmount();
                    } else {
                        html.append("<td></td>");
                        html.append("<td>").append(String.format(Locale.US, "%.2f", transaction.getAmount())).append("</td>");
                        runningBalance += transaction.getAmount();
                    }
                    html.append("<td>").append(String.format(Locale.US, "%.2f", runningBalance)).append("</td>");
                    html.append("</tr>");
                }
            }
            // صف الإجمالي
            html.append("<tr style='font-weight:bold;background:#f0f0f0;'>");
            html.append("<td colspan='2'>الإجمالي خلال الفترة</td>");
            html.append("<td>").append(String.format(Locale.US, "%.2f", totalDebit)).append("</td>");
            html.append("<td>").append(String.format(Locale.US, "%.2f", totalCredit)).append("</td>");
            html.append("<td></td>");
            html.append("</tr>");
            html.append("</table>");
        }

        html.append("</body>");
        html.append("</html>");
        return html.toString();
    }

    private String formatAmount(double amount) {
        return String.format(Locale.getDefault(), "%.2f", amount);
    }

    private void setDefaultDates() {
        Calendar cal = Calendar.getInstance();
        String toDate = dateFormat.format(cal.getTime());
        cal.add(Calendar.DATE, -3); // أول أمس
        String fromDate = dateFormat.format(cal.getTime());
        startDateInput.setText(fromDate);
        endDateInput.setText(toDate);
    }

    private Account getSelectedAccount(List<Account> accounts, String selectedAccountName) {
        // استخدم القائمة الداخلية للبحث عن الحساب بالاسم
        for (Account acc : allAccounts) {
            if (acc.getName().equals(selectedAccountName)) {
                return acc;
            }
        }
        return null;
    }

    private void sortTransactionsByDate(List<Transaction> transactions) {
        Collections.sort(transactions, (a, b) -> Long.compare(a.getDate(), b.getDate()));
    }

    private boolean isTransactionInDateRange(Transaction transaction, Date startDate, Date endDate) {
        long transactionDate = transaction.getDate();
        return transactionDate >= startDate.getTime() && transactionDate <= endDate.getTime();
    }

    private void filterTransactionsByDateRange(List<Transaction> transactions, Date startDate, Date endDate) {
        transactions.removeIf(t -> !isTransactionInDateRange(t, startDate, endDate));
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
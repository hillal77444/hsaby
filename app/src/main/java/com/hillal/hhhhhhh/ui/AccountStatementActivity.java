package com.hillal.hhhhhhh.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
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

public class AccountStatementActivity extends AppCompatActivity {
    private AccountStatementViewModel viewModel;
    private AutoCompleteTextView accountDropdown;
    private TextInputEditText startDateInput, endDateInput;
    private MaterialButton btnShowReport;
    private WebView webView;
    private Calendar calendar;
    private SimpleDateFormat dateFormat;

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
            ArrayAdapter<Account> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                accounts
            );
            accountDropdown.setAdapter(adapter);
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

            if (start.after(end)) {
                Toast.makeText(this, "تاريخ البداية يجب أن يكون قبل تاريخ النهاية", Toast.LENGTH_SHORT).show();
                return;
            }

            // البحث عن الحساب المحدد
            viewModel.getAllAccounts().observe(this, accounts -> {
                Account selectedAccount = null;
                for (Account account : accounts) {
                    if (account.getName().equals(selectedAccountName)) {
                        selectedAccount = account;
                        break;
                    }
                }

                if (selectedAccount != null) {
                    // جلب المعاملات أولاً
                    viewModel.getTransactionsForAccountInDateRange(
                        selectedAccount.getId(),
                        start,
                        end
                    ).observe(this, transactions -> {
                        // بناء وعرض التقرير بعد وصول المعاملات
                        String htmlContent = generateReportHtml(selectedAccount, start, end, transactions);
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

        // جدول المعاملات
        html.append("<table>");
        html.append("<tr>");
        html.append("<th>التاريخ</th>");
        html.append("<th>الوصف</th>");
        html.append("<th>مدين</th>");
        html.append("<th>دائن</th>");
        html.append("<th>الرصيد</th>");
        html.append("</tr>");

        double runningBalance = 0;
        for (Transaction transaction : transactions) {
            html.append("<tr>");
            html.append("<td>").append(dateFormat.format(transaction.getDate())).append("</td>");
            html.append("<td>").append(transaction.getDescription()).append("</td>");
            if (transaction.getType().equals("debit")) {
                html.append("<td>").append(formatAmount(transaction.getAmount())).append("</td>");
                html.append("<td></td>");
                runningBalance -= transaction.getAmount();
            } else {
                html.append("<td></td>");
                html.append("<td>").append(formatAmount(transaction.getAmount())).append("</td>");
                runningBalance += transaction.getAmount();
            }
            html.append("<td>").append(formatAmount(runningBalance)).append("</td>");
            html.append("</tr>");
        }

        html.append("</table>");
        html.append("</body>");
        html.append("</html>");
        return html.toString();
    }

    private String formatAmount(double amount) {
        return String.format(Locale.getDefault(), "%.2f ريال", amount);
    }

    private void setDefaultDates() {
        Calendar cal = Calendar.getInstance();
        String toDate = dateFormat.format(cal.getTime());
        cal.add(Calendar.DATE, -2); // أول أمس
        String fromDate = dateFormat.format(cal.getTime());
        startDateInput.setText(fromDate);
        endDateInput.setText(toDate);
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
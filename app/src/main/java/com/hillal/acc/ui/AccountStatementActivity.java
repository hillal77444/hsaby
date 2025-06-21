package com.hillal.acc.ui;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.content.Context;
import android.widget.Button;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.LiveData;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.hillal.acc.R;
import com.hillal.acc.data.model.Account;
import com.hillal.acc.data.model.Transaction;
import com.hillal.acc.viewmodel.AccountStatementViewModel;
import com.github.florent37.singledateandtimepicker.dialog.SingleDateAndTimePickerDialog;
import com.hillal.acc.data.repository.TransactionRepository;
import com.hillal.acc.data.room.AppDatabase;
import com.hillal.acc.App;
import com.hillal.acc.ui.common.AccountPickerDialog;
import com.hillal.acc.ui.transactions.TransactionsViewModel;

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
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;

public class AccountStatementActivity extends AppCompatActivity {
    private AccountStatementViewModel viewModel;
    private TransactionsViewModel transactionsViewModel;
    private AutoCompleteTextView accountDropdown;
    private TextInputEditText startDateInput, endDateInput;
    private WebView webView;
    private Calendar calendar;
    private SimpleDateFormat dateFormat;
    private List<Account> allAccounts = new ArrayList<>();
    private List<Transaction> allTransactions = new ArrayList<>();
    private Map<Long, Map<String, Double>> accountBalancesMap = new HashMap<>();
    private TransactionRepository transactionRepository;
    private LinearLayout currencyButtonsLayout;
    private String selectedCurrency = null;
    private List<Transaction> lastAccountTransactions = new ArrayList<>();
    private Account lastSelectedAccount = null;
    private ImageButton btnPrint;

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
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

        // تهيئة ViewModels
        viewModel = new ViewModelProvider(this).get(AccountStatementViewModel.class);
        transactionsViewModel = new ViewModelProvider(this).get(TransactionsViewModel.class);

        // تهيئة واجهة المستخدم
        initializeViews();
        setupDatePickers();
        loadAccounts();

        // تعيين التواريخ الافتراضية
        setDefaultDates();

        transactionRepository = new TransactionRepository(((App) getApplication()).getDatabase());
    }

    private void initializeViews() {
        accountDropdown = findViewById(R.id.accountDropdown);
        startDateInput = findViewById(R.id.startDateInput);
        endDateInput = findViewById(R.id.endDateInput);
        webView = findViewById(R.id.webView);
        btnPrint = findViewById(R.id.btnPrintInCard);
        currencyButtonsLayout = findViewById(R.id.currencyButtonsLayout);
        currencyButtonsLayout.setVisibility(View.GONE);
        btnPrint.setOnClickListener(v -> printReport());
        accountDropdown.setFocusable(false);
        accountDropdown.setOnClickListener(v -> showAccountPicker());
        // TextWatcher للتواريخ
        startDateInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { updateReport(); }
        });
        endDateInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { updateReport(); }
        });
    }

    private void setupDatePickers() {
        startDateInput.setOnClickListener(v -> showDatePicker(startDateInput));
        endDateInput.setOnClickListener(v -> showDatePicker(endDateInput));
    }

    private void showDatePicker(TextInputEditText input) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_simple_date_picker);

        NumberPicker dayPicker = dialog.findViewById(R.id.dayPicker);
        NumberPicker monthPicker = dialog.findViewById(R.id.monthPicker);
        NumberPicker yearPicker = dialog.findViewById(R.id.yearPicker);
        TextView btnOk = dialog.findViewById(R.id.btnOk);
        TextView btnCancel = dialog.findViewById(R.id.btnCancel);

        // جلب التاريخ من الحقل إذا كان موجودًا
        Calendar cal = Calendar.getInstance();
        String currentText = input.getText() != null ? input.getText().toString() : "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            Date parsed = sdf.parse(toEnglishDigits(currentText));
            if (parsed != null) {
                cal.setTime(parsed);
            }
        } catch (Exception ignored) {}

        int selectedYear = cal.get(Calendar.YEAR);
        int selectedMonth = cal.get(Calendar.MONTH) + 1; // Calendar.MONTH يبدأ من 0
        int selectedDay = cal.get(Calendar.DAY_OF_MONTH);

        yearPicker.setMinValue(selectedYear - 50);
        yearPicker.setMaxValue(selectedYear + 10);
        yearPicker.setValue(selectedYear);
        yearPicker.setFormatter(value -> String.format(java.util.Locale.ENGLISH, "%d", value));

        String[] arabicMonths = {"يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"};
        monthPicker.setMinValue(1);
        monthPicker.setMaxValue(12);
        monthPicker.setDisplayedValues(arabicMonths);
        monthPicker.setValue(selectedMonth);
        monthPicker.setFormatter(value -> String.format(java.util.Locale.ENGLISH, "%d", value));

        dayPicker.setMinValue(1);
        dayPicker.setMaxValue(getDaysInMonth(selectedYear, selectedMonth));
        dayPicker.setValue(selectedDay);
        dayPicker.setFormatter(value -> String.format(java.util.Locale.ENGLISH, "%d", value));

        // تحديث الأيام حسب الشهر والسنة المختارة
        monthPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            int maxDay = getDaysInMonth(yearPicker.getValue(), newVal);
            dayPicker.setMaxValue(maxDay);
        });
        yearPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            int maxDay = getDaysInMonth(newVal, monthPicker.getValue());
            dayPicker.setMaxValue(maxDay);
        });

        btnOk.setOnClickListener(v -> {
            Calendar selectedCal = Calendar.getInstance();
            selectedCal.set(Calendar.YEAR, yearPicker.getValue());
            selectedCal.set(Calendar.MONTH, monthPicker.getValue() - 1);
            selectedCal.set(Calendar.DAY_OF_MONTH, dayPicker.getValue());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            String date = sdf.format(selectedCal.getTime());
            input.setText(toEnglishDigits(date));
            dialog.dismiss();
        });
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // جعل الـ Dialog يظهر من أسفل الشاشة
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = android.view.WindowManager.LayoutParams.MATCH_PARENT;
            params.gravity = android.view.Gravity.BOTTOM;
            dialog.getWindow().setAttributes(params);
        }

        dialog.show();
        setNumberPickerSelectionBg(dayPicker);
        setNumberPickerSelectionBg(monthPicker);
        setNumberPickerSelectionBg(yearPicker);
    }

    private int getDaysInMonth(int year, int month) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, 1);
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    private void loadAccounts() {
        // تحميل الحسابات
        viewModel.getAllAccounts().observe(this, accounts -> {
            if (accounts == null) return;
            allAccounts = accounts;
        });

        // تحميل المعاملات
        transactionsViewModel.getTransactions().observe(this, transactions -> {
            if (transactions != null) {
                allTransactions = transactions;
            }
        });

        // تحميل أرصدة الحسابات
        transactionsViewModel.getAccountBalancesMap().observe(this, balancesMap -> {
            if (balancesMap != null) {
                accountBalancesMap = balancesMap;
            }
        });
    }

    private void showAccountPicker() {
        if (allAccounts == null || allAccounts.isEmpty()) {
            Toast.makeText(this, "جاري تحميل الحسابات...", Toast.LENGTH_SHORT).show();
            return;
        }
        AccountPickerDialog dialog = new AccountPickerDialog(
            this,
            allAccounts,
            allTransactions,
            accountBalancesMap,
            account -> {
                accountDropdown.setText(account.getName());
                lastSelectedAccount = account;
                onAccountSelected(account);
            }
        );
        dialog.show();
    }

    private void onAccountSelected(Account account) {
        // استخدم allTransactions بدلاً من جلب البيانات من جديد
        List<Transaction> accountTransactions = new ArrayList<>();
        for (Transaction t : allTransactions) {
            if (t.getAccountId() == account.getId()) {
                accountTransactions.add(t);
            }
        }
        
        android.util.Log.d("AccountStatement", "Account ID: " + account.getId());
        android.util.Log.d("AccountStatement", "All transactions count: " + allTransactions.size());
        android.util.Log.d("AccountStatement", "Account transactions count: " + accountTransactions.size());
        
        lastAccountTransactions = accountTransactions;
        if (lastAccountTransactions.isEmpty()) {
            currencyButtonsLayout.setVisibility(View.GONE);
            webView.loadDataWithBaseURL(null, "<p>لا توجد بيانات</p>", "text/html", "UTF-8", null);
            return;
        }
        
        // طباعة تفاصيل المعاملات للفحص
        for (Transaction t : lastAccountTransactions) {
            android.util.Log.d("AccountStatement", "Transaction: ID=" + t.getId() + ", AccountID=" + t.getAccountId() + ", Amount=" + t.getAmount() + ", Currency=" + t.getCurrency());
        }
        
        java.util.LinkedHashSet<String> currencies = new java.util.LinkedHashSet<>();
        for (Transaction t : lastAccountTransactions) {
            currencies.add(t.getCurrency().trim());
        }
        
        android.util.Log.d("AccountStatement", "Currencies found: " + currencies);
        
        currencyButtonsLayout.removeAllViews();
        for (String currency : currencies) {
            MaterialButton btn = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            btn.setText(currency);
            btn.setCheckable(true);
            boolean isSelected = currency.equals(selectedCurrency) || (selectedCurrency == null && currencies.iterator().next().equals(currency));
            btn.setChecked(isSelected);
            
            // إعداد الألوان والتصميم
            btn.setTextColor(isSelected ? Color.WHITE : Color.parseColor("#1976d2"));
            btn.setBackgroundColor(isSelected ? Color.parseColor("#1976d2") : Color.parseColor("#e3f0ff"));
            btn.setCornerRadius(40); // حواف دائرية
            btn.setTextSize(16);
            
            // إضافة تباعد بين الأزرار
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(16);
            btn.setLayoutParams(params);
            
            btn.setOnClickListener(v -> setSelectedCurrency(currency));
            currencyButtonsLayout.addView(btn);
        }
        currencyButtonsLayout.setVisibility(View.VISIBLE);
        if (selectedCurrency == null || !currencies.contains(selectedCurrency)) {
            selectedCurrency = currencies.iterator().next();
        }
        setSelectedCurrency(selectedCurrency);
    }

    private void setSelectedCurrency(String currency) {
        selectedCurrency = currency;
        for (int i = 0; i < currencyButtonsLayout.getChildCount(); i++) {
            MaterialButton btn = (MaterialButton) currencyButtonsLayout.getChildAt(i);
            boolean isSelected = btn.getText().toString().equals(currency);
            btn.setChecked(isSelected);
            btn.setBackgroundColor(isSelected ? Color.parseColor("#1976d2") : Color.parseColor("#e3f0ff"));
            btn.setTextColor(isSelected ? Color.WHITE : Color.parseColor("#1976d2"));
        }
        updateReport();
    }

    private void updateReport() {
        if (lastSelectedAccount == null || selectedCurrency == null) return;
        
        Date startDate = null;
        Date endDate = null;
        
        try {
            startDate = dateFormat.parse(startDateInput.getText().toString());
            endDate = dateFormat.parse(endDateInput.getText().toString());
        } catch (Exception ignored) {
            return;
        }
        
        // إذا كانت التواريخ معكوسة، بدّل القيم
        if (startDate.after(endDate)) {
            Date temp = startDate;
            startDate = endDate;
            endDate = temp;
        }
        
        List<Transaction> filtered = new ArrayList<>();
        for (Transaction t : lastAccountTransactions) {
            if (t.getCurrency().trim().equals(selectedCurrency.trim())) {
                // تحويل تاريخ المعاملة إلى تاريخ فقط (بدون وقت)
                Calendar transactionCal = Calendar.getInstance();
                transactionCal.setTimeInMillis(t.getTransactionDate());
                transactionCal.set(Calendar.HOUR_OF_DAY, 0);
                transactionCal.set(Calendar.MINUTE, 0);
                transactionCal.set(Calendar.SECOND, 0);
                transactionCal.set(Calendar.MILLISECOND, 0);
                Date transactionDateOnly = transactionCal.getTime();
                
                // مقارنة التواريخ فقط (بدون وقت)
                if (!transactionDateOnly.before(startDate) && !transactionDateOnly.after(endDate)) {
                    filtered.add(t);
                }
            }
        }
        
        if (filtered.isEmpty()) {
            webView.loadDataWithBaseURL(null, "<p>لا توجد بيانات لهذه العملة أو الفترة</p>", "text/html", "UTF-8", null);
            return;
        }
        
        // استخدام بداية اليوم السابق للحصول على الرصيد السابق
        Calendar prevDayCal = Calendar.getInstance();
        prevDayCal.setTime(startDate);
        prevDayCal.add(Calendar.DAY_OF_MONTH, -1);
        prevDayCal.set(Calendar.HOUR_OF_DAY, 23);
        prevDayCal.set(Calendar.MINUTE, 59);
        prevDayCal.set(Calendar.SECOND, 59);
        prevDayCal.set(Calendar.MILLISECOND, 999);
        
        LiveData<Double> prevBalanceLive = transactionRepository.getBalanceUntilDate(
            lastSelectedAccount.getId(), 
            prevDayCal.getTimeInMillis(), 
            selectedCurrency
        );
        
        prevBalanceLive.observe(this, prevBalance -> {
            Map<String, Double> previousBalances = new HashMap<>();
            previousBalances.put(selectedCurrency, prevBalance != null ? prevBalance : 0.0);
            Map<String, List<Transaction>> currencyMap = new HashMap<>();
            currencyMap.put(selectedCurrency, filtered);
            String htmlContent = generateReportHtml(lastSelectedAccount, startDate, endDate, filtered, previousBalances, currencyMap);
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
        });
    }

    private String generateReportHtml(Account account, Date startDate, Date endDate, List<Transaction> transactions, Map<String, Double> previousBalances, Map<String, List<Transaction>> currencyMap) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html dir='rtl' lang='ar'>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<style>");
        html.append("body { font-family: 'Cairo', Arial, sans-serif; margin: 0; background: #f5f7fa; }");
        html.append(".report-header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); padding: 8px 6px 6px 6px; margin: 4px; text-align: center; color: white; }");
        html.append(".report-header p { color: #fff; margin: 1px 0; font-size: 0.8em; font-weight: 500; }");
        html.append(".report-header .account-info { font-size: 1em; font-weight: bold; margin-bottom: 3px; }");
        html.append(".report-header .period { font-size: 0.75em; opacity: 0.9; }");
        html.append("table { width: calc(100% - 8px); border-collapse: collapse; margin: 4px; background: #fff; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 6px rgba(0,0,0,0.08); }");
        html.append("th, td { border: 1px solid #e8eaed; padding: 6px 4px; text-align: right; font-size: 0.8em; }");
        html.append("th { background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%); color: #495057; font-weight: 600; font-size: 0.75em; }");
        html.append("tr:nth-child(even) { background: #f8f9fa; }");
        html.append("tr:hover { background: #e3f2fd; transition: background 0.2s; }");
        html.append(".balance-row { background: #e8f5e8 !important; font-weight: 500; }");
        html.append(".total-row { background: linear-gradient(135deg, #f0f0f0 0%, #e0e0e0 100%) !important; font-weight: bold; color: #2c3e50; }");
        html.append("@media print { .report-header { box-shadow: none; background: #f0f0f0 !important; color: #333 !important; border: 1px solid #ccc; } .report-header p { color: #333 !important; } table { box-shadow: none; } body { background: #fff; } }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div class='report-header'>");
        html.append("<p class='account-info'>الاسم: ").append(account.getName()).append(" العملة: ").append(selectedCurrency).append("</p>");
        html.append("<p class='period'>من <b>").append(dateFormat.format(startDate)).append("</b> إلى <b>").append(dateFormat.format(endDate)).append("</b></p>");
        html.append("</div>");
        sortTransactionsByDate(transactions);
        double previousBalance = previousBalances != null && previousBalances.containsKey(selectedCurrency) ? previousBalances.get(selectedCurrency) : 0;
        double totalDebit = 0;
        double totalCredit = 0;
        for (Transaction t : transactions) {
            if (t.getTransactionDate() >= startDate.getTime() && t.getTransactionDate() <= endDate.getTime()) {
                if (t.getType().equals("عليه") || t.getType().equalsIgnoreCase("debit")) {
                    totalDebit += t.getAmount();
                } else {
                    totalCredit += t.getAmount();
                }
            }
        }
        html.append("<table>");
        html.append("<tr>");
        html.append("<th>التاريخ</th>");
        html.append("<th>له</th>");
        html.append("<th>عليه</th>");
        html.append("<th>الوصف</th>");
        html.append("<th>الرصيد</th>");
        html.append("</tr>");
        html.append("<tr class='balance-row'>");
        html.append("<td>").append(dateFormat.format(startDate)).append("</td>");
        html.append("<td></td><td></td>");
        html.append("<td>الرصيد السابق</td>");
        html.append("<td>").append(String.format(Locale.US, "%.2f", previousBalance)).append("</td>");
        html.append("</tr>");
        double runningBalance = previousBalance;
        for (Transaction transaction : transactions) {
            if (transaction.getTransactionDate() >= startDate.getTime() && transaction.getTransactionDate() <= endDate.getTime()) {
                html.append("<tr>");
                html.append("<td>").append(dateFormat.format(new Date(transaction.getTransactionDate()))).append("</td>");
                if (transaction.getType().equals("عليه") || transaction.getType().equalsIgnoreCase("debit")) {
                    html.append("<td></td>");
                    html.append("<td style='color: #d32f2f;'>").append(String.format(Locale.US, "%.2f", transaction.getAmount())).append("</td>");
                } else {
                    html.append("<td style='color: #388e3c;'>").append(String.format(Locale.US, "%.2f", transaction.getAmount())).append("</td>");
                    html.append("<td></td>");
                }
                html.append("<td>").append(transaction.getDescription()).append("</td>");
                if (transaction.getType().equals("عليه") || transaction.getType().equalsIgnoreCase("debit")) {
                    runningBalance -= transaction.getAmount();
                } else {
                    runningBalance += transaction.getAmount();
                }
                html.append("<td style='font-weight: 500;'>").append(String.format(Locale.US, "%.2f", runningBalance)).append("</td>");
                html.append("</tr>");
            }
        }
        html.append("<tr class='total-row'>");
        html.append("<td>الإجمالي خلال الفترة</td>");
        html.append("<td style='color: #388e3c;'>").append(String.format(Locale.US, "%.2f", totalCredit)).append("</td>");
        html.append("<td style='color: #d32f2f;'>").append(String.format(Locale.US, "%.2f", totalDebit)).append("</td>");
        html.append("<td></td>");
        html.append("<td></td>");
        html.append("</tr>");
        html.append("</table>");
        html.append("</body>");
        html.append("</html>");
        return html.toString();
    }

    private String formatAmount(double amount) {
        return String.format(Locale.getDefault(), "%.2f", amount);
    }

    private void setDefaultDates() {
        // اجعل التاريخ الافتراضي يغطي كل المعاملات (من تاريخ قديم جدًا إلى اليوم)
        Calendar cal = Calendar.getInstance();
        String toDate = dateFormat.format(cal.getTime());
        cal.add(Calendar.YEAR, -10); // قبل 10 سنوات
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
        Collections.sort(transactions, (a, b) -> Long.compare(a.getTransactionDate(), b.getTransactionDate()));
    }

    private boolean isTransactionInDateRange(Transaction transaction, Date startDate, Date endDate) {
        long transactionDate = transaction.getTransactionDate();
        return transactionDate >= startDate.getTime() && transactionDate <= endDate.getTime();
    }

    private void filterTransactionsByDateRange(List<Transaction> transactions, Date startDate, Date endDate) {
        transactions.removeIf(t -> !isTransactionInDateRange(t, startDate, endDate));
    }

    private void printReport() {
        if (webView.getContentHeight() == 0) {
            Toast.makeText(this, "لا يوجد تقرير للطباعة", Toast.LENGTH_SHORT).show();
            return;
        }

        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        String jobName = getString(R.string.app_name) + " Document";

        PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter(jobName);

        printManager.print(jobName, printAdapter, new PrintAttributes.Builder().build());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private String toEnglishDigits(String value) {
        return value.replace("٠", "0")
                    .replace("١", "1")
                    .replace("٢", "2")
                    .replace("٣", "3")
                    .replace("٤", "4")
                    .replace("٥", "5")
                    .replace("٦", "6")
                    .replace("٧", "7")
                    .replace("٨", "8")
                    .replace("٩", "9");
    }

    private void setNumberPickerSelectionBg(NumberPicker picker) {
        try {
            java.lang.reflect.Field selectionDividerField = NumberPicker.class.getDeclaredField("mSelectionDivider");
            selectionDividerField.setAccessible(true);
            selectionDividerField.set(picker, getDrawable(R.drawable.picker_selected_bg));
        } catch (Exception e) {
            // تجاهل أي خطأ (قد لا يعمل على كل الأجهزة)
        }
    }
} 
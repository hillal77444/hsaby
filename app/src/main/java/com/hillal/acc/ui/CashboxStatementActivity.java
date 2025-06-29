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
import com.hillal.acc.data.entities.Cashbox;
import com.hillal.acc.data.model.Account;
import com.hillal.acc.data.model.Transaction;
import com.hillal.acc.viewmodel.CashboxViewModel;
import com.hillal.acc.viewmodel.AccountViewModel;
import com.hillal.acc.data.repository.TransactionRepository;
import com.hillal.acc.data.room.AppDatabase;
import com.hillal.acc.App;
import com.hillal.acc.ui.cashbox.CashboxPickerBottomSheet;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;

public class CashboxStatementActivity extends AppCompatActivity {
    private CashboxViewModel cashboxViewModel;
    private AccountViewModel accountViewModel;
    private AutoCompleteTextView cashboxDropdown;
    private TextInputEditText startDateInput, endDateInput;
    private WebView webView;
    private Calendar calendar;
    private SimpleDateFormat dateFormat;
    private List<Cashbox> allCashboxes = new ArrayList<>();
    private List<Transaction> allTransactions = new ArrayList<>();
    private TransactionRepository transactionRepository;
    private LinearLayout currencyButtonsLayout;
    private String selectedCurrency = null;
    private List<Transaction> lastCashboxTransactions = new ArrayList<>();
    private Cashbox lastSelectedCashbox = null;
    private ImageButton btnPrint;
    private Map<Long, Account> accountMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cashbox_statement);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("كشف الصندوق التفصيلي");
        }

        calendar = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

        cashboxViewModel = new ViewModelProvider(this).get(CashboxViewModel.class);
        accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);
        initializeViews();
        setupDatePickers();
        loadCashboxes();
        setDefaultDates();
        transactionRepository = new TransactionRepository(((App) getApplication()).getDatabase());
        loadAccountsMap();
    }

    private void initializeViews() {
        cashboxDropdown = findViewById(R.id.cashboxDropdown);
        startDateInput = findViewById(R.id.startDateInput);
        endDateInput = findViewById(R.id.endDateInput);
        webView = findViewById(R.id.webView);
        btnPrint = findViewById(R.id.btnPrintInCard);
        currencyButtonsLayout = findViewById(R.id.currencyButtonsLayout);
        currencyButtonsLayout.setVisibility(View.GONE);
        btnPrint.setOnClickListener(v -> printReport());
        cashboxDropdown.setFocusable(false);
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
        int selectedMonth = cal.get(Calendar.MONTH) + 1;
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

    private void loadCashboxes() {
        cashboxViewModel.getAllCashboxes().observe(this, cashboxes -> {
            if (cashboxes == null) return;
            allCashboxes = cashboxes;
            List<String> names = new ArrayList<>();
            for (Cashbox c : allCashboxes) names.add(c.name);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, names);
            cashboxDropdown.setAdapter(adapter);
            cashboxDropdown.setOnItemClickListener((parent, view, position, id) -> {
                Cashbox selected = allCashboxes.get(position);
                cashboxDropdown.setText(selected.name, false);
                lastSelectedCashbox = selected;
                onCashboxSelected(selected);
            });
            cashboxDropdown.setOnClickListener(v -> cashboxDropdown.showDropDown());
        });
        transactionRepository.getAllTransactions().observe(this, transactions -> {
            if (transactions != null) {
                allTransactions = transactions;
            }
        });
    }

    private void loadAccountsMap() {
        accountViewModel.getAllAccounts().observe(this, accounts -> {
            if (accounts != null) {
                for (Account acc : accounts) {
                    accountMap.put(acc.getId(), acc);
                }
            }
        });
    }

    private void showCashboxPicker() {
        // حذف الدالة بالكامل لأنها لم تعد مطلوبة
    }

    private void onCashboxSelected(Cashbox cashbox) {
        List<Transaction> cashboxTransactions = new ArrayList<>();
        for (Transaction t : allTransactions) {
            if (t.getCashboxId() == cashbox.id) {
                cashboxTransactions.add(t);
            }
        }
        lastCashboxTransactions = cashboxTransactions;
        if (lastCashboxTransactions.isEmpty()) {
            currencyButtonsLayout.setVisibility(View.GONE);
            webView.loadDataWithBaseURL(null, "<p>لا توجد بيانات</p>", "text/html", "UTF-8", null);
            return;
        }
        LinkedHashSet<String> currencies = new LinkedHashSet<>();
        for (Transaction t : lastCashboxTransactions) {
            currencies.add(t.getCurrency().trim());
        }
        currencyButtonsLayout.removeAllViews();
        for (String currency : currencies) {
            MaterialButton btn = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            btn.setText(currency);
            btn.setCheckable(true);
            boolean isSelected = currency.equals(selectedCurrency) || (selectedCurrency == null && currencies.iterator().next().equals(currency));
            btn.setChecked(isSelected);
            btn.setTextColor(isSelected ? Color.WHITE : Color.parseColor("#1976d2"));
            btn.setBackgroundColor(isSelected ? Color.parseColor("#1976d2") : Color.parseColor("#e3f0ff"));
            btn.setCornerRadius(40);
            btn.setTextSize(16);
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
        if (lastSelectedCashbox == null || selectedCurrency == null) return;
        Date startDate = null;
        Date endDate = null;
        try {
            startDate = dateFormat.parse(startDateInput.getText().toString());
            endDate = dateFormat.parse(endDateInput.getText().toString());
        } catch (Exception ignored) {
            return;
        }
        final Date finalStartDate;
        final Date finalEndDate;
        if (startDate.after(endDate)) {
            finalStartDate = endDate;
            finalEndDate = startDate;
        } else {
            finalStartDate = startDate;
            finalEndDate = endDate;
        }
        List<Transaction> filtered = new ArrayList<>();
        for (Transaction t : lastCashboxTransactions) {
            if (t.getCurrency().trim().equals(selectedCurrency.trim())) {
                Calendar transactionCal = Calendar.getInstance();
                transactionCal.setTimeInMillis(t.getTransactionDate());
                transactionCal.set(Calendar.HOUR_OF_DAY, 0);
                transactionCal.set(Calendar.MINUTE, 0);
                transactionCal.set(Calendar.SECOND, 0);
                transactionCal.set(Calendar.MILLISECOND, 0);
                Date transactionDateOnly = transactionCal.getTime();
                if (!transactionDateOnly.before(finalStartDate) && !transactionDateOnly.after(finalEndDate)) {
                    filtered.add(t);
                }
            }
        }
        if (filtered.isEmpty()) {
            webView.loadDataWithBaseURL(null, "<p>لا توجد بيانات لهذه العملة أو الفترة</p>", "text/html", "UTF-8", null);
            return;
        }
        Calendar prevDayCal = Calendar.getInstance();
        prevDayCal.setTime(finalStartDate);
        prevDayCal.add(Calendar.DAY_OF_MONTH, -1);
        prevDayCal.set(Calendar.HOUR_OF_DAY, 23);
        prevDayCal.set(Calendar.MINUTE, 59);
        prevDayCal.set(Calendar.SECOND, 59);
        prevDayCal.set(Calendar.MILLISECOND, 999);
        // الرصيد السابق غير مهم هنا للصندوق غالباً، يمكن تعديله لاحقاً إذا لزم
        Map<String, Double> previousBalances = new HashMap<>();
        previousBalances.put(selectedCurrency, 0.0);
        Map<String, List<Transaction>> currencyMap = new HashMap<>();
        currencyMap.put(selectedCurrency, filtered);
        String htmlContent = generateReportHtml(lastSelectedCashbox, finalStartDate, finalEndDate, filtered, previousBalances, currencyMap);
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
    }

    private String generateReportHtml(Cashbox cashbox, Date startDate, Date endDate, List<Transaction> transactions, Map<String, Double> previousBalances, Map<String, List<Transaction>> currencyMap) {
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
        html.append("<p class='account-info'>الصندوق: ").append(cashbox.name).append(" العملة: ").append(selectedCurrency).append("</p>");
        html.append("<p class='period'>من <b>").append(dateFormat.format(startDate)).append("</b> إلى <b>").append(dateFormat.format(endDate)).append("</b></p>");
        html.append("</div>");
        sortTransactionsByDate(transactions);
        double previousBalance = previousBalances != null && previousBalances.containsKey(selectedCurrency) ? previousBalances.get(selectedCurrency) : 0;
        double totalDebit = 0;
        double totalCredit = 0;
        Calendar startCal = Calendar.getInstance();
        startCal.setTime(startDate);
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        Calendar endCal = Calendar.getInstance();
        endCal.setTime(endDate);
        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        endCal.set(Calendar.MILLISECOND, 999);
        long startTime = startCal.getTimeInMillis();
        long endTime = endCal.getTimeInMillis();
        for (Transaction t : transactions) {
            if (t.getTransactionDate() >= startTime && t.getTransactionDate() <= endTime) {
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
        html.append("<th>اسم العميل</th>");
        html.append("<th>له</th>");
        html.append("<th>عليه</th>");
        html.append("<th>الوصف</th>");
        html.append("<th>الرصيد</th>");
        html.append("</tr>");
        html.append("<tr class='balance-row'>");
        html.append("<td>").append(dateFormat.format(startDate)).append("</td>");
        html.append("<td></td><td></td><td></td>");
        html.append("<td>الرصيد السابق</td>");
        html.append("<td>").append(String.format(Locale.US, "%.2f", previousBalance)).append("</td>");
        html.append("</tr>");
        double runningBalance = previousBalance;
        for (Transaction transaction : transactions) {
            if (transaction.getTransactionDate() >= startTime && transaction.getTransactionDate() <= endTime) {
                html.append("<tr>");
                html.append("<td>").append(dateFormat.format(new Date(transaction.getTransactionDate()))).append("</td>");
                // اسم العميل
                String clientName = accountMap.containsKey(transaction.getAccountId()) ? accountMap.get(transaction.getAccountId()).getName() : "-";
                html.append("<td>").append(clientName).append("</td>");
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
        html.append("<td></td>");
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
        Calendar cal = Calendar.getInstance();
        String toDate = dateFormat.format(cal.getTime());
        cal.add(Calendar.DATE, -4);
        String fromDate = dateFormat.format(cal.getTime());
        startDateInput.setText(fromDate);
        endDateInput.setText(toDate);
    }

    private void sortTransactionsByDate(List<Transaction> transactions) {
        Collections.sort(transactions, (a, b) -> Long.compare(a.getTransactionDate(), b.getTransactionDate()));
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
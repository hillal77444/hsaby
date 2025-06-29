package com.hillal.acc.ui.reports;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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
import com.hillal.acc.App;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.lang.StringBuilder;

public class CashboxStatementFragment extends Fragment {
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
    private long selectedCashboxId = -1;
    private long mainCashboxId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cashbox_statement, container, false);
        initializeViews(view);
        calendar = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        cashboxViewModel = new ViewModelProvider(this).get(CashboxViewModel.class);
        accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);
        transactionRepository = new TransactionRepository(((App) requireActivity().getApplication()).getDatabase());
        setupDatePickers();
        loadCashboxes();
        setDefaultDates();
        loadAccountsMap();
        // تفعيل التكبير والتصغير في WebView
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setSupportZoom(true);
        return view;
    }

    private void initializeViews(View view) {
        cashboxDropdown = view.findViewById(R.id.cashboxDropdown);
        startDateInput = view.findViewById(R.id.startDateInput);
        endDateInput = view.findViewById(R.id.endDateInput);
        webView = view.findViewById(R.id.webView);
        btnPrint = view.findViewById(R.id.btnPrintInCard);
        currencyButtonsLayout = view.findViewById(R.id.currencyButtonsLayout);
        currencyButtonsLayout.setVisibility(View.GONE);
        btnPrint.setOnClickListener(v -> printReport());
        cashboxDropdown.setFocusable(false);
        cashboxDropdown.setOnClickListener(v -> cashboxDropdown.showDropDown());
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
        // يمكنك نقل منطق DatePicker من الـ Activity هنا إذا كان لديك Dialog مخصص
        // أو استخدم DatePickerDialog عادي
        Calendar cal = Calendar.getInstance();
        String currentText = input.getText() != null ? input.getText().toString() : "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            Date parsed = sdf.parse(toEnglishDigits(currentText));
            if (parsed != null) {
                cal.setTime(parsed);
            }
        } catch (Exception ignored) {}
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        android.app.DatePickerDialog dialog = new android.app.DatePickerDialog(requireContext(), (view, y, m, d) -> {
            Calendar selectedCal = Calendar.getInstance();
            selectedCal.set(y, m, d);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            String date = sdf.format(selectedCal.getTime());
            input.setText(toEnglishDigits(date));
        }, year, month, day);
        dialog.show();
    }

    private void loadCashboxes() {
        LiveData<List<Cashbox>> cashboxesLiveData = cashboxViewModel.getAllCashboxes();
        if (cashboxesLiveData == null) {
            Toast.makeText(requireContext(), "حدث خطأ في تحميل الصناديق. الرجاء إعادة تشغيل التطبيق.", Toast.LENGTH_LONG).show();
            return;
        }
        cashboxesLiveData.observe(getViewLifecycleOwner(), cashboxes -> {
            allCashboxes = cashboxes != null ? cashboxes : new ArrayList<>();
            List<String> names = new ArrayList<>();
            for (Cashbox c : allCashboxes) {
                names.add(c.name);
            }
            if (!names.contains("➕ إضافة صندوق جديد...")) {
                names.add("➕ إضافة صندوق جديد...");
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, names);
            cashboxDropdown.setAdapter(adapter);
            cashboxDropdown.setText("", false);
            selectedCashboxId = -1;
            lastSelectedCashbox = null;
        });

        cashboxDropdown.setOnItemClickListener((parent, view, position, id) -> {
            if (position == allCashboxes.size()) {
                Toast.makeText(requireContext(), "ميزة إضافة صندوق جديد غير متوفرة بعد", Toast.LENGTH_SHORT).show();
            } else {
                lastSelectedCashbox = allCashboxes.get(position);
                selectedCashboxId = lastSelectedCashbox.id;
                onCashboxSelected(lastSelectedCashbox);
            }
        });

        transactionRepository.getAllTransactions().observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null) {
                allTransactions = transactions;
            }
        });
    }

    private void loadAccountsMap() {
        accountViewModel.getAllAccounts().observe(getViewLifecycleOwner(), accounts -> {
            if (accounts != null) {
                for (Account acc : accounts) {
                    accountMap.put(acc.getId(), acc);
                }
            }
        });
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
            MaterialButton btn = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
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
        html.append("<table>");
        html.append("<tr><th>التاريخ</th><th>الحساب</th><th>الوصف</th><th>دائن</th><th>مدين</th><th>الرصيد</th></tr>");
        double balance = previousBalance;
        for (Transaction t : transactions) {
            html.append("<tr>");
            html.append("<td>").append(dateFormat.format(new Date(t.getTransactionDate()))).append("</td>");
            String accountName = accountMap.containsKey(t.getAccountId()) ? accountMap.get(t.getAccountId()).getName() : "";
            html.append("<td>").append(accountName).append("</td>");
            html.append("<td>").append(t.getDescription() != null ? t.getDescription() : "").append("</td>");
            if (t.getType().equalsIgnoreCase("credit") || t.getType().equals("له")) {
                html.append("<td>").append(formatAmount(t.getAmount())).append("</td><td></td>");
                balance += t.getAmount();
                totalCredit += t.getAmount();
            } else {
                html.append("<td></td><td>").append(formatAmount(t.getAmount())).append("</td>");
                balance -= t.getAmount();
                totalDebit += t.getAmount();
            }
            html.append("<td>").append(formatAmount(balance)).append("</td>");
            html.append("</tr>");
        }
        html.append("<tr class='total-row'><td colspan='3'>الإجمالي</td><td>").append(formatAmount(totalCredit)).append("</td><td>").append(formatAmount(totalDebit)).append("</td><td>").append(formatAmount(balance)).append("</td></tr>");
        html.append("</table>");
        html.append("</body></html>");
        return html.toString();
    }

    private void setDefaultDates() {
        Calendar cal = Calendar.getInstance();
        endDateInput.setText(dateFormat.format(cal.getTime()));
        cal.set(Calendar.DAY_OF_MONTH, 1);
        startDateInput.setText(dateFormat.format(cal.getTime()));
    }

    private void printReport() {
        if (webView != null) {
            android.print.PrintManager printManager = (android.print.PrintManager) requireContext().getSystemService(android.content.Context.PRINT_SERVICE);
            android.print.PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter("كشف الصندوق");
            String jobName = "كشف الصندوق";
            printManager.print(jobName, printAdapter, new android.print.PrintAttributes.Builder().build());
        }
    }

    private String toEnglishDigits(String value) {
        return value.replace("٠", "0").replace("١", "1").replace("٢", "2").replace("٣", "3").replace("٤", "4")
                .replace("٥", "5").replace("٦", "6").replace("٧", "7").replace("٨", "8").replace("٩", "9");
    }

    private void sortTransactionsByDate(List<Transaction> transactions) {
        // Implementation of sortTransactionsByDate method
    }

    private String formatAmount(double amount) {
        if (Math.abs(amount - Math.round(amount)) < 0.01) {
            return String.format(Locale.US, "%,.0f", amount);
        } else {
            return String.format(Locale.US, "%,.2f", amount);
        }
    }
} 
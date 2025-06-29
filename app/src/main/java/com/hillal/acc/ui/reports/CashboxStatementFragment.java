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
        // يمكنك نقل منطق إنشاء HTML من الـ Activity هنا
        // ...
        return "<html><body>تقرير الصندوق</body></html>"; // اختصرنا هنا، انقل منطقك الأصلي
    }

    private void setDefaultDates() {
        Calendar cal = Calendar.getInstance();
        endDateInput.setText(dateFormat.format(cal.getTime()));
        cal.set(Calendar.DAY_OF_MONTH, 1);
        startDateInput.setText(dateFormat.format(cal.getTime()));
    }

    private void printReport() {
        // منطق الطباعة كما في الـ Activity
    }

    private String toEnglishDigits(String value) {
        return value.replace("٠", "0").replace("١", "1").replace("٢", "2").replace("٣", "3").replace("٤", "4")
                .replace("٥", "5").replace("٦", "6").replace("٧", "7").replace("٨", "8").replace("٩", "9");
    }
} 
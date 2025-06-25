package com.hillal.acc.ui.transactions;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hillal.acc.R;
import com.hillal.acc.data.model.Transaction;
import com.hillal.acc.databinding.FragmentTransactionsBinding;
import com.hillal.acc.ui.adapters.TransactionAdapter;
import com.hillal.acc.viewmodel.AccountViewModel;
import com.hillal.acc.data.model.Account;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import com.hillal.acc.App;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.hillal.acc.data.repository.AccountRepository;
import com.hillal.acc.ui.transactions.TransactionViewModelFactory;
import com.hillal.acc.data.remote.RetrofitClient;
import com.hillal.acc.data.remote.ApiService;
import com.hillal.acc.ui.common.AccountPickerDialog;
import com.hillal.acc.data.preferences.UserPreferences;
import com.google.android.material.appbar.MaterialToolbar;

public class TransactionsFragment extends Fragment {
    private FragmentTransactionsBinding binding;
    private TransactionsViewModel viewModel;
    private TransactionAdapter adapter;
    private AccountViewModel accountViewModel;
    private TransactionViewModel transactionViewModel;
    private com.hillal.acc.data.repository.TransactionRepository transactionRepository;
    private Calendar startDate;
    private Calendar endDate;
    private String selectedAccount = null;
    private List<Transaction> allTransactions = new ArrayList<>();
    private List<Account> allAccounts = new ArrayList<>();
    private Map<Long, Map<String, Double>> accountBalancesMap = new HashMap<>();
    private boolean isStartDate = true;
    private boolean isFirstLoad = true;
    private long lastSyncTime = 0;
    private Map<Long, Account> accountMap = new HashMap<>();
    private boolean isSearchActive = false; // متغير لتتبع حالة البحث
    private String currentSearchText = ""; // متغير لتخزين نص البحث الحالي

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App app = (App) requireActivity().getApplication();
        viewModel = new ViewModelProvider(this).get(TransactionsViewModel.class);
        accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);
        AccountRepository accountRepository = app.getAccountRepository();
        com.hillal.acc.ui.transactions.TransactionViewModelFactory factory = new com.hillal.acc.ui.transactions.TransactionViewModelFactory(accountRepository);
        transactionViewModel = new ViewModelProvider(this, factory).get(TransactionViewModel.class);
        transactionRepository = new com.hillal.acc.data.repository.TransactionRepository(app.getDatabase());
        setHasOptionsMenu(true);
        
        // تهيئة التواريخ الافتراضية
        startDate = Calendar.getInstance();
        startDate.add(Calendar.DAY_OF_MONTH, -4); // قبل 4 أيام
        startDate.set(Calendar.HOUR_OF_DAY, 0);
        startDate.set(Calendar.MINUTE, 0);
        startDate.set(Calendar.SECOND, 0);
        startDate.set(Calendar.MILLISECOND, 0);
        
        endDate = Calendar.getInstance(); // اليوم الحالي
        endDate.set(Calendar.HOUR_OF_DAY, 23);
        endDate.set(Calendar.MINUTE, 59);
        endDate.set(Calendar.SECOND, 59);
        endDate.set(Calendar.MILLISECOND, 999);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTransactionsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // تهيئة الـ RecyclerView والـ Adapter بشكل صحيح
        RecyclerView recyclerView = view.findViewById(R.id.transactionsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TransactionAdapter(new TransactionAdapter.TransactionDiffCallback(), requireContext(), getViewLifecycleOwner());
        recyclerView.setAdapter(adapter);

        // إعداد المستمعين للأزرار بشكل منفصل
        setupAdapterListeners();
        
        // إعداد الفلاتر
        setupAccountFilter();
        setupDateFilter();
        
        // إعداد FAB
        setupFab();
        
        // مراقبة البيانات
        observeAccountsAndTransactions();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.transactions_toolbar_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        if (searchView != null) {
            searchView.setQueryHint("بحث في الوصف...");
            searchView.setMaxWidth(Integer.MAX_VALUE);
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) { return true; }
                @Override
                public boolean onQueryTextChange(String newText) {
                    String query = newText.trim();
                    isSearchActive = !query.isEmpty(); // تحديث حالة البحث
                    currentSearchText = query.toLowerCase(); // تحديث نص البحث الحالي
                    if (query.isEmpty()) {
                        // عند إفراغ البحث، نعود للسلوك القديم
                        viewModel.loadTransactionsByDateRange(startDate.getTimeInMillis(), endDate.getTimeInMillis());
                    } else {
                        // عند البحث، نتجاهل التاريخ ونطبق الفلاتر الأخرى
                        applyAllFilters();
                    }
                    return true;
                }
            });
            
            // إضافة مستمع لإغلاق البحث
            searchView.setOnCloseListener(() -> {
                isSearchActive = false; // إعادة تعيين حالة البحث
                currentSearchText = ""; // إفراغ نص البحث
                applyAllFilters(); // إعادة تطبيق الفلاتر بالسلوك القديم
                return false;
            });
            
            // منع الإغلاق التلقائي عند فقدان التركيز (إغلاق الكيبورد فقط)
            searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus && !searchView.isIconified()) {
                    v.post(() -> searchView.requestFocus());
                }
            });
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void setupAdapterListeners() {
        // مستمع الحذف
        adapter.setOnDeleteClickListener(transaction -> {
            new AlertDialog.Builder(requireContext())
                .setTitle("تأكيد الحذف")
                .setMessage("هل أنت متأكد من حذف هذا القيد؟")
                .setPositiveButton("نعم", (dialog, which) -> {
                    if (!isNetworkAvailable()) {
                        Toast.makeText(requireContext(), "يرجى الاتصال بالإنترنت لحذف القيد", Toast.LENGTH_SHORT).show();
                        return;
                    }
        
                    // عرض مؤشر تحميل
                    ProgressDialog progressDialog = new ProgressDialog(requireContext());
                    progressDialog.setMessage("جاري حذف القيد...");
                    progressDialog.setCancelable(false);
                    progressDialog.show();
        
                    // الحصول على token المستخدم
                    String token = requireContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                            .getString("token", null);
        
                    if (token == null) {
                        progressDialog.dismiss();
                        Toast.makeText(requireContext(), "يرجى تسجيل الدخول أولاً", Toast.LENGTH_SHORT).show();
                        return;
                    }
        
                    // إرسال طلب الحذف إلى السيرفر
                    RetrofitClient.getApiService().deleteTransaction("Bearer " + token, transaction.getServerId())
                        .enqueue(new retrofit2.Callback<Void>() {
                            @Override
                            public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {
                                progressDialog.dismiss();
                                if (response.isSuccessful()) {
                                    // إذا نجح الحذف من السيرفر، نقوم بحذفه من قاعدة البيانات المحلية
                                    transactionViewModel.deleteTransaction(transaction);
                                    requireActivity().runOnUiThread(() -> {
                                        Toast.makeText(requireContext(), "تم حذف القيد بنجاح", Toast.LENGTH_SHORT).show();
                                    });
                                } else {
                                    requireActivity().runOnUiThread(() -> {
                                        Toast.makeText(requireContext(), "فشل في حذف القيد من السيرفر", Toast.LENGTH_SHORT).show();
                                    });
                                }
                            }
        
                            @Override
                            public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                                progressDialog.dismiss();
                                requireActivity().runOnUiThread(() -> {
                                    Toast.makeText(requireContext(), "خطأ في الاتصال بالسيرفر", Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
                })
                .setNegativeButton("لا", null)
                .show();
        });

        // مستمع التعديل
        adapter.setOnEditClickListener(transaction -> {
            Bundle args = new Bundle();
            args.putLong("transactionId", transaction.getId());
            Navigation.findNavController(requireView()).navigate(R.id.action_transactions_to_editTransaction, args);
        });

        // مستمع واتساب
        adapter.setOnWhatsAppClickListener((transaction, phoneNumber) -> {
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                Toast.makeText(requireContext(), "رقم الهاتف غير متوفر", Toast.LENGTH_SHORT).show();
                return;
            }

            // الحصول على معلومات الحساب
            accountViewModel.getAccountById(transaction.getAccountId()).observe(getViewLifecycleOwner(), account -> {
                if (account != null) {
                    // مراقبة الرصيد حتى التاريخ
                    transactionRepository.getBalanceUntilDate(transaction.getAccountId(), transaction.getTransactionDate(), transaction.getCurrency())
                        .observe(getViewLifecycleOwner(), balance -> {
                            if (balance != null) {
                                String type = transaction.getType();
                                String message = buildWhatsAppMessage(account.getName(), transaction, balance, type);
                                sendWhatsAppMessage(requireContext(), phoneNumber, message);
                            }
                        });
                }
            });
        });

        // مستمع SMS
        adapter.setOnSmsClickListener((transaction, phoneNumber) -> {
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                Toast.makeText(requireContext(), "رقم الهاتف غير متوفر", Toast.LENGTH_SHORT).show();
                return;
            }
            accountViewModel.getAccountById(transaction.getAccountId()).observe(getViewLifecycleOwner(), account -> {
                if (account != null) {
                    transactionRepository.getBalanceUntilDate(transaction.getAccountId(), transaction.getTransactionDate(), transaction.getCurrency())
                        .observe(getViewLifecycleOwner(), balance -> {
                            if (balance != null) {
                                String type = transaction.getType();
                                String amountStr = String.format(Locale.US, "%.0f", transaction.getAmount());
                                String balanceStr = String.format(Locale.US, "%.0f", Math.abs(balance));
                                String currency = transaction.getCurrency();
                                String typeText = (type.equalsIgnoreCase("credit") || type.equals("له")) ? "لكم" : "عليكم";
                                String balanceText = (balance >= 0) ? "الرصيد لكم " : "الرصيد عليكم ";
                                String message = "حسابكم لدينا:\n"
                                        + typeText + " " + amountStr + " " + currency + "\n"
                                        + transaction.getDescription() + "\n"
                                        + balanceText + balanceStr + " " + currency;
                                Intent smsIntent = new Intent(Intent.ACTION_VIEW);
                                smsIntent.setData(Uri.parse("smsto:" + phoneNumber));
                                smsIntent.putExtra("sms_body", message);
                                startActivity(smsIntent);
                            }
                        });
                }
            });
        });
    }

    private void setupAccountFilter() {
        // تحميل الحسابات
        accountViewModel.getAllAccounts().observe(getViewLifecycleOwner(), accounts -> {
            if (accounts != null) {
                allAccounts = accounts;
                // تحديث خريطة الحسابات
                accountMap.clear();
                for (Account account : accounts) {
                    accountMap.put(account.getId(), account);
                }
                // تعيين الخريطة للـ adapter بعد كل تحديث
                if (adapter != null) {
                    adapter.setAccountMap(accountMap);
                }
            }
        });

        // إعداد مستمع النقر على حقل اختيار الحساب
        binding.accountFilterDropdown.setFocusable(false);
        binding.accountFilterDropdown.setOnClickListener(v -> showAccountPicker());
    }

    private void showAccountPicker() {
        if (allAccounts == null || allAccounts.isEmpty()) {
            Toast.makeText(requireContext(), "جاري تحميل الحسابات...", Toast.LENGTH_SHORT).show();
            return;
        }

        AccountPickerDialog dialog = new AccountPickerDialog(
            requireContext(),
            allAccounts,
            allTransactions,
            accountBalancesMap,
            account -> {
                selectedAccount = account.getName();
                binding.accountFilterDropdown.setText(account.getName());
                applyAllFilters();
            }
        );
        dialog.show();
    }

    private void setupDateFilter() {
        updateDateInputs();
        binding.startDateFilter.setOnClickListener(v -> {
            showDatePicker(true);
        });
        binding.endDateFilter.setOnClickListener(v -> {
            showDatePicker(false);
        });
    }

    private void showDatePicker(boolean isStart) {
        // استخدم Dialog عجلة التاريخ مثل AccountStatementActivity
        android.app.Dialog dialog = new android.app.Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_simple_date_picker);

        android.widget.NumberPicker dayPicker = dialog.findViewById(R.id.dayPicker);
        android.widget.NumberPicker monthPicker = dialog.findViewById(R.id.monthPicker);
        android.widget.NumberPicker yearPicker = dialog.findViewById(R.id.yearPicker);
        android.widget.TextView btnOk = dialog.findViewById(R.id.btnOk);
        android.widget.TextView btnCancel = dialog.findViewById(R.id.btnCancel);

        // جلب التاريخ الحالي (بداية أو نهاية)
        java.util.Calendar cal = isStart ? (Calendar) startDate.clone() : (Calendar) endDate.clone();
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
            if (isStart) {
                // بداية اليوم
                selectedCal.set(Calendar.HOUR_OF_DAY, 0);
                selectedCal.set(Calendar.MINUTE, 0);
                selectedCal.set(Calendar.SECOND, 0);
                selectedCal.set(Calendar.MILLISECOND, 0);
                startDate = selectedCal;
            } else {
                // نهاية اليوم
                selectedCal.set(Calendar.HOUR_OF_DAY, 23);
                selectedCal.set(Calendar.MINUTE, 59);
                selectedCal.set(Calendar.SECOND, 59);
                selectedCal.set(Calendar.MILLISECOND, 999);
                endDate = selectedCal;
            }
            updateDateInputs();
            
            // إذا كان البحث نشط، نطبق الفلاتر مباشرة
            // وإلا نعيد تحميل البيانات بالتواريخ الجديدة
            if (isSearchActive) {
                applyAllFilters();
            } else {
                viewModel.loadTransactionsByDateRange(startDate.getTimeInMillis(), endDate.getTimeInMillis());
            }
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

        // تلوين العنصر المختار في كل NumberPicker بخلفية مخصصة (نفس الدالة)
        setNumberPickerSelectionBg(dayPicker);
        setNumberPickerSelectionBg(monthPicker);
        setNumberPickerSelectionBg(yearPicker);
    }

    // دالة لحساب عدد الأيام في الشهر
    private int getDaysInMonth(int year, int month) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, 1);
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    // دالة تلوين العنصر المختار في NumberPicker
    private void setNumberPickerSelectionBg(android.widget.NumberPicker picker) {
        try {
            java.lang.reflect.Field selectionDividerField = android.widget.NumberPicker.class.getDeclaredField("mSelectionDivider") ;
            selectionDividerField.setAccessible(true);
            selectionDividerField.set(picker, requireContext().getDrawable(R.drawable.picker_selected_bg));
        } catch (Exception e) {
            // تجاهل أي خطأ
        }
    }

    private void updateDateInputs() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH);
        binding.startDateFilter.setText(sdf.format(startDate.getTime()));
        binding.endDateFilter.setText(sdf.format(endDate.getTime()));
    }

    private void setupFab() {
        binding.fabAddTransaction.setOnClickListener(v -> {
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_transactions_to_addTransaction);
        });
    }

    private void observeAccountsAndTransactions() {
        // عرض مؤشر التحميل
        binding.progressBar.setVisibility(View.VISIBLE);
        
        // مراقبة الحسابات
        accountViewModel.getAllAccounts().observe(getViewLifecycleOwner(), accounts -> {
            if (accounts != null) {
                allAccounts = accounts;
                // تحديث خريطة الحسابات
                accountMap.clear();
                for (Account account : accounts) {
                    accountMap.put(account.getId(), account);
                }
                
                // تحديث المحول بالحسابات
                adapter.setAccountMap(accountMap);
                
                // تحميل المعاملات مع التصفية الافتراضية
                viewModel.loadTransactionsByDateRange(startDate.getTimeInMillis(), endDate.getTimeInMillis());
            }
        });
    
        // مراقبة المعاملات
        viewModel.getTransactions().observe(getViewLifecycleOwner(), transactions -> {
            // إخفاء مؤشر التحميل
            binding.progressBar.setVisibility(View.GONE);
            
            if (transactions != null) {
                allTransactions = transactions;
                applyAllFilters();
            } else {
                allTransactions = new ArrayList<>();
                applyAllFilters();
            }
        });

        // مراقبة أرصدة الحسابات
        viewModel.getAccountBalancesMap().observe(getViewLifecycleOwner(), balancesMap -> {
            if (balancesMap != null) {
                accountBalancesMap = balancesMap;
            }
        });
    }

    private void showDeleteConfirmationDialog(Transaction transaction) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_transaction)
                .setMessage(R.string.confirm_delete)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    viewModel.deleteTransaction(transaction);
                    Toast.makeText(requireContext(), R.string.transaction_deleted, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void applyAllFilters() {
        List<Transaction> filtered = new ArrayList<>();
        
        // تحويل التواريخ إلى بداية اليوم ونهاية اليوم للمقارنة
        Calendar startCal = Calendar.getInstance();
        startCal.setTimeInMillis(startDate.getTimeInMillis());
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        
        Calendar endCal = Calendar.getInstance();
        endCal.setTimeInMillis(endDate.getTimeInMillis());
        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        endCal.set(Calendar.MILLISECOND, 999);
        
        long startTime = startCal.getTimeInMillis();
        long endTime = endCal.getTimeInMillis();
        
        double totalAmount = 0.0;
        
        for (Transaction t : allTransactions) {
            boolean match = true;
            
            // فلترة الحساب
            if (selectedAccount != null && !selectedAccount.isEmpty()) {
                Account account = null;
                if (adapter != null && adapter.getAccountMap() != null) {
                    account = adapter.getAccountMap().get(t.getAccountId());
                }
                String accountName = (account != null) ? account.getName() : null;
                if (accountName == null || !accountName.equals(selectedAccount)) match = false;
            }
            
            // فلترة التاريخ - نتجاهلها فقط إذا كان البحث نشط
            if (!isSearchActive) {
                long transactionDate = t.getTransactionDate();
                if (transactionDate < startTime || transactionDate > endTime) {
                    match = false;
                }
            }
            
            // فلترة الوصف - نطبقها فقط إذا كان البحث نشط
            if (isSearchActive && !currentSearchText.isEmpty()) {
                String description = t.getDescription() != null ? t.getDescription().toLowerCase() : "";
                if (!description.contains(currentSearchText)) {
                    match = false;
                }
            }
            
            if (match) {
                filtered.add(t);
                totalAmount += t.getAmount();
            }
        }
        
        // تحديث الإحصائيات
        binding.totalTransactionsText.setText(String.valueOf(filtered.size()));
        binding.totalAmountText.setText(String.format(Locale.ENGLISH, "%.2f", totalAmount));        
        adapter.submitList(filtered);
        binding.transactionsRecyclerView.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
        binding.emptyView.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private boolean isUserLoggedIn() {
        try {
            // التحقق من البيانات المحفوظة في SharedPreferences
            android.content.SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE);
            String savedToken = prefs.getString("auth_token", null);
            String savedUserId = prefs.getString("user_id", null);
            
            // إذا كانت البيانات موجودة، نعتبر المستخدم مسجل الدخول
            if (savedToken != null && savedUserId != null) {
                return true;
            }

            // إذا لم تكن البيانات موجودة، نتحقق من قاعدة البيانات
            App app = (App) requireActivity().getApplication();
            return app.getDatabase().userDao().getCurrentUser() != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // تحديث القائمة فقط إذا كانت فارغة
        if (adapter.getItemCount() == 0) {
            loadTransactions();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void loadTransactions() {
        // عرض مؤشر التحميل
        binding.progressBar.setVisibility(View.VISIBLE);
        
        // تحميل البيانات من قاعدة البيانات المحلية
        viewModel.getTransactions().observe(getViewLifecycleOwner(), transactions -> {
            // إخفاء مؤشر التحميل
            binding.progressBar.setVisibility(View.GONE);
            
            if (transactions != null && !transactions.isEmpty()) {
                // تحديث القائمة
                allTransactions = transactions;
                applyAllFilters();
            } else {
                // عرض رسالة عدم وجود بيانات
                binding.emptyView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void loadAccounts() {
        // ... existing code ...
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private String buildWhatsAppMessage(String accountName, Transaction transaction, double balance, String type) {
        // تنسيق التاريخ بالإنجليزي
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
        String date = dateFormat.format(new Date(transaction.getTransactionDate()));
    
        // تحديد الجملة حسب النوع
        String typeMessage;
        if ("credit".equalsIgnoreCase(type)) {
            typeMessage = "قيدنا إلى حسابكم";
        } else if ("debit".equalsIgnoreCase(type)) {
            typeMessage = "قيد على حسابكم";
        } else {
            typeMessage = "تفاصيل القيد المحاسبي";
        }
    
        // تحديد جملة الرصيد حسب القيمة
        String balanceMessage;
        if (balance >= 0) {
            balanceMessage = String.format(Locale.ENGLISH, "الرصيد لكم: %.2f %s", balance, transaction.getCurrency());
        } else {
            balanceMessage = String.format(Locale.ENGLISH, "الرصيد عليكم: %.2f %s", Math.abs(balance), transaction.getCurrency());
        }

        // جلب اسم المستخدم من التفضيلات
        String userName = new UserPreferences(requireContext()).getUserName();

        // بناء الرسالة مع جميع الأرقام بالإنجليزي واسم المرسل في النهاية
        return String.format(Locale.ENGLISH,
            "السيد / *%s*\n" +
            "──────────────\n" +
            "%s\n" +
            "──────────────\n" +
            "التاريخ: %s\n" +
            "المبلغ: %.2f %s\n" +
            "البيان: %s\n" +
            "%s\n" +
            "──────────────\n" +
            "تم الإرسال بواسطة:\n*%s*",
            accountName,
            typeMessage,
            date,
            transaction.getAmount(),
            transaction.getCurrency(),
            transaction.getDescription(),
            balanceMessage,
            userName
        );
    }

    private void sendWhatsAppMessage(Context context, String phoneNumber, String message) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String url = "https://api.whatsapp.com/send?phone=" + phoneNumber + "&text=" + Uri.encode(message);
            intent.setData(Uri.parse(url));
            Intent chooser = Intent.createChooser(intent, "اختر تطبيق واتساب");
            startActivity(chooser);
        } catch (Exception e) {
            Toast.makeText(context, "حدث خطأ أثناء فتح واتساب", Toast.LENGTH_SHORT).show();
        }
    }

    // دالة للتحقق من وجود اتصال بالإنترنت
    private boolean isNetworkAvailable() {
        try {
            android.net.ConnectivityManager connectivityManager = (android.net.ConnectivityManager) 
                requireContext().getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                return activeNetworkInfo != null && activeNetworkInfo.isConnected();
            }
        } catch (Exception e) {
            // تجاهل أي خطأ
        }
        return false;
    }
} 
package com.hillal.hhhhhhh.ui.transactions;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.model.Transaction;
import com.hillal.hhhhhhh.databinding.FragmentTransactionsBinding;
import com.hillal.hhhhhhh.ui.adapters.TransactionAdapter;
import com.hillal.hhhhhhh.viewmodel.AccountViewModel;
import com.hillal.hhhhhhh.data.model.Account;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import com.hillal.hhhhhhh.App;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.hillal.hhhhhhh.data.repository.AccountRepository;
import com.hillal.hhhhhhh.ui.transactions.TransactionViewModelFactory;
import com.hillal.hhhhhhh.data.remote.RetrofitClient;
import com.hillal.hhhhhhh.data.remote.ApiService;

public class TransactionsFragment extends Fragment {
    private FragmentTransactionsBinding binding;
    private TransactionsViewModel viewModel;
    private TransactionAdapter adapter;
    private AccountViewModel accountViewModel;
    private TransactionViewModel transactionViewModel;
    private com.hillal.hhhhhhh.data.repository.TransactionRepository transactionRepository;
    private Calendar startDate;
    private Calendar endDate;
    private String selectedAccount = null;
    private String selectedCurrency = null;
    private List<Transaction> allTransactions = new ArrayList<>();
    private boolean isStartDate = true; // متغير لتتبع أي تاريخ يتم تعديله
    private boolean isFirstLoad = true;
    private long lastSyncTime = 0;
    private Map<Long, Account> accountMap = new HashMap<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App app = (App) requireActivity().getApplication();
        viewModel = new ViewModelProvider(this).get(TransactionsViewModel.class);
        accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);
        AccountRepository accountRepository = app.getAccountRepository();
        com.hillal.hhhhhhh.ui.transactions.TransactionViewModelFactory factory = new com.hillal.hhhhhhh.ui.transactions.TransactionViewModelFactory(accountRepository);
        transactionViewModel = new ViewModelProvider(this, factory).get(TransactionViewModel.class);
        transactionRepository = new com.hillal.hhhhhhh.data.repository.TransactionRepository(app.getDatabase());
        
        // تهيئة التواريخ الافتراضية
        startDate = Calendar.getInstance();
        startDate.add(Calendar.DAY_OF_MONTH, -4); // قبل 4 أيام
        endDate = Calendar.getInstance(); // اليوم الحالي
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

        // تهيئة RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.transactionsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // إنشاء المحول مع تمرير السياق
        adapter = new TransactionAdapter(new TransactionAdapter.TransactionDiffCallback(), requireContext());
        recyclerView.setAdapter(adapter);

        setupAccountFilter();
        setupCurrencyFilter();
        setupDateFilter();
        setupFab();
        
        observeAccountsAndTransactions();

        // إعداد المستمعين للأزرار
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

        adapter.setOnEditClickListener(transaction -> {
            // التنقل إلى صفحة تعديل القيد
            Bundle args = new Bundle();
            args.putLong("transactionId", transaction.getId());
            Navigation.findNavController(view).navigate(R.id.action_transactions_to_editTransaction, args);
        });

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
    }

    private void setupAccountFilter() {
        accountViewModel.getAllAccounts().observe(getViewLifecycleOwner(), accounts -> {
            List<String> accountNames = new ArrayList<>();
            for (Account account : accounts) {
                accountNames.add(account.getName());
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                accountNames
            );
            binding.accountFilterDropdown.setAdapter(adapter);
            binding.accountFilterDropdown.setOnItemClickListener((parent, view, position, id) -> {
                selectedAccount = accountNames.get(position);
                applyAllFilters();
            });
        });
    }

    private void setupCurrencyFilter() {
        binding.currencyChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipYer) {
                selectedCurrency = getString(R.string.currency_yer);
            } else if (checkedId == R.id.chipSar) {
                selectedCurrency = getString(R.string.currency_sar);
            } else if (checkedId == R.id.chipUsd) {
                selectedCurrency = getString(R.string.currency_usd);
            } else {
                selectedCurrency = null;
            }
            applyAllFilters();
        });
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
            viewModel.loadTransactionsByDateRange(startDate.getTimeInMillis(), endDate.getTimeInMillis());
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
        accountViewModel.getAllAccounts().observe(getViewLifecycleOwner(), accounts -> {
            accountMap.clear();
            for (Account account : accounts) {
                accountMap.put(account.getId(), account);
            }
            adapter.setAccountMap(accountMap);
            // تحميل المعاملات مع التصفية الافتراضية
            viewModel.loadTransactionsByDateRange(startDate.getTimeInMillis(), endDate.getTimeInMillis());
            viewModel.getTransactions().observe(getViewLifecycleOwner(), transactions -> {
                allTransactions = transactions != null ? transactions : new ArrayList<>();
                applyAllFilters();
            });
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
        for (Transaction t : allTransactions) {
            boolean match = true;
            if (selectedAccount != null && !selectedAccount.isEmpty()) {
                Account account = null;
                if (adapter != null && adapter.getAccountMap() != null) {
                    account = adapter.getAccountMap().get(t.getAccountId());
                }
                String accountName = (account != null) ? account.getName() : null;
                if (accountName == null || !accountName.equals(selectedAccount)) match = false;
            }
            if (selectedCurrency != null && !selectedCurrency.isEmpty()) {
                if (!selectedCurrency.equals(t.getCurrency())) match = false;
            }
            long transactionDate = t.getTransactionDate();
            if (transactionDate < startDate.getTimeInMillis() || transactionDate > endDate.getTimeInMillis()) {
                match = false;
            }
            if (match) filtered.add(t);
        }
        adapter.submitList(filtered);
        binding.transactionsRecyclerView.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
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
            balanceMessage = String.format(Locale.ENGLISH, "الرصيد لكم حتى تاريخ: %.2f %s", balance, transaction.getCurrency());
        } else {
            balanceMessage = String.format(Locale.ENGLISH, "الرصيد عليكم حتى تاريخ: %.2f %s", Math.abs(balance), transaction.getCurrency());
        }
    
        // بناء الرسالة مع جميع الأرقام بالإنجليزي
        return String.format(Locale.ENGLISH,
            "مرحباً %s\n\n" +
            "%s:\n" +
            "التاريخ: %s\n" +
            "المبلغ: %.2f %s\n" +
            "البيان: %s\n" +
            "%s",
            accountName,
            typeMessage,
            date,
            transaction.getAmount(),
            transaction.getCurrency(),
            transaction.getDescription(),
            balanceMessage
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
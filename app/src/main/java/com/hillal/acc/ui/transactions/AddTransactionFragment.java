package com.hillal.acc.ui.transactions;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.widget.EditText;
import android.view.WindowManager;
import android.app.Dialog;
import android.view.Window;
import android.view.Gravity;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.SharedPreferences;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.hillal.acc.R;
import com.hillal.acc.data.model.Account;
import com.hillal.acc.data.model.Transaction;
import com.hillal.acc.databinding.FragmentAddTransactionBinding;
import com.hillal.acc.viewmodel.AccountViewModel;
import com.hillal.acc.data.preferences.UserPreferences;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import com.hillal.acc.ui.common.AccountPickerBottomSheet;
import com.hillal.acc.ui.transactions.NotificationUtils;
import com.hillal.acc.data.repository.TransactionRepository;
import com.hillal.acc.App;
import com.hillal.acc.data.entities.Cashbox;
import com.hillal.acc.viewmodel.CashboxViewModel;
import com.hillal.acc.ui.cashbox.AddCashboxDialog;
import com.hillal.acc.data.repository.CashboxRepository;
import com.hillal.acc.ui.transactions.CashboxHelper;

import java.util.HashSet;
import java.util.Set;
import android.util.TypedValue;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.os.Build;

public class AddTransactionFragment extends Fragment implements com.hillal.acc.ui.cashbox.AddCashboxDialog.OnCashboxAddedListener {
    private FragmentAddTransactionBinding binding;
    private TransactionsViewModel transactionsViewModel;
    private AccountViewModel accountViewModel;
    private UserPreferences userPreferences;
    private long selectedAccountId = -1;
    private final Calendar calendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", new Locale("ar"));
    private List<Account> allAccounts = new ArrayList<>();
    private List<Transaction> allTransactions = new ArrayList<>();
    private Map<Long, Map<String, Double>> accountBalancesMap = new HashMap<>();
    private Transaction lastSavedTransaction = null;
    private Account lastSavedAccount = null;
    private double lastSavedBalance = 0.0;
    private TransactionRepository transactionRepository;
    private boolean isDialogShown = false;
    private long selectedCashboxId = -1;
    private long mainCashboxId = -1;
    private List<Cashbox> allCashboxes = new ArrayList<>();
    private CashboxViewModel cashboxViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        transactionsViewModel = new ViewModelProvider(this).get(TransactionsViewModel.class);
        accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);
        userPreferences = new UserPreferences(requireContext());
        App app = (App) requireActivity().getApplication();
        transactionRepository = new TransactionRepository(app.getDatabase());
        cashboxViewModel = new ViewModelProvider(this).get(CashboxViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAddTransactionBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        setupViews();
        setupListeners();
        loadAccounts();
        loadAllTransactions();
        setupAccountPicker();
        setupCashboxDropdown();
        transactionsViewModel.getAccountBalancesMap().observe(getViewLifecycleOwner(), balancesMap -> {
            accountBalancesMap = balancesMap != null ? balancesMap : new HashMap<>();
        });
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            requireActivity().getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        }
        // ضبط insets للجذر عند ظهور الكيبورد أو أزرار النظام
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            int bottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            if (bottom == 0) {
                bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            }
            v.setPadding(0, 0, 0, bottom);
            return insets;
        });
        setupViews();
        setupListeners();
        loadAccounts();
        loadAllTransactions();
        setupAccountPicker();
        setupCashboxDropdown();
        transactionsViewModel.getAccountBalancesMap().observe(getViewLifecycleOwner(), balancesMap -> {
            accountBalancesMap = balancesMap != null ? balancesMap : new HashMap<>();
        });
    }

    private void setupViews() {
        // Set initial date
        updateDateField();

        // تعيين ريال يمني كخيار افتراضي
        binding.radioYer.setChecked(true);
        
        // إضافة اسم المستخدم في الملاحظات
        String userName = userPreferences.getUserName();
        if (!userName.isEmpty()) {
            binding.notesEditText.setText(userName);
        }
    }

    private void setupListeners() {
        binding.debitButton.setOnClickListener(v -> saveTransaction(true));
        binding.creditButton.setOnClickListener(v -> saveTransaction(false));
        binding.cancelButton.setOnClickListener(v -> Navigation.findNavController(requireView()).navigateUp());
        
        // Date picker listener
        binding.dateEditText.setOnClickListener(v -> showDatePicker());
    }

    private void loadAccounts() {
        accountViewModel.getAllAccounts().observe(getViewLifecycleOwner(), accounts -> {
            if (accounts != null && !accounts.isEmpty()) {
                // إنشاء خريطة لتخزين عدد المعاملات لكل حساب
                Map<Long, Integer> accountTransactionCount = new HashMap<>();
                
                // حساب عدد المعاملات لكل حساب
                for (Transaction transaction : allTransactions) {
                    long accountId = transaction.getAccountId();
                    accountTransactionCount.put(accountId, 
                        accountTransactionCount.getOrDefault(accountId, 0) + 1);
                }
                
                // ترتيب الحسابات حسب عدد المعاملات (تنازلياً)
                accounts.sort((a1, a2) -> {
                    int count1 = accountTransactionCount.getOrDefault(a1.getId(), 0);
                    int count2 = accountTransactionCount.getOrDefault(a2.getId(), 0);
                    return Integer.compare(count2, count1); // ترتيب تنازلي
                });
                
                allAccounts = accounts;
                setupAccountDropdown(accounts);
            }
        });
    }

    private void setupAccountDropdown(List<Account> accounts) {
        // لم يعد هناك داعي للـ AutoComplete التقليدي
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    updateDateField();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void updateDateField() {
        binding.dateEditText.setText(dateFormat.format(calendar.getTime()));
    }

    private void saveTransaction(boolean isDebit) {
        if (selectedAccountId == -1) {
            Toast.makeText(requireContext(), "الرجاء اختيار الحساب", Toast.LENGTH_SHORT).show();
            return;
        }

        String amountStr = binding.amountEditText.getText().toString();
        String description = binding.descriptionEditText.getText().toString();
        String notes = binding.notesEditText.getText().toString();
        String currency = getSelectedCurrency();

        if (amountStr.isEmpty()) {
            binding.amountEditText.setError(getString(R.string.error_amount_required));
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);

            accountViewModel.getAccountById(selectedAccountId).observe(getViewLifecycleOwner(), account -> {
                if (account != null) {
                    Transaction transaction = new Transaction(
                        selectedAccountId,
                        amount,
                        isDebit ? "debit" : "credit",
                        description,
                        currency
                    );
                    transaction.setNotes(notes);
                    transaction.setTransactionDate(calendar.getTimeInMillis());
                    transaction.setUpdatedAt(System.currentTimeMillis());
                    transaction.setServerId(-1);
                    transaction.setWhatsappEnabled(account.isWhatsappEnabled());

                    // تحسين منطق اختيار الصندوق
                    long cashboxIdToSave;
                    if (selectedCashboxId != -1) {
                        // إذا تم اختيار صندوق محدد
                        cashboxIdToSave = selectedCashboxId;
                    } else if (mainCashboxId != -1) {
                        // إذا لم يتم اختيار صندوق، استخدم الصندوق الرئيسي
                        cashboxIdToSave = mainCashboxId;
                    } else if (!allCashboxes.isEmpty()) {
                        // إذا لم يكن هناك صندوق رئيسي، استخدم أول صندوق متاح
                        cashboxIdToSave = allCashboxes.get(0).id;
                    } else {
                        // إذا لم تكن هناك صناديق على الإطلاق، لا تحفظ صندوق
                        cashboxIdToSave = -1;
                        Toast.makeText(requireContext(), "تحذير: لا توجد صناديق متاحة", Toast.LENGTH_SHORT).show();
                    }
                    
                    transaction.setCashboxId(cashboxIdToSave);

                    transactionsViewModel.insertTransaction(transaction);
                    lastSavedTransaction = transaction;
                    lastSavedAccount = account;
                    // احسب الرصيد حتى تاريخ المعاملة
                    transactionRepository.getBalanceUntilDate(selectedAccountId, transaction.getTransactionDate(), currency)
                        .observe(getViewLifecycleOwner(), balance -> {
                            lastSavedBalance = (balance != null) ? balance : 0.0;
                            showSuccessDialog();
                        });

                    // عند حفظ المعاملة (بعد التأكد أن البيان غير فارغ)
                    String desc = binding.descriptionEditText.getText().toString().trim();
                    if (!desc.isEmpty()) {
                        SharedPreferences prefs = requireContext().getSharedPreferences("suggestions", Context.MODE_PRIVATE);
                        String key = "descriptions_" + selectedAccountId;
                        Set<String> suggestions = prefs.getStringSet(key, new HashSet<>());
                        suggestions = new HashSet<>(suggestions); // حتى لا تكون read-only
                        suggestions.add(desc);
                        prefs.edit().putStringSet(key, suggestions).apply();
                        loadAccountSuggestions(selectedAccountId); // لتحديث الاقتراحات فورًا
                    }
                }
            });
        } catch (NumberFormatException e) {
            binding.amountEditText.setError(getString(R.string.error_invalid_amount));
        }
    }

    private String getSelectedCurrency() {
        int checkedId = binding.currencyRadioGroup.getCheckedRadioButtonId();
        if (checkedId == R.id.radioYer) {
            return getString(R.string.currency_yer);
        } else if (checkedId == R.id.radioSar) {
            return getString(R.string.currency_sar);
        } else {
            return getString(R.string.currency_usd);
        }
    }

    private void setupAccountPicker() {
        binding.accountAutoComplete.setFocusable(false);
        binding.accountAutoComplete.setOnClickListener(v -> showAccountPickerBottomSheet());
    }

    private void showAccountPickerBottomSheet() {
        if (allAccounts == null || allAccounts.isEmpty()) {
            Toast.makeText(requireContext(), "جاري تحميل الحسابات...", Toast.LENGTH_SHORT).show();
            return;
        }
        AccountPickerBottomSheet bottomSheet = new AccountPickerBottomSheet(
            allAccounts,
            allTransactions,
            accountBalancesMap,
            account -> {
                binding.accountAutoComplete.setText(account.getName());
                selectedAccountId = account.getId();
                loadAccountSuggestions(selectedAccountId); // تحميل اقتراحات الحساب المختار
            }
        );
        bottomSheet.show(getParentFragmentManager(), "AccountPicker");
    }

    private void loadAllTransactions() {
        TransactionsViewModel txViewModel = new ViewModelProvider(this).get(TransactionsViewModel.class);
        txViewModel.getTransactions().observe(getViewLifecycleOwner(), txs -> {
            if (txs != null) allTransactions = txs;
        });
        txViewModel.loadAllTransactions();
    }

    private void setupCashboxDropdown() {
        cashboxViewModel.getAllCashboxes().observe(getViewLifecycleOwner(), cashboxes -> {
            allCashboxes = cashboxes != null ? cashboxes : new ArrayList<>();
            List<String> names = new ArrayList<>();
            mainCashboxId = -1;
            for (Cashbox c : allCashboxes) {
                names.add(c.name);
            }
            if (!allCashboxes.isEmpty()) {
                // اختر أول صندوق كافتراضي
                mainCashboxId = allCashboxes.get(0).id;
                binding.cashboxAutoComplete.setText(allCashboxes.get(0).name, false);
                selectedCashboxId = mainCashboxId;
            }
            // أضف خيار إضافة صندوق جديد إذا لم يكن موجودًا بالفعل
            if (!names.contains("➕ إضافة صندوق جديد...")) {
                names.add("➕ إضافة صندوق جديد...");
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, names);
            binding.cashboxAutoComplete.setAdapter(adapter);
            binding.cashboxAutoComplete.setOnItemClickListener((parent, v, position, id) -> {
                if (position == allCashboxes.size()) {
                    // خيار إضافة صندوق جديد
                    openAddCashboxDialog();
                } else {
                    selectedCashboxId = allCashboxes.get(position).id;
                }
            });
            // تفعيل القائمة المنسدلة عند النقر
            binding.cashboxAutoComplete.setFocusable(false);
            binding.cashboxAutoComplete.setOnClickListener(v -> binding.cashboxAutoComplete.showDropDown());
        });
    }

    private void openAddCashboxDialog() {
        android.util.Log.d("AddTransactionFragment", "openAddCashboxDialog called");
        AddCashboxDialog dialog = new AddCashboxDialog();
        android.util.Log.d("AddTransactionFragment", "AddCashboxDialog created");
        dialog.setListener(this);
        android.util.Log.d("AddTransactionFragment", "setListener called");
        dialog.show(getParentFragmentManager(), "AddCashboxDialog");
        android.util.Log.d("AddTransactionFragment", "Dialog shown");
    }

    @Override
    public void onCashboxAdded(String name) {
        android.util.Log.d("AddTransactionFragment", "=== onCashboxAdded called ===");
        android.util.Log.d("AddTransactionFragment", "Name received: " + name);
        
        // إظهار dialog تحميل
        ProgressDialog loadingDialog = new ProgressDialog(requireContext());
        loadingDialog.setMessage("جاري إضافة الصندوق...");
        loadingDialog.setCancelable(false);
        loadingDialog.show();

        // استخدام CashboxHelper لإضافة الصندوق
        CashboxHelper.addCashboxToServer(requireContext(), cashboxViewModel, name, 
            new CashboxHelper.CashboxCallback() {
                @Override
                public void onSuccess(Cashbox cashbox) {
                    android.util.Log.d("AddTransactionFragment", "Cashbox added successfully: id=" + cashbox.id + ", name=" + cashbox.name);
                    loadingDialog.dismiss();
                    // سيتم تحديث القائمة تلقائياً عبر LiveData
                    // حدد الصندوق الجديد تلقائياً بعد إضافته
                    binding.cashboxAutoComplete.setText(cashbox.name, false);
                    selectedCashboxId = cashbox.id;
                    CashboxHelper.showSuccessMessage(requireContext(), "تم إضافة الصندوق بنجاح");
                }

                @Override
                public void onError(String error) {
                    android.util.Log.e("AddTransactionFragment", "Error adding cashbox: " + error);
                    loadingDialog.dismiss();
                    CashboxHelper.showErrorMessage(requireContext(), error);
                }
            });
    }

    private boolean isNetworkAvailable() {
        return CashboxHelper.isNetworkAvailable(requireContext());
    }

    private void showSuccessDialog() {
        if (getContext() == null || isDialogShown) return;
        isDialogShown = true;
        Dialog dialog = new Dialog(requireContext());
        View sheetView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_transaction_success, null);
        dialog.setContentView(sheetView);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = WindowManager.LayoutParams.WRAP_CONTENT;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.CENTER;
            window.setAttributes(params);
        }
        // أزرار
        View btnWhatsapp = sheetView.findViewById(R.id.btnSendWhatsapp);
        View btnSms = sheetView.findViewById(R.id.btnSendSms);
        View btnAddAnother = sheetView.findViewById(R.id.btnAddAnother);
        View btnExit = sheetView.findViewById(R.id.btnExit);
        // واتساب
        if (lastSavedAccount != null && lastSavedAccount.isWhatsappEnabled()) {
            btnWhatsapp.setVisibility(View.GONE);
        } else {
            btnWhatsapp.setVisibility(View.VISIBLE);
        }
        btnWhatsapp.setOnClickListener(v -> {
            if (lastSavedAccount != null && lastSavedTransaction != null) {
                String phone = lastSavedAccount.getPhoneNumber();
                if (phone == null || phone.isEmpty()) {
                    Toast.makeText(getContext(), "رقم الهاتف غير متوفر", Toast.LENGTH_SHORT).show();
                    return;
                }
                String msg = NotificationUtils.buildWhatsAppMessage(getContext(), lastSavedAccount.getName(), lastSavedTransaction, lastSavedBalance, lastSavedTransaction.getType());
                NotificationUtils.sendWhatsAppMessage(getContext(), phone, msg);
            }
        });
        // SMS
        btnSms.setOnClickListener(v -> {
            if (lastSavedAccount != null && lastSavedTransaction != null) {
                String phone = lastSavedAccount.getPhoneNumber();
                if (phone == null || phone.isEmpty()) {
                    Toast.makeText(getContext(), "رقم الهاتف غير متوفر", Toast.LENGTH_SHORT).show();
                    return;
                }
                String type = lastSavedTransaction.getType();
                String amountStr = String.format(java.util.Locale.US, "%.0f", lastSavedTransaction.getAmount());
                String balanceStr = String.format(java.util.Locale.US, "%.0f", Math.abs(lastSavedBalance));
                String currency = lastSavedTransaction.getCurrency();
                String typeText = (type.equalsIgnoreCase("credit") || type.equals("له")) ? "لكم" : "عليكم";
                String balanceText = (lastSavedBalance >= 0) ? "الرصيد لكم " : "الرصيد عليكم ";
                String message = "حسابكم لدينا:\n"
                        + typeText + " " + amountStr + " " + currency + "\n"
                        + lastSavedTransaction.getDescription() + "\n"
                        + balanceText + balanceStr + " " + currency;
                NotificationUtils.sendSmsMessage(getContext(), phone, message);
            }
        });
        // رجوع (إضافة قيد جديد لنفس العميل)
        btnAddAnother.setOnClickListener(v -> {
            dialog.dismiss();
            isDialogShown = false;
            clearFieldsForAnother();
        });
        // خروج
        btnExit.setOnClickListener(v -> {
            dialog.dismiss();
            isDialogShown = false;
            View view = getView();
            if (isAdded() && view != null) {
                Navigation.findNavController(view).navigateUp();
            }
        });
        dialog.setCancelable(false);
        dialog.show();
    }

    private void clearFieldsForAnother() {
        binding.amountEditText.setText("");
        binding.descriptionEditText.setText("");
        // لا تفرغ الحساب المختار
        // لا تفرغ العملة
        // لا تفرغ الملاحظات
        calendar.setTimeInMillis(System.currentTimeMillis());
        updateDateField();
    }

    private void loadAccountSuggestions(long accountId) {
        SharedPreferences prefs = requireContext().getSharedPreferences("suggestions", Context.MODE_PRIVATE);
        Set<String> suggestions = prefs.getStringSet("descriptions_" + accountId, new HashSet<>());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.dropdown_item_suggestion, new ArrayList<>(suggestions));
        ((AutoCompleteTextView) binding.descriptionEditText).setAdapter(adapter);
        int itemHeightPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics());
        binding.descriptionEditText.setDropDownHeight(itemHeightPx * 4);
        ((AutoCompleteTextView) binding.descriptionEditText).setDropDownBackgroundResource(R.drawable.bg_dropdown_suggestions);
        binding.descriptionEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                ((AutoCompleteTextView) binding.descriptionEditText).showDropDown();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
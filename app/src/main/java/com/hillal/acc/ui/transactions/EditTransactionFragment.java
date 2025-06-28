package com.hillal.acc.ui.transactions;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.hillal.acc.R;
import com.hillal.acc.data.model.Account;
import com.hillal.acc.data.model.Transaction;
import com.hillal.acc.databinding.FragmentEditTransactionBinding;
import com.hillal.acc.viewmodel.AccountViewModel;
import com.hillal.acc.data.preferences.UserPreferences;
import com.hillal.acc.data.entities.Cashbox;
import com.hillal.acc.viewmodel.CashboxViewModel;
import com.hillal.acc.ui.cashbox.AddCashboxDialog;
import com.hillal.acc.data.repository.CashboxRepository;
import com.hillal.acc.ui.transactions.CashboxHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;

public class EditTransactionFragment extends Fragment implements com.hillal.acc.ui.cashbox.AddCashboxDialog.OnCashboxAddedListener {
    private FragmentEditTransactionBinding binding;
    private TransactionsViewModel transactionsViewModel;
    private AccountViewModel accountViewModel;
    private UserPreferences userPreferences;
    private long transactionId;
    private long selectedAccountId = -1;
    private final Calendar calendar = Calendar.getInstance();
    private Transaction oldTransaction;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", new Locale("ar"));
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
        cashboxViewModel = new ViewModelProvider(this).get(CashboxViewModel.class);
        
        if (getArguments() != null) {
            transactionId = getArguments().getLong("transactionId", -1);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEditTransactionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews();
        setupListeners();
        loadAccounts();
        loadTransaction();
        setupCashboxDropdown();
    }

    private void setupViews() {
        // Set initial date
        updateDateField();
    }

    private void setupListeners() {
        binding.saveButton.setOnClickListener(v -> updateTransaction());
        binding.cancelButton.setOnClickListener(v -> Navigation.findNavController(requireView()).navigateUp());
        
        // Date picker listener
        binding.dateEditText.setOnClickListener(v -> showDatePicker());
    }

    private void loadAccounts() {
        accountViewModel.getAllAccounts().observe(getViewLifecycleOwner(), accounts -> {
            if (accounts != null && !accounts.isEmpty()) {
                setupAccountDropdown(accounts);
            }
        });
    }

    private void setupAccountDropdown(List<Account> accounts) {
        String[] accountNames = new String[accounts.size()];
        for (int i = 0; i < accounts.size(); i++) {
            accountNames[i] = accounts.get(i).getName();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                accountNames
        );

        binding.accountAutoComplete.setAdapter(adapter);
        binding.accountAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
            selectedAccountId = accounts.get(position).getId();
        });
    }

    private void loadTransaction() {
        if (transactionId != -1) {
            transactionsViewModel.getTransactionById(transactionId).observe(getViewLifecycleOwner(), transaction -> {
                if (transaction != null) {
                    oldTransaction = transaction;
                    populateTransactionData(transaction);
                }
            });
        }
    }

    private void populateTransactionData(Transaction transaction) {
        selectedAccountId = transaction.getAccountId();
        
        // Load account name
        accountViewModel.getAccountById(transaction.getAccountId()).observe(getViewLifecycleOwner(), account -> {
            if (account != null) {
                binding.accountAutoComplete.setText(account.getName(), false);
            }
        });

        binding.amountEditText.setText(String.valueOf(transaction.getAmount()));
        binding.descriptionEditText.setText(transaction.getDescription());
        
        // إضافة اسم المستخدم في الملاحظات إذا كانت فارغة
        String notes = transaction.getNotes();
        if (notes == null || notes.isEmpty()) {
            String userName = userPreferences.getUserName();
            if (!userName.isEmpty()) {
                binding.notesEditText.setText(userName);
            }
        } else {
            binding.notesEditText.setText(notes);
        }

        // Set transaction type
        if ("debit".equals(transaction.getType())) {
            binding.radioDebit.setChecked(true);
        } else {
            binding.radioCredit.setChecked(true);
        }

        // Set currency
        if (getString(R.string.currency_yer).equals(transaction.getCurrency())) {
            binding.radioYer.setChecked(true);
        } else if (getString(R.string.currency_sar).equals(transaction.getCurrency())) {
            binding.radioSar.setChecked(true);
        } else {
            binding.radioUsd.setChecked(true);
        }

        // Set date
        calendar.setTimeInMillis(transaction.getTransactionDate());
        updateDateField();
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

    private void setupCashboxDropdown() {
        cashboxViewModel.getAllCashboxes().observe(getViewLifecycleOwner(), cashboxes -> {
            android.util.Log.d("EditTransaction", "Setting up cashbox dropdown with " + (cashboxes != null ? cashboxes.size() : 0) + " cashboxes");
            allCashboxes = cashboxes != null ? cashboxes : new ArrayList<>();
            List<String> names = new ArrayList<>();
            mainCashboxId = -1;
            for (Cashbox c : allCashboxes) {
                names.add(c.name);
                if (mainCashboxId == -1 && (c.name.equals("الرئيسي") || c.name.equalsIgnoreCase("main"))) {
                    mainCashboxId = c.id;
                    android.util.Log.d("EditTransaction", "Found main cashbox: " + c.name + " (ID: " + c.id + ")");
                }
            }
            if (mainCashboxId == -1 && !allCashboxes.isEmpty()) {
                mainCashboxId = allCashboxes.get(0).id;
                android.util.Log.d("EditTransaction", "Using first cashbox as main: " + allCashboxes.get(0).name + " (ID: " + allCashboxes.get(0).id + ")");
            }
            names.add("➕ إضافة صندوق جديد...");
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, names);
            binding.cashboxAutoComplete.setAdapter(adapter);
            
            // اختيار الصندوق المرتبط بالمعاملة أو الرئيسي
            if (oldTransaction != null && oldTransaction.getCashboxId() != -1) {
                android.util.Log.d("EditTransaction", "Old transaction cashbox ID: " + oldTransaction.getCashboxId());
                // البحث عن الصندوق المرتبط بالمعاملة
                Cashbox transactionCashbox = null;
                for (Cashbox cashbox : allCashboxes) {
                    if (cashbox.id == oldTransaction.getCashboxId()) {
                        transactionCashbox = cashbox;
                        break;
                    }
                }
                
                if (transactionCashbox != null) {
                    binding.cashboxAutoComplete.setText(transactionCashbox.name, false);
                    selectedCashboxId = transactionCashbox.id;
                    android.util.Log.d("EditTransaction", "Set to old transaction cashbox: " + transactionCashbox.name + " (ID: " + transactionCashbox.id + ")");
                } else if (mainCashboxId != -1) {
                    // إذا لم يتم العثور على الصندوق المرتبط، استخدم الرئيسي
                    for (Cashbox cashbox : allCashboxes) {
                        if (cashbox.id == mainCashboxId) {
                            binding.cashboxAutoComplete.setText(cashbox.name, false);
                            selectedCashboxId = mainCashboxId;
                            android.util.Log.d("EditTransaction", "Set to main cashbox: " + cashbox.name + " (ID: " + cashbox.id + ")");
                            break;
                        }
                    }
                }
            } else if (mainCashboxId != -1) {
                // إذا لم تكن هناك معاملة قديمة، استخدم الصندوق الرئيسي
                for (Cashbox cashbox : allCashboxes) {
                    if (cashbox.id == mainCashboxId) {
                        binding.cashboxAutoComplete.setText(cashbox.name, false);
                        selectedCashboxId = mainCashboxId;
                        android.util.Log.d("EditTransaction", "Set to main cashbox (no old transaction): " + cashbox.name + " (ID: " + cashbox.id + ")");
                        break;
                    }
                }
            }
            
            binding.cashboxAutoComplete.setOnItemClickListener((parent, v, position, id) -> {
                android.util.Log.d("EditTransaction", "Cashbox item clicked at position: " + position);
                if (position == allCashboxes.size()) {
                    // خيار إضافة صندوق جديد
                    android.util.Log.d("EditTransaction", "Opening add cashbox dialog");
                    openAddCashboxDialog();
                } else {
                    selectedCashboxId = allCashboxes.get(position).id;
                    android.util.Log.d("EditTransaction", "Selected cashbox: " + allCashboxes.get(position).name + " (ID: " + allCashboxes.get(position).id + ")");
                    // إزالة رسالة الخطأ إذا كانت موجودة
                    binding.cashboxAutoComplete.setError(null);
                }
            });
            
            // تفعيل القائمة المنسدلة عند النقر
            binding.cashboxAutoComplete.setFocusable(false);
            binding.cashboxAutoComplete.setOnClickListener(v -> binding.cashboxAutoComplete.showDropDown());
        });
    }

    private void openAddCashboxDialog() {
        AddCashboxDialog dialog = new AddCashboxDialog();
        dialog.setListener(this);
        dialog.show(getParentFragmentManager(), "AddCashboxDialog");
    }

    @Override
    public void onCashboxAdded(String name) {
        android.util.Log.d("EditTransactionFragment", "onCashboxAdded called with name: " + name);
        
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
                    android.util.Log.d("EditTransactionFragment", "Cashbox added successfully: id=" + cashbox.id + ", name=" + cashbox.name);
                    loadingDialog.dismiss();
                    // سيتم تحديث القائمة تلقائياً عبر LiveData
                    // حدد الصندوق الجديد تلقائياً بعد إضافته
                    binding.cashboxAutoComplete.setText(cashbox.name, false);
                    selectedCashboxId = cashbox.id;
                    android.util.Log.d("EditTransaction", "Updated selectedCashboxId to: " + selectedCashboxId);
                    CashboxHelper.showSuccessMessage(requireContext(), "تم إضافة الصندوق بنجاح");
                }

                @Override
                public void onError(String error) {
                    android.util.Log.e("EditTransactionFragment", "Error adding cashbox: " + error);
                    loadingDialog.dismiss();
                    CashboxHelper.showErrorMessage(requireContext(), error);
                }
            });
    }

    private boolean isNetworkAvailable() {
        return CashboxHelper.isNetworkAvailable(requireContext());
    }

    private void updateTransaction() {
        if (selectedAccountId == -1) {
            Toast.makeText(requireContext(), "الرجاء اختيار الحساب", Toast.LENGTH_SHORT).show();
            return;
        }

        String amountStr = binding.amountEditText.getText().toString();
        String description = binding.descriptionEditText.getText().toString();
        String notes = binding.notesEditText.getText().toString();
        boolean isDebit = binding.radioDebit.isChecked();
        String currency = getSelectedCurrency();

        if (amountStr.isEmpty()) {
            binding.amountEditText.setError(getString(R.string.error_amount_required));
            return;
        }

        // التحقق من اختيار الصندوق
        String selectedCashboxName = binding.cashboxAutoComplete.getText().toString().trim();
        android.util.Log.d("EditTransaction", "Selected cashbox name: " + selectedCashboxName);
        android.util.Log.d("EditTransaction", "Selected cashbox ID: " + selectedCashboxId);
        
        if (selectedCashboxName.isEmpty() || selectedCashboxName.equals("➕ إضافة صندوق جديد...")) {
            Toast.makeText(requireContext(), "الرجاء اختيار الصندوق", Toast.LENGTH_SHORT).show();
            binding.cashboxAutoComplete.setError("مطلوب اختيار صندوق");
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);

            if (oldTransaction == null) {
                Toast.makeText(requireContext(), "حدث خطأ في تحميل بيانات المعاملة الأصلية", Toast.LENGTH_SHORT).show();
                return;
            }

            Transaction transaction = new Transaction();
            transaction.setId(transactionId);
            transaction.setAccountId(selectedAccountId);
            transaction.setAmount(amount);
            transaction.setType(isDebit ? "debit" : "credit");
            transaction.setDescription(description);
            transaction.setNotes(notes);
            transaction.setCurrency(currency);
            transaction.setTransactionDate(calendar.getTimeInMillis());
            transaction.setUpdatedAt(System.currentTimeMillis());
            transaction.setServerId(oldTransaction.getServerId());
            transaction.setWhatsappEnabled(oldTransaction.isWhatsappEnabled());
            transaction.setSyncStatus(0);

            // البحث عن الصندوق المختار من النص المعروض
            long cashboxIdToSave = -1;
            
            android.util.Log.d("EditTransaction", "All cashboxes count: " + allCashboxes.size());
            for (Cashbox cashbox : allCashboxes) {
                android.util.Log.d("EditTransaction", "Checking cashbox: " + cashbox.name + " (ID: " + cashbox.id + ")");
                if (cashbox.name.equals(selectedCashboxName)) {
                    cashboxIdToSave = cashbox.id;
                    android.util.Log.d("EditTransaction", "Found matching cashbox: " + cashbox.name + " (ID: " + cashbox.id + ")");
                    break;
                }
            }
            
            android.util.Log.d("EditTransaction", "Cashbox ID from name search: " + cashboxIdToSave);
            
            // إذا لم يتم العثور على الصندوق من النص، استخدم selectedCashboxId
            if (cashboxIdToSave == -1 && selectedCashboxId != -1) {
                cashboxIdToSave = selectedCashboxId;
                android.util.Log.d("EditTransaction", "Using selectedCashboxId: " + selectedCashboxId);
            }
            
            // إذا لم يتم العثور على صندوق محدد، استخدم الصندوق الرئيسي
            if (cashboxIdToSave == -1 && mainCashboxId != -1) {
                cashboxIdToSave = mainCashboxId;
                android.util.Log.d("EditTransaction", "Using mainCashboxId: " + mainCashboxId);
            }
            
            // إذا لم يكن هناك صندوق رئيسي، استخدم أول صندوق متاح
            if (cashboxIdToSave == -1 && !allCashboxes.isEmpty()) {
                cashboxIdToSave = allCashboxes.get(0).id;
                android.util.Log.d("EditTransaction", "Using first available cashbox: " + allCashboxes.get(0).id);
            }
            
            // إذا لم تكن هناك صناديق على الإطلاق، احتفظ بالصندوق القديم
            if (cashboxIdToSave == -1) {
                cashboxIdToSave = oldTransaction.getCashboxId();
                android.util.Log.d("EditTransaction", "Using old transaction cashbox: " + oldTransaction.getCashboxId());
                if (cashboxIdToSave == -1) {
                    Toast.makeText(requireContext(), "خطأ: لا توجد صناديق متاحة", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            
            // التحقق النهائي من أن الصندوق صحيح
            if (cashboxIdToSave == -1) {
                Toast.makeText(requireContext(), "خطأ: لم يتم تحديد صندوق صحيح", Toast.LENGTH_SHORT).show();
                return;
            }
            
            android.util.Log.d("EditTransaction", "Final cashbox ID to save: " + cashboxIdToSave);
            transaction.setCashboxId(cashboxIdToSave);

            transactionsViewModel.updateTransaction(transaction);
            Toast.makeText(requireContext(), R.string.transaction_updated, Toast.LENGTH_SHORT).show();
            Navigation.findNavController(requireView()).navigateUp();
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 
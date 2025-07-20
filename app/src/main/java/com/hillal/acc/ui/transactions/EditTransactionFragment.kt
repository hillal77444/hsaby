package com.hillal.acc.ui.transactions

import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.DatePicker
import android.widget.Toast
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation.findNavController
import com.hillal.acc.R
import com.hillal.acc.data.entities.Cashbox
import com.hillal.acc.data.model.Account
import com.hillal.acc.data.model.Transaction
import com.hillal.acc.data.preferences.UserPreferences
import com.hillal.acc.databinding.FragmentEditTransactionBinding
import com.hillal.acc.ui.cashbox.AddCashboxDialog
import com.hillal.acc.ui.cashbox.AddCashboxDialog.OnCashboxAddedListener
import com.hillal.acc.ui.transactions.CashboxHelper.isNetworkAvailable
import com.hillal.acc.ui.transactions.CashboxHelper.showErrorMessage
import com.hillal.acc.ui.transactions.CashboxHelper.showSuccessMessage
import com.hillal.acc.viewmodel.AccountViewModel
import com.hillal.acc.viewmodel.CashboxViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditTransactionFragment : Fragment(), OnCashboxAddedListener {
    private var binding: FragmentEditTransactionBinding? = null
    private var transactionsViewModel: TransactionsViewModel? = null
    private var accountViewModel: AccountViewModel? = null
    private var userPreferences: UserPreferences? = null
    private var transactionId: Long = 0
    private var selectedAccountId: Long = -1
    private val calendar: Calendar = Calendar.getInstance()
    private var oldTransaction: Transaction? = null
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("ar"))
    private var selectedCashboxId: Long = -1
    private var mainCashboxId: Long = -1
    private var allCashboxes: MutableList<Cashbox> = ArrayList<Cashbox>()
    private var cashboxViewModel: CashboxViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        transactionsViewModel =
            ViewModelProvider(this).get<TransactionsViewModel>(TransactionsViewModel::class.java)
        accountViewModel =
            ViewModelProvider(this).get<AccountViewModel>(AccountViewModel::class.java)
        userPreferences = UserPreferences(requireContext())
        cashboxViewModel =
            ViewModelProvider(this).get<CashboxViewModel>(CashboxViewModel::class.java)

        if (getArguments() != null) {
            transactionId = getArguments()!!.getLong("transactionId", -1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEditTransactionBinding.inflate(inflater, container, false)
        return binding!!.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            requireActivity().getWindow().setStatusBarColor(Color.TRANSPARENT)
        }
        setupViews()
        setupListeners()
        loadAccounts()
        loadTransaction()
        // setupCashboxDropdown سيتم استدعاؤها من loadTransaction بعد تحميل المعاملة
        // ضبط insets للجذر لرفع المحتوى مع الكيبورد وأزرار النظام
        ViewCompat.setOnApplyWindowInsetsListener(
            view,
            OnApplyWindowInsetsListener { v: View?, insets: WindowInsetsCompat? ->
                var bottom = insets!!.getInsets(WindowInsetsCompat.Type.ime()).bottom
                if (bottom == 0) {
                    bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
                }
                v!!.setPadding(
                    v.getPaddingLeft(),
                    v.getPaddingTop(),
                    v.getPaddingRight(),
                    bottom
                )
                insets
            })
    }

    private fun setupViews() {
        // Set initial date
        updateDateField()
    }

    private fun setupListeners() {
        binding!!.saveButton.setOnClickListener(View.OnClickListener { v: View? -> updateTransaction() })
        binding!!.cancelButton.setOnClickListener(View.OnClickListener { v: View? ->
            findNavController(
                requireView()
            ).navigateUp()
        })


        // Date picker listener
        binding!!.dateEditText.setOnClickListener(View.OnClickListener { v: View? -> showDatePicker() })
    }

    private fun loadAccounts() {
        accountViewModel!!.getAllAccounts()
            .observe(getViewLifecycleOwner(), Observer { accounts: MutableList<Account?>? ->
                if (accounts != null && !accounts.isEmpty()) {
                    setupAccountDropdown(accounts)
                }
            })
    }

    private fun setupAccountDropdown(accounts: MutableList<Account?>) {
        val accountNames = arrayOfNulls<String>(accounts.size)
        for (i in accounts.indices) {
            accountNames[i] = accounts.get(i)!!.getName()
        }

        val adapter = ArrayAdapter<String?>(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            accountNames
        )

        binding!!.accountAutoComplete.setAdapter<ArrayAdapter<String?>?>(adapter)
        binding!!.accountAutoComplete.setOnItemClickListener(AdapterView.OnItemClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long ->
            selectedAccountId = accounts.get(position)!!
                .getId()
        })
    }

    private fun loadTransaction() {
        if (transactionId != -1L) {
            transactionsViewModel!!.getTransactionById(transactionId)!!
                .observe(getViewLifecycleOwner(), Observer { transaction: Transaction? ->
                    if (transaction != null) {
                        oldTransaction = transaction
                        populateTransactionData(transaction)
                    }
                })
        }
    }

    private fun populateTransactionData(transaction: Transaction) {
        Log.d("EditTransaction", "Populating transaction data for ID: " + transaction.getId())
        Log.d("EditTransaction", "Transaction cashbox ID: " + transaction.getCashboxId())

        selectedAccountId = transaction.getAccountId()


        // Load account name
        accountViewModel!!.getAccountById(transaction.getAccountId())
            .observe(getViewLifecycleOwner(), Observer { account: Account? ->
                if (account != null) {
                    binding!!.accountAutoComplete.setText(account.getName(), false)
                }
            })

        binding!!.amountEditText.setText(transaction.getAmount().toString())
        binding!!.descriptionEditText.setText(transaction.getDescription())


        // إضافة اسم المستخدم في الملاحظات إذا كانت فارغة
        val notes = transaction.getNotes()
        if (notes == null || notes.isEmpty()) {
            val userName = userPreferences!!.getUserName()
            if (!userName.isEmpty()) {
                binding!!.notesEditText.setText(userName)
            }
        } else {
            binding!!.notesEditText.setText(notes)
        }

        // Set transaction type
        if ("debit" == transaction.getType()) {
            binding!!.radioDebit.setChecked(true)
        } else {
            binding!!.radioCredit.setChecked(true)
        }

        // Set currency
        if (getString(R.string.currency_yer) == transaction.getCurrency()) {
            binding!!.radioYer.setChecked(true)
        } else if (getString(R.string.currency_sar) == transaction.getCurrency()) {
            binding!!.radioSar.setChecked(true)
        } else {
            binding!!.radioUsd.setChecked(true)
        }

        // Set date
        calendar.setTimeInMillis(transaction.getTransactionDate())
        updateDateField()

        setupCashboxDropdown()
    }

    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            DatePickerDialog.OnDateSetListener { view: DatePicker?, year: Int, month: Int, dayOfMonth: Int ->
                calendar.set(year, month, dayOfMonth)
                updateDateField()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun updateDateField() {
        binding!!.dateEditText.setText(dateFormat.format(calendar.getTime()))
    }

    private fun setupCashboxDropdown() {
        cashboxViewModel!!.getAllCashboxes()
            .observe(getViewLifecycleOwner(), Observer { cashboxes: MutableList<Cashbox?>? ->
                Log.d(
                    "EditTransaction",
                    "Setting up cashbox dropdown with " + (if (cashboxes != null) cashboxes.size else 0) + " cashboxes"
                )
                allCashboxes = if (cashboxes != null) cashboxes.filterNotNull().toMutableList() else ArrayList<Cashbox>()
                val names: MutableList<String?> = ArrayList<String?>()
                mainCashboxId = -1
                for (c in allCashboxes) {
                    names.add(c.name)
                    if (mainCashboxId == -1L && (c.name == "الرئيسي" || c.name.equals(
                            "main",
                            ignoreCase = true
                        ))
                    ) {
                        mainCashboxId = c.id
                        Log.d(
                            "EditTransaction",
                            "Found main cashbox: " + c.name + " (ID: " + c.id + ")"
                        )
                    }
                }
                if (mainCashboxId == -1L && !allCashboxes.isEmpty()) {
                    mainCashboxId = allCashboxes.get(0).id
                    Log.d(
                        "EditTransaction",
                        "Using first cashbox as main: " + allCashboxes.get(0).name + " (ID: " + allCashboxes.get(
                            0
                        ).id + ")"
                    )
                }
                names.add("➕ إضافة صندوق جديد...")
                val adapter = ArrayAdapter<String?>(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    names
                )
                binding!!.cashboxAutoComplete.setAdapter<ArrayAdapter<String?>?>(adapter)


                // اختيار الصندوق المرتبط بالمعاملة أو الرئيسي
                if (oldTransaction != null && oldTransaction!!.getCashboxId() != -1L) {
                    Log.d(
                        "EditTransaction",
                        "Old transaction cashbox ID: " + oldTransaction!!.getCashboxId()
                    )
                    // البحث عن الصندوق المرتبط بالمعاملة
                    var transactionCashbox: Cashbox? = null
                    for (cashbox in allCashboxes) {
                        if (cashbox.id == oldTransaction!!.getCashboxId()) {
                            transactionCashbox = cashbox
                            break
                        }
                    }

                    if (transactionCashbox != null) {
                        binding!!.cashboxAutoComplete.setText(transactionCashbox.name, false)
                        selectedCashboxId = transactionCashbox.id
                        Log.d(
                            "EditTransaction",
                            "Set to old transaction cashbox: " + transactionCashbox.name + " (ID: " + transactionCashbox.id + ")"
                        )
                    } else if (mainCashboxId != -1L && selectedCashboxId == -1L) {
                        // إذا لم يتم العثور على الصندوق المرتبط، استخدم الرئيسي فقط إذا لم يتم اختيار صندوق بعد
                        for (cashbox in allCashboxes) {
                            if (cashbox.id == mainCashboxId) {
                                binding!!.cashboxAutoComplete.setText(cashbox.name, false)
                                selectedCashboxId = mainCashboxId
                                Log.d(
                                    "EditTransaction",
                                    "Set to main cashbox: " + cashbox.name + " (ID: " + cashbox.id + ")"
                                )
                                break
                            }
                        }
                    }
                } else if (mainCashboxId != -1L && selectedCashboxId == -1L) {
                    // إذا لم تكن هناك معاملة قديمة، استخدم الصندوق الرئيسي فقط إذا لم يتم اختيار صندوق بعد
                    for (cashbox in allCashboxes) {
                        if (cashbox.id == mainCashboxId) {
                            binding!!.cashboxAutoComplete.setText(cashbox.name, false)
                            selectedCashboxId = mainCashboxId
                            Log.d(
                                "EditTransaction",
                                "Set to main cashbox (no old transaction): " + cashbox.name + " (ID: " + cashbox.id + ")"
                            )
                            break
                        }
                    }
                }


                // إذا كان المستخدم قد اختار صندوقاً بالفعل، احتفظ باختياره
                if (selectedCashboxId != -1L) {
                    for (cashbox in allCashboxes) {
                        if (cashbox.id == selectedCashboxId) {
                            binding!!.cashboxAutoComplete.setText(cashbox.name, false)
                            Log.d(
                                "EditTransaction",
                                "Keeping user selection: " + cashbox.name + " (ID: " + cashbox.id + ")"
                            )
                            break
                        }
                    }
                }

                binding!!.cashboxAutoComplete.setOnItemClickListener(AdapterView.OnItemClickListener { parent: AdapterView<*>?, v: View?, position: Int, id: Long ->
                    Log.d("EditTransaction", "Cashbox item clicked at position: " + position)
                    if (position == allCashboxes.size) {
                        // خيار إضافة صندوق جديد
                        Log.d("EditTransaction", "Opening add cashbox dialog")
                        openAddCashboxDialog()
                    } else {
                        selectedCashboxId = allCashboxes.get(position).id
                        Log.d(
                            "EditTransaction",
                            "Selected cashbox: " + allCashboxes.get(position).name + " (ID: " + allCashboxes.get(
                                position
                            ).id + ")"
                        )
                        // إزالة رسالة الخطأ إذا كانت موجودة
                        binding!!.cashboxAutoComplete.setError(null)
                    }
                })


                // تفعيل القائمة المنسدلة عند النقر
                binding!!.cashboxAutoComplete.setFocusable(false)
                binding!!.cashboxAutoComplete.setOnClickListener(View.OnClickListener { v: View? -> binding!!.cashboxAutoComplete.showDropDown() })
            })
    }

    private fun openAddCashboxDialog() {
        val dialog = AddCashboxDialog()
        dialog.setListener(this)
        dialog.show(getParentFragmentManager(), "AddCashboxDialog")
    }

    override fun onCashboxAdded(name: String?) {
        Log.d("EditTransactionFragment", "onCashboxAdded called with name: " + name)

        // إظهار dialog تحميل
        val loadingDialog = ProgressDialog(requireContext())
        loadingDialog.setMessage("جاري إضافة الصندوق...")
        loadingDialog.setCancelable(false)
        loadingDialog.show()

        // استخدام CashboxHelper لإضافة الصندوق
        CashboxHelper.addCashboxToServer(
            requireContext(), cashboxViewModel!!, name,
            object : CashboxHelper.CashboxCallback {
                override fun onSuccess(cashbox: Cashbox?) {
                    Log.d(
                        "EditTransactionFragment",
                        "Cashbox added successfully: id=" + (cashbox?.id ?: "null") + ", name=" + (cashbox?.name ?: "null")
                    )
                    loadingDialog.dismiss()
                    // سيتم تحديث القائمة تلقائياً عبر LiveData
                    // حدد الصندوق الجديد تلقائياً بعد إضافته
                    if (cashbox != null) {
                        binding!!.cashboxAutoComplete.setText(cashbox.name, false)
                        selectedCashboxId = cashbox.id
                        Log.d("EditTransaction", "Updated selectedCashboxId to: " + selectedCashboxId)
                        showSuccessMessage(requireContext(), "تم إضافة الصندوق بنجاح")
                    } else {
                        showErrorMessage(requireContext(), "فشل في إضافة الصندوق")
                    }
                }

                override fun onError(error: String?) {
                    Log.e("EditTransactionFragment", "Error adding cashbox: " + error)
                    loadingDialog.dismiss()
                    showErrorMessage(requireContext(), error)
                }
            })
    }

    private val isNetworkAvailable: Boolean
        get() = isNetworkAvailable(requireContext())

    private fun updateTransaction() {
        if (selectedAccountId == -1L) {
            Toast.makeText(requireContext(), "الرجاء اختيار الحساب", Toast.LENGTH_SHORT).show()
            return
        }

        val amountStr = binding!!.amountEditText.getText().toString()
        val description = binding!!.descriptionEditText.getText().toString()
        val notes = binding!!.notesEditText.getText().toString()
        val isDebit = binding!!.radioDebit.isChecked()
        val currency = this.selectedCurrency

        if (amountStr.isEmpty()) {
            binding!!.amountEditText.setError(getString(R.string.error_amount_required))
            return
        }

        // التحقق من اختيار الصندوق
        val selectedCashboxName =
            binding!!.cashboxAutoComplete.getText().toString().trim { it <= ' ' }
        Log.d("EditTransaction", "Selected cashbox name: " + selectedCashboxName)
        Log.d("EditTransaction", "Selected cashbox ID: " + selectedCashboxId)

        if (selectedCashboxName.isEmpty() || selectedCashboxName == "➕ إضافة صندوق جديد...") {
            Toast.makeText(requireContext(), "الرجاء اختيار الصندوق", Toast.LENGTH_SHORT).show()
            binding!!.cashboxAutoComplete.setError("مطلوب اختيار صندوق")
            return
        }

        try {
            val amount = amountStr.toDouble()

            if (oldTransaction == null) {
                Toast.makeText(
                    requireContext(),
                    "حدث خطأ في تحميل بيانات المعاملة الأصلية",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            val transaction = Transaction()
            transaction.setId(transactionId)
            transaction.setAccountId(selectedAccountId)
            transaction.setAmount(amount)
            transaction.setType(if (isDebit) "debit" else "credit")
            transaction.setDescription(description)
            transaction.setNotes(notes)
            transaction.setCurrency(currency)
            transaction.setTransactionDate(calendar.getTimeInMillis())
            transaction.setUpdatedAt(System.currentTimeMillis())
            transaction.setServerId(oldTransaction!!.getServerId())
            transaction.setWhatsappEnabled(oldTransaction!!.isWhatsappEnabled())
            transaction.setSyncStatus(0)

            // البحث عن الصندوق المختار من النص المعروض
            var cashboxIdToSave: Long = -1

            Log.d("EditTransaction", "All cashboxes count: " + allCashboxes.size)
            for (cashbox in allCashboxes) {
                Log.d(
                    "EditTransaction",
                    "Checking cashbox: " + cashbox.name + " (ID: " + cashbox.id + ")"
                )
                if (cashbox.name == selectedCashboxName) {
                    cashboxIdToSave = cashbox.id
                    Log.d(
                        "EditTransaction",
                        "Found matching cashbox: " + cashbox.name + " (ID: " + cashbox.id + ")"
                    )
                    break
                }
            }

            Log.d("EditTransaction", "Cashbox ID from name search: " + cashboxIdToSave)


            // إذا لم يتم العثور على الصندوق من النص، استخدم selectedCashboxId
            if (cashboxIdToSave == -1L && selectedCashboxId != -1L) {
                cashboxIdToSave = selectedCashboxId
                Log.d("EditTransaction", "Using selectedCashboxId: " + selectedCashboxId)
            }


            // إذا لم يتم العثور على صندوق محدد، استخدم الصندوق الرئيسي
            if (cashboxIdToSave == -1L && mainCashboxId != -1L) {
                cashboxIdToSave = mainCashboxId
                Log.d("EditTransaction", "Using mainCashboxId: " + mainCashboxId)
            }


            // إذا لم يكن هناك صندوق رئيسي، استخدم أول صندوق متاح
            if (cashboxIdToSave == -1L && !allCashboxes.isEmpty()) {
                cashboxIdToSave = allCashboxes.get(0).id
                Log.d("EditTransaction", "Using first available cashbox: " + allCashboxes.get(0).id)
            }


            // إذا لم تكن هناك صناديق على الإطلاق، احتفظ بالصندوق القديم
            if (cashboxIdToSave == -1L) {
                cashboxIdToSave = oldTransaction!!.getCashboxId()
                Log.d(
                    "EditTransaction",
                    "Using old transaction cashbox: " + oldTransaction!!.getCashboxId()
                )
                if (cashboxIdToSave == -1L) {
                    Toast.makeText(
                        requireContext(),
                        "خطأ: لا توجد صناديق متاحة",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
            }


            // التحقق النهائي من أن الصندوق صحيح
            if (cashboxIdToSave == -1L) {
                Toast.makeText(requireContext(), "خطأ: لم يتم تحديد صندوق صحيح", Toast.LENGTH_SHORT)
                    .show()
                return
            }

            Log.d("EditTransaction", "Final cashbox ID to save: " + cashboxIdToSave)
            transaction.setCashboxId(cashboxIdToSave)

            transactionsViewModel!!.updateTransaction(transaction)
            Toast.makeText(requireContext(), R.string.transaction_updated, Toast.LENGTH_SHORT)
                .show()
            findNavController(requireView()).navigateUp()
        } catch (e: NumberFormatException) {
            binding!!.amountEditText.setError(getString(R.string.error_invalid_amount))
        }
    }

    private val selectedCurrency: String
        get() {
            val checkedId = binding!!.currencyRadioGroup.getCheckedRadioButtonId()
            if (checkedId == R.id.radioYer) {
                return getString(R.string.currency_yer)
            } else if (checkedId == R.id.radioSar) {
                return getString(R.string.currency_sar)
            } else {
                return getString(R.string.currency_usd)
            }
        }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
package com.hillal.acc.ui.transactions

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.view.WindowManager
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
import com.hillal.acc.App
import com.hillal.acc.R
import com.hillal.acc.data.entities.Cashbox
import com.hillal.acc.data.model.Account
import com.hillal.acc.data.model.Transaction
import com.hillal.acc.data.preferences.UserPreferences
import com.hillal.acc.data.repository.TransactionRepository
import com.hillal.acc.databinding.FragmentAddTransactionBinding
import com.hillal.acc.ui.cashbox.AddCashboxDialog
import com.hillal.acc.ui.cashbox.AddCashboxDialog.OnCashboxAddedListener
import com.hillal.acc.ui.common.AccountPickerBottomSheet
import com.hillal.acc.viewmodel.AccountViewModel
import com.hillal.acc.viewmodel.CashboxViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

class AddTransactionFragment : Fragment() {
    private var transactionsViewModel: TransactionsViewModel? = null
    private var accountViewModel: AccountViewModel? = null
    private var cashboxViewModel: CashboxViewModel? = null
    private var transactionRepository: TransactionRepository? = null
    private var userPreferences: UserPreferences? = null
    private var allAccounts: List<Account> = emptyList()
    private var allCashboxes: List<Cashbox> = emptyList()
    private var suggestions: List<String> = emptyList()
    private var selectedAccountId: Long = -1L
    private var selectedCashboxId: Long = -1L
    private var mainCashboxId: Long = -1L
    private var isDialogShown = false
    private var lastSavedTransaction: Transaction? = null
    private var lastSavedAccount: Account? = null
    private var lastSavedBalance = 0.0
    private val calendar: Calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("ar"))
    private var accountBalancesMap: Map<Long, Map<String, Double>> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        transactionsViewModel = ViewModelProvider(this).get(TransactionsViewModel::class.java)
        accountViewModel = ViewModelProvider(this).get(AccountViewModel::class.java)
        cashboxViewModel = ViewModelProvider(this).get(CashboxViewModel::class.java)
        userPreferences = UserPreferences(requireContext())
        val app = requireActivity().getApplication() as App
        transactionRepository = TransactionRepository(app.getDatabase())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
<<<<<<< HEAD
    ): View {
        // تحميل الحسابات
        accountViewModel!!.getAllAccounts().observe(this) { accounts ->
            allAccounts = accounts.filterNotNull()
        }
        // تحميل الصناديق
        cashboxViewModel!!.getAllCashboxes().observe(this) { cashboxes ->
            allCashboxes = cashboxes.filterNotNull()
        }
        // تحميل الاقتراحات (افتراضيًا للحساب الأول)
        if (allAccounts.isNotEmpty()) {
            loadAccountSuggestions(allAccounts[0].id)
        }
        // تحميل الأرصدة
        transactionsViewModel!!.accountBalancesMap.observe(this) { balancesMap ->
            accountBalancesMap = balancesMap?.mapNotNull { (k, v) ->
                if (k != null && v != null) k to v.filterKeys { it != null }.mapKeys { it.key!! }.filterValues { it != null }.mapValues { it.value!! } else null
            }?.toMap() ?: emptyMap()
        }
        return androidx.compose.ui.platform.ComposeView(requireContext()).apply {
            setContent {
                AddTransactionScreen(
                    accounts = allAccounts,
                    cashboxes = allCashboxes,
                    suggestions = suggestions,
                    accountBalancesMap = accountBalancesMap,
                    selectedAccountId = selectedAccountId,
                    selectedCashboxId = selectedCashboxId,
                    onAccountSelected = { id ->
                        selectedAccountId = id
                        loadAccountSuggestions(id)
                    },
                    onCashboxSelected = { id ->
                        selectedCashboxId = id
                    },
                    onAddCashbox = { name ->
                        openAddCashboxDialog(name)
                    },
                    onSuggestionSelected = { suggestion ->
                        // يمكن تحديث حقل البيان في Compose عبر State
                    },
                    onSaveCredit = { accountId, amount, description, notes, currency, date, cashboxId ->
                        saveTransactionFromCompose(
                            accountId, amount, description, notes, currency, date, cashboxId, isDebit = false
                        )
                    },
                    onSaveDebit = { accountId, amount, description, notes, currency, date, cashboxId ->
                        saveTransactionFromCompose(
                            accountId, amount, description, notes, currency, date, cashboxId, isDebit = true
                        )
                    },
                    onCancel = { requireActivity().onBackPressedDispatcher.onBackPressed() }
                )
            }
        }
=======
    ): View? {
        binding = FragmentAddTransactionBinding.inflate(inflater, container, false)
        val view: View = binding!!.getRoot()
        setupViews()
        setupListeners()
        loadAccounts()
        loadAllTransactions()
        setupAccountPicker()
        setupCashboxDropdown()
        transactionsViewModel!!.accountBalancesMap.observe(
            getViewLifecycleOwner(),
            Observer { balancesMap: MutableMap<Long?, MutableMap<String?, Double?>?>? ->
                accountBalancesMap =
                    if (balancesMap != null) balancesMap else HashMap<Long?, MutableMap<String?, Double?>?>()
            })
        return view
>>>>>>> parent of 867d86c04 (تصميم اضافه معامله بشكل جديد)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            requireActivity().getWindow().setStatusBarColor(Color.TRANSPARENT)
        }
        // ضبط insets للجذر عند ظهور الكيبورد أو أزرار النظام
        ViewCompat.setOnApplyWindowInsetsListener(
            view,
            OnApplyWindowInsetsListener { v: View?, insets: WindowInsetsCompat? ->
                var bottom = insets!!.getInsets(WindowInsetsCompat.Type.ime()).bottom
                if (bottom == 0) {
                    bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
                }
                v!!.setPadding(0, 0, 0, bottom)
                insets
            })
        setupViews()
        setupListeners()
        loadAccounts()
        loadAllTransactions()
        setupAccountPicker()
        setupCashboxDropdown()
        transactionsViewModel!!.accountBalancesMap.observe(
            getViewLifecycleOwner(),
            Observer { balancesMap: MutableMap<Long?, MutableMap<String?, Double?>?>? ->
                // accountBalancesMap =
                //     if (balancesMap != null) balancesMap else HashMap<Long?, MutableMap<String?, Double?>?>()
            })
    }

    private fun setupViews() {
        // Set initial date
        updateDateField()

        // تعيين ريال يمني كخيار افتراضي
        binding!!.radioYer.setChecked(true)


        // إضافة اسم المستخدم في الملاحظات
        val userName = userPreferences!!.getUserName()
        if (!userName.isEmpty()) {
            binding!!.notesEditText.setText(userName)
        }
    }

    private fun setupListeners() {
        binding!!.debitButton.setOnClickListener(View.OnClickListener { v: View? ->
            saveTransaction(
                true
            )
        })
        binding!!.creditButton.setOnClickListener(View.OnClickListener { v: View? ->
            saveTransaction(
                false
            )
        })
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
                    // إنشاء خريطة لتخزين عدد المعاملات لكل حساب
                    val accountTransactionCount: MutableMap<Long?, Int?> = HashMap<Long?, Int?>()


                    // حساب عدد المعاملات لكل حساب
                    for (transaction in allTransactions) {
                        val accountId = transaction.getAccountId()
                        accountTransactionCount.put(
                            accountId,
                            accountTransactionCount.getOrDefault(accountId, 0)!! + 1
                        )
                    }


                    // ترتيب الحسابات حسب عدد المعاملات (تنازلياً)
                    accounts.sortWith(compareByDescending { account -> accountTransactionCount.getOrDefault(account!!.getId(), 0) })

                    allAccounts = accounts
                    setupAccountDropdown(accounts)
                }
            })
    }

    private fun setupAccountDropdown(accounts: MutableList<Account?>?) {
        // لم يعد هناك داعي للـ AutoComplete التقليدي
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

    private fun saveTransaction(isDebit: Boolean) {
        if (selectedAccountId == -1L) {
            Toast.makeText(requireContext(), "الرجاء اختيار الحساب", Toast.LENGTH_SHORT).show()
            return
        }

        val amountStr = binding!!.amountEditText.getText().toString()
        val description = binding!!.descriptionEditText.getText().toString()
        val notes = binding!!.notesEditText.getText().toString()
        val currency = this.selectedCurrency

        if (amountStr.isEmpty()) {
            binding!!.amountEditText.setError(getString(R.string.error_amount_required))
            return
        }

        try {
            val amount = amountStr.toDouble()

            accountViewModel!!.getAccountById(selectedAccountId)
                .observe(getViewLifecycleOwner(), Observer { account: Account? ->
                    if (account != null) {
                        val transaction = Transaction(
                            selectedAccountId,
                            amount,
                            if (isDebit) "debit" else "credit",
                            description,
                            currency
                        )
                        transaction.id = System.currentTimeMillis()
                        transaction.setNotes(notes)
                        transaction.setTransactionDate(calendar.getTimeInMillis())
                        transaction.setUpdatedAt(System.currentTimeMillis())
                        transaction.setServerId(-1)
                        transaction.setWhatsappEnabled(account.isWhatsappEnabled())

                        // تحسين منطق اختيار الصندوق
                        val cashboxIdToSave: Long
                        if (selectedCashboxId != -1L) {
                            // إذا تم اختيار صندوق محدد
                            cashboxIdToSave = selectedCashboxId
                        } else if (mainCashboxId != -1L) {
                            // إذا لم يتم اختيار صندوق، استخدم الصندوق الرئيسي
                            cashboxIdToSave = mainCashboxId
                        } else if (!allCashboxes.isEmpty()) {
                            // إذا لم يكن هناك صندوق رئيسي، استخدم أول صندوق متاح
                            cashboxIdToSave = allCashboxes.get(0).id
                        } else {
                            // إذا لم تكن هناك صناديق على الإطلاق، لا تحفظ صندوق
                            cashboxIdToSave = -1
                            Toast.makeText(
                                requireContext(),
                                "تحذير: لا توجد صناديق متاحة",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        transaction.setCashboxId(cashboxIdToSave)

                        transactionsViewModel!!.insertTransaction(transaction)
                        lastSavedTransaction = transaction
                        lastSavedAccount = account
                        // احسب الرصيد حتى تاريخ المعاملة
                        transactionRepository!!.getBalanceUntilDate(
                            selectedAccountId,
                            transaction.getTransactionDate(),
                            currency
                        )
                            .observe(getViewLifecycleOwner(), Observer { balance: Double? ->
                                lastSavedBalance = if (balance != null) balance else 0.0
                                showSuccessDialog()
                            })

                        // عند حفظ المعاملة (بعد التأكد أن البيان غير فارغ)
                        val desc =
                            binding!!.descriptionEditText.getText().toString().trim { it <= ' ' }
                        if (!desc.isEmpty()) {
                            val prefs = requireContext().getSharedPreferences(
                                "suggestions",
                                Context.MODE_PRIVATE
                            )
                            val key = "descriptions_" + selectedAccountId
                            var suggestions: MutableSet<String?> =
                                prefs.getStringSet(key, HashSet<String?>())!!
                            suggestions = HashSet<String?>(suggestions) // حتى لا تكون read-only
                            suggestions.add(desc)
                            prefs.edit().putStringSet(key, suggestions).apply()
                            loadAccountSuggestions(selectedAccountId) // لتحديث الاقتراحات فورًا
                        }
                    }
                })
        } catch (e: NumberFormatException) {
            binding!!.amountEditText.setError(getString(R.string.error_invalid_amount))
        }
    }

    private fun saveTransactionFromCompose(
        accountId: Long,
        amount: Double,
        description: String,
        notes: String,
        currency: String,
        date: Long,
        cashboxId: Long,
        isDebit: Boolean
    ) {
        // نفس منطق saveTransaction القديم لكن استخدم القيم القادمة من Compose
        if (accountId == -1L) {
            Toast.makeText(requireContext(), "الرجاء اختيار الحساب", Toast.LENGTH_SHORT).show()
            return
        }
        accountViewModel!!.getAccountById(accountId)
            .observe(viewLifecycleOwner, Observer { account: Account? ->
                if (account != null) {
                    val transaction = Transaction(
                        accountId,
                        amount,
                        if (isDebit) "debit" else "credit",
                        description,
                        currency
                    )
                    transaction.id = System.currentTimeMillis()
                    transaction.setNotes(notes)
                    transaction.setTransactionDate(date)
                    transaction.setUpdatedAt(System.currentTimeMillis())
                    transaction.setServerId(-1)
                    transaction.setWhatsappEnabled(account.isWhatsappEnabled())
                    transaction.setCashboxId(cashboxId)
                    transactionsViewModel!!.insertTransaction(transaction)
                    lastSavedTransaction = transaction
                    lastSavedAccount = account
                    transactionRepository!!.getBalanceUntilDate(
                        accountId,
                        transaction.getTransactionDate(),
                        currency
                    ).observe(viewLifecycleOwner, Observer { balance: Double? ->
                        lastSavedBalance = balance ?: 0.0
                        showSuccessDialog()
                    })
                    // تحديث اقتراحات البيان
                    if (description.isNotBlank()) {
                        val prefs = requireContext().getSharedPreferences(
                            "suggestions",
                            Context.MODE_PRIVATE
                        )
                        val key = "descriptions_" + accountId
                        var suggestions: MutableSet<String?> =
                            prefs.getStringSet(key, HashSet<String?>())!!
                        suggestions = HashSet<String?>(suggestions)
                        suggestions.add(description)
                        prefs.edit().putStringSet(key, suggestions).apply()
                        loadAccountSuggestions(accountId)
                    }
                }
            })
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

    private fun setupAccountPicker() {
        binding!!.accountAutoComplete.setFocusable(false)
        binding!!.accountAutoComplete.setOnClickListener(View.OnClickListener { v: View? -> showAccountPickerBottomSheet() })
    }

    private fun showAccountPickerBottomSheet() {
        if (allAccounts == null || allAccounts!!.isEmpty()) {
            Toast.makeText(requireContext(), "جاري تحميل الحسابات...", Toast.LENGTH_SHORT).show()
            return
        }
        val bottomSheet = AccountPickerBottomSheet(
            allAccounts,
            allTransactions,
            accountBalancesMap,
            AccountPickerBottomSheet.OnAccountSelectedListener { account: Account? ->
                binding!!.accountAutoComplete.setText(account!!.getName())
                selectedAccountId = account.getId()
                loadAccountSuggestions(selectedAccountId) // تحميل اقتراحات الحساب المختار
            }
        )
        bottomSheet.show(getParentFragmentManager(), "AccountPicker")
    }

    private fun loadAllTransactions() {
        val txViewModel =
            ViewModelProvider(this).get<TransactionsViewModel>(TransactionsViewModel::class.java)
        txViewModel.getTransactions()
            .observe(getViewLifecycleOwner(), Observer { txs: MutableList<Transaction>? ->
                if (txs != null) allTransactions = txs
            })
        txViewModel.loadAllTransactions()
    }

    private fun setupCashboxDropdown() {
        cashboxViewModel!!.getAllCashboxes()
            .observe(getViewLifecycleOwner(), Observer { cashboxes: MutableList<Cashbox?>? ->
                allCashboxes = if (cashboxes != null) cashboxes.filterNotNull().toMutableList() else ArrayList<Cashbox>()
                val names: MutableList<String?> = ArrayList<String?>()
                mainCashboxId = -1
                for (c in allCashboxes) {
                    names.add(c.name)
                }
                if (!allCashboxes.isEmpty()) {
                    // اختر أول صندوق كافتراضي
                    mainCashboxId = allCashboxes.get(0).id
                    binding!!.cashboxAutoComplete.setText(allCashboxes.get(0).name, false)
                    selectedCashboxId = mainCashboxId
                }
                // أضف خيار إضافة صندوق جديد إذا لم يكن موجودًا بالفعل
                if (!names.contains("➕ إضافة صندوق جديد...")) {
                    names.add("➕ إضافة صندوق جديد...")
                }
                val adapter = ArrayAdapter<String?>(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    names
                )
                binding!!.cashboxAutoComplete.setAdapter<ArrayAdapter<String?>?>(adapter)
                binding!!.cashboxAutoComplete.setOnItemClickListener(AdapterView.OnItemClickListener { parent: AdapterView<*>?, v: View?, position: Int, id: Long ->
                    if (position == allCashboxes.size) {
                        // خيار إضافة صندوق جديد
                        openAddCashboxDialog()
                    } else {
                        selectedCashboxId = allCashboxes.get(position).id
                    }
                })
                // تفعيل القائمة المنسدلة عند النقر
                binding!!.cashboxAutoComplete.setFocusable(false)
                binding!!.cashboxAutoComplete.setOnClickListener(View.OnClickListener { v: View? -> binding!!.cashboxAutoComplete.showDropDown() })
            })
    }

    private fun openAddCashboxDialog() {
        Log.d("AddTransactionFragment", "openAddCashboxDialog called")
        val dialog = AddCashboxDialog()
        Log.d("AddTransactionFragment", "AddCashboxDialog created")
        dialog.setListener(this)
        Log.d("AddTransactionFragment", "setListener called")
        dialog.show(getParentFragmentManager(), "AddCashboxDialog")
        Log.d("AddTransactionFragment", "Dialog shown")
    }

    override fun onCashboxAdded(name: String?) {
        Log.d("AddTransactionFragment", "=== onCashboxAdded called ===")
        Log.d("AddTransactionFragment", "Name received: " + name)


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
                        "AddTransactionFragment",
                        "Cashbox added successfully: id=" + cashbox?.id + ", name=" + cashbox?.name
                    )
                    loadingDialog.dismiss()
                    // سيتم تحديث القائمة تلقائياً عبر LiveData
                    // حدد الصندوق الجديد تلقائياً بعد إضافته
                    binding!!.cashboxAutoComplete.setText(cashbox?.name, false)
                    selectedCashboxId = cashbox?.id ?: -1
                    CashboxHelper.showSuccessMessage(requireContext(), "تم إضافة الصندوق بنجاح")
                }

                override fun onError(error: String?) {
                    Log.e("AddTransactionFragment", "Error adding cashbox: " + error)
                    loadingDialog.dismiss()
                    CashboxHelper.showErrorMessage(requireContext(), error)
                }
            })
    }

    private val isNetworkAvailable: Boolean
        get() = CashboxHelper.isNetworkAvailable(requireContext())

    private fun showSuccessDialog() {
        if (getContext() == null || isDialogShown) return
        isDialogShown = true
        val dialog = Dialog(requireContext())
        val sheetView =
            LayoutInflater.from(getContext()).inflate(R.layout.dialog_transaction_success, null)
        dialog.setContentView(sheetView)
        dialog.getWindow()!!.setBackgroundDrawableResource(android.R.color.transparent)
        val window = dialog.getWindow()
        if (window != null) {
            val params = window.getAttributes()
            params.width = WindowManager.LayoutParams.WRAP_CONTENT
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            params.gravity = Gravity.CENTER
            window.setAttributes(params)
        }
        // أزرار
        val btnWhatsapp = sheetView.findViewById<View>(R.id.btnSendWhatsapp)
        val btnSms = sheetView.findViewById<View>(R.id.btnSendSms)
        val btnAddAnother = sheetView.findViewById<View>(R.id.btnAddAnother)
        val btnExit = sheetView.findViewById<View>(R.id.btnExit)
        // واتساب
        if (lastSavedAccount != null && lastSavedAccount!!.isWhatsappEnabled()) {
            btnWhatsapp.setVisibility(View.GONE)
        } else {
            btnWhatsapp.setVisibility(View.VISIBLE)
        }
        btnWhatsapp.setOnClickListener(View.OnClickListener { v: View? ->
            if (lastSavedAccount != null) {
                lastSavedTransaction?.let { transaction ->
                    val phone = lastSavedAccount!!.getPhoneNumber()
                    if (phone == null || phone.isEmpty()) {
                        Toast.makeText(requireContext(), "رقم الهاتف غير متوفر", Toast.LENGTH_SHORT).show()
                        return@OnClickListener
                    }
                    val msg = NotificationUtils.buildWhatsAppMessage(
                        requireContext(),
                        lastSavedAccount!!.getName(),
                        transaction,
                        lastSavedBalance,
                        transaction.getType()
                    )
                    NotificationUtils.sendWhatsAppMessage(requireContext(), phone, msg)
                }
            }
        })
        // SMS
        btnSms.setOnClickListener(View.OnClickListener { v: View? ->
            if (lastSavedAccount != null) {
                lastSavedTransaction?.let { transaction ->
                    val phone = lastSavedAccount!!.getPhoneNumber()
                    if (phone == null || phone.isEmpty()) {
                        Toast.makeText(requireContext(), "رقم الهاتف غير متوفر", Toast.LENGTH_SHORT).show()
                        return@OnClickListener
                    }
                    val type = transaction.getType()
                    val amountStr = String.format(Locale.US, "%.0f", transaction.getAmount())
                    val balanceStr = String.format(Locale.US, "%.0f", abs(lastSavedBalance))
                    val currency = transaction.getCurrency()
                    val typeText =
                        if (type.equals("credit", ignoreCase = true) || type == "له") "لكم" else "عليكم"
                    val balanceText = if (lastSavedBalance >= 0) "الرصيد لكم " else "الرصيد عليكم "
                    val message = ("حسابكم لدينا:\n"
                            + typeText + " " + amountStr + " " + currency + "\n"
                            + transaction.getDescription() + "\n"
                            + balanceText + balanceStr + " " + currency)
                    NotificationUtils.sendSmsMessage(requireContext(), phone, message)
                }
            }
        })
        // رجوع (إضافة قيد جديد لنفس العميل)
        btnAddAnother.setOnClickListener(View.OnClickListener { v: View? ->
            dialog.dismiss()
            isDialogShown = false
            clearFieldsForAnother()
        })
        // خروج
        btnExit.setOnClickListener(View.OnClickListener { v: View? ->
            dialog.dismiss()
            isDialogShown = false
            val view = getView()
            if (isAdded() && view != null) {
                findNavController(view).navigateUp()
            }
        })
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun clearFieldsForAnother() {
        binding?.let {
            it.amountEditText.setText("")
            it.descriptionEditText.setText("")
            calendar.setTimeInMillis(System.currentTimeMillis())
            updateDateField()
        }
    }

    private fun loadAccountSuggestions(accountId: Long) {
        val prefs = requireContext().getSharedPreferences("suggestions", Context.MODE_PRIVATE)
        val key = "descriptions_" + accountId
        val set = prefs.getStringSet(key, HashSet<String>())
        suggestions = set?.filterNotNull() ?: emptyList()
        val adapter = ArrayAdapter<String?>(
            requireContext(),
            R.layout.dropdown_item_suggestion,
            ArrayList<String?>(suggestions)
        )
        binding?.let { b ->
            b.descriptionEditText.setAdapter<ArrayAdapter<String?>?>(adapter)
            val itemHeightPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                48f,
                getResources().getDisplayMetrics()
            ).toInt()
            b.descriptionEditText.setDropDownHeight(itemHeightPx * 4)
            b.descriptionEditText.setDropDownBackgroundResource(R.drawable.bg_dropdown_suggestions)
            b.descriptionEditText.setOnFocusChangeListener(OnFocusChangeListener { v: View?, hasFocus: Boolean ->
                if (hasFocus) {
                    b.descriptionEditText.showDropDown()
                }
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
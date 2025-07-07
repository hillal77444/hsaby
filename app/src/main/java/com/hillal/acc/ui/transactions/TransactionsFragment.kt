package com.hillal.acc.ui.transactions

import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.net.ConnectivityManager
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.NumberPicker
import android.widget.NumberPicker.OnValueChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hillal.acc.App
import com.hillal.acc.R
import com.hillal.acc.data.model.Account
import com.hillal.acc.data.model.Transaction
import com.hillal.acc.data.remote.RetrofitClient
import com.hillal.acc.data.repository.TransactionRepository
import com.hillal.acc.databinding.FragmentTransactionsBinding
import com.hillal.acc.ui.adapters.TransactionAdapter
import com.hillal.acc.ui.adapters.TransactionAdapter.OnDeleteClickListener
import com.hillal.acc.ui.adapters.TransactionAdapter.OnEditClickListener
import com.hillal.acc.ui.adapters.TransactionAdapter.OnSmsClickListener
import com.hillal.acc.ui.adapters.TransactionAdapter.OnWhatsAppClickListener
import com.hillal.acc.ui.adapters.TransactionAdapter.TransactionDiffCallback
import com.hillal.acc.ui.common.AccountPickerBottomSheet
import com.hillal.acc.ui.transactions.NotificationUtils.buildWhatsAppMessage
import com.hillal.acc.viewmodel.AccountViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

class TransactionsFragment : Fragment() {
    private var binding: FragmentTransactionsBinding? = null
    private var viewModel: TransactionsViewModel? = null
    private var adapter: TransactionAdapter? = null
    private var accountViewModel: AccountViewModel? = null
    private var transactionViewModel: TransactionViewModel? = null
    private var transactionRepository: TransactionRepository? = null
    private var startDate: Calendar? = null
    private var endDate: Calendar? = null
    private var selectedAccount: String? = null
    private var allTransactions: MutableList<Transaction> = ArrayList<Transaction>()
    private var allAccounts: MutableList<Account>? = ArrayList<Account>()
    private var accountBalancesMap: MutableMap<Long?, MutableMap<String?, Double?>?> =
        HashMap<Long?, MutableMap<String?, Double?>?>()
    private val isStartDate = true
    private val isFirstLoad = true
    private val lastSyncTime: Long = 0
    private val accountMap: MutableMap<Long?, Account?> = HashMap<Long?, Account?>()
    private var isSearchActive = false // متغير لتتبع حالة البحث
    private var currentSearchText = "" // متغير لتخزين نص البحث الحالي

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = requireActivity().getApplication() as App
        viewModel =
            ViewModelProvider(this).get<TransactionsViewModel>(TransactionsViewModel::class.java)
        accountViewModel =
            ViewModelProvider(this).get<AccountViewModel>(AccountViewModel::class.java)
        val accountRepository = app.getAccountRepository()
        val factory = TransactionViewModelFactory(accountRepository)
        transactionViewModel = ViewModelProvider(this, factory).get<TransactionViewModel>(
            TransactionViewModel::class.java
        )
        transactionRepository = TransactionRepository(app.getDatabase())
        setHasOptionsMenu(true)


        // تهيئة التواريخ الافتراضية
        startDate = Calendar.getInstance()
        startDate!!.add(Calendar.DAY_OF_MONTH, -4) // قبل 4 أيام
        startDate!!.set(Calendar.HOUR_OF_DAY, 0)
        startDate!!.set(Calendar.MINUTE, 0)
        startDate!!.set(Calendar.SECOND, 0)
        startDate!!.set(Calendar.MILLISECOND, 0)

        endDate = Calendar.getInstance() // اليوم الحالي
        endDate!!.set(Calendar.HOUR_OF_DAY, 23)
        endDate!!.set(Calendar.MINUTE, 59)
        endDate!!.set(Calendar.SECOND, 59)
        endDate!!.set(Calendar.MILLISECOND, 999)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding!!.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // تهيئة الـ RecyclerView والـ Adapter بشكل صحيح
        val recyclerView = view.findViewById<RecyclerView>(R.id.transactionsRecyclerView)
        recyclerView.setLayoutManager(LinearLayoutManager(getContext()))
        adapter =
            TransactionAdapter(TransactionDiffCallback(), requireContext(), getViewLifecycleOwner())
        recyclerView.setAdapter(adapter)

        // إعداد المستمعين للأزرار بشكل منفصل
        setupAdapterListeners()


        // إعداد الفلاتر
        setupAccountFilter()
        setupDateFilter()


        // إعداد FAB
        setupFab()


        // مراقبة البيانات
        observeAccountsAndTransactions()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.transactions_toolbar_menu, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.getActionView() as SearchView?
        if (searchView != null) {
            searchView.setQueryHint("بحث في الوصف...")
            searchView.setMaxWidth(Int.Companion.MAX_VALUE)


            // إعدادات إضافية لضمان نوع كيبورد ثابت
            searchView.setInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL)
            searchView.setImeOptions(EditorInfo.IME_ACTION_DONE)

            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return true
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    val query = newText.trim { it <= ' ' }
                    isSearchActive = !query.isEmpty() // تحديث حالة البحث
                    currentSearchText =
                        query.lowercase(Locale.getDefault()) // تحديث نص البحث الحالي
                    if (query.isEmpty()) {
                        // عند إفراغ البحث، نعود للسلوك القديم
                        viewModel!!.loadTransactionsByDateRange(
                            startDate!!.getTimeInMillis(),
                            endDate!!.getTimeInMillis()
                        )
                    } else {
                        // عند البحث، نبحث في قاعدة البيانات مباشرة بالوصف
                        viewModel!!.searchTransactionsByDescription("%" + query + "%")!!
                            .observe(
                                getViewLifecycleOwner(),
                                Observer { results: MutableList<Transaction?>? ->
                                    if (results != null) {
                                        // تطبيق فلتر الحساب فقط على النتائج
                                        val filtered: MutableList<Transaction?> =
                                            ArrayList<Transaction?>()
                                        var totalAmount = 0.0

                                        for (t in results) {
                                            var match = true


                                            // فلترة الحساب فقط
                                            if (selectedAccount != null && !selectedAccount!!.isEmpty()) {
                                                var account: Account? = null
                                                if (adapter != null && adapter!!.getAccountMap() != null) {
                                                    account = adapter!!.getAccountMap()
                                                        .get(t!!.getAccountId())
                                                }
                                                val accountName =
                                                    if (account != null) account.getName() else null
                                                if (accountName == null || accountName != selectedAccount) {
                                                    match = false
                                                }
                                            }

                                            if (match) {
                                                filtered.add(t)
                                                totalAmount += t!!.getAmount()
                                            }
                                        }


                                        // تحديث الإحصائيات والقائمة
                                        binding!!.totalTransactionsText.setText(filtered.size.toString())
                                        binding!!.totalAmountText.setText(
                                            String.format(
                                                Locale.ENGLISH,
                                                "%.2f",
                                                totalAmount
                                            )
                                        )
                                        adapter!!.submitList(filtered)
                                        binding!!.transactionsRecyclerView.setVisibility(if (filtered.isEmpty()) View.GONE else View.VISIBLE)
                                        binding!!.emptyView.setVisibility(if (filtered.isEmpty()) View.VISIBLE else View.GONE)
                                    }
                                })
                    }
                    return true
                }
            })


            // إضافة مستمع لإغلاق البحث
            searchView.setOnCloseListener(SearchView.OnCloseListener {
                isSearchActive = false // إعادة تعيين حالة البحث
                currentSearchText = "" // إفراغ نص البحث
                // إعادة تحميل البيانات بالتواريخ المحددة (السلوك القديم)
                viewModel!!.loadTransactionsByDateRange(
                    startDate!!.getTimeInMillis(),
                    endDate!!.getTimeInMillis()
                )
                false
            })


            // منع إغلاق SearchView عند فقدان التركيز (إغلاق الكيبورد)
            searchView.setOnQueryTextFocusChangeListener(OnFocusChangeListener { v: View?, hasFocus: Boolean ->
                if (!hasFocus) {
                    // لا تغلق البحث ولا تعيد collapse أبداً
                    searchView.setIconified(false)
                    searchView.setQuery(currentSearchText, false)
                    // أعِد التركيز للبحث تلقائياً إذا كان البحث نشطاً
                    if (isSearchActive) {
                        searchView.requestFocus()
                    }
                }
            })
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun setupAdapterListeners() {
        // مستمع الحذف
        adapter!!.setOnDeleteClickListener(OnDeleteClickListener { transaction: Transaction? ->
            AlertDialog.Builder(requireContext())
                .setTitle("تأكيد الحذف")
                .setMessage("هل أنت متأكد من حذف هذا القيد؟")
                .setPositiveButton(
                    "نعم",
                    DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                        if (!this.isNetworkAvailable) {
                            Toast.makeText(
                                requireContext(),
                                "يرجى الاتصال بالإنترنت لحذف القيد",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@OnClickListener
                        }
                        // عرض مؤشر تحميل
                        val progressDialog = ProgressDialog(requireContext())
                        progressDialog.setMessage("جاري حذف القيد...")
                        progressDialog.setCancelable(false)
                        progressDialog.show()


                        // الحصول على token المستخدم
                        val token = requireContext().getSharedPreferences(
                            "auth_prefs",
                            Context.MODE_PRIVATE
                        )
                            .getString("token", null)

                        if (token == null) {
                            progressDialog.dismiss()
                            Toast.makeText(
                                requireContext(),
                                "يرجى تسجيل الدخول أولاً",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@OnClickListener
                        }


                        // إرسال طلب الحذف إلى السيرفر
                        RetrofitClient.getApiService()
                            .deleteTransaction("Bearer " + token, transaction!!.getServerId())
                            .enqueue(object : Callback<Void?> {
                                override fun onResponse(
                                    call: Call<Void?>,
                                    response: Response<Void?>
                                ) {
                                    progressDialog.dismiss()
                                    if (response.isSuccessful()) {
                                        // إذا نجح الحذف من السيرفر، نقوم بحذفه من قاعدة البيانات المحلية
                                        transactionViewModel!!.deleteTransaction(transaction)
                                        requireActivity().runOnUiThread(Runnable {
                                            Toast.makeText(
                                                requireContext(),
                                                "تم حذف القيد بنجاح",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        })
                                    } else {
                                        requireActivity().runOnUiThread(Runnable {
                                            Toast.makeText(
                                                requireContext(),
                                                "فشل في حذف القيد من السيرفر",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        })
                                    }
                                }

                                override fun onFailure(call: Call<Void?>, t: Throwable) {
                                    progressDialog.dismiss()
                                    requireActivity().runOnUiThread(Runnable {
                                        Toast.makeText(
                                            requireContext(),
                                            "خطأ في الاتصال بالسيرفر",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    })
                                }
                            })
                    })
                .setNegativeButton("لا", null)
                .show()
        })

        // مستمع التعديل
        adapter!!.setOnEditClickListener(OnEditClickListener { transaction: Transaction? ->
            val args = Bundle()
            args.putLong("transactionId", transaction!!.getId())
            findNavController(requireView()).navigate(
                R.id.action_transactions_to_editTransaction,
                args
            )
        })

        // مستمع واتساب
        adapter!!.setOnWhatsAppClickListener(OnWhatsAppClickListener { transaction: Transaction?, phoneNumber: String? ->
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                Toast.makeText(requireContext(), "رقم الهاتف غير متوفر", Toast.LENGTH_SHORT).show()
                return@OnWhatsAppClickListener
            }
            // الحصول على معلومات الحساب
            accountViewModel!!.getAccountById(transaction!!.getAccountId())
                .observe(getViewLifecycleOwner(), Observer { account: Account? ->
                    if (account != null) {
                        // مراقبة الرصيد حتى التاريخ
                        transactionRepository!!.getBalanceUntilDate(
                            transaction.getAccountId(),
                            transaction.getTransactionDate(),
                            transaction.getCurrency()
                        )
                            .observe(getViewLifecycleOwner(), Observer { balance: Double? ->
                                if (balance != null) {
                                    val type = transaction.getType()
                                    val message = buildWhatsAppMessage(
                                        account.getName(),
                                        transaction,
                                        balance,
                                        type
                                    )
                                    sendWhatsAppMessage(requireContext(), phoneNumber, message)
                                }
                            })
                    }
                })
        })

        // مستمع SMS
        adapter!!.setOnSmsClickListener(OnSmsClickListener { transaction: Transaction?, phoneNumber: String? ->
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                Toast.makeText(requireContext(), "رقم الهاتف غير متوفر", Toast.LENGTH_SHORT).show()
                return@OnSmsClickListener
            }
            accountViewModel!!.getAccountById(transaction!!.getAccountId())
                .observe(getViewLifecycleOwner(), Observer { account: Account? ->
                    if (account != null) {
                        transactionRepository!!.getBalanceUntilDate(
                            transaction.getAccountId(),
                            transaction.getTransactionDate(),
                            transaction.getCurrency()
                        )
                            .observe(getViewLifecycleOwner(), Observer { balance: Double? ->
                                if (balance != null) {
                                    val type = transaction.getType()
                                    val amountStr =
                                        String.format(Locale.US, "%.0f", transaction.getAmount())
                                    val balanceStr = String.format(Locale.US, "%.0f", abs(balance))
                                    val currency = transaction.getCurrency()
                                    val typeText = if (type.equals(
                                            "credit",
                                            ignoreCase = true
                                        ) || type == "له"
                                    ) "لكم" else "عليكم"
                                    val balanceText =
                                        if (balance >= 0) "الرصيد لكم " else "الرصيد عليكم "
                                    val message = ("حسابكم لدينا:\n"
                                            + typeText + " " + amountStr + " " + currency + "\n"
                                            + transaction.getDescription() + "\n"
                                            + balanceText + balanceStr + " " + currency)
                                    sendSmsMessage(requireContext(), phoneNumber, message)
                                }
                            })
                    }
                })
        })
    }

    private fun setupAccountFilter() {
        // تحميل الحسابات
        accountViewModel!!.getAllAccounts()
            .observe(getViewLifecycleOwner(), Observer { accounts: MutableList<Account>? ->
                if (accounts != null) {
                    allAccounts = accounts
                    // تحديث خريطة الحسابات
                    accountMap.clear()
                    for (account in accounts) {
                        accountMap.put(account.getId(), account)
                    }
                    // تعيين الخريطة للـ adapter بعد كل تحديث
                    if (adapter != null) {
                        adapter!!.setAccountMap(accountMap)
                    }
                }
            })

        // إعداد مستمع النقر على حقل اختيار الحساب
        binding!!.accountFilterDropdown.setFocusable(false)
        binding!!.accountFilterDropdown.setOnClickListener(View.OnClickListener { v: View? -> showAccountPicker() })
    }

    private fun showAccountPicker() {
        if (allAccounts == null || allAccounts!!.isEmpty()) {
            Toast.makeText(requireContext(), "جاري تحميل الحسابات...", Toast.LENGTH_SHORT).show()
            return
        }
        val bottomSheet = AccountPickerBottomSheet(
            allAccounts,
            allTransactions,
            accountBalancesMap,
            AccountPickerBottomSheet.OnAccountSelectedListener { account: Account? ->
                selectedAccount = account!!.getName()
                binding!!.accountFilterDropdown.setText(account.getName())
                applyAllFilters()
            }
        )
        bottomSheet.show(getParentFragmentManager(), "AccountPicker")
    }

    private fun setupDateFilter() {
        updateDateInputs()
        binding!!.startDateFilter.setOnClickListener(View.OnClickListener { v: View? ->
            showDatePicker(true)
        })
        binding!!.endDateFilter.setOnClickListener(View.OnClickListener { v: View? ->
            showDatePicker(false)
        })
    }

    private fun showDatePicker(isStart: Boolean) {
        // استخدم Dialog عجلة التاريخ مثل AccountStatementActivity
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_simple_date_picker)

        val dayPicker = dialog.findViewById<NumberPicker>(R.id.dayPicker)
        val monthPicker = dialog.findViewById<NumberPicker>(R.id.monthPicker)
        val yearPicker = dialog.findViewById<NumberPicker>(R.id.yearPicker)
        val btnOk = dialog.findViewById<TextView>(R.id.btnOk)
        val btnCancel = dialog.findViewById<TextView>(R.id.btnCancel)

        // جلب التاريخ الحالي (بداية أو نهاية)
        val cal = if (isStart) startDate!!.clone() as Calendar else endDate!!.clone() as Calendar
        val selectedYear = cal.get(Calendar.YEAR)
        val selectedMonth = cal.get(Calendar.MONTH) + 1
        val selectedDay = cal.get(Calendar.DAY_OF_MONTH)

        yearPicker.setMinValue(selectedYear - 50)
        yearPicker.setMaxValue(selectedYear + 10)
        yearPicker.setValue(selectedYear)
        yearPicker.setFormatter(NumberPicker.Formatter { value: Int ->
            String.format(
                Locale.ENGLISH,
                "%d",
                value
            )
        })

        val arabicMonths = arrayOf<String?>(
            "يناير",
            "فبراير",
            "مارس",
            "أبريل",
            "مايو",
            "يونيو",
            "يوليو",
            "أغسطس",
            "سبتمبر",
            "أكتوبر",
            "نوفمبر",
            "ديسمبر"
        )
        monthPicker.setMinValue(1)
        monthPicker.setMaxValue(12)
        monthPicker.setDisplayedValues(arabicMonths)
        monthPicker.setValue(selectedMonth)
        monthPicker.setFormatter(NumberPicker.Formatter { value: Int ->
            String.format(
                Locale.ENGLISH,
                "%d",
                value
            )
        })

        dayPicker.setMinValue(1)
        dayPicker.setMaxValue(getDaysInMonth(selectedYear, selectedMonth))
        dayPicker.setValue(selectedDay)
        dayPicker.setFormatter(NumberPicker.Formatter { value: Int ->
            String.format(
                Locale.ENGLISH,
                "%d",
                value
            )
        })

        monthPicker.setOnValueChangedListener(OnValueChangeListener { picker: NumberPicker?, oldVal: Int, newVal: Int ->
            val maxDay = getDaysInMonth(yearPicker.getValue(), newVal)
            dayPicker.setMaxValue(maxDay)
        })
        yearPicker.setOnValueChangedListener(OnValueChangeListener { picker: NumberPicker?, oldVal: Int, newVal: Int ->
            val maxDay = getDaysInMonth(newVal, monthPicker.getValue())
            dayPicker.setMaxValue(maxDay)
        })

        btnOk.setOnClickListener(View.OnClickListener { v: View? ->
            val selectedCal = Calendar.getInstance()
            selectedCal.set(Calendar.YEAR, yearPicker.getValue())
            selectedCal.set(Calendar.MONTH, monthPicker.getValue() - 1)
            selectedCal.set(Calendar.DAY_OF_MONTH, dayPicker.getValue())
            if (isStart) {
                // بداية اليوم
                selectedCal.set(Calendar.HOUR_OF_DAY, 0)
                selectedCal.set(Calendar.MINUTE, 0)
                selectedCal.set(Calendar.SECOND, 0)
                selectedCal.set(Calendar.MILLISECOND, 0)
                startDate = selectedCal
            } else {
                // نهاية اليوم
                selectedCal.set(Calendar.HOUR_OF_DAY, 23)
                selectedCal.set(Calendar.MINUTE, 59)
                selectedCal.set(Calendar.SECOND, 59)
                selectedCal.set(Calendar.MILLISECOND, 999)
                endDate = selectedCal
            }
            updateDateInputs()


            // إذا كان البحث نشط، نطبق الفلاتر مباشرة
            // وإلا نعيد تحميل البيانات بالتواريخ الجديدة
            if (isSearchActive) {
                applyAllFilters()
            } else {
                viewModel!!.loadTransactionsByDateRange(
                    startDate!!.getTimeInMillis(),
                    endDate!!.getTimeInMillis()
                )
            }
            dialog.dismiss()
        })
        btnCancel.setOnClickListener(View.OnClickListener { v: View? -> dialog.dismiss() })

        // جعل الـ Dialog يظهر من أسفل الشاشة
        if (dialog.getWindow() != null) {
            dialog.getWindow()!!.setBackgroundDrawableResource(android.R.color.transparent)
            val params = dialog.getWindow()!!.getAttributes()
            params.width = WindowManager.LayoutParams.MATCH_PARENT
            params.gravity = Gravity.BOTTOM
            dialog.getWindow()!!.setAttributes(params)
        }

        dialog.show()

        // تلوين العنصر المختار في كل NumberPicker بخلفية مخصصة (نفس الدالة)
        setNumberPickerSelectionBg(dayPicker)
        setNumberPickerSelectionBg(monthPicker)
        setNumberPickerSelectionBg(yearPicker)
    }

    // دالة لحساب عدد الأيام في الشهر
    private fun getDaysInMonth(year: Int, month: Int): Int {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    // دالة تلوين العنصر المختار في NumberPicker
    private fun setNumberPickerSelectionBg(picker: NumberPicker?) {
        try {
            val selectionDividerField =
                NumberPicker::class.java.getDeclaredField("mSelectionDivider")
            selectionDividerField.setAccessible(true)
            selectionDividerField.set(
                picker,
                requireContext().getDrawable(R.drawable.picker_selected_bg)
            )
        } catch (e: Exception) {
            // تجاهل أي خطأ
        }
    }

    private fun updateDateInputs() {
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH)
        binding!!.startDateFilter.setText(sdf.format(startDate!!.getTime()))
        binding!!.endDateFilter.setText(sdf.format(endDate!!.getTime()))
    }

    private fun setupFab() {
        binding!!.fabAddTransaction.setOnClickListener(View.OnClickListener { v: View? ->
            findNavController(requireView())
                .navigate(R.id.action_transactions_to_addTransaction)
        })

        val originalMargin =
            (binding!!.fabAddTransaction.getLayoutParams() as MarginLayoutParams).bottomMargin
        ViewCompat.setOnApplyWindowInsetsListener(
            binding!!.fabAddTransaction,
            OnApplyWindowInsetsListener { v: View?, insets: WindowInsetsCompat? ->
                val bottom = insets!!.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
                val params = v!!.getLayoutParams() as MarginLayoutParams
                params.bottomMargin = originalMargin + bottom
                v.setLayoutParams(params)
                insets
            })
    }

    private fun observeAccountsAndTransactions() {
        // عرض مؤشر التحميل
        binding!!.progressBar.setVisibility(View.VISIBLE)


        // مراقبة الحسابات
        accountViewModel!!.getAllAccounts()
            .observe(getViewLifecycleOwner(), Observer { accounts: MutableList<Account>? ->
                if (accounts != null) {
                    allAccounts = accounts
                    // تحديث خريطة الحسابات
                    accountMap.clear()
                    for (account in accounts) {
                        accountMap.put(account.getId(), account)
                    }


                    // تحديث المحول بالحسابات
                    adapter!!.setAccountMap(accountMap)


                    // تحميل المعاملات مع التصفية الافتراضية
                    viewModel!!.loadTransactionsByDateRange(
                        startDate!!.getTimeInMillis(),
                        endDate!!.getTimeInMillis()
                    )
                }
            })


        // مراقبة المعاملات
        viewModel!!.getTransactions()
            .observe(getViewLifecycleOwner(), Observer { transactions: MutableList<Transaction>? ->
                // إخفاء مؤشر التحميل
                binding!!.progressBar.setVisibility(View.GONE)
                if (transactions != null) {
                    allTransactions = transactions
                    applyAllFilters()
                } else {
                    allTransactions = ArrayList<Transaction>()
                    applyAllFilters()
                }
            })

        // مراقبة أرصدة الحسابات
        viewModel!!.accountBalancesMap.observe(
            getViewLifecycleOwner(),
            Observer { balancesMap: MutableMap<Long?, MutableMap<String?, Double?>?>? ->
                if (balancesMap != null) {
                    accountBalancesMap = balancesMap
                }
            })
    }

    private fun showDeleteConfirmationDialog(transaction: Transaction?) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_transaction)
            .setMessage(R.string.confirm_delete)
            .setPositiveButton(
                R.string.yes,
                DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                    viewModel!!.deleteTransaction(transaction)
                    Toast.makeText(
                        requireContext(),
                        R.string.transaction_deleted,
                        Toast.LENGTH_SHORT
                    ).show()
                })
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun applyAllFilters() {
        val filtered: MutableList<Transaction?> = ArrayList<Transaction?>()


        // تحويل التواريخ إلى بداية اليوم ونهاية اليوم للمقارنة
        val startCal = Calendar.getInstance()
        startCal.setTimeInMillis(startDate!!.getTimeInMillis())
        startCal.set(Calendar.HOUR_OF_DAY, 0)
        startCal.set(Calendar.MINUTE, 0)
        startCal.set(Calendar.SECOND, 0)
        startCal.set(Calendar.MILLISECOND, 0)

        val endCal = Calendar.getInstance()
        endCal.setTimeInMillis(endDate!!.getTimeInMillis())
        endCal.set(Calendar.HOUR_OF_DAY, 23)
        endCal.set(Calendar.MINUTE, 59)
        endCal.set(Calendar.SECOND, 59)
        endCal.set(Calendar.MILLISECOND, 999)

        val startTime = startCal.getTimeInMillis()
        val endTime = endCal.getTimeInMillis()

        var totalAmount = 0.0

        for (t in allTransactions) {
            var match = true


            // فلترة الحساب
            if (selectedAccount != null && !selectedAccount!!.isEmpty()) {
                var account: Account? = null
                if (adapter != null && adapter!!.getAccountMap() != null) {
                    account = adapter!!.getAccountMap().get(t.getAccountId())
                }
                val accountName = if (account != null) account.getName() else null
                if (accountName == null || accountName != selectedAccount) match = false
            }


            // فلترة التاريخ
            val transactionDate = t.getTransactionDate()
            if (transactionDate < startTime || transactionDate > endTime) {
                match = false
            }

            if (match) {
                filtered.add(t)
                totalAmount += t.getAmount()
            }
        }


        // تحديث الإحصائيات
        binding!!.totalTransactionsText.setText(filtered.size.toString())
        binding!!.totalAmountText.setText(String.format(Locale.ENGLISH, "%.2f", totalAmount))
        adapter!!.submitList(filtered)
        binding!!.transactionsRecyclerView.setVisibility(if (filtered.isEmpty()) View.GONE else View.VISIBLE)
        binding!!.emptyView.setVisibility(if (filtered.isEmpty()) View.VISIBLE else View.GONE)
    }

    private val isUserLoggedIn: Boolean
        get() {
            try {
                // التحقق من البيانات المحفوظة في SharedPreferences
                val prefs = requireContext().getSharedPreferences(
                    "user_prefs",
                    Context.MODE_PRIVATE
                )
                val savedToken = prefs.getString("auth_token", null)
                val savedUserId = prefs.getString("user_id", null)


                // إذا كانت البيانات موجودة، نعتبر المستخدم مسجل الدخول
                if (savedToken != null && savedUserId != null) {
                    return true
                }

                // إذا لم تكن البيانات موجودة، نتحقق من قاعدة البيانات
                val app = requireActivity().getApplication() as App
                return app.getDatabase().userDao().getCurrentUser() != null
            } catch (e: Exception) {
                return false
            }
        }

    override fun onResume() {
        super.onResume()
        // تحديث القائمة فقط إذا كانت فارغة
        if (adapter!!.getItemCount() == 0) {
            loadTransactions()
        }
    }

    override fun onPause() {
        super.onPause()
    }

    private fun loadTransactions() {
        // عرض مؤشر التحميل
        binding!!.progressBar.setVisibility(View.VISIBLE)


        // تحميل البيانات من قاعدة البيانات المحلية
        viewModel!!.getTransactions()
            .observe(getViewLifecycleOwner(), Observer { transactions: MutableList<Transaction>? ->
                // إخفاء مؤشر التحميل
                binding!!.progressBar.setVisibility(View.GONE)
                if (transactions != null && !transactions.isEmpty()) {
                    // تحديث القائمة
                    allTransactions = transactions
                    applyAllFilters()
                } else {
                    // عرض رسالة عدم وجود بيانات
                    binding!!.emptyView.setVisibility(View.VISIBLE)
                }
            })
    }

    private fun loadAccounts() {
        // ... existing code ...
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun buildWhatsAppMessage(
        accountName: String,
        transaction: Transaction,
        balance: Double,
        type: String?
    ): String {
        // استخدم الدالة المساعدة الجديدة
        return buildWhatsAppMessage(requireContext(), accountName, transaction, balance, type)
    }

    private fun sendWhatsAppMessage(context: Context, phoneNumber: String, message: String?) {
        NotificationUtils.sendWhatsAppMessage(context, phoneNumber, message)
    }

    private fun sendSmsMessage(context: Context, phoneNumber: String?, message: String?) {
        NotificationUtils.sendSmsMessage(context, phoneNumber, message)
    }

    private val isNetworkAvailable: Boolean
        // دالة للتحقق من وجود اتصال بالإنترنت
        get() {
            try {
                val connectivityManager =
                    requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
                if (connectivityManager != null) {
                    val activeNetworkInfo = connectivityManager.getActiveNetworkInfo()
                    return activeNetworkInfo != null && activeNetworkInfo.isConnected()
                }
            } catch (e: Exception) {
                // تجاهل أي خطأ
            }
            return false
        }
}
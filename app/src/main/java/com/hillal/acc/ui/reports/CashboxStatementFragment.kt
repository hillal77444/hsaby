package com.hillal.acc.ui.reports

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.DatePicker
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.hillal.acc.App
import com.hillal.acc.R
import com.hillal.acc.data.entities.Cashbox
import com.hillal.acc.data.model.Account
import com.hillal.acc.data.model.Transaction
import com.hillal.acc.data.repository.TransactionRepository
import com.hillal.acc.data.room.CashboxSummary
import com.hillal.acc.viewmodel.AccountViewModel
import com.hillal.acc.viewmodel.CashboxViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.CharSequence
import kotlin.Comparator
import kotlin.Double
import kotlin.Exception
import kotlin.Int
import kotlin.String
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashSet
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.contains
import kotlin.collections.containsKey
import kotlin.collections.get
import kotlin.collections.sort
import kotlin.math.abs
import kotlin.toString

class CashboxStatementFragment : Fragment() {
    private var cashboxViewModel: CashboxViewModel? = null
    private var accountViewModel: AccountViewModel? = null
    private var cashboxDropdown: AutoCompleteTextView? = null
    private var startDateInput: TextInputEditText? = null
    private var endDateInput: TextInputEditText? = null
    private var webView: WebView? = null
    private var calendar: Calendar? = null
    private var dateFormat: SimpleDateFormat? = null
    private var allCashboxes: MutableList<Cashbox> = ArrayList<Cashbox>()
    private var allTransactions: MutableList<Transaction> = ArrayList<Transaction>()
    private var transactionRepository: TransactionRepository? = null
    private var currencyButtonsLayout: LinearLayout? = null
    private var selectedCurrency: String? = null
    private var lastCashboxTransactions: MutableList<Transaction> = ArrayList<Transaction>()
    private var lastSelectedCashbox: Cashbox? = null
    private var btnPrint: ImageButton? = null
    private val accountMap: MutableMap<Long?, Account?> = HashMap<Long?, Account?>()
    private var selectedCashboxId: Long = -1L
    private val mainCashboxId: Long = -1L
    private var isSummaryMode = true
    private var allCurrencies: MutableList<String> = ArrayList<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_cashbox_statement, container, false)
        initializeViews(view)
        calendar = Calendar.getInstance()
        dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        cashboxViewModel =
            ViewModelProvider(this).get<CashboxViewModel>(CashboxViewModel::class.java)
        accountViewModel =
            ViewModelProvider(this).get<AccountViewModel>(AccountViewModel::class.java)
        transactionRepository =
            TransactionRepository((requireActivity().getApplication() as App).getDatabase())
        setupDatePickers()
        setDefaultDates()
        loadAccountsMap()
        // تفعيل التكبير والتصغير في WebView
        webView!!.getSettings().setBuiltInZoomControls(true)
        webView!!.getSettings().setDisplayZoomControls(false)
        webView!!.getSettings().setSupportZoom(true)
        // إعدادات إضافية لضمان العرض الصحيح
        webView!!.getSettings().setLoadWithOverviewMode(true)
        webView!!.getSettings().setUseWideViewPort(true)
        webView!!.getSettings().setJavaScriptEnabled(true)
        webView!!.getSettings().setDomStorageEnabled(true)
        // ضبط العرض ليتناسب مع الشاشة
        webView!!.getSettings().setLayoutAlgorithm(android.webkit.WebSettings.LayoutAlgorithm.SINGLE_COLUMN)
        // إعدادات إضافية لضمان العرض الصحيح
        webView!!.getSettings().setDefaultTextEncodingName("UTF-8")
        webView!!.getSettings().setAllowFileAccess(true)
        webView!!.getSettings().setAllowContentAccess(true)
        // ضبط العرض ليتناسب مع الشاشة
        webView!!.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY)
        webView!!.setHorizontalScrollBarEnabled(false)
        loadCashboxes()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // ضبط insets للـ WebView لرفع المحتوى فوق أزرار النظام أو الشريط السفلي
        ViewCompat.setOnApplyWindowInsetsListener(
            webView!!,
            OnApplyWindowInsetsListener { v: View?, insets: WindowInsetsCompat? ->
                val bottom = insets!!.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
                // إضافة padding إضافي أسفل WebView لتجنب تغطية الأزرار السفلية
                val extraBottomPadding = 120 // 120dp إضافية أسفل المحتوى
                v!!.setPadding(
                    v.getPaddingLeft(),
                    v.getPaddingTop(),
                    v.getPaddingRight(),
                    bottom + extraBottomPadding
                )
                insets
            })
    }

    private fun initializeViews(view: View) {
        cashboxDropdown = view.findViewById<AutoCompleteTextView>(R.id.cashboxDropdown)
        startDateInput = view.findViewById<TextInputEditText>(R.id.startDateInput)
        endDateInput = view.findViewById<TextInputEditText>(R.id.endDateInput)
        webView = view.findViewById<WebView?>(R.id.webView)
        btnPrint = view.findViewById<ImageButton>(R.id.btnPrintInCard)
        currencyButtonsLayout = view.findViewById<LinearLayout>(R.id.currencyButtonsLayout)
        currencyButtonsLayout!!.setVisibility(View.GONE)
        
        // إعدادات إضافية لضمان العرض الصحيح للـ WebView
        webView!!.setHorizontalScrollBarEnabled(false)
        webView!!.setVerticalScrollBarEnabled(true)
        webView!!.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY)
        webView!!.setOverScrollMode(View.OVER_SCROLL_NEVER)
        // إعدادات إضافية للتمرير السلس
        webView!!.setVerticalFadingEdgeEnabled(false)
        webView!!.setHorizontalFadingEdgeEnabled(false)
        webView!!.setFadingEdgeLength(0)
        // إعدادات إضافية لضمان عدم وجود محتوى تحت الأزرار
        webView!!.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY)
        webView!!.setVerticalScrollBarEnabled(true)
        webView!!.setHorizontalScrollBarEnabled(false)
        btnPrint!!.setOnClickListener(View.OnClickListener { v: View? -> printReport() })
        cashboxDropdown!!.setFocusable(false)
        cashboxDropdown!!.setOnClickListener(View.OnClickListener { v: View? -> cashboxDropdown!!.showDropDown() })
        startDateInput!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateReport()
            }
        })
        endDateInput!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateReport()
            }
        })
    }

    private fun setupDatePickers() {
        startDateInput!!.setOnClickListener(View.OnClickListener { v: View? ->
            showDatePicker(
                startDateInput!!
            )
        })
        endDateInput!!.setOnClickListener(View.OnClickListener { v: View? ->
            showDatePicker(
                endDateInput!!
            )
        })
    }

    private fun showDatePicker(input: TextInputEditText) {
        // يمكنك نقل منطق DatePicker من الـ Activity هنا إذا كان لديك Dialog مخصص
        // أو استخدم DatePickerDialog عادي
        val cal = Calendar.getInstance()
        val currentText = if (input.getText() != null) input.getText().toString() else ""
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            val parsed = sdf.parse(toEnglishDigits(currentText))
            if (parsed != null) {
                cal.setTime(parsed)
            }
        } catch (ignored: Exception) {
        }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val dialog = DatePickerDialog(
            requireContext(),
            DatePickerDialog.OnDateSetListener { view: DatePicker?, y: Int, m: Int, d: Int ->
                val selectedCal = Calendar.getInstance()
                selectedCal.set(y, m, d)
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
                val date = sdf.format(selectedCal.getTime())
                input.setText(toEnglishDigits(date))
            },
            year,
            month,
            day
        )
        dialog.show()
    }

    private fun loadCashboxes() {
        val cashboxesLiveData = cashboxViewModel!!.getAllCashboxes()
        if (cashboxesLiveData == null) {
            Toast.makeText(
                requireContext(),
                "حدث خطأ في تحميل الصناديق. الرجاء إعادة تشغيل التطبيق.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        cashboxesLiveData.observe(
            getViewLifecycleOwner(),
            Observer { cashboxes: MutableList<Cashbox?>? ->
                allCashboxes = if (cashboxes != null) cashboxes.filterNotNull().toMutableList() else ArrayList<Cashbox>()
                val names: MutableList<String?> = ArrayList<String?>()
                for (c in allCashboxes) {
                    names.add(c.name)
                }
                if (!names.contains("➕ إضافة صندوق جديد...")) {
                    names.add("➕ إضافة صندوق جديد...")
                }
                val adapter = ArrayAdapter<String?>(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    names
                )
                cashboxDropdown!!.setAdapter<ArrayAdapter<String?>?>(adapter)
                cashboxDropdown!!.setText("", false)
                selectedCashboxId = -1L
                lastSelectedCashbox = null
                if (isSummaryMode) {
                    loadSummaryWithOptimizedQueries()
                }
            })

        cashboxDropdown!!.setOnItemClickListener(AdapterView.OnItemClickListener { parent: AdapterView<*>, view: View?, position: Int, id: Long ->
            if (position == allCashboxes.size) {
                Toast.makeText(
                    requireContext(),
                    "ميزة إضافة صندوق جديد غير متوفرة بعد",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                lastSelectedCashbox = allCashboxes.get(position)
                selectedCashboxId = lastSelectedCashbox!!.id ?: -1L
                isSummaryMode = false
                currencyButtonsLayout!!.setVisibility(View.GONE)
                onCashboxSelected(lastSelectedCashbox!!)
            }
        })

        // استخدام استعلامات محسنة لجلب ملخص الصناديق والعملات
        if (isSummaryMode) {
            loadSummaryWithOptimizedQueries()
        }
    }

    private fun loadAccountsMap() {
        accountViewModel!!.getAllAccounts()
            .observe(getViewLifecycleOwner(), Observer { accounts: MutableList<Account>? ->
                if (accounts != null) {
                    for (acc in accounts) {
                        accountMap.put(acc.getId(), acc)
                    }
                }
            })
    }

    private fun onCashboxSelected(cashbox: Cashbox) {
        isSummaryMode = false
        
        // استخدام استعلام محسن بدلاً من فلترة جميع المعاملات
        transactionRepository!!.getTransactionsByCashbox(cashbox.id ?: -1L)
            .observe(getViewLifecycleOwner(), Observer { transactions: MutableList<Transaction>? ->
                if (transactions != null) {
                    allTransactions = transactions.filterNotNull().toMutableList()
                    lastCashboxTransactions = allTransactions
                    
                    if (lastCashboxTransactions.isEmpty()) {
                        currencyButtonsLayout!!.setVisibility(View.GONE)
                        webView!!.loadDataWithBaseURL(null, "<p>لا توجد بيانات</p>", "text/html", "UTF-8", null)
                        return@Observer
                    }
                    
                    val currencies = LinkedHashSet<String>()
                    for (t in lastCashboxTransactions) {
                        currencies.add(t.getCurrency().trim { it <= ' ' })
                    }
                    currencyButtonsLayout!!.removeAllViews()
                    for (currency in currencies) {
                        val btn = MaterialButton(
                            requireContext(),
                            null,
                            com.google.android.material.R.attr.materialButtonOutlinedStyle
                        )
                        btn.setText(currency)
                        btn.setCheckable(true)
                        val isSelected =
                            currency == selectedCurrency || (selectedCurrency == null && currencies.iterator()
                                .next() == currency)
                        btn.setChecked(isSelected)
                        btn.setTextColor(if (isSelected) Color.WHITE else Color.parseColor("#1976d2"))
                        btn.setBackgroundColor(
                            if (isSelected) Color.parseColor("#1976d2") else Color.parseColor(
                                "#e3f0ff"
                            )
                        )
                        btn.setCornerRadius(40)
                        btn.setTextSize(16f)
                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        params.setMarginEnd(16)
                        btn.setLayoutParams(params)
                        btn.setOnClickListener(View.OnClickListener { v: View? -> setSelectedCurrency(currency) })
                        currencyButtonsLayout!!.addView(btn)
                    }
                    currencyButtonsLayout!!.setVisibility(View.VISIBLE)
                    if (selectedCurrency == null || !currencies.contains(selectedCurrency)) {
                        selectedCurrency = currencies.iterator().next()
                    }
                    setSelectedCurrency(selectedCurrency)
                }
            })
    }

    private fun setSelectedCurrency(currency: String?) {
        selectedCurrency = currency
        for (i in 0..<currencyButtonsLayout!!.getChildCount()) {
            val btn = currencyButtonsLayout!!.getChildAt(i) as MaterialButton
            val isSelected = btn.getText().toString() == currency
            btn.setChecked(isSelected)
            btn.setBackgroundColor(
                if (isSelected) Color.parseColor("#1976d2") else Color.parseColor(
                    "#e3f0ff"
                )
            )
            btn.setTextColor(if (isSelected) Color.WHITE else Color.parseColor("#1976d2"))
        }
        updateReport()
    }

    private fun updateReport() {
        if (lastSelectedCashbox == null || selectedCurrency == null) return
        var startDate: Date? = null
        var endDate: Date? = null
        try {
            startDate = dateFormat!!.parse(startDateInput!!.getText().toString())
            endDate = dateFormat!!.parse(endDateInput!!.getText().toString())
        } catch (ignored: Exception) {
            return
        }
        val finalStartDate: Date?
        val finalEndDate: Date?
        if (startDate.after(endDate)) {
            finalStartDate = endDate
            finalEndDate = startDate
        } else {
            finalStartDate = startDate
            finalEndDate = endDate
        }
        val filtered: MutableList<Transaction> = ArrayList<Transaction>()
        for (t in lastCashboxTransactions) {
            if (t.getCurrency().trim { it <= ' ' } == selectedCurrency!!.trim { it <= ' ' }) {
                val transactionCal = Calendar.getInstance()
                transactionCal.setTimeInMillis(t.getTransactionDate())
                transactionCal.set(Calendar.HOUR_OF_DAY, 0)
                transactionCal.set(Calendar.MINUTE, 0)
                transactionCal.set(Calendar.SECOND, 0)
                transactionCal.set(Calendar.MILLISECOND, 0)
                val transactionDateOnly = transactionCal.getTime()
                if (!transactionDateOnly.before(finalStartDate) && !transactionDateOnly.after(
                        finalEndDate
                    )
                ) {
                    filtered.add(t)
                }
            }
        }
        if (filtered.isEmpty()) {
            webView!!.loadDataWithBaseURL(
                null,
                "<p>لا توجد بيانات لهذه العملة أو الفترة</p>",
                "text/html",
                "UTF-8",
                null
            )
            return
        }
        // الرصيد السابق غير مهم هنا للصندوق غالباً، يمكن تعديله لاحقاً إذا لزم
        val previousBalances: MutableMap<String?, Double?> = HashMap<String?, Double?>()
        previousBalances.put(selectedCurrency, 0.0)
        val currencyMap: MutableMap<String?, MutableList<Transaction?>?> =
            HashMap<String?, MutableList<Transaction?>?>()
        currencyMap.put(selectedCurrency, filtered.map { it as Transaction? }.toMutableList())
        val htmlContent = generateReportHtml(
            lastSelectedCashbox!!,
            finalStartDate,
            finalEndDate,
            filtered,
            previousBalances,
            currencyMap
        )
        webView!!.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    private fun generateReportHtml(
        cashbox: Cashbox,
        startDate: Date,
        endDate: Date,
        transactions: MutableList<Transaction>,
        previousBalances: MutableMap<String?, Double?>?,
        currencyMap: MutableMap<String?, MutableList<Transaction?>?>?
    ): String {
        val html = StringBuilder()
        html.append("<!DOCTYPE html>")
        html.append("<html dir='rtl' lang='ar'>")
        html.append("<head>")
        html.append("<meta charset='UTF-8'>")
        html.append("<style>")
        html.append("body { font-family: 'Cairo', Arial, sans-serif; margin: 0; background: #f5f7fa; }")
        html.append(".report-header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); padding: 8px 6px 6px 6px; margin: 4px; text-align: center; color: white; }")
        html.append(".report-header p { color: #fff; margin: 1px 0; font-size: 0.8em; font-weight: 500; }")
        html.append(".report-header .account-info { font-size: 1em; font-weight: bold; margin-bottom: 3px; }")
        html.append(".report-header .period { font-size: 0.75em; opacity: 0.9; }")
        html.append("table { width: calc(100% - 8px); border-collapse: collapse; margin: 4px; background: #fff; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 6px rgba(0,0,0,0.08); }")
        html.append("th, td { border: 1px solid #e8eaed; padding: 6px 4px; text-align: right; font-size: 0.8em; }")
        html.append("th { background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%); color: #495057; font-weight: 600; font-size: 0.75em; }")
        html.append("tr:nth-child(even) { background: #f8f9fa; }")
        html.append("tr:hover { background: #e3f2fd; transition: background 0.2s; }")
        html.append(".balance-row { background: #e8f5e8 !important; font-weight: 500; }")
        html.append(".total-row { background: linear-gradient(135deg, #f0f0f0 0%, #e0e0e0 100%) !important; font-weight: bold; color: #2c3e50; }")
        html.append("@media print { .report-header { box-shadow: none; background: #f0f0f0 !important; color: #333 !important; border: 1px solid #ccc; } .report-header p { color: #333 !important; } table { box-shadow: none; } body { background: #fff; } }")
        html.append("</style>")
        html.append("</head>")
        html.append("<body>")
        html.append("<div class='report-header'>")
        html.append("<p class='account-info'>الصندوق: ").append(cashbox.name).append(" العملة: ")
            .append(selectedCurrency).append("</p>")
        html.append("<p class='period'>من <b>").append(dateFormat!!.format(startDate))
            .append("</b> إلى <b>").append(
                dateFormat!!.format(endDate)
            ).append("</b></p>")
        html.append("</div>")
        sortTransactionsByDate(transactions)
        val previousBalance =
            calculatePreviousBalance(cashbox.id, selectedCurrency!!, startDate.getTime())
        var totalDebit = 0.0
        var totalCredit = 0.0
        html.append("<table>")
        html.append("<tr><th>التاريخ</th><th>الحساب</th><th>الوصف</th><th>دائن</th><th>مدين</th><th>الرصيد</th></tr>")
        var balance = previousBalance
        html.append("<tr class='balance-row'><td colspan='5'>الرصيد السابق حتى ")
            .append(dateFormat!!.format(startDate)).append(":</td><td>")
            .append(formatAmount(previousBalance)).append("</td></tr>")
        for (t in transactions) {
            html.append("<tr>")
            html.append("<td>").append(dateFormat!!.format(Date(t.getTransactionDate())))
                .append("</td>")
            val accountName =
                if (accountMap.containsKey(t.getAccountId())) accountMap.get(t.getAccountId())!!
                    .getName() else ""
            html.append("<td>").append(accountName).append("</td>")
            html.append("<td>").append(if (t.getDescription() != null) t.getDescription() else "")
                .append("</td>")
            if (t.getType().equals("credit", ignoreCase = true) || t.getType() == "له") {
                html.append("<td>").append(formatAmount(t.getAmount())).append("</td><td></td>")
                balance += t.getAmount()
                totalCredit += t.getAmount()
            } else {
                html.append("<td></td><td>").append(formatAmount(t.getAmount())).append("</td>")
                balance -= t.getAmount()
                totalDebit += t.getAmount()
            }
            html.append("<td>").append(formatAmount(balance)).append("</td>")
            html.append("</tr>")
        }
        html.append("<tr class='total-row'><td colspan='3'>الإجمالي</td><td>")
            .append(formatAmount(totalCredit)).append("</td><td>").append(formatAmount(totalDebit))
            .append("</td><td>").append(formatAmount(balance)).append("</td></tr>")
        html.append("</table>")
        html.append("<div class='bottom-spacer'></div>")
        html.append("</body></html>")
        return html.toString()
    }

    private fun setDefaultDates() {
        val cal = Calendar.getInstance()
        endDateInput!!.setText(dateFormat!!.format(cal.getTime()))
        cal.add(Calendar.DAY_OF_MONTH, -6)
        startDateInput!!.setText(dateFormat!!.format(cal.getTime()))
    }

    private fun printReport() {
        if (webView != null) {
            val printManager =
                requireContext().getSystemService(Context.PRINT_SERVICE) as PrintManager
            val printAdapter = webView!!.createPrintDocumentAdapter("كشف الصندوق")
            val jobName = "كشف الصندوق"
            printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
        }
    }

    private fun toEnglishDigits(value: String): String {
        return value.replace("٠", "0").replace("١", "1").replace("٢", "2").replace("٣", "3")
            .replace("٤", "4")
            .replace("٥", "5").replace("٦", "6").replace("٧", "7").replace("٨", "8")
            .replace("٩", "9")
    }

    private fun sortTransactionsByDate(transactions: MutableList<Transaction>) {
        transactions.sortWith(compareBy { it.getTransactionDate() })
    }

    private fun formatAmount(amount: Double): String {
        if (abs(amount - Math.round(amount)) < 0.01) {
            return String.format(Locale.US, "%,.0f", amount)
        } else {
            return String.format(Locale.US, "%,.2f", amount)
        }
    }

    private fun calculatePreviousBalance(
        cashboxId: Long,
        currency: String,
        beforeTime: Long
    ): Double {
        var balance = 0.0
        for (t in allTransactions) {
            if (t.getCashboxId() == cashboxId && t.getCurrency().trim { it <= ' ' } == currency.trim { it <= ' ' } && t.getTransactionDate() < beforeTime) {
                if (t.getType().equals("credit", ignoreCase = true) || t.getType() == "له") {
                    balance += t.getAmount()
                } else {
                    balance -= t.getAmount()
                }
            }
        }
        return balance
    }





    private fun setSummarySelectedCurrency(currency: String?) {
        selectedCurrency = currency
        for (i in 0 until currencyButtonsLayout!!.getChildCount()) {
            val btn = currencyButtonsLayout!!.getChildAt(i) as MaterialButton
            val isSelected = btn.getText().toString() == currency
            btn.setChecked(isSelected)
            btn.setBackgroundColor(
                if (isSelected) Color.parseColor("#1976d2") else Color.parseColor(
                    "#e3f0ff"
                )
            )
            btn.setTextColor(if (isSelected) Color.WHITE else Color.parseColor("#1976d2"))
        }
        updateSummaryReport()
    }

    private fun loadSummaryWithOptimizedQueries() {
        // جلب العملات المتوفرة
        transactionRepository!!.getAvailableCurrenciesInCashboxes()
            .observe(getViewLifecycleOwner(), Observer { currencies: List<String>? ->
                if (currencies != null && currencies.isNotEmpty()) {
                    allCurrencies = currencies.toMutableList()
                    if (selectedCurrency == null || !allCurrencies.contains(selectedCurrency)) {
                        selectedCurrency = allCurrencies[0]
                    }
                    setupCurrencyButtons()
                    updateSummaryReport()
                } else {
                    webView!!.loadDataWithBaseURL(null, "<p>لا توجد بيانات</p>", "text/html", "UTF-8", null)
                    currencyButtonsLayout!!.setVisibility(View.GONE)
                }
            })
    }

    private fun setupCurrencyButtons() {
        currencyButtonsLayout!!.removeAllViews()
        for (currency in allCurrencies) {
            val btn = MaterialButton(
                requireContext(),
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            )
            btn.setText(currency)
            btn.setCheckable(true)
            val isSelected = currency == selectedCurrency
            btn.setChecked(isSelected)
            btn.setTextColor(if (isSelected) Color.WHITE else Color.parseColor("#1976d2"))
            btn.setBackgroundColor(
                if (isSelected) Color.parseColor("#1976d2") else Color.parseColor(
                    "#e3f0ff"
                )
            )
            btn.setCornerRadius(40)
            btn.setTextSize(16f)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMarginEnd(16)
            btn.setLayoutParams(params)
            btn.setOnClickListener(View.OnClickListener { v: View? ->
                setSummarySelectedCurrency(currency)
            })
            currencyButtonsLayout!!.addView(btn)
        }
        currencyButtonsLayout!!.setVisibility(View.VISIBLE)
    }

    private fun updateSummaryReport() {
        if (selectedCurrency == null) return
        
        // جلب ملخص الصناديق للعملة المحددة
        transactionRepository!!.getCashboxesSummary()
            .observe(getViewLifecycleOwner(), Observer { summaries: List<CashboxSummary>? ->
                if (summaries != null) {
                    val filteredSummaries = summaries.filter { it.currency == selectedCurrency }
                    if (filteredSummaries.isNotEmpty()) {
                        val html = generateOptimizedCashboxesSummaryHtml(allCashboxes, filteredSummaries, selectedCurrency!!)
                        webView!!.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                    } else {
                        webView!!.loadDataWithBaseURL(null, "<p>لا توجد بيانات لهذه العملة</p>", "text/html", "UTF-8", null)
                    }
                }
            })
    }

    private fun generateOptimizedCashboxesSummaryHtml(
        cashboxes: MutableList<Cashbox>,
        summaries: List<CashboxSummary>,
        currency: String
    ): String {
        val html = StringBuilder()
        html.append("<!DOCTYPE html>")
        html.append("<html dir='rtl' lang='ar'>")
        html.append("<head>")
        html.append("<meta charset='UTF-8'>")
        html.append("<style>")
        html.append("body { font-family: 'Cairo', Arial, sans-serif; margin: 0; background: #f5f7fa; }")
        html.append(".report-header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); padding: 8px 6px 6px 6px; margin: 4px; text-align: center; color: white; }")
        html.append(".report-header p { color: #fff; margin: 1px 0; font-size: 0.8em; font-weight: 500; }")
        html.append(".report-header .account-info { font-size: 1em; font-weight: bold; margin-bottom: 3px; }")
        html.append(".report-header .period { font-size: 0.75em; opacity: 0.9; }")
        html.append("table { width: calc(100% - 8px); border-collapse: collapse; margin: 4px; background: #fff; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 6px rgba(0,0,0,0.08); }")
        html.append("th, td { border: 1px solid #e8eaed; padding: 6px 4px; text-align: right; font-size: 0.8em; }")
        html.append("th { background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%); color: #495057; font-weight: 600; font-size: 0.75em; }")
        html.append("tr:nth-child(even) { background: #f8f9fa; }")
        html.append("tr:hover { background: #e3f2fd; transition: background 0.2s; }")
        html.append(".balance-row { background: #e8f5e8 !important; font-weight: 500; }")
        html.append(".total-row { background: linear-gradient(135deg, #f0f0f0 0%, #e0e0e0 100%) !important; font-weight: bold; color: #2c3e50; }")
        html.append("@media print { .report-header { box-shadow: none; background: #f0f0f0 !important; color: #333 !important; border: 1px solid #ccc; } .report-header p { color: #333 !important; } table { box-shadow: none; } body { background: #fff; } }")
        html.append("</style>")
        html.append("</head>")
        html.append("<body>")
        html.append("<div class='report-header'>")
        html.append("<p class='account-info'>ملخص الصناديق - العملة: ").append(currency).append("</p>")
        html.append("<p class='period'>تاريخ التقرير: ").append(SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())).append("</p>")
        html.append("</div>")
        html.append("<table>")
        html.append("<tr><th>اسم الصندوق</th><th>إجمالي الدائن (له)</th><th>إجمالي المدين (عليه)</th><th>الرصيد التراكمي</th></tr>")
        
        var totalCredit = 0.0
        var totalDebit = 0.0
        var totalBalance = 0.0
        
        for (cashbox in cashboxes) {
            val summary = summaries.find { it.cashboxId == cashbox.id }
            if (summary != null) {
                html.append("<tr>")
                html.append("<td>").append(cashbox.name).append("</td>")
                html.append("<td>").append(formatAmount(summary.totalCredit)).append("</td>")
                html.append("<td>").append(formatAmount(summary.totalDebit)).append("</td>")
                html.append("<td>").append(formatAmount(summary.balance)).append("</td>")
                html.append("</tr>")
                
                totalCredit += summary.totalCredit
                totalDebit += summary.totalDebit
                totalBalance += summary.balance
            }
        }
        
        // إضافة صف الإجمالي
        html.append("<tr class='total-row'>")
        html.append("<td><strong>الإجمالي</strong></td>")
        html.append("<td><strong>").append(formatAmount(totalCredit)).append("</strong></td>")
        html.append("<td><strong>").append(formatAmount(totalDebit)).append("</strong></td>")
        html.append("<td><strong>").append(formatAmount(totalBalance)).append("</strong></td>")
        html.append("</tr>")
        
        html.append("</table>")
        html.append("<div class='bottom-spacer'></div>")
        html.append("</body></html>")
        return html.toString()
    }

    private fun getOptimizedCSS(): String {
        return """
            body { 
                font-family: 'Cairo', Arial, sans-serif; 
                margin: 0; 
                padding: 8px; 
                padding-bottom: 150px; 
                background: #f5f7fa; 
                max-width: 100%; 
                overflow-x: hidden; 
                word-wrap: break-word;
            }
            .report-header { 
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); 
                border-radius: 8px; 
                box-shadow: 0 2px 10px rgba(0,0,0,0.1); 
                padding: 8px 6px 6px 6px; 
                margin: 4px; 
                text-align: center; 
                color: white; 
            }
            .report-header p { 
                color: #fff; 
                margin: 1px 0; 
                font-size: 0.8em; 
                font-weight: 500; 
            }
            .report-header .account-info { 
                font-size: 1em; 
                font-weight: bold; 
                margin-bottom: 3px; 
            }
            .report-header .period { 
                font-size: 0.75em; 
                opacity: 0.9; 
            }
            table { 
                width: 100%; 
                max-width: 100%; 
                border-collapse: collapse; 
                margin: 4px 0; 
                background: #fff; 
                border-radius: 8px; 
                overflow: hidden; 
                box-shadow: 0 1px 6px rgba(0,0,0,0.08); 
                table-layout: fixed; 
            }
            th, td { 
                border: 1px solid #e8eaed; 
                padding: 6px 4px; 
                text-align: right; 
                font-size: 0.8em; 
                word-wrap: break-word; 
                overflow-wrap: break-word; 
            }
            th { 
                background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%); 
                color: #495057; 
                font-weight: 600; 
                font-size: 0.75em; 
            }
            tr:nth-child(even) { 
                background: #f8f9fa; 
            }
            tr:hover { 
                background: #e3f2fd; 
                transition: background 0.2s; 
            }
            .balance-row { 
                background: #e8f5e8 !important; 
                font-weight: 500; 
            }
            .total-row { 
                background: linear-gradient(135deg, #f0f0f0 0%, #e0e0e0 100%) !important; 
                font-weight: bold; 
                color: #2c3e50; 
            }
            @media print { 
                .report-header { 
                    box-shadow: none; 
                    background: #f0f0f0 !important; 
                    color: #333 !important; 
                    border: 1px solid #ccc; 
                } 
                .report-header p { 
                    color: #333 !important; 
                } 
                table { 
                    box-shadow: none; 
                } 
                body { 
                    background: #fff; 
                } 
            }
            @media screen and (max-width: 600px) { 
                body { 
                    padding: 4px; 
                    padding-bottom: 120px; 
                } 
                table { 
                    font-size: 0.7em; 
                } 
                th, td { 
                    padding: 4px 2px; 
                } 
            }
            /* إضافة مساحة إضافية أسفل المحتوى */
            .bottom-spacer {
                height: 150px;
                width: 100%;
            }
        """.trimIndent()
    }
}
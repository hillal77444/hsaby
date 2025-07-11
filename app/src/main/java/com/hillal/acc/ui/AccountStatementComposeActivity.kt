package com.hillal.acc.ui

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hillal.acc.R
import com.hillal.acc.data.model.Account
import com.hillal.acc.data.model.Transaction
import com.hillal.acc.ui.accounts.ResponsiveAccountsTheme
import com.hillal.acc.ui.theme.LocalResponsiveDimensions
import com.hillal.acc.ui.theme.ProvideResponsiveDimensions
import com.hillal.acc.viewmodel.AccountStatementViewModel
import com.hillal.acc.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.io.File
import androidx.core.content.FileProvider
import com.hillal.acc.ui.common.AccountPickerField
import com.itextpdf.text.Document
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import com.itextpdf.text.Font
import com.itextpdf.text.Paragraph
import com.itextpdf.text.Element
import android.net.Uri
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PageRange
import android.print.PrintAttributes
import android.os.ParcelFileDescriptor
import java.io.FileOutputStream
import com.itextpdf.text.BaseColor
import com.hillal.acc.util.ArabicReshaper
import com.hillal.acc.util.arabic.ArabicUtilities

class AccountStatementComposeActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private var selectedAccount: Account? = null
    private var startDate: String = ""
    private var endDate: String = ""
    private var selectedCurrency: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // إعداد التواريخ الافتراضية
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        startDate = dateFormat.format(calendar.time)
        endDate = dateFormat.format(calendar.time)
        
        setContent {
            ProvideResponsiveDimensions {
                ResponsiveAccountsTheme {
                    AccountStatementScreen(
                        onBackPressed = { finish() },
                        onPrint = { printReport() }
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AccountStatementScreen(
        onBackPressed: () -> Unit,
        onPrint: () -> Unit
    ) {
        val dimensions = LocalResponsiveDimensions.current
        val context = LocalContext.current
        val accountStatementViewModel: AccountStatementViewModel = viewModel()
        val transactionViewModel: TransactionViewModel = viewModel()
        
        val accounts by accountStatementViewModel.allAccounts.observeAsState(initial = emptyList())
        val transactions by transactionViewModel.getAllTransactions().observeAsState(initial = emptyList())
        var selectedAccountState by remember { mutableStateOf<Account?>(null) }
        var startDateState by remember { mutableStateOf("") }
        var endDateState by remember { mutableStateOf("") }
        var selectedCurrencyState by remember { mutableStateOf<String?>(null) }
        var availableCurrencies by remember { mutableStateOf(listOf<String>()) }
        var showAccountPicker by remember { mutableStateOf(false) }
        var showStartDatePicker by remember { mutableStateOf(false) }
        var showEndDatePicker by remember { mutableStateOf(false) }
        var reportHtml by remember { mutableStateOf("") }
        
        // حساب أرصدة العملات لكل حساب
        val balancesMap = remember(transactions) {
            accounts.associate { account ->
                val txs = transactions.filter { it.accountId == account.id }
                val currencyMap = txs.groupBy { it.currency ?: "" }.mapValues { (_, txs) ->
                    txs.fold(0.0) { acc, tx ->
                        when (tx.type) {
                            "debit" -> acc - tx.amount
                            "credit" -> acc + tx.amount
                            else -> acc
                        }
                    }
                }
                account.id to currencyMap
            }
        }

        // تحديث التقرير عند تغيير البيانات
        LaunchedEffect(selectedAccountState, startDateState, endDateState, selectedCurrencyState, transactions) {
            if (selectedAccountState != null) {
                selectedAccount = selectedAccountState
                selectedCurrency = selectedCurrencyState
                updateReport(context, selectedAccountState!!, startDateState, endDateState, selectedCurrencyState, transactions, reportHtml) { html ->
                    reportHtml = html
                }
            }
        }

        // 2. عند فتح الصفحة أو اختيار الحساب، عيّن التواريخ والعملات
        LaunchedEffect(selectedAccountState) {
            if (selectedAccountState != null) {
                val calendar = Calendar.getInstance()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
                val today = calendar.time
                calendar.add(Calendar.DAY_OF_MONTH, -3)
                val threeDaysAgo = calendar.time
                startDateState = dateFormat.format(threeDaysAgo)
                endDateState = dateFormat.format(today)
                // استخراج العملات من معاملات الحساب
                val accountTxs = transactions.filter { it.accountId == selectedAccountState!!.id }
                val currencies = accountTxs.mapNotNull { it.currency }.distinct()
                availableCurrencies = currencies
                selectedCurrencyState = currencies.firstOrNull()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // شريط العنوان
            TopAppBar(
                title = { 
                    Text(
                        text = "كشف الحساب التفصيلي",
                        fontSize = dimensions.titleFont,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "رجوع",
                            modifier = Modifier.size(dimensions.iconSize)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onPrint) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_print),
                            contentDescription = "طباعة",
                            modifier = Modifier.size(dimensions.iconSize)
                        )
                    }
                    // استبدل زر المشاركة ليستخدم shareReportAsPdf مباشرة
                    IconButton(onClick = {
                        shareReportAsPdf(
                            selectedAccountState,
                            startDateState,
                            endDateState,
                            selectedCurrencyState,
                            transactions
                        )
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "مشاركة",
                            modifier = Modifier.size(dimensions.iconSize)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(dimensions.spacingMedium)
            ) {
                // بطاقة التحكم
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(dimensions.cardCorner * 0.5f),
                    elevation = CardDefaults.cardElevation(3.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(dimensions.spacingMedium)
                    ) {
                        // اختيار الحساب
                        AccountPickerField(
                            label = "الحساب",
                            accounts = accounts,
                            transactions = transactions,
                            balancesMap = balancesMap,
                            selectedAccount = selectedAccountState,
                            onAccountSelected = { selectedAccountState = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(dimensions.spacingSmall))
                        
                        // التواريخ
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
                        ) {
                            // تاريخ البداية
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "من تاريخ",
                                    fontSize = dimensions.bodyFont * 0.85f,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(dimensions.spacingSmall))
                                OutlinedButton(
                                    onClick = { showStartDatePicker = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(dimensions.cardCorner * 0.5f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color(0xFFF5F5F5)
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        modifier = Modifier.size(dimensions.iconSize * 0.6f)
                                    )
                                    Spacer(modifier = Modifier.width(dimensions.spacingSmall))
                                    Text(
                                        text = startDateState,
                                        fontSize = dimensions.bodyFont * 0.8f
                                    )
                                }
                            }
                            
                            // تاريخ النهاية
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "إلى تاريخ",
                                    fontSize = dimensions.bodyFont * 0.85f,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(dimensions.spacingSmall))
                                OutlinedButton(
                                    onClick = { showEndDatePicker = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(dimensions.cardCorner * 0.5f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color(0xFFF5F5F5)
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        modifier = Modifier.size(dimensions.iconSize * 0.6f)
                                    )
                                    Spacer(modifier = Modifier.width(dimensions.spacingSmall))
                                    Text(
                                        text = endDateState,
                                        fontSize = dimensions.bodyFont * 0.8f
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(dimensions.spacingMedium))
                
                // عرض التقرير
                if (selectedAccountState != null && reportHtml.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(dimensions.cardCorner * 0.5f),
                        elevation = CardDefaults.cardElevation(3.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(dimensions.spacingMedium)
                        ) {
                            Text(
                                text = "كشف الحساب - ${selectedAccountState?.name}",
                                fontSize = dimensions.bodyFont * 0.9f,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = dimensions.spacingSmall)
                            )
                            // 3. أزرار العملة في رأس التقرير
                            if (selectedAccountState != null && availableCurrencies.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = dimensions.spacingSmall),
                                    horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
                                ) {
                                    availableCurrencies.forEach { currency ->
                                        val selected = currency == selectedCurrencyState
                                        Button(
                                            onClick = { selectedCurrencyState = currency },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (selected) MaterialTheme.colorScheme.primary else Color(0xFFF5F5F5),
                                                contentColor = if (selected) Color.White else MaterialTheme.colorScheme.primary
                                            ),
                                            shape = RoundedCornerShape(dimensions.cardCorner * 0.5f),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(currency)
                                        }
                                    }
                                }
                            }
                            AndroidView(
                                factory = { context ->
                                    WebView(context).apply {
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        settings.setSupportZoom(true)
                                        settings.builtInZoomControls = true
                                        settings.displayZoomControls = false
                                        isVerticalScrollBarEnabled = true
                                        isHorizontalScrollBarEnabled = true
                                        overScrollMode = WebView.OVER_SCROLL_IF_CONTENT_SCROLLS
                                        webView = this
                                    }
                                },
                                update = { webView ->
                                    webView.loadDataWithBaseURL(
                                        null,
                                        reportHtml,
                                        "text/html",
                                        "UTF-8",
                                        null
                                    )
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                } else if (selectedAccountState == null) {
                    // رسالة عند عدم اختيار حساب
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(dimensions.cardCorner * 0.5f),
                        elevation = CardDefaults.cardElevation(2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(dimensions.spacingMedium),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(dimensions.iconSize * 1.5f)
                            )
                            Spacer(modifier = Modifier.height(dimensions.spacingSmall))
                            Text(
                                text = "اختر الحساب لعرض كشف الحساب",
                                fontSize = dimensions.bodyFont * 0.9f,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    // رسالة عند تحميل التقرير
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(dimensions.cardCorner * 0.5f),
                        elevation = CardDefaults.cardElevation(2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(dimensions.spacingMedium),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(dimensions.iconSize),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(dimensions.spacingSmall))
                            Text(
                                text = "جاري تحضير التقرير...",
                                fontSize = dimensions.bodyFont * 0.9f,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
        
        // عرض اختيار تاريخ البداية
        if (showStartDatePicker) {
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    startDateState = String.format(Locale.ENGLISH, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                    showStartDatePicker = false
                },
                Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.MONTH),
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        
        // عرض اختيار تاريخ النهاية
        if (showEndDatePicker) {
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    endDateState = String.format(Locale.ENGLISH, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                    showEndDatePicker = false
                },
                Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.MONTH),
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun updateReport(
        context: Context,
        account: Account,
        startDateStr: String,
        endDateStr: String,
        selectedCurrency: String?,
        allTransactions: List<Transaction>,
        currentHtml: String,
        onHtmlGenerated: (String) -> Unit
    ) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            val startDate = dateFormat.parse(startDateStr)
            val endDateRaw = dateFormat.parse(endDateStr)
            // ضبط نهاية اليوم لـ endDate
            val endCal = Calendar.getInstance().apply {
                time = endDateRaw
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            val endDate = endCal.time
            
            if (startDate == null || endDate == null) return
            
            // 4. عند توليد التقرير، صفِّ المعاملات حسب العملة المختارة فقط
            val filteredTransactions = allTransactions.filter { tx ->
                tx.accountId == account.id &&
                (selectedCurrency == null || tx.currency == selectedCurrency) &&
                isTransactionInDateRange(tx, startDate, endDate)
            }.sortedBy { it.transactionDate }
            
            // 5. الرصيد التراكمي يبدأ من رصيد الحساب قبل الفترة
            val startDateObj = dateFormat.parse(startDateStr)
            val previousBalance = allTransactions.filter { tx ->
                tx.accountId == account.id &&
                (selectedCurrency == null || tx.currency == selectedCurrency) &&
                Date(tx.transactionDate).before(startDateObj)
            }.fold(0.0) { acc, tx ->
                when (tx.type) {
                    "debit" -> acc - tx.amount
                    "credit" -> acc + tx.amount
                    else -> acc
                }
            }

            // إنشاء HTML للتقرير
            val html = generateReportHtml(account, startDate, endDate, filteredTransactions, previousBalance)
            onHtmlGenerated(html)
            
        } catch (e: Exception) {
            Toast.makeText(context, "خطأ في تحديث التقرير", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isTransactionInDateRange(transaction: Transaction, startDate: Date, endDate: Date): Boolean {
        val transactionDate = Date(transaction.transactionDate)
        return transactionDate >= startDate && transactionDate <= endDate
    }

    // دالة مساعدة لمقارنة الأيام فقط (بدون وقت)
    private fun isSameDayOrInRange(txDate: Date, start: Date, end: Date): Boolean {
        val calTx = Calendar.getInstance().apply { time = txDate }
        val calStart = Calendar.getInstance().apply { time = start }
        val calEnd = Calendar.getInstance().apply { time = end }
        calTx.set(Calendar.HOUR_OF_DAY, 0)
        calTx.set(Calendar.MINUTE, 0)
        calTx.set(Calendar.SECOND, 0)
        calTx.set(Calendar.MILLISECOND, 0)
        calStart.set(Calendar.HOUR_OF_DAY, 0)
        calStart.set(Calendar.MINUTE, 0)
        calStart.set(Calendar.SECOND, 0)
        calStart.set(Calendar.MILLISECOND, 0)
        calEnd.set(Calendar.HOUR_OF_DAY, 0)
        calEnd.set(Calendar.MINUTE, 0)
        calEnd.set(Calendar.SECOND, 0)
        calEnd.set(Calendar.MILLISECOND, 0)
        return !calTx.before(calStart) && !calTx.after(calEnd)
    }

    private fun generateReportHtml(
        account: Account,
        startDate: Date,
        endDate: Date,
        transactions: List<Transaction>,
        previousBalance: Double
    ): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val displayDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
        
        var totalDebit = 0.0
        var totalCredit = 0.0
        var balance = previousBalance
        
        val transactionRows = transactions.joinToString("") { transaction ->
            when (transaction.type) {
                "debit" -> {
                    totalDebit += transaction.amount
                    balance -= transaction.amount
                    """
                    <tr>
                        <td>${displayDateFormat.format(Date(transaction.transactionDate))}</td>
                        <td>${transaction.description}</td>
                        <td class="debit">${String.format(Locale.ENGLISH, "%.2f", transaction.amount)}</td>
                        <td></td>
                        <td>${String.format(Locale.ENGLISH, "%.2f", balance)}</td>
                    </tr>
                    """
                }
                "credit" -> {
                    totalCredit += transaction.amount
                    balance += transaction.amount
                    """
                    <tr>
                        <td>${displayDateFormat.format(Date(transaction.transactionDate))}</td>
                        <td>${transaction.description}</td>
                        <td></td>
                        <td class="credit">${String.format(Locale.ENGLISH, "%.2f", transaction.amount)}</td>
                        <td>${String.format(Locale.ENGLISH, "%.2f", balance)}</td>
                    </tr>
                    """
                }
                else -> ""
            }
        }
        
        return """
        <!DOCTYPE html>
        <html dir="rtl">
        <head>
            <meta charset="UTF-8">
            <style>
                @font-face {
                    font-family: 'Cairo';
                    src: url('file:///android_asset/fonts/Cairo/static/Cairo-Regular.ttf');
                    font-weight: normal;
                }
                @font-face {
                    font-family: 'Cairo';
                    src: url('file:///android_asset/fonts/Cairo/static/Cairo-Bold.ttf');
                    font-weight: bold;
                }
                body { font-family: 'Cairo', Arial, sans-serif; margin: 0; padding: 0; background-color: #f5f5f5; }
                .header, .account-info-row, .transactions-table, .summary {
                    width: 100%;
                    max-width: 100vw;
                    box-sizing: border-box;
                    margin: 0 auto 10px auto;
                }
                .header { background-color: #1976d2; color: white; padding: 16px; border-radius: 10px; margin-bottom: 10px; font-weight: bold; }
                .account-info-row { background-color: white; padding: 8px 10px; border-radius: 8px; margin-bottom: 10px; font-size: 0.88em; display: flex; flex-direction: row; align-items: center; justify-content: flex-start; gap: 10px; color: #333; }
                .account-info-row span { font-weight: bold; color: #1976d2; font-size: 0.88em; }
                .account-info-row .divider { color: #aaa; font-weight: normal; margin: 0 4px; }
                .transactions-table { width: 100%; border-collapse: collapse; background-color: white; border-radius: 10px; overflow: hidden; table-layout: fixed; }
                .transactions-table th { background-color: #1976d2; color: white; padding: 10px; text-align: center; font-size: 0.88em; font-weight: bold; }
                .transactions-table td { padding: 8px; text-align: center; border-bottom: 1px solid #eee; word-break: break-word; font-size: 0.88em; }
                .debit { color: #d32f2f; font-weight: bold; }
                .credit { color: #388e3c; font-weight: bold; }
                .summary { background-color: white; padding: 10px; border-radius: 10px; margin-top: 10px; width: 100%; }
                .summary-row { display: flex; justify-content: space-between; margin: 6px 0; }
                .summary-label { font-weight: bold; color: #666; }
                .summary-value { font-weight: bold; color: #1976d2; }
            </style>
        </head>
        <body>
            <div class="header">
                <h1 style="font-size:1.1em; margin:0 0 8px 0;">كشف الحساب التفصيلي</h1>
            </div>
            <div class="account-info-row">
                <span>اسم الحساب: ${account.name}</span>
                <span class="divider">|</span>
                <span>رقم الهاتف: ${account.phoneNumber}</span>
                <span class="divider">|</span>
                <span>الفترة: من ${displayDateFormat.format(startDate)} إلى ${displayDateFormat.format(endDate)}</span>
            </div>
            <table class="transactions-table">
                <thead>
                    <tr>
                        <th>التاريخ</th>
                        <th>الوصف</th>
                        <th>عليه</th>
                        <th>له</th>
                        <th>الرصيد</th>
                    </tr>
                </thead>
                <tbody>
                    $transactionRows
                </tbody>
            </table>
            <div class="summary">
                <h3 style="font-size:0.95em; margin-bottom:8px;">ملخص الحساب</h3>
                <div class="summary-row">
                    <span class="summary-label">إجمالي عليه:</span>
                    <span class="summary-value debit">${String.format(Locale.ENGLISH, "%.2f", totalDebit)}</span>
                </div>
                <div class="summary-row">
                    <span class="summary-label">إجمالي له:</span>
                    <span class="summary-value credit">${String.format(Locale.ENGLISH, "%.2f", totalCredit)}</span>
                </div>
                <div class="summary-row">
                    <span class="summary-label">الرصيد النهائي:</span>
                    <span class="summary-value">${String.format(Locale.ENGLISH, "%.2f", balance)}</span>
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    private fun printReport() {
        if (webView != null) {
            val printManager = getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
            val printAdapter = webView.createPrintDocumentAdapter("كشف الحساب")
            printManager.print("كشف الحساب", printAdapter, null)
        }
    }

    private fun shareReportAsPdf(
        selectedAccount: Account?,
        startDate: String,
        endDate: String,
        selectedCurrency: String?,
        transactions: List<Transaction>
    ) {
        if (selectedAccount != null) {
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
                val startDateObj = dateFormat.parse(startDate)
                val endDateRaw = dateFormat.parse(endDate)
                // ضبط نهاية اليوم لـ endDate
                val endCal = Calendar.getInstance().apply {
                    time = endDateRaw
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                val endDate = endCal.time
                val filteredTransactions = transactions.filter { tx ->
                    tx.accountId == selectedAccount.id &&
                    (selectedCurrency == null || tx.currency == selectedCurrency) &&
                    Date(tx.transactionDate) >= startDateObj && Date(tx.transactionDate) <= endDate
                }.sortedBy { it.transactionDate }
                val pdfFile = generateAccountStatementPdfWithITextG(
                    context = this,
                    account = selectedAccount,
                    startDate = startDate,
                    endDate = endDate,
                    selectedCurrency = selectedCurrency,
                    transactions = filteredTransactions,
                    allTransactions = transactions // Pass all transactions for previous balance calculation
                )
                sharePdfFile(pdfFile)
            } catch (e: Exception) {
                val errorMsg = "خطأ في مشاركة التقرير: ${e.message}\n${e.stackTraceToString()}"
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Error", errorMsg)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "يرجى اختيار حساب أولاً", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateAccountStatementPdfWithITextG(
        context: Context,
        account: Account,
        startDate: String,
        endDate: Date,
        selectedCurrency: String?,
        transactions: List<Transaction>, // معاملات الفترة فقط
        allTransactions: List<Transaction> // كل معاملات الحساب
    ): File {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val displayDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
        val startDateObj = dateFormat.parse(startDate)
        val endDateObj = endDate
        val safeAccountName = account.name.replace(Regex("[^\u0600-\u06FFa-zA-Z0-9_]"), "_")
        val fileName = "كشف_حساب_${safeAccountName}_${startDate}_${endDate}_itextg.pdf"
        val pdfFile = File(context.cacheDir, fileName)

        val document = Document()
        val writer = PdfWriter.getInstance(document, FileOutputStream(pdfFile))
        document.open()

        // تحميل خط Cairo-Regular للنص العادي
        val cairoRegularFontFile = File(context.cacheDir, "Cairo-Regular.ttf")
        if (!cairoRegularFontFile.exists()) {
            context.assets.open("fonts/Cairo/static/Cairo-Regular.ttf").use { input ->
                cairoRegularFontFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        // تحميل خط Cairo-Bold للعناوين
        val cairoBoldFontFile = File(context.cacheDir, "Cairo-Bold.ttf")
        if (!cairoBoldFontFile.exists()) {
            context.assets.open("fonts/Cairo/static/Cairo-Bold.ttf").use { input ->
                cairoBoldFontFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        val cairoBaseFont = com.itextpdf.text.pdf.BaseFont.createFont(cairoRegularFontFile.absolutePath, com.itextpdf.text.pdf.BaseFont.IDENTITY_H, com.itextpdf.text.pdf.BaseFont.EMBEDDED)
        val cairoBoldBaseFont = com.itextpdf.text.pdf.BaseFont.createFont(cairoBoldFontFile.absolutePath, com.itextpdf.text.pdf.BaseFont.IDENTITY_H, com.itextpdf.text.pdf.BaseFont.EMBEDDED)
        val fontCairo = com.itextpdf.text.Font(cairoBaseFont, 13f, Font.NORMAL)
        val fontCairoBold = com.itextpdf.text.Font(cairoBoldBaseFont, 15f, Font.BOLD)
        val fontHeader = com.itextpdf.text.Font(cairoBoldBaseFont, 13f, Font.BOLD, BaseColor.WHITE)
        val fontDebit = com.itextpdf.text.Font(cairoBaseFont, 13f, Font.NORMAL, BaseColor(0xD3, 0x2F, 0x2F))
        val fontCredit = com.itextpdf.text.Font(cairoBaseFont, 13f, Font.NORMAL, BaseColor(0x38, 0x8E, 0x3C))

        // عنوان التقرير
        val title = Paragraph(ArabicUtilities.reshape("كشف الحساب التفصيلي"), fontCairoBold)
        title.alignment = Element.ALIGN_CENTER
        document.add(title)
        document.add(Paragraph(" "))

        // معلومات الحساب
        val info = "اسم الحساب: " + ArabicUtilities.reshape(account.name) + "   |   رقم الهاتف: " + account.phoneNumber + "   |   الفترة: من " + startDate + " إلى " + displayDateFormat.format(endDate) +
            (if (selectedCurrency != null) "   |   العملة: " + ArabicUtilities.reshape(selectedCurrency) else "")
        val infoPara = Paragraph(info, fontCairo)
        infoPara.alignment = Element.ALIGN_RIGHT
        document.add(infoPara)
        document.add(Paragraph(" "))

        // إنشاء الجدول مع تحديد عدد الأعمدة ونسب العرض
        val table = PdfPTable(5)
        table.widthPercentage = 100f
        table.setWidths(floatArrayOf(2f, 5f, 2f, 2f, 2f))

        // رؤوس الجدول مع ألوان (PDF)
        val headers = listOf("الرصيد", "تفاصيل", "عليه", "له", "التاريخ")
        for (h in headers) {
            val cell = PdfPCell(Paragraph(ArabicUtilities.reshape(h), fontHeader))
            cell.horizontalAlignment = Element.ALIGN_CENTER
            cell.backgroundColor = BaseColor(0x19, 0x76, 0xD2) // أزرق
            cell.runDirection = PdfWriter.RUN_DIRECTION_RTL // أضف هذا السطر
            table.addCell(cell)
        }

        // حساب الرصيد السابق من كل العمليات
        val previousBalance = allTransactions
            .filter { 
                it.accountId == account.id &&
                (selectedCurrency == null || it.currency == selectedCurrency) &&
                Date(it.transactionDate).before(startDateObj)
            }
            .fold(0.0) { acc, tx ->
                when (tx.type) {
                    "debit" -> acc - tx.amount
                    "credit" -> acc + tx.amount
                    else -> acc
                }
            }
        var balance = previousBalance
        var totalDebit = 0.0
        var totalCredit = 0.0

        // صف الرصيد السابق (قبل التكرار على المعاملات)
        val prevRow = listOf(
            PdfPCell(Paragraph(String.format(Locale.ENGLISH, "%.2f", previousBalance), fontCairo)), // الرصيد
            PdfPCell(Paragraph(ArabicUtilities.reshape("الرصيد السابق"), fontCairo)), // تفاصيل
            PdfPCell(Paragraph("", fontCairo)), // عليه
            PdfPCell(Paragraph("", fontCairo)), // له
            PdfPCell(Paragraph("", fontCairo)) // التاريخ
        )
        for (cell in prevRow) {
            cell.horizontalAlignment = Element.ALIGN_CENTER
            cell.backgroundColor = BaseColor(0xF5, 0xF5, 0xF5)
            cell.runDirection = PdfWriter.RUN_DIRECTION_RTL // أضف هذا السطر
            table.addCell(cell)
        }

        // المعاملات
        val filteredTxs = transactions.filter { tx ->
            val txDay = Date(tx.transactionDate)
            val startDay = startDateObj
            val endDay = endDateObj
            isSameDayOrInRange(txDay, startDay, endDay) && (selectedCurrency == null || tx.currency == selectedCurrency)
        }.sortedBy { it.transactionDate }

        for (tx in filteredTxs) {
            val dateStr = displayDateFormat.format(Date(tx.transactionDate))
            val desc = tx.description ?: ""
            val debit = if (tx.type == "debit") String.format(Locale.ENGLISH, "%.2f", tx.amount) else ""
            val credit = if (tx.type == "credit") String.format(Locale.ENGLISH, "%.2f", tx.amount) else ""
            if (tx.type == "debit") {
                balance -= tx.amount
                totalDebit += tx.amount
            } else if (tx.type == "credit") {
                balance += tx.amount
                totalCredit += tx.amount
            }
            val row = listOf(
                PdfPCell(Paragraph(String.format(Locale.ENGLISH, "%.2f", balance), fontCairo)), // الرصيد
                PdfPCell(Paragraph(ArabicUtilities.reshape(desc), fontCairo)), // تفاصيل (الوصف)
                PdfPCell(Paragraph(debit, fontDebit)), // عليه
                PdfPCell(Paragraph(credit, fontCredit)), // له
                PdfPCell(Paragraph(dateStr, fontCairo)) // التاريخ
            )
            for (cell in row) {
                cell.horizontalAlignment = Element.ALIGN_CENTER
                cell.runDirection = PdfWriter.RUN_DIRECTION_RTL // أضف هذا السطر
                table.addCell(cell)
            }
        }

        document.add(table)
        document.add(Paragraph(" "))

        // ملخص الحساب
        val summary = listOf(
            ArabicUtilities.reshape("إجمالي عليه: ") + String.format(Locale.ENGLISH, "%.2f", totalDebit),
            ArabicUtilities.reshape("إجمالي له: ") + String.format(Locale.ENGLISH, "%.2f", totalCredit),
            ArabicUtilities.reshape("الرصيد النهائي: ") + String.format(Locale.ENGLISH, "%.2f", balance)
        )
        for (s in summary) {
            val para = Paragraph(s, fontHeader)
            para.alignment = Element.ALIGN_RIGHT
            document.add(para)
        }

        document.close()
        writer.close()
        return pdfFile
    }

    private fun sharePdfFile(pdfFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                pdfFile
            )
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "كشف الحساب - ${selectedAccount?.name}")
                putExtra(Intent.EXTRA_TEXT, "كشف الحساب التفصيلي")
                type = "application/pdf"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "مشاركة كشف الحساب"))
        } catch (e: Exception) {
            val errorMsg = "خطأ في مشاركة الملف: ${e.message}\n${e.stackTraceToString()}"
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Error", errorMsg)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
        }
    }

    // تعريف دالة مشاركة ملف PDF إذا لم تكن موجودة
    fun sharePdfFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "مشاركة كشف الحساب"))
    }
} 
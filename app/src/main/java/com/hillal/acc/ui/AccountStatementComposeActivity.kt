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
import com.itextpdf.text.pdf.PdfWriter
import java.io.StringReader
import com.itextpdf.text.pdf.BaseFont
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font
import android.net.Uri

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
                    IconButton(onClick = {
                        exportWebViewToPdfAndShare(webView, context, selectedAccountState?.name ?: "", startDateState, endDateState)
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
            val endDate = dateFormat.parse(endDateStr)
            
            if (startDate == null || endDate == null) return
            
            // 4. عند توليد التقرير، صفِّ المعاملات حسب العملة المختارة فقط
            val filteredTransactions = allTransactions.filter { tx ->
                tx.accountId == account.id &&
                (selectedCurrency == null || tx.currency == selectedCurrency) &&
                isTransactionInDateRange(tx, startDate, endDate)
            }.sortedBy { it.createdAt }
            
            // 5. الرصيد التراكمي يبدأ من رصيد الحساب قبل الفترة
            val startDateObj = dateFormat.parse(startDateStr)
            val previousBalance = allTransactions.filter { tx ->
                tx.accountId == account.id &&
                (selectedCurrency == null || tx.currency == selectedCurrency) &&
                Date(tx.createdAt).before(startDateObj)
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
        val transactionDate = Date(transaction.createdAt)
        return transactionDate >= startDate && transactionDate <= endDate
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
                        <td>${displayDateFormat.format(Date(transaction.createdAt))}</td>
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
                        <td>${displayDateFormat.format(Date(transaction.createdAt))}</td>
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
                    font-family: 'Amiri';
                    src: url('file:///android_asset/fonts/Amiri-1.002/Amiri-Regular.ttf');
                }
                body { font-family: 'Amiri', Arial, sans-serif; margin: 0; padding: 0; background-color: #f5f5f5; }
                .header, .account-info-row, .transactions-table, .summary {
                    width: 100%;
                    max-width: 100vw;
                    box-sizing: border-box;
                    margin: 0 auto 10px auto;
                }
                .header { background-color: #1976d2; color: white; padding: 16px; border-radius: 10px; margin-bottom: 10px; }
                .account-info-row { background-color: white; padding: 8px 10px; border-radius: 8px; margin-bottom: 10px; font-size: 0.88em; display: flex; flex-direction: row; align-items: center; justify-content: flex-start; gap: 10px; color: #333; }
                .account-info-row span { font-weight: bold; color: #1976d2; font-size: 0.88em; }
                .account-info-row .divider { color: #aaa; font-weight: normal; margin: 0 4px; }
                .transactions-table { width: 100%; border-collapse: collapse; background-color: white; border-radius: 10px; overflow: hidden; table-layout: fixed; }
                .transactions-table th { background-color: #1976d2; color: white; padding: 10px; text-align: center; font-size: 0.88em; }
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

    // أضف الدالة الجديدة لمشاركة PDF من WebView مباشرة
    class MyLayoutResultCallback(val onFinish: () -> Unit) : PrintDocumentAdapter.LayoutResultCallback() {
        override fun onLayoutFinished(info: PrintDocumentInfo?, changed: Boolean) {
            onFinish()
        }
    }

    class MyWriteResultCallback(val onFinish: () -> Unit) : PrintDocumentAdapter.WriteResultCallback() {
        override fun onWriteFinished(pages: Array<PageRange>) {
            onFinish()
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
                val endDateObj = dateFormat.parse(endDate)
                val filteredTransactions = transactions.filter { tx ->
                    tx.accountId == selectedAccount.id &&
                    (selectedCurrency == null || tx.currency == selectedCurrency) &&
                    Date(tx.createdAt) >= startDateObj && Date(tx.createdAt) <= endDateObj
                }.sortedBy { it.createdAt }
                val pdfFile = generateAccountStatementPdf(
                    selectedAccount,
                    startDate,
                    endDate,
                    selectedCurrency,
                    filteredTransactions
                )
                sharePdfFile(pdfFile)
            } catch (e: Exception) {
                Toast.makeText(this, "خطأ في مشاركة التقرير: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "يرجى اختيار حساب أولاً", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateAccountStatementPdf(
        account: Account,
        startDate: String,
        endDate: String,
        selectedCurrency: String?,
        transactions: List<Transaction>
    ): File {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val startDateObj = dateFormat.parse(startDate)
        // تجهيز اسم الملف: كشف_حساب_اسم_الحساب_من_إلى.pdf
        val safeAccountName = account.name.replace(Regex("[^\u0600-\u06FFa-zA-Z0-9_]"), "_")
        val fileName = "كشف_حساب_${safeAccountName}_${startDate}_${endDate}.pdf"
        val pdfFile = File(cacheDir, fileName)
        val document = Document()
        val outputStream = FileOutputStream(pdfFile)
        val writer = PdfWriter.getInstance(document, outputStream)
        document.open()

        // تحميل خط Amiri من assets
        val fontStream = assets.open("fonts/Amiri-1.002/Amiri-Regular.ttf")
        val fontBytes = ByteArray(fontStream.available())
        fontStream.read(fontBytes)
        fontStream.close()
        val baseFont = BaseFont.createFont("Amiri-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, false, fontBytes, null)
        val arabicFont = com.itextpdf.text.Font(baseFont, 14f, com.itextpdf.text.Font.NORMAL, com.itextpdf.text.BaseColor(33, 33, 33))
        val titleFont = com.itextpdf.text.Font(baseFont, 18f, com.itextpdf.text.Font.BOLD, com.itextpdf.text.BaseColor(25, 118, 210))
        val headerFont = com.itextpdf.text.Font(baseFont, 14f, com.itextpdf.text.Font.BOLD, com.itextpdf.text.BaseColor.WHITE)
        val debitFont = com.itextpdf.text.Font(baseFont, 12f, com.itextpdf.text.Font.BOLD, com.itextpdf.text.BaseColor(211, 47, 47))
        val creditFont = com.itextpdf.text.Font(baseFont, 12f, com.itextpdf.text.Font.BOLD, com.itextpdf.text.BaseColor(56, 142, 60))
        val summaryLabelFont = com.itextpdf.text.Font(baseFont, 12f, com.itextpdf.text.Font.BOLD, com.itextpdf.text.BaseColor(102, 102, 102))
        val summaryValueFont = com.itextpdf.text.Font(baseFont, 12f, com.itextpdf.text.Font.BOLD, com.itextpdf.text.BaseColor(25, 118, 210))

        // العنوان الرئيسي
        val title = com.itextpdf.text.Paragraph("كشف الحساب التفصيلي", titleFont)
        title.alignment = com.itextpdf.text.Element.ALIGN_RIGHT
        document.add(title)
        document.add(com.itextpdf.text.Paragraph(" "))

        // معلومات الحساب في سطر واحد
        val info = com.itextpdf.text.Paragraph(
            "اسم الحساب: ${account.name}   |   رقم الهاتف: ${account.phoneNumber}   |   الفترة: من $startDate إلى $endDate" +
            (if (selectedCurrency != null) "   |   العملة: $selectedCurrency" else ""),
            arabicFont
        )
        info.alignment = com.itextpdf.text.Element.ALIGN_RIGHT
        document.add(info)
        document.add(com.itextpdf.text.Paragraph(" "))

        // جدول المعاملات
        val table = com.itextpdf.text.pdf.PdfPTable(5)
        table.widthPercentage = 100f
        table.setWidths(floatArrayOf(2f, 4f, 2f, 2f, 2f))
        val headerBg = com.itextpdf.text.BaseColor(25, 118, 210)
        val headers = listOf("التاريخ", "الوصف", "مدين", "دائن", "الرصيد")
        for (h in headers) {
            val cell = com.itextpdf.text.pdf.PdfPCell(com.itextpdf.text.Phrase(h, headerFont))
            cell.backgroundColor = headerBg
            cell.horizontalAlignment = com.itextpdf.text.Element.ALIGN_CENTER
            cell.verticalAlignment = com.itextpdf.text.Element.ALIGN_MIDDLE
            cell.setPadding(6f)
            table.addCell(cell)
        }
        val displayDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
        var totalDebit = 0.0
        var totalCredit = 0.0
        var balance = 0.0
        // حساب الرصيد السابق قبل الفترة
        val previousBalance = transactions
            .filter { Date(it.createdAt) < startDateObj }
            .fold(0.0) { acc, tx ->
                when (tx.type) {
                    "debit" -> acc - tx.amount
                    "credit" -> acc + tx.amount
                    else -> acc
                }
            }
        // أضف صف الرصيد السابق في أول الجدول
        val prevBalanceCell = com.itextpdf.text.pdf.PdfPCell(com.itextpdf.text.Phrase("", arabicFont))
        val prevDescCell = com.itextpdf.text.pdf.PdfPCell(com.itextpdf.text.Phrase("الرصيد السابق", arabicFont))
        val prevDebitCell = com.itextpdf.text.pdf.PdfPCell(com.itextpdf.text.Phrase("", arabicFont))
        val prevCreditCell = com.itextpdf.text.pdf.PdfPCell(com.itextpdf.text.Phrase("", arabicFont))
        val prevBalanceValueCell = com.itextpdf.text.pdf.PdfPCell(com.itextpdf.text.Phrase(String.format(Locale.ENGLISH, "%.2f", previousBalance), arabicFont))
        table.addCell(prevBalanceCell)
        table.addCell(prevDescCell)
        table.addCell(prevDebitCell)
        table.addCell(prevCreditCell)
        table.addCell(prevBalanceValueCell)
        transactions.forEach { tx ->
            val dateCell = com.itextpdf.text.pdf.PdfPCell(com.itextpdf.text.Phrase(displayDateFormat.format(Date(tx.createdAt)), arabicFont))
            table.addCell(dateCell)
            val descCell = com.itextpdf.text.pdf.PdfPCell(com.itextpdf.text.Phrase(tx.description ?: "", arabicFont))
            table.addCell(descCell)
            if (tx.type == "debit") {
                val debitCell = com.itextpdf.text.pdf.PdfPCell(com.itextpdf.text.Phrase(String.format(Locale.ENGLISH, "%.2f", tx.amount), debitFont))
                table.addCell(debitCell)
                val emptyCell = com.itextpdf.text.pdf.PdfPCell(com.itextpdf.text.Phrase("", arabicFont))
                table.addCell(emptyCell)
                balance -= tx.amount
                totalDebit += tx.amount
            } else {
                val emptyCell = com.itextpdf.text.pdf.PdfPCell(com.itextpdf.text.Phrase("", arabicFont))
                table.addCell(emptyCell)
                val creditCell = com.itextpdf.text.pdf.PdfPCell(com.itextpdf.text.Phrase(String.format(Locale.ENGLISH, "%.2f", tx.amount), creditFont))
                table.addCell(creditCell)
                balance += tx.amount
                totalCredit += tx.amount
            }
            val balanceCell = com.itextpdf.text.pdf.PdfPCell(com.itextpdf.text.Phrase(String.format(Locale.ENGLISH, "%.2f", balance), arabicFont))
            table.addCell(balanceCell)
        }
        document.add(table)
        document.add(com.itextpdf.text.Paragraph(" "))

        // ملخص الحساب
        val summaryTitle = com.itextpdf.text.Paragraph("ملخص الحساب", summaryLabelFont)
        summaryTitle.alignment = com.itextpdf.text.Element.ALIGN_RIGHT
        document.add(summaryTitle)
        val summaryTable = com.itextpdf.text.pdf.PdfPTable(2)
        summaryTable.widthPercentage = 60f
        summaryTable.horizontalAlignment = com.itextpdf.text.Element.ALIGN_RIGHT
        summaryTable.setWidths(floatArrayOf(2f, 2f))
        val debitLabelCell = com.itextpdf.text.pdf.PdfPCell(com.itextpdf.text.Phrase("إجمالي عليه:", summaryLabelFont))
        summaryTable.addCell(debitLabelCell)
        val debitValueCell = com.itextpdf.text.pdf.PdfPCell(com.itextpdf.text.Phrase(String.format(Locale.ENGLISH, "%.2f", totalDebit), debitFont))
        summaryTable.addCell(debitValueCell)
        val creditLabelCell = com.itextpdf.text.pdf.PdfPCell(com.itextpdf.text.Phrase("إجمالي له:", summaryLabelFont))
        summaryTable.addCell(creditLabelCell)
        val creditValueCell = com.itextpdf.text.pdf.PdfPCell(com.itextpdf.text.Phrase(String.format(Locale.ENGLISH, "%.2f", totalCredit), creditFont))
        summaryTable.addCell(creditValueCell)
        val balanceLabelCell = com.itextpdf.text.pdf.PdfPCell(com.itextpdf.text.Phrase("الرصيد النهائي:", summaryLabelFont))
        summaryTable.addCell(balanceLabelCell)
        val balanceValueCell = com.itextpdf.text.pdf.PdfPCell(com.itextpdf.text.Phrase(String.format(Locale.ENGLISH, "%.2f", balance), summaryValueFont))
        summaryTable.addCell(balanceValueCell)
        document.add(summaryTable)

        document.close()
        outputStream.close()
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
            Toast.makeText(this, "خطأ في مشاركة الملف: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportWebViewToPdfAndShare(webView: WebView, context: Context, accountName: String, startDate: String, endDate: String) {
        val safeAccountName = accountName.replace(Regex("[^\u0600-\u06FFa-zA-Z0-9_]"), "_")
        val fileName = "كشف_حساب_${safeAccountName}_${startDate}_${endDate}.pdf"
        val file = File(context.cacheDir, fileName)

        val printAdapter = webView.createPrintDocumentAdapter(fileName)
        val printAttributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setResolution(PrintAttributes.Resolution("pdf", "pdf", 600, 600))
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        val pdfFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE)

        printAdapter.onLayout(
            null, printAttributes, null,
            MyLayoutResultCallback {
                printAdapter.onWrite(
                    arrayOf(PageRange.ALL_PAGES),
                    pdfFileDescriptor,
                    null,
                    MyWriteResultCallback {
                        pdfFileDescriptor.close()
                        sharePdfFile(context, file)
                    }
                )
            }, null
        )
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

    fun createAndShareArabicPdf(context: Context, text: String, fontName: String = "Amiri-Regular.ttf") {
        // 1. إنشاء مستند PDF
        val document = PDDocument()
        val page = PDPage(PDRectangle.A4)
        document.addPage(page)

        // 2. تحميل الخط العربي من assets/fonts
        val fontStream = context.assets.open("fonts/Amiri-1.002/$fontName")
        val font = PDType0Font.load(document, fontStream)

        // 3. كتابة النص العربي
        val contentStream = PDPageContentStream(document, page)
        contentStream.beginText()
        contentStream.setFont(font, 18f)
        // حدد مكان النص (x, y)
        contentStream.newLineAtOffset(50f, 750f)
        contentStream.showText(text)
        contentStream.endText()
        contentStream.close()

        // 4. حفظ الملف في cache
        val file = File(context.cacheDir, "arabic_report.pdf")
        document.save(file)
        document.close()

        // 5. مشاركة الملف
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "مشاركة PDF"))
    }
} 
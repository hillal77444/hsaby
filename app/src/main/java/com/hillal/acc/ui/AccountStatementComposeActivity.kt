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
import com.itextpdf.text.Rectangle
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.core.content.ContextCompat
import android.content.res.Resources
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.hillal.acc.data.preferences.UserPreferences

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
                                modifier = Modifier
                                    .fillMaxSize()
                                    .navigationBarsPadding()
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
            val html = generateReportHtml(account, startDate, endDate, filteredTransactions, previousBalance, context)
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

    // دوال مساعدة لجلب الترويسة والشعار
    private fun getReportHeaderData(context: Context): Triple<String, String, Bitmap?> {
        val prefs = context.getSharedPreferences("report_header_prefs", Context.MODE_PRIVATE)
        val rightHeader = prefs.getString("right_header", null) ?: "تطبيق مالي برو"
        val leftHeader = prefs.getString("left_header", null) ?: "رقم التواصل: 0500000000"
        val logoPath = prefs.getString("logo_path", null)
        var logoBitmap: Bitmap? = null
        if (!logoPath.isNullOrEmpty()) {
            try {
                val file = java.io.File(context.filesDir, logoPath)
                if (file.exists()) {
                    logoBitmap = BitmapFactory.decodeFile(file.absolutePath)
                }
            } catch (_: Exception) {}
        }
        if (logoBitmap == null) {
            // استخدم شعار التطبيق الافتراضي
            val drawable: Drawable? = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
            if (drawable is BitmapDrawable) {
                logoBitmap = drawable.bitmap
            }
        }
        return Triple(rightHeader, leftHeader, logoBitmap)
    }

    // دالة لتحويل Bitmap إلى Base64 (لاستخدامها في HTML img src)
    private fun bitmapToBase64(bitmap: Bitmap?): String? {
        if (bitmap == null) return null
        val outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun generateReportHtml(
        account: Account,
        startDate: Date,
        endDate: Date,
        transactions: List<Transaction>,
        previousBalance: Double,
        context: Context? = null
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
        // صف الإجمالي وصف الرصيد النهائي
        val summaryRows = """
            <tr class="summary-row">
                <td></td>
                <td style="font-weight:bold;">الإجمالي</td>
                <td class="debit">${String.format(Locale.ENGLISH, "%.2f", totalDebit)}</td>
                <td class="credit">${String.format(Locale.ENGLISH, "%.2f", totalCredit)}</td>
                <td></td>
            </tr>
            <tr class="summary-row">
                <td></td>
                <td style="font-weight:bold;">الرصيد</td>
                <td></td>
                <td></td>
                <td style="font-weight:bold;">${String.format(Locale.ENGLISH, "%.2f", balance)}</td>
            </tr>
        """
        // جلب الترويسة والشعار واسم المستخدم
        var headerHtml = ""
        var clientCardHtml = ""
        if (context != null) {
            val (rightHeader, leftHeader, logoBitmap) = getReportHeaderData(context)
            // دعم السطر الجديد في الترويسة
            val rightHeaderHtml = rightHeader.replace("\n", "<br>")
            val leftHeaderHtml = leftHeader.replace("\n", "<br>")
            val logoBase64 = bitmapToBase64(logoBitmap)
            val logoImgTag = if (logoBase64 != null) {
                "<img src=\"data:image/png;base64,$logoBase64\" style=\"width:80px;height:80px;border-radius:50%;object-fit:cover;display:block;margin:0 auto;\" alt=\"logo\" />"
            } else {
                ""
            }
            val userPreferences = UserPreferences(context)
            val userName = userPreferences.userName ?: "اسم المستخدم"
            // ترويسة علوية (يمين ويسار)
            headerHtml = """
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:0;">
                <div style="font-weight:bold;font-size:1.1em;color:#1976d2;white-space:pre-line;text-align:right;flex:2;">$rightHeaderHtml</div>
                <div style="flex:1;text-align:center;">
                    $logoImgTag
                    <div style="font-size:1.15em;font-weight:bold;color:#1976d2;margin-top:4px;letter-spacing:0.5px;">$userName</div>
                </div>
                <div style="font-weight:bold;font-size:1.1em;color:#1976d2;white-space:pre-line;text-align:left;flex:2;">$leftHeaderHtml</div>
            </div>
            """
            // بطاقة بيانات العميل
            clientCardHtml = """
            <div style="border:1px solid #1976d2;border-radius:8px;padding:10px 0 10px 0;margin-bottom:10px;background:#f9f9f9;">
                <div style="font-size:1.1em;font-weight:bold;text-align:center;margin-bottom:4px;">كشف حساب : ${account.name}</div>
                <div style="text-align:center;font-size:0.98em;color:#333;">
                    من تاريخ: ${displayDateFormat.format(startDate)}
                    &nbsp; إلى تاريخ: ${displayDateFormat.format(endDate)}
                    ${if (transactions.firstOrNull()?.currency != null) "&nbsp; العملة: ${transactions.firstOrNull()?.currency}" else ""}
                </div>
            </div>
            """
        }
        return """
        <!DOCTYPE html>
        <html dir="rtl">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, minimum-scale=0.5, maximum-scale=3.0, user-scalable=yes" />
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
                html, body {
                    font-family: 'Cairo', Arial, sans-serif;
                    margin: 0; padding: 0;
                    background-color: #f5f5f5;
                    font-size: 12px;
                    box-sizing: border-box;
                    width: 100vw;
                    max-width: 100vw;
                    overflow-x: auto;
                    /* السماح بالتمرير الأفقي عند التكبير */
                    touch-action: pan-x pan-y;
                }
                .table-container {
                    background: white;
                    border-radius: 10px;
                    box-shadow: 0 2px 8px #eee;
                    padding: 0;
                    margin-bottom: 10px;
                    width: 100vw;
                    max-width: 100vw;
                    overflow-x: auto;
                }
                table {
                    width: 100%;
                    max-width: 100vw;
                    table-layout: fixed;
                    border-collapse: collapse;
                }
                th, td {
                    border: 1px solid #bbb;
                    padding: 4px 2px;
                    text-align: center;
                    font-size: 0.78em;
                    word-break: break-word;
                }
                th {
                    background: #1976d2;
                    color: white;
                    font-weight: bold;
                    font-size: 0.85em;
                }
                /* توزيع الأعمدة بدقة */
                th:nth-child(1), td:nth-child(1) { width: 18%; }
                th:nth-child(2), td:nth-child(2) { width: 36%; text-align: right; }
                th:nth-child(3), td:nth-child(3) { width: 14%; }
                th:nth-child(4), td:nth-child(4) { width: 14%; }
                th:nth-child(5), td:nth-child(5) { width: 18%; }
                .debit { color: #d32f2f; font-weight: bold; }
                .credit { color: #388e3c; font-weight: bold; }
                .summary-row { background: #f5f5f5; font-weight: bold; }
                /* تحسين التباعد بين العناصر */
                .client-card, .header-section { margin-bottom: 8px; }
            </style>
        </head>
        <body>
            $headerHtml
            $clientCardHtml
            <div class="table-container">
            <table>
                <thead>
                    <tr>
                        <th>التاريخ</th>
                        <th>البيان</th>
                        <th>عليه</th>
                        <th>له</th>
                        <th>الرصيد</th>
                    </tr>
                </thead>
                <tbody>
                    $transactionRows
                    $summaryRows
                </tbody>
            </table>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    private fun printReport() {
        if (webView != null) {
            webView.settings.builtInZoomControls = true
            webView.settings.displayZoomControls = false
            webView.settings.useWideViewPort = true
            webView.settings.loadWithOverviewMode = true
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
                val userPreferences = UserPreferences(this)
                val userName = userPreferences.userName ?: "اسم المستخدم"
                val pdfFile = generateAccountStatementPdfWithITextG(
                    context = this,
                    account = selectedAccount,
                    userName = userName,
                    startDate = startDate,
                    endDate = endDate,
                    selectedCurrency = selectedCurrency,
                    transactions = filteredTransactions,
                    allTransactions = transactions
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
        userName: String,
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

        // جلب الترويسة والشعار
        val (rightHeader, leftHeader, logoBitmap) = getReportHeaderData(context)
        // صف الترويسة والشعار
        val headerTable = PdfPTable(3)
        headerTable.widthPercentage = 100f
        headerTable.setWidths(floatArrayOf(2f, 1f, 2f))
        // الترويسة اليسرى (في أقصى اليمين)
        val leftCell = PdfPCell()
        leftCell.horizontalAlignment = Element.ALIGN_CENTER
        leftCell.verticalAlignment = Element.ALIGN_MIDDLE // <-- اجعل النص في وسط الخلية عموديًا
        leftCell.border = Rectangle.NO_BORDER
        leftCell.runDirection = PdfWriter.RUN_DIRECTION_RTL
        leftHeader.split("\n").forEach { line ->
            val para = Paragraph(ArabicUtilities.reshape(line), fontCairoBold)
            para.alignment = Element.ALIGN_CENTER // محاذاة النص أفقيًا في الوسط
            leftCell.addElement(para)
        }
        headerTable.addCell(leftCell)
        // الشعار واسم المستخدم واسم الحساب في الوسط (كما هو)
        val logoCell = PdfPCell()
        logoCell.border = Rectangle.NO_BORDER
        logoCell.horizontalAlignment = Element.ALIGN_CENTER
        logoCell.verticalAlignment = Element.ALIGN_MIDDLE
        logoCell.runDirection = PdfWriter.RUN_DIRECTION_RTL
        if (logoBitmap != null && logoBitmap.width > 0 && logoBitmap.height > 0) {
            try {
                val stream = java.io.ByteArrayOutputStream()
                logoBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val image = com.itextpdf.text.Image.getInstance(stream.toByteArray())
                image.scaleAbsolute(60f, 60f)
                image.alignment = Element.ALIGN_CENTER
                logoCell.addElement(image)
            } catch (e: Exception) {
                // تجاهل الشعار إذا حدث خطأ
            }
        }
        val userNamePara = Paragraph(ArabicUtilities.reshape(userName), fontCairoBold)
        userNamePara.alignment = Element.ALIGN_CENTER
        logoCell.addElement(userNamePara)
        val accountNamePara = Paragraph(ArabicUtilities.reshape(account.name), fontCairo)
        accountNamePara.alignment = Element.ALIGN_CENTER
        logoCell.addElement(accountNamePara)
        headerTable.addCell(logoCell)
        // الترويسة اليمنى (في أقصى اليسار)
        val rightCell = PdfPCell()
        rightCell.horizontalAlignment = Element.ALIGN_CENTER
        rightCell.verticalAlignment = Element.ALIGN_MIDDLE // <-- اجعل النص في وسط الخلية عموديًا
        rightCell.border = Rectangle.NO_BORDER
        rightCell.runDirection = PdfWriter.RUN_DIRECTION_RTL
        rightHeader.split("\n").forEach { line ->
            val para = Paragraph(ArabicUtilities.reshape(line), fontCairoBold)
            para.alignment = Element.ALIGN_CENTER // محاذاة النص أفقيًا في الوسط
            rightCell.addElement(para)
        }
        headerTable.addCell(rightCell)
        document.add(headerTable)
        document.add(Paragraph(" "))

        // --- بطاقة معلومات الحساب (بعد الهيدر مباشرة) ---
        val infoTable = PdfPTable(4)
        infoTable.widthPercentage = 100f
        infoTable.setWidths(floatArrayOf(3f, 3f, 3f, 3f))
        // اسم الحساب
        val accountNameCell = PdfPCell(Paragraph(ArabicUtilities.reshape("الحساب: ${account.name}"), fontCairo))
        accountNameCell.backgroundColor = BaseColor(0xF5, 0xF5, 0xF5)
        accountNameCell.border = Rectangle.BOX
        accountNameCell.horizontalAlignment = Element.ALIGN_CENTER
        accountNameCell.runDirection = PdfWriter.RUN_DIRECTION_RTL
        infoTable.addCell(accountNameCell)
        // رقم الهاتف
        val phoneCell = PdfPCell(Paragraph(ArabicUtilities.reshape("الهاتف: ${account.phoneNumber ?: "-"}"), fontCairo))
        phoneCell.backgroundColor = BaseColor(0xF5, 0xF5, 0xF5)
        phoneCell.border = Rectangle.BOX
        phoneCell.horizontalAlignment = Element.ALIGN_CENTER
        phoneCell.runDirection = PdfWriter.RUN_DIRECTION_RTL
        infoTable.addCell(phoneCell)
        // الفترة
        val periodCell = PdfPCell(Paragraph(ArabicUtilities.reshape("الفترة: من $startDate إلى ${displayDateFormat.format(endDate)}"), fontCairo))
        periodCell.backgroundColor = BaseColor(0xF5, 0xF5, 0xF5)
        periodCell.border = Rectangle.BOX
        periodCell.horizontalAlignment = Element.ALIGN_CENTER
        periodCell.runDirection = PdfWriter.RUN_DIRECTION_RTL
        infoTable.addCell(periodCell)
        // العملة
        val currencyCell = PdfPCell(Paragraph(ArabicUtilities.reshape("العملة: ${selectedCurrency ?: "-"}"), fontCairo))
        currencyCell.backgroundColor = BaseColor(0xF5, 0xF5, 0xF5)
        currencyCell.border = Rectangle.BOX
        currencyCell.horizontalAlignment = Element.ALIGN_CENTER
        currencyCell.runDirection = PdfWriter.RUN_DIRECTION_RTL
        infoTable.addCell(currencyCell)
        document.add(infoTable)
        document.add(Paragraph(" "))

        // حذف إضافة العنوان ومعلومات الحساب من الأعلى
        // (تم حذف الكود الخاص بـ titleTable و infoTable)

        // إنشاء الجدول مع تحديد عدد الأعمدة ونسب العرض
        val table = PdfPTable(5)
        table.widthPercentage = 100f
        table.setWidths(floatArrayOf(2f, 2f, 2f, 5f, 2f))

        // رؤوس الجدول مع ألوان (PDF)
        val headers = listOf("الرصيد", "عليه", "له", "تفاصيل", "التاريخ")
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
            PdfPCell(Paragraph("", fontCairo)), // عليه
            PdfPCell(Paragraph("", fontCairo)), // له
            PdfPCell(Paragraph(ArabicUtilities.reshape("الرصيد السابق"), fontCairo)), // تفاصيل
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
                PdfPCell(Paragraph(debit, fontDebit)), // عليه
                PdfPCell(Paragraph(credit, fontCredit)), // له
                PdfPCell(Paragraph(ArabicUtilities.reshape(desc), fontCairo)), // تفاصيل (الوصف)
                PdfPCell(Paragraph(dateStr, fontCairo)) // التاريخ
            )
            for (cell in row) {
                cell.horizontalAlignment = Element.ALIGN_CENTER
                cell.runDirection = PdfWriter.RUN_DIRECTION_RTL // أضف هذا السطر
                table.addCell(cell)
            }
        }

        // صف الإجمالي
        val summaryRow = listOf(
            PdfPCell(Paragraph("", fontCairo)), // الرصيد
            PdfPCell(Paragraph(String.format(Locale.ENGLISH, "%.2f", totalDebit), fontDebit)), // عليه
            PdfPCell(Paragraph(String.format(Locale.ENGLISH, "%.2f", totalCredit), fontCredit)), // له
            PdfPCell(Paragraph(ArabicUtilities.reshape("الإجمالي"), fontCairoBold)), // تفاصيل
            PdfPCell(Paragraph("", fontCairo)) // التاريخ
        )
        for (cell in summaryRow) {
            cell.horizontalAlignment = Element.ALIGN_CENTER
            cell.backgroundColor = BaseColor(0xF5, 0xF5, 0xF5)
            cell.runDirection = PdfWriter.RUN_DIRECTION_RTL
            table.addCell(cell)
        }
        // صف الرصيد النهائي
        val balanceRow = listOf(
            PdfPCell(Paragraph(String.format(Locale.ENGLISH, "%.2f", balance), fontCairoBold)), // الرصيد
            PdfPCell(Paragraph("", fontCairo)), // عليه
            PdfPCell(Paragraph("", fontCairo)), // له
            PdfPCell(Paragraph(ArabicUtilities.reshape("الرصيد"), fontCairoBold)), // تفاصيل
            PdfPCell(Paragraph("", fontCairo)) // التاريخ
        )
        for (cell in balanceRow) {
            cell.horizontalAlignment = Element.ALIGN_CENTER
            cell.backgroundColor = BaseColor(0xF5, 0xF5, 0xF5)
            cell.runDirection = PdfWriter.RUN_DIRECTION_RTL
            table.addCell(cell)
        }

        document.add(table)
        document.add(Paragraph(" "))
        // --- احذف كود الملخص النصي (summary) بعد الجدول ---
        // (تم حذف الكود الذي يضيف summary خارج الجدول)

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
            // معالجة أخطاء المشاركة بشكل واضح
            val errorMsg = "خطأ في مشاركة الملف: ${e.message}\n${e.stackTraceToString()}"
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Error", errorMsg)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "تعذر مشاركة التقرير. يرجى التأكد من الصلاحيات أو إعادة المحاولة.", Toast.LENGTH_LONG).show()
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
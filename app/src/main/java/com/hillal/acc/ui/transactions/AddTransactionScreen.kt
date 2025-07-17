package com.hillal.acc.ui.transactions

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.livedata.observeAsState
import androidx.navigation.NavController
import com.hillal.acc.R
import com.hillal.acc.data.entities.Cashbox
import com.hillal.acc.data.model.Account
import com.hillal.acc.data.model.Transaction
import com.hillal.acc.data.preferences.UserPreferences
import com.hillal.acc.data.repository.TransactionRepository
import com.hillal.acc.ui.cashbox.AddCashboxDialog
import com.hillal.acc.ui.common.AccountPickerField
import com.hillal.acc.ui.common.CashboxPickerField
import com.hillal.acc.ui.transactions.TransactionsViewModel
import com.hillal.acc.viewmodel.AccountViewModel
import com.hillal.acc.viewmodel.CashboxViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Notes
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.filled.Star
import com.hillal.acc.ui.theme.AppTheme
import com.hillal.acc.ui.theme.LocalAppDimensions
import com.hillal.acc.ui.theme.AppDimensions
import com.hillal.acc.ui.transactions.NotificationUtils
import java.util.Locale
import kotlin.math.abs
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.foundation.rememberScrollState

// دوال مساعدة لتحويل الأرقام إلى كلمات
private fun wholeNumberToWords(number: Long): String {
    if (number == 0L) return "صفر"
    if (number < 0L) return "سالب "+wholeNumberToWords(-number)

    val hundredsMap = mapOf(
        1L to "مائة",
        2L to "مئتان",
        3L to "ثلاثمائة",
        4L to "أربعمائة",
        5L to "خمسمائة",
        6L to "ستمائة",
        7L to "سبعمائة",
        8L to "ثمانمائة",
        9L to "تسعمائة"
    )
    val tens = arrayOf("", "عشرة", "عشرون", "ثلاثون", "أربعون", "خمسون", "ستون", "سبعون", "ثمانون", "تسعون")
    val ones = arrayOf("", "واحد", "اثنان", "ثلاثة", "أربعة", "خمسة", "ستة", "سبعة", "ثمانية", "تسعة")

    return when {
        number < 10L -> ones[number.toInt()]
        number < 20L -> when (number) {
            10L -> "عشرة"
            11L -> "أحد عشر"
            12L -> "اثنا عشر"
            13L -> "ثلاثة عشر"
            14L -> "أربعة عشر"
            15L -> "خمسة عشر"
            16L -> "ستة عشر"
            17L -> "سبعة عشر"
            18L -> "ثمانية عشر"
            19L -> "تسعة عشر"
            else -> ones[(number - 10).toInt()] + " عشر"
        }
        number < 100L -> {
            val ten = number / 10L
            val one = number % 10L
            when {
                one == 0L -> tens[ten.toInt()]
                else -> ones[one.toInt()] + " و" + tens[ten.toInt()]
            }
        }
        number < 1000L -> {
            val hundred = number / 100L
            val remainder = number % 100L
            val hundredWord = hundredsMap[hundred] ?: (ones[hundred.toInt()] + "مائة")
            return when {
                remainder == 0L -> hundredWord
                else -> hundredWord + " و" + wholeNumberToWords(remainder)
            }
        }
        number < 1000000L -> {
            val thousand = number / 1000L
            val remainder = number % 1000L
            val thousandWords = when {
                thousand == 1L -> "ألف"
                thousand == 2L -> "ألفان"
                thousand < 11L -> wholeNumberToWords(thousand) + " آلاف"
                else -> wholeNumberToWords(thousand) + " ألف"
            }
            when {
                remainder == 0L -> thousandWords
                else -> thousandWords + " و" + wholeNumberToWords(remainder)
            }
        }
        number < 1000000000L -> {
            val million = number / 1000000L
            val remainder = number % 1000000L
            val millionWords = when {
                million == 1L -> "مليون"
                million == 2L -> "مليونان"
                million < 11L -> wholeNumberToWords(million) + " ملايين"
                else -> wholeNumberToWords(million) + " مليون"
            }
            when {
                remainder == 0L -> millionWords
                else -> millionWords + " و" + wholeNumberToWords(remainder)
            }
        }
        else -> {
            val billion = number / 1000000000L
            val remainder = number % 1000000000L
            val billionWords = when {
                billion == 1L -> "مليار"
                billion == 2L -> "ملياران"
                billion < 11L -> wholeNumberToWords(billion) + " مليارات"
                else -> wholeNumberToWords(billion) + " مليار"
            }
            when {
                remainder == 0L -> billionWords
                else -> billionWords + " و" + wholeNumberToWords(remainder)
            }
        }
    }
}

private fun decimalToWords(decimal: Int): String {
    if (decimal == 0) return ""
    
    val ones = arrayOf("", "واحد", "اثنان", "ثلاثة", "أربعة", "خمسة", "ستة", "سبعة", "ثمانية", "تسعة")
    val tens = arrayOf("", "عشرة", "عشرون", "ثلاثون", "أربعون", "خمسون", "ستون", "سبعون", "ثمانون", "تسعون")
    
    return when {
        decimal < 10 -> ones[decimal]
        decimal < 20 -> when (decimal) {
            10 -> "عشرة"
            11 -> "أحد عشر"
            12 -> "اثنا عشر"
            13 -> "ثلاثة عشر"
            14 -> "أربعة عشر"
            15 -> "خمسة عشر"
            16 -> "ستة عشر"
            17 -> "سبعة عشر"
            18 -> "ثمانية عشر"
            19 -> "تسعة عشر"
            else -> ones[decimal - 10] + " عشر"
        }
        decimal < 100 -> {
            val ten = decimal / 10
            val one = decimal % 10
            when {
                one == 0 -> tens[ten]
                else -> ones[one] + " و" + tens[ten]
            }
        }
        else -> "مائة"
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    navController: NavController,
    transactionsViewModel: TransactionsViewModel = viewModel(),
    accountViewModel: AccountViewModel = viewModel(),
    cashboxViewModel: CashboxViewModel = viewModel(),
    transactionRepository: TransactionRepository,
    userPreferences: UserPreferences = UserPreferences(LocalContext.current)
) {
    AppTheme {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val coroutineScope = rememberCoroutineScope()
        val dimens = LocalAppDimensions.current
        val colors = MaterialTheme.colorScheme
        val typography = MaterialTheme.typography

        // State variables
        var selectedAccount by remember { mutableStateOf<Account?>(null) }
        var selectedAccountId by remember { mutableStateOf(-1L) }
        var selectedCashbox by remember { mutableStateOf<Cashbox?>(null) }
        var selectedCashboxId by remember { mutableStateOf(-1L) }
        var mainCashboxId by remember { mutableStateOf(-1L) }
        var allAccounts by remember { mutableStateOf(listOf<Account>()) }
        var allCashboxes by remember { mutableStateOf(listOf<Cashbox>()) }
        var allTransactions by remember { mutableStateOf(listOf<Transaction>()) }
        var accountBalancesMap by remember { mutableStateOf(mapOf<Long, Map<String, Double>>()) }
        var amount by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf(userPreferences.getUserName()) }
        var currency by remember { mutableStateOf(context.getString(R.string.currency_yer)) }
        var date by remember { mutableStateOf(Calendar.getInstance().timeInMillis) }
        var showDatePicker by remember { mutableStateOf(false) }
        var showAccountPicker by remember { mutableStateOf(false) }
        var showCashboxDialog by remember { mutableStateOf(false) }
        var isDialogShown by remember { mutableStateOf(false) }
        var lastSavedTransaction by remember { mutableStateOf<Transaction?>(null) }
        var lastSavedAccount by remember { mutableStateOf<Account?>(null) }
        var lastSavedBalance by remember { mutableStateOf(0.0) }
        var suggestions by remember { mutableStateOf(listOf<String>()) }
        var lastAmountUpdate by remember { mutableStateOf("") }
        val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH) }
        
        // دالة للتحقق من صحة إدخال المبلغ (أرقام إنجليزية وفواصل فقط)
        fun isValidAmount(input: String): Boolean {
            if (input.isEmpty()) return true
            // يسمح بالأرقام الإنجليزية والفواصل والنقاط فقط
            val regex = Regex("^[0-9.,]*$")
            return regex.matches(input)
        }
        
        // دالة لتنظيف وتنسيق المبلغ
        fun formatAmount(input: String): String {
            if (input.isEmpty()) return ""
            // إزالة جميع الأحرف غير المسموح بها
            val cleaned = input.replace(Regex("[^0-9.,]"), "")
            // التأكد من وجود فاصلة واحدة فقط
            val commaCount = cleaned.count { it == ',' }
            if (commaCount > 1) {
                val parts = cleaned.split(",")
                return parts.take(2).joinToString(",")
            }
            // تنسيق الأرقام بإضافة فواصل للآلاف
            val parts = cleaned.split(",")
            if (parts.size == 2) {
                val wholePart = parts[0]
                val decimalPart = parts[1]
                val formattedWhole = wholePart.reversed().chunked(3).joinToString(",").reversed()
                return "$formattedWhole,$decimalPart"
            } else {
                val wholePart = cleaned
                val formattedWhole = wholePart.reversed().chunked(3).joinToString(",").reversed()
                return formattedWhole
            }
        }
        
        // دالة لتحويل الأرقام إلى كلمات باللغة العربية
        fun numberToArabicWords(number: String): String {
            if (number.isEmpty()) return ""
            
            // تنظيف المبلغ من الفواصل وتحويله إلى رقم
            val cleanNumber = number.replace(",", "")
            val doubleValue = cleanNumber.toDoubleOrNull() ?: return ""
            
            return when {
                doubleValue == 0.0 -> "صفر"
                doubleValue < 1.0 -> {
                    val decimal = (doubleValue * 100).toInt()
                    when (decimal) {
                        0 -> "صفر"
                        in 1..9 -> "صفر و${decimalToWords(decimal)}"
                        else -> decimalToWords(decimal)
                    }
                }
                else -> {
                    val whole = doubleValue.toLong()
                    val decimal = ((doubleValue - whole) * 100).toInt()
                    
                    val wholeWords = wholeNumberToWords(whole)
                    val decimalWords = if (decimal > 0) decimalToWords(decimal) else ""
                    
                    when {
                        decimalWords.isEmpty() -> wholeWords
                        wholeWords == "صفر" -> decimalWords
                        else -> "$wholeWords و$decimalWords"
                    }
                }
            }
        }

        // تحديث الكتابة تلقائياً عند تغيير المبلغ
        LaunchedEffect(amount) {
            lastAmountUpdate = amount
        }
        
        // Observers
        LaunchedEffect(Unit) {
            accountViewModel.getAllAccounts().observe(lifecycleOwner) { accounts ->
                if (accounts != null) {
                    // ترتيب الحسابات حسب عدد المعاملات
                    val accountTransactionCount = mutableMapOf<Long, Int>()
                    for (transaction in allTransactions) {
                        val accountId = transaction.getAccountId()
                        accountTransactionCount[accountId] = accountTransactionCount.getOrDefault(accountId, 0) + 1
                    }
                    val sortedAccounts = accounts.filterNotNull().sortedByDescending { account ->
                        accountTransactionCount.getOrDefault(account.getId(), 0)
                    }
                    allAccounts = sortedAccounts
                }
            }
            cashboxViewModel.getAllCashboxes().observe(lifecycleOwner) { cashboxes ->
                if (cashboxes != null) {
                    allCashboxes = cashboxes.filterNotNull()
                    if (allCashboxes.isNotEmpty()) {
                        mainCashboxId = allCashboxes[0].id
                        if (selectedCashboxId == -1L) {
                            selectedCashbox = allCashboxes[0]
                            selectedCashboxId = allCashboxes[0].id
                        }
                    }
                }
            }
            transactionsViewModel.accountBalancesMap.observe(lifecycleOwner) { balancesMap ->
                if (balancesMap != null) {
                    accountBalancesMap = balancesMap.filterKeys { it != null }.mapKeys { it.key!! } as Map<Long, Map<String, Double>>
                }
            }
            transactionsViewModel.getTransactions().observe(lifecycleOwner) { txs ->
                if (txs != null) allTransactions = txs
            }
        }

        // Suggestions
        fun loadAccountSuggestions(accountId: Long) {
            val prefs = context.getSharedPreferences("suggestions", Context.MODE_PRIVATE)
            val set = prefs.getStringSet("descriptions_" + accountId, setOf()) ?: setOf()
            suggestions = set.filterNotNull()
        }

        // Save Transaction
        fun saveTransaction(isDebit: Boolean) {
            if (selectedAccountId == -1L) {
                Toast.makeText(context, "الرجاء اختيار الحساب", Toast.LENGTH_SHORT).show()
                return
            }
            if (amount.isEmpty()) {
                Toast.makeText(context, context.getString(R.string.error_amount_required), Toast.LENGTH_SHORT).show()
                return
            }
            // تنظيف المبلغ من الفواصل
            val cleanAmount = amount.replace(",", "")
            val amountDouble = cleanAmount.toDoubleOrNull()
            if (amountDouble == null) {
                Toast.makeText(context, context.getString(R.string.error_invalid_amount), Toast.LENGTH_SHORT).show()
                return
            }
            accountViewModel.getAccountById(selectedAccountId).observe(lifecycleOwner) { account ->
                if (account != null) {
                    val transaction = Transaction(
                        selectedAccountId,
                        amountDouble,
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
                    // Cashbox logic
                    val cashboxIdToSave = when {
                        selectedCashboxId != -1L -> selectedCashboxId
                        mainCashboxId != -1L -> mainCashboxId
                        allCashboxes.isNotEmpty() -> allCashboxes[0].id
                        else -> -1L
                    }
                    transaction.setCashboxId(cashboxIdToSave)
                    transactionsViewModel.insertTransaction(transaction)
                    lastSavedTransaction = transaction
                    lastSavedAccount = account
                    // Get balance
                    transactionRepository.getBalanceUntilDate(selectedAccountId, transaction.getTransactionDate(), currency)
                        .observe(lifecycleOwner) { balance ->
                            lastSavedBalance = balance ?: 0.0
                            isDialogShown = true
                        }
                    // Save suggestion
                    val desc = description.trim()
                    if (desc.isNotEmpty()) {
                        val prefs = context.getSharedPreferences("suggestions", Context.MODE_PRIVATE)
                        val key = "descriptions_" + selectedAccountId
                        val oldSet = prefs.getStringSet(key, setOf()) ?: setOf()
                        val newSet = oldSet.toMutableSet()
                        newSet.add(desc)
                        prefs.edit().putStringSet(key, newSet).apply()
                        loadAccountSuggestions(selectedAccountId)
                    }
                }
            }
        }

        // UI
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenWidth = maxWidth
            val scrollState = rememberScrollState()
            val cardShape = RoundedCornerShape(dimens.cardCorner)
            val cardMaxHeight = this@BoxWithConstraints.maxHeight * 0.18f
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .imePadding()
                    .background(colors.background),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingSmall)
            ) {
                // بطاقة العنوان في الأعلى
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimens.spacingMedium, vertical = dimens.spacingSmall),
                    shape = cardShape,
                    colors = CardDefaults.cardColors(containerColor = colors.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(cardMaxHeight)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        colors.primaryContainer,
                                        colors.secondaryContainer.copy(alpha = 0.5f)
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(400f, 100f)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // رسم ديكوري في الخلفية
                        Box(
                            modifier = Modifier
                                .size(cardMaxHeight * 1.1f)
                                .align(Alignment.CenterEnd)
                                .offset(x = dimens.spacingMedium, y = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachMoney,
                                contentDescription = null,
                                tint = colors.primary.copy(alpha = 0.10f),
                                modifier = Modifier.size(cardMaxHeight * 1.1f)
                            )
                        }
                        // أيقونة جميلة فوق العنوان
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = colors.secondary,
                            modifier = Modifier
                                .size(cardMaxHeight * 0.4f)
                                .align(Alignment.Center)
                        )
                        // نص العنوان في المقدمة
                        Text(
                            text = "إضافة معاملة جديدة",
                            style = typography.headlineSmall,
                            color = colors.onPrimaryContainer,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(dimens.spacingSmall))
                // Main Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = dimens.spacingMedium, end = dimens.spacingMedium, bottom = dimens.spacingSmall)
                        .shadow(4.dp, cardShape),
                    shape = cardShape,
                    colors = CardDefaults.cardColors(containerColor = colors.surface)
                ) {
                    Column(modifier = Modifier.padding(dimens.spacingSmall)) {
                        // Row: حساب وصندوق
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(dimens.spacingSmall)) {
                            Box(Modifier.weight(1f)) {
                                AccountPickerField(
                                    accounts = allAccounts,
                                    transactions = allTransactions,
                                    balancesMap = accountBalancesMap,
                                    selectedAccount = selectedAccount,
                                    onAccountSelected = { account ->
                                        selectedAccount = account
                                        selectedAccountId = account.getId()
                                        loadAccountSuggestions(selectedAccountId)
                                    }
                                )
                            }
                            Box(Modifier.weight(1f)) {
                                CashboxPickerField(
                                    cashboxes = allCashboxes,
                                    selectedCashbox = allCashboxes.find { it.id == selectedCashboxId },
                                    onCashboxSelected = { cashbox ->
                                        selectedCashboxId = cashbox.id
                                    },
                                    onAddCashbox = { name ->
                                        CashboxHelper.addCashboxToServer(
                                            context, cashboxViewModel, name,
                                            object : CashboxHelper.CashboxCallback {
                                                override fun onSuccess(cashbox: Cashbox?) {
                                                    selectedCashboxId = cashbox?.id ?: -1L
                                                    CashboxHelper.showSuccessMessage(context, "تم إضافة الصندوق بنجاح")
                                                }
                                                override fun onError(error: String?) {
                                                    CashboxHelper.showErrorMessage(context, error)
                                                }
                                            })
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(dimens.spacingSmall))
                        // Row: مبلغ وتاريخ
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(dimens.spacingSmall)) {
                            Column(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = amount,
                                    onValueChange = { newValue ->
                                        if (isValidAmount(newValue)) {
                                            amount = newValue // بدون تنسيق فواصل
                                            lastAmountUpdate = amount // تحديث للكتابة التلقائية
                                        }
                                    },
                                    label = { Text("المبلغ", fontSize = typography.bodyLarge.fontSize) },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                                    textStyle = typography.bodyLarge,
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                
                                // عرض المبلغ بالكلمات العربية
                                if (lastAmountUpdate.isNotBlank()) {
                                    val arabicWords = numberToArabicWords(lastAmountUpdate)
                                    if (arabicWords.isNotBlank()) {
                                        Text(
                                            text = arabicWords,
                                            style = typography.bodySmall.copy(
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Normal
                                            ),
                                            color = colors.primary.copy(alpha = 0.7f),
                                            modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                                            maxLines = 2,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = dateFormat.format(Date(date)),
                                onValueChange = {},
                                label = { Text("التاريخ", fontSize = typography.bodyLarge.fontSize) },
                                modifier = Modifier.weight(1f).clickable { showDatePicker = true },
                                enabled = true,
                                readOnly = true,
                                leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                                textStyle = typography.bodyLarge,
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    containerColor = colors.surface,
                                    focusedBorderColor = colors.primary,
                                    unfocusedBorderColor = colors.onSurface,
                                    disabledBorderColor = colors.surfaceVariant,
                                    disabledTextColor = colors.onSurface
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(dimens.spacingSmall))
                        // Description with suggestions (ExposedDropdownMenuBox)
                        var expandedSuggestions by remember { mutableStateOf(false) }
                        var showAllSuggestions by remember { mutableStateOf(false) }
                        var descriptionState by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
                        ExposedDropdownMenuBox(
                            expanded = expandedSuggestions,
                            onExpandedChange = { /* لا تفعل شيء هنا */ }
                        ) {
                            OutlinedTextField(
                                value = descriptionState,
                                onValueChange = {
                                    descriptionState = it
                                    description = it.text
                                    showAllSuggestions = false
                                    expandedSuggestions = it.text.isNotEmpty() && suggestions.any { s -> s.startsWith(it.text) && s != it.text }
                                },
                                label = { Text("البيان", fontSize = typography.bodyLarge.fontSize) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                                textStyle = typography.bodyLarge,
                                trailingIcon = {
                                    if (suggestions.isNotEmpty()) {
                                        IconButton(onClick = {
                                            showAllSuggestions = true
                                            expandedSuggestions = true
                                        }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    }
                                }
                            )
                            ExposedDropdownMenu(
                                expanded = expandedSuggestions,
                                onDismissRequest = {
                                    expandedSuggestions = false
                                    showAllSuggestions = false
                                }
                            ) {
                                val filtered = if (showAllSuggestions)
                                    suggestions.take(10)
                                else if (descriptionState.text.isNotEmpty())
                                    suggestions.filter { it.startsWith(descriptionState.text) && it != descriptionState.text }.take(10)
                                else
                                    emptyList()
                                filtered.forEach { suggestion ->
                                    DropdownMenuItem(
                                        text = { Text(suggestion, fontSize = typography.bodyLarge.fontSize) },
                                        onClick = {
                                            descriptionState = TextFieldValue(
                                                suggestion,
                                                TextRange(suggestion.length)
                                            )
                                            description = suggestion
                                            expandedSuggestions = false
                                            showAllSuggestions = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(dimens.spacingSmall))
                        // Currency Radio Buttons
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currency == context.getString(R.string.currency_yer),
                                onClick = { currency = context.getString(R.string.currency_yer) }
                            )
                            Text("يمني", fontSize = typography.bodyLarge.fontSize)
                            Spacer(modifier = Modifier.width(dimens.spacingSmall))
                            RadioButton(
                                selected = currency == context.getString(R.string.currency_sar),
                                onClick = { currency = context.getString(R.string.currency_sar) }
                            )
                            Text("سعودي", fontSize = typography.bodyLarge.fontSize)
                            Spacer(modifier = Modifier.width(dimens.spacingSmall))
                            RadioButton(
                                selected = currency == context.getString(R.string.currency_usd),
                                onClick = { currency = context.getString(R.string.currency_usd) }
                            )
                            Text("دولار", fontSize = typography.bodyLarge.fontSize)
                        }
                        Spacer(modifier = Modifier.height(dimens.spacingSmall))
                        // Notes (hidden by default)
                        val showNotes = false
                        if (showNotes) {
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("ملاحظات", fontSize = typography.bodyLarge.fontSize) },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                                textStyle = typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(dimens.spacingSmall))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(dimens.spacingMedium)) // مسافة بسيطة فوق الأزرار
                // Action Buttons في الأسفل
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimens.spacingMedium, vertical = dimens.spacingSmall),
                    horizontalArrangement = Arrangement.spacedBy(dimens.spacingSmall)
                ) {
                    Button(
                        onClick = { saveTransaction(false) },
                        modifier = Modifier
                            .weight(1f)
                            .height(dimens.buttonHeight),
                        shape = RoundedCornerShape(dimens.buttonCorner),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50), contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null)
                        Spacer(Modifier.width(dimens.spacingSmall))
                        Text("له", fontSize = typography.bodyLarge.fontSize)
                    }
                    Button(
                        onClick = { saveTransaction(true) },
                        modifier = Modifier
                            .weight(1f)
                            .height(dimens.buttonHeight),
                        shape = RoundedCornerShape(dimens.buttonCorner),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336), contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null)
                        Spacer(Modifier.width(dimens.spacingSmall))
                        Text("عليه", fontSize = typography.bodyLarge.fontSize)
                    }
                    Button(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier
                            .weight(1f)
                            .height(dimens.buttonHeight),
                        shape = RoundedCornerShape(dimens.buttonCorner)
                    ) {
                        Text("إلغاء", fontSize = typography.bodyLarge.fontSize)
                    }
                }
                Spacer(modifier = Modifier.height(dimens.spacingMedium))
            }
        }

        // Date Picker Dialog
        if (showDatePicker) {
            val calendar = Calendar.getInstance().apply { timeInMillis = date }
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    date = calendar.timeInMillis
                    showDatePicker = false
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Account Picker BottomSheet
        if (showAccountPicker) {
            AccountPickerBottomSheetCompose(
                show = showAccountPicker,
                accounts = allAccounts,
                transactions = allTransactions,
                balancesMap = accountBalancesMap,
                onAccountSelected = { account ->
                    selectedAccount = account
                    selectedAccountId = account.getId()
                    // suggestions
                    val prefs = context.getSharedPreferences("suggestions", Context.MODE_PRIVATE)
                    val set = prefs.getStringSet("descriptions_" + selectedAccountId, setOf()) ?: setOf()
                    suggestions = set.filterNotNull()
                    showAccountPicker = false
                },
                onDismiss = { showAccountPicker = false },
                dimens = dimens
            )
        }

        // Cashbox Dialog
        if (showCashboxDialog) {
            AddCashboxDialogCompose(
                show = showCashboxDialog,
                onCashboxAdded = { name ->
                    CashboxHelper.addCashboxToServer(
                        context, cashboxViewModel, name,
                        object : CashboxHelper.CashboxCallback {
                            override fun onSuccess(cashbox: Cashbox?) {
                                selectedCashbox = cashbox
                                selectedCashboxId = cashbox?.id ?: -1L
                                CashboxHelper.showSuccessMessage(context, "تم إضافة الصندوق بنجاح")
                                showCashboxDialog = false
                            }
                            override fun onError(error: String?) {
                                CashboxHelper.showErrorMessage(context, error)
                            }
                        })
                },
                onDismiss = { showCashboxDialog = false }
            )
        }

        // Success Dialog
        if (isDialogShown) {
            Dialog(onDismissRequest = { isDialogShown = false }) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 200.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // أيقونة نجاح
                        Icon(
                            painter = painterResource(id = R.drawable.ic_wallet),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "تمت إضافة المعاملة بنجاح!",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "هل ترغب بإرسال إشعار؟",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Medium)
                        )
                        Spacer(Modifier.height(8.dp))
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            text = "تفاصيل المعاملة:",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.height(8.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("الحساب: ${lastSavedAccount?.getName() ?: ""}", style = MaterialTheme.typography.bodyMedium)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AttachMoney, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("المبلغ: ${lastSavedTransaction?.getAmount() ?: 0.0} ${lastSavedTransaction?.getCurrency() ?: ""}", style = MaterialTheme.typography.bodyMedium)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("البيان: ${lastSavedTransaction?.getDescription() ?: ""}", style = MaterialTheme.typography.bodyMedium)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("الرصيد الجديد: $lastSavedBalance ${lastSavedTransaction?.getCurrency() ?: ""}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            text = "اختر الإجراء المطلوب:",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(24.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            
                            Button(
                                onClick = {
                                    // إرسال SMS
                                    lastSavedAccount?.let { account ->
                                        lastSavedTransaction?.let { transaction ->
                                            val phone = account.getPhoneNumber()
                                            if (!phone.isNullOrEmpty()) {
                                                val type = transaction.getType()
                                                val amountStr = String.format(Locale.US, "%.0f", transaction.getAmount())
                                                val balanceStr = String.format(Locale.US, "%.0f", abs(lastSavedBalance))
                                                val currency = transaction.getCurrency()
                                                val typeText = if (type.equals("credit", true) || type == "له") "لكم" else "عليكم"
                                                val balanceText = if (lastSavedBalance >= 0) "الرصيد لكم " else "الرصيد عليكم "
                                                val message = "حسابكم لدينا:\n" +
                                                        typeText + " " + amountStr + " " + currency + "\n" +
                                                        transaction.getDescription() + "\n" +
                                                        balanceText + balanceStr + " " + currency
                                                NotificationUtils.sendSmsMessage(context, phone, message)
                                            } else {
                                                Toast.makeText(context, "رقم الهاتف غير متوفر", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                    isDialogShown = false
                                    // إعادة تعيين الحقول
                                    amount = ""
                                    description = ""
                                    date = System.currentTimeMillis()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_sms),
                                    contentDescription = null,
                                    tint = colors.onPrimary,
                                    modifier = Modifier.size(dimens.smsIconSize)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("إرسال SMS", fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        isDialogShown = false
                                        // إعادة تعيين الحقول
                                        amount = ""
                                        description = ""
                                        date = System.currentTimeMillis()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(" قيد جديد", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = {
                                        isDialogShown = false
                                        navController.navigateUp()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("خروج", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountPickerBottomSheetCompose(
    show: Boolean,
    accounts: List<Account>,
    transactions: List<Transaction>,
    balancesMap: Map<Long, Map<String, Double>>,
    onAccountSelected: (Account) -> Unit,
    onDismiss: () -> Unit,
    dimens: AppDimensions // أضف هذا الباراميتر
) {
    var search by remember { mutableStateOf("") }
    val filteredAccounts = if (search.isBlank()) accounts else accounts.filter { it.getName()?.contains(search) == true }
    if (show) {
        ModalBottomSheet(onDismissRequest = onDismiss) {
            Column(Modifier.fillMaxWidth().padding(dimens.spacingMedium)) {
                Text(text = "اختر الحساب", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(dimens.spacingSmall))
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("بحث") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(dimens.spacingSmall))
                Divider()
                LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    items(filteredAccounts) { account ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = dimens.spacingSmall)
                                .clickable {
                                    onAccountSelected(account)
                                    onDismiss()
                                },
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(Modifier.padding(dimens.spacingSmall)) {
                                Text(account.getName() ?: "", fontWeight = FontWeight.Bold)
                                val balance = balancesMap[account.getId()]?.values?.sum() ?: 0.0
                                Text("الرصيد: ${balance}", fontSize = 13.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(dimens.spacingSmall))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("إغلاق")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCashboxDialogCompose(
    show: Boolean,
    onCashboxAdded: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("إضافة صندوق جديد") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            error = false
                        },
                        label = { Text("اسم الصندوق") },
                        isError = error,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (error) {
                        Text("يرجى إدخال اسم الصندوق", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isBlank()) {
                        error = true
                    } else {
                        onCashboxAdded(name.trim())
                        name = ""
                        error = false
                        onDismiss()
                    }
                }) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                Button(onClick = {
                    name = ""
                    error = false
                    onDismiss()
                }) {
                    Text("إلغاء")
                }
            }
        )
    }
} 

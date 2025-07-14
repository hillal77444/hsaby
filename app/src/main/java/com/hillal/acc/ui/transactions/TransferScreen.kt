package com.hillal.acc.ui.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hillal.acc.data.model.Account
import com.hillal.acc.data.entities.Cashbox
import com.hillal.acc.ui.common.AccountPickerField
import com.hillal.acc.ui.common.CashboxPickerField
import com.hillal.acc.ui.theme.LocalAppDimensions
import com.hillal.acc.ui.theme.AppTheme
import com.hillal.acc.R
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.navigation.NavController
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

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
fun TransferScreen(
    accounts: List<Account>,
    cashboxes: List<Cashbox>,
    currencies: List<String>,
    transactions: List<com.hillal.acc.data.model.Transaction>,
    balancesMap: Map<Long, Map<String, Double>>,
    onAddCashbox: (String) -> Unit,
    onTransfer: (fromAccount: Account, toAccount: Account, cashbox: Cashbox, currency: String, amount: String, notes: String) -> Unit,
    onNavigateBack: () -> Unit = {}
) {
    AppTheme {
        val dimens = LocalAppDimensions.current
        val colors = MaterialTheme.colorScheme
        val typography = MaterialTheme.typography
        val haptic = LocalHapticFeedback.current
        val coroutineScope = rememberCoroutineScope()
        
        var fromAccount by remember { mutableStateOf<Account?>(null) }
        var toAccount by remember { mutableStateOf<Account?>(null) }
        var selectedCashbox by remember { mutableStateOf<Cashbox?>(cashboxes.firstOrNull()) }
        var selectedCurrency by remember { mutableStateOf(currencies.firstOrNull() ?: "") }
        var amount by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var successMessage by remember { mutableStateOf<String?>(null) }
        var showCurrencyMenu by remember { mutableStateOf(false) }
        var isTransferring by remember { mutableStateOf(false) }
        var lastAmountUpdate by remember { mutableStateOf("") }
        
        val scrollState = rememberScrollState()
        
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
        
        // تحديث الصندوق المحدد تلقائياً عند تغيير قائمة الصناديق
        LaunchedEffect(cashboxes) {
            if (selectedCashbox == null && cashboxes.isNotEmpty()) {
                selectedCashbox = cashboxes.first()
            }
        }
        
        // تحديث العملة المحددة تلقائياً عند تغيير قائمة العملات
        LaunchedEffect(currencies) {
            if (selectedCurrency.isBlank() && currencies.isNotEmpty()) {
                selectedCurrency = currencies.first()
            }
        }
        
        // تحديث الكتابة تلقائياً عند تغيير المبلغ
        LaunchedEffect(amount) {
            lastAmountUpdate = amount
        }
        
        // Animations
        val headerScale by animateFloatAsState(
            targetValue = if (fromAccount != null && toAccount != null) 1.05f else 1f,
            animationSpec = tween(durationMillis = 300)
        )
        
        val cardElevation by animateDpAsState(
            targetValue = if (isTransferring) 8.dp else 4.dp,
            animationSpec = tween(durationMillis = 200)
        )
        
        val buttonScale by animateFloatAsState(
            targetValue = if (fromAccount != null && toAccount != null && selectedCashbox != null && amount.isNotBlank()) 1.02f else 1f,
            animationSpec = tween(durationMillis = 200)
        )

        Surface(
            color = colors.background,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = dimens.spacingMedium)
                    .statusBarsPadding()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingSmall) // Reduced spacing
            ) {
                Spacer(Modifier.height(dimens.spacingMedium)) // Reduced top spacing
                
                // Compact Header
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer(scaleX = headerScale, scaleY = headerScale),
                    shape = RoundedCornerShape(dimens.cardCorner),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        colors.primary.copy(alpha = 0.1f),
                                        colors.primary.copy(alpha = 0.05f)
                                    )
                                )
                            )
                            .padding(dimens.spacingSmall) // Reduced padding
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_accounts),
                                contentDescription = null,
                                modifier = Modifier.size(dimens.iconSize),
                                tint = colors.primary
                            )
                            Spacer(Modifier.width(dimens.spacingSmall))
                            Text(
                                text = "تحويل بين الحسابات",
                                style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colors.primary
                            )
                        }
                    }
                }
                
                // Card: Accounts (Horizontal Layout)
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(durationMillis = 600)
                    ) + fadeIn(animationSpec = tween(durationMillis = 600))
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = cardElevation,
                                shape = RoundedCornerShape(dimens.cardCorner)
                            ),
                        shape = RoundedCornerShape(dimens.cardCorner),
                        colors = CardDefaults.cardColors(containerColor = colors.surface)
                    ) {
                        Column(Modifier.padding(dimens.spacingMedium)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = dimens.spacingSmall)
                            ) {
                                Icon(
                                    Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(dimens.iconSize * 0.8f)
                                )
                                Spacer(Modifier.width(dimens.spacingSmall))
                                Text(
                                    "الحسابات",
                                    style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colors.primary
                                )
                            }
                            
                            // Horizontal layout for accounts
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(dimens.spacingSmall)
                            ) {
                                // From Account
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("من:", style = typography.bodySmall, color = colors.onSurface)
                                    AccountPickerField(
                                        label = "الحساب",
                                        accounts = accounts,
                                        transactions = transactions,
                                        balancesMap = balancesMap,
                                        selectedAccount = fromAccount,
                                        onAccountSelected = { 
                                            fromAccount = it
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    )
                                }
                                
                                // To Account
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("إلى:", style = typography.bodySmall, color = colors.onSurface)
                                    AccountPickerField(
                                        label = "الحساب",
                                        accounts = accounts,
                                        transactions = transactions,
                                        balancesMap = balancesMap,
                                        selectedAccount = toAccount,
                                        onAccountSelected = { 
                                            toAccount = it
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Card: Settings (Horizontal Layout)
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(durationMillis = 800)
                    ) + fadeIn(animationSpec = tween(durationMillis = 800))
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = cardElevation,
                                shape = RoundedCornerShape(dimens.cardCorner)
                            ),
                        shape = RoundedCornerShape(dimens.cardCorner),
                        colors = CardDefaults.cardColors(containerColor = colors.surface)
                    ) {
                        Column(Modifier.padding(dimens.spacingMedium)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = dimens.spacingSmall)
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = colors.secondary,
                                    modifier = Modifier.size(dimens.iconSize * 0.8f)
                                )
                                Spacer(Modifier.width(dimens.spacingSmall))
                                Text(
                                    "الإعدادات",
                                    style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colors.secondary
                                )
                            }
                            
                            // Horizontal layout for settings
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(dimens.spacingSmall)
                            ) {
                                // Cashbox
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("صندوق:", style = typography.bodySmall, color = colors.onSurface)
                                    CashboxPickerField(
                                        cashboxes = cashboxes,
                                        selectedCashbox = selectedCashbox,
                                        onCashboxSelected = { 
                                            selectedCashbox = it
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        },
                                        onAddCashbox = onAddCashbox
                                    )
                                }
                                
                                // Currency
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("عملة:", style = typography.bodySmall, color = colors.onSurface)
                                    ExposedDropdownMenuBox(
                                        expanded = showCurrencyMenu,
                                        onExpandedChange = { 
                                            showCurrencyMenu = !showCurrencyMenu
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    ) {
                                        OutlinedTextField(
                                            value = selectedCurrency,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("العملة") },
                                            modifier = Modifier.menuAnchor().fillMaxWidth().imePadding(),
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCurrencyMenu) },
                                            colors = androidx.compose.material3.TextFieldDefaults.outlinedTextFieldColors(
                                                containerColor = colors.background,
                                                focusedBorderColor = colors.primary,
                                                unfocusedBorderColor = colors.outline
                                            )
                                        )
                                        DropdownMenu(
                                            expanded = showCurrencyMenu,
                                            onDismissRequest = { showCurrencyMenu = false }
                                        ) {
                                            currencies.forEach { currency ->
                                                DropdownMenuItem(
                                                    text = { Text(currency) },
                                                    onClick = {
                                                        selectedCurrency = currency
                                                        showCurrencyMenu = false
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Card: Amount (Full Width)
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(durationMillis = 1000)
                    ) + fadeIn(animationSpec = tween(durationMillis = 1000))
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = cardElevation,
                                shape = RoundedCornerShape(dimens.cardCorner)
                            ),
                        shape = RoundedCornerShape(dimens.cardCorner),
                        colors = CardDefaults.cardColors(containerColor = colors.surface)
                    ) {
                        Column(Modifier.padding(dimens.spacingMedium)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = dimens.spacingSmall)
                            ) {
                                Icon(
                                    Icons.Default.AttachMoney,
                                    contentDescription = null,
                                    tint = colors.tertiary,
                                    modifier = Modifier.size(dimens.iconSize * 0.8f)
                                )
                                Spacer(Modifier.width(dimens.spacingSmall))
                                Text(
                                    "المبلغ",
                                    style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colors.tertiary
                                )
                            }
                            Column {
                                                            OutlinedTextField(
                                value = amount,
                                onValueChange = { newValue ->
                                    if (isValidAmount(newValue)) {
                                        amount = newValue // بدون تنسيق فواصل
                                        lastAmountUpdate = amount // تحديث للكتابة التلقائية
                                    }
                                },
                                    label = { Text("أدخل المبلغ") },
                                    modifier = Modifier.fillMaxWidth().imePadding(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number
                                    ),
                                    colors = androidx.compose.material3.TextFieldDefaults.outlinedTextFieldColors(
                                        containerColor = colors.background,
                                        focusedBorderColor = colors.primary,
                                        unfocusedBorderColor = colors.outline
                                    )
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
                                            modifier = Modifier.padding(top = 6.dp, start = 4.dp),
                                            maxLines = 2,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Card: Details (Notes Only)
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(durationMillis = 1200)
                    ) + fadeIn(animationSpec = tween(durationMillis = 1200))
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = cardElevation,
                                shape = RoundedCornerShape(dimens.cardCorner)
                            ),
                        shape = RoundedCornerShape(dimens.cardCorner),
                        colors = CardDefaults.cardColors(containerColor = colors.surface)
                    ) {
                        Column(Modifier.padding(dimens.spacingMedium)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = dimens.spacingSmall)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = colors.tertiary,
                                    modifier = Modifier.size(dimens.iconSize * 0.8f)
                                )
                                Spacer(Modifier.width(dimens.spacingSmall))
                                Text(
                                    "التفاصيل",
                                    style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colors.tertiary
                                )
                            }
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("ملاحظات (اختياري)") },
                                modifier = Modifier.fillMaxWidth().imePadding(),
                                singleLine = false,
                                maxLines = 2,
                                colors = androidx.compose.material3.TextFieldDefaults.outlinedTextFieldColors(
                                    containerColor = colors.background,
                                    focusedBorderColor = colors.primary,
                                    unfocusedBorderColor = colors.outline
                                )
                            )
                        }
                    }
                }
                
                // Animated success message
                AnimatedVisibility(
                    visible = successMessage != null,
                    enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(durationMillis = 500)
                    ) + fadeIn(animationSpec = tween(durationMillis = 500)),
                    exit = slideOutVertically(
                        targetOffsetY = { -it },
                        animationSpec = tween(durationMillis = 300)
                    ) + fadeOut(animationSpec = tween(durationMillis = 300))
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8)),
                        shape = RoundedCornerShape(dimens.cardCorner)
                    ) {
                        Row(
                            modifier = Modifier.padding(dimens.spacingMedium),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(dimens.iconSize)
                            )
                            Spacer(Modifier.width(dimens.spacingSmall))
                            Text(
                                successMessage ?: "",
                                color = Color(0xFF4CAF50),
                                style = typography.bodyMedium
                            )
                        }
                    }
                }
                
                // Animated error message
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(durationMillis = 500)
                    ) + fadeIn(animationSpec = tween(durationMillis = 500)),
                    exit = slideOutVertically(
                        targetOffsetY = { -it },
                        animationSpec = tween(durationMillis = 300)
                    ) + fadeOut(animationSpec = tween(durationMillis = 300))
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = colors.errorContainer),
                        shape = RoundedCornerShape(dimens.cardCorner)
                    ) {
                        Row(
                            modifier = Modifier.padding(dimens.spacingMedium),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = colors.error,
                                modifier = Modifier.size(dimens.iconSize)
                            )
                            Spacer(Modifier.width(dimens.spacingSmall))
                            Text(
                                errorMessage ?: "",
                                color = colors.error,
                                style = typography.bodyMedium
                            )
                        }
                    }
                }
                
                Spacer(Modifier.weight(1f))
                
                // Enhanced transfer button with loading state
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (fromAccount == null || toAccount == null || selectedCashbox == null || amount.isBlank() || selectedCurrency.isBlank()) {
                            errorMessage = "يرجى تعبئة جميع الحقول المطلوبة"
                            successMessage = null
                        } else if (!isValidAmount(amount) || amount.replace(",", "").toDoubleOrNull() == null) {
                            errorMessage = "يرجى إدخال مبلغ صحيح"
                            successMessage = null
                        } else {
                            isTransferring = true
                            errorMessage = null
                            successMessage = "تم التحويل بنجاح!"
                            // تنظيف المبلغ من الفواصل
                            val cleanAmount = amount.replace(",", "")
                            onTransfer(fromAccount!!, toAccount!!, selectedCashbox!!, selectedCurrency, cleanAmount, notes)
                            
                            // Simulate loading and then close screen
                            coroutineScope.launch {
                                delay(2000)
                                isTransferring = false
                                // Close screen after success
                                delay(500)
                                onNavigateBack()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimens.buttonHeight)
                        .graphicsLayer(scaleX = buttonScale, scaleY = buttonScale),
                    shape = RoundedCornerShape(dimens.buttonCorner),
                    enabled = !isTransferring
                ) {
                    if (isTransferring) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(dimens.iconSize * 0.8f),
                            color = colors.onPrimary
                        )
                        Spacer(Modifier.width(dimens.spacingSmall))
                    }
                    Text(
                        if (isTransferring) "جاري التحويل..." else "تحويل",
                        style = typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                
                Spacer(Modifier.height(dimens.spacingMedium)) // Reduced bottom spacing
            }
        }
    }
} 
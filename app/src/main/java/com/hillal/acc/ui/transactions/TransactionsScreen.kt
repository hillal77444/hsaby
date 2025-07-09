package com.hillal.acc.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.shadow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar
import com.hillal.acc.data.model.Transaction
import com.hillal.acc.data.model.Account
import com.hillal.acc.ui.common.AccountPickerField
import androidx.compose.ui.geometry.Offset
import com.hillal.acc.R
import com.hillal.acc.ui.common.TransactionCard
import com.hillal.acc.ui.common.ActionCircleButton
import com.hillal.acc.ui.common.getDateString
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

// Extension functions for Transaction
fun Transaction.getAccountName(accountMap: Map<Long, Account>? = null): String? {
    return accountMap?.get(this.getAccountId())?.getName()
}

fun Transaction.getPhoneNumber(accountMap: Map<Long, Account>? = null): String? {
    return accountMap?.get(this.getAccountId())?.getPhoneNumber()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    transactions: List<Transaction>,
    onAddClick: () -> Unit,
    onDelete: (Transaction) -> Unit,
    onEdit: (Transaction) -> Unit,
    onWhatsApp: (Transaction) -> Unit,
    onSms: (Transaction) -> Unit, // <-- Fix: accept Transaction parameter
    modifier: Modifier = Modifier,
    accounts: List<Account> = emptyList(),
    balancesMap: Map<Long, Map<String, Double>> = emptyMap(), // أضف هذا
    selectedAccount: Account? = null, // أضف هذا
    onAccountFilter: (Account?) -> Unit = {},
    startDate: Long? = null,
    endDate: Long? = null,
    onDateFilter: (Long?, Long?) -> Unit = { _, _ -> },
    searchQuery: String = "",
    onSearch: (String) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale("ar")) }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    // مقاسات نسبية
    val cardCorner = screenWidth * 0.045f
    val cardPadding = screenWidth * 0.025f
    val buttonHeight = screenHeight * 0.045f
    val buttonMinWidth = screenWidth * 0.15f
    val iconSize = screenWidth * 0.07f
    val textFieldFont = (screenWidth.value * 0.035f).sp
    val statCardCorner = screenWidth * 0.035f
    val statIconSize = screenWidth * 0.06f
    val statFont = (screenWidth.value * 0.038f).sp
    val statLabelFont = (screenWidth.value * 0.028f).sp
    val statCardPaddingV = screenHeight * 0.012f
    val statCardPaddingH = screenWidth * 0.02f

    var isSearchActive by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FB))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = screenHeight * 0.09f, start = 8.dp, end = 8.dp)
        ) {
            // البطاقة العلوية الجديدة
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = cardPadding, bottom = cardPadding),
                shape = RoundedCornerShape(cardCorner),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(cardPadding)) {
                    // السطر الأول: اختيار الحساب أو البحث
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isSearchActive) {
                            // اختيار الحساب ممتد للطرف
                            Box(Modifier.weight(1f)) {
                                AccountPickerField(
                                    label = "الحساب",
                                    accounts = accounts,
                                    transactions = transactions,
                                    balancesMap = balancesMap,
                                    selectedAccount = selectedAccount,
                                    onAccountSelected = onAccountFilter,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            IconButton(
                                onClick = { isSearchActive = true },
                                modifier = Modifier.size(iconSize * 1.1f)
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "بحث", modifier = Modifier.size(iconSize), tint = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = onSearch,
                                label = { Text("بحث في الوصف", fontSize = textFieldFont) },
                                modifier = Modifier.weight(1f),
                                textStyle = LocalTextStyle.current.copy(fontSize = textFieldFont),
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { isSearchActive = false }) {
                                        Icon(Icons.Default.Close, contentDescription = "إغلاق البحث", modifier = Modifier.size(iconSize), tint = Color.Gray)
                                    }
                                },
                                shape = RoundedCornerShape(cardCorner)
                            )
                        }
                    }
                    // السطر الثاني: التواريخ
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = cardPadding),
                        horizontalArrangement = Arrangement.spacedBy(cardPadding),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showStartPicker = true },
                            modifier = Modifier.height(buttonHeight).weight(1f)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(iconSize))
                            Spacer(Modifier.width(cardPadding / 2))
                            Text(startDate?.let { dateFormat.format(Date(it)) } ?: "من", fontSize = textFieldFont)
                        }
                        OutlinedButton(
                            onClick = { showEndPicker = true },
                            modifier = Modifier.height(buttonHeight).weight(1f)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(iconSize))
                            Spacer(Modifier.width(cardPadding / 2))
                            Text(endDate?.let { dateFormat.format(Date(it)) } ?: "إلى", fontSize = textFieldFont)
                        }
                    }
                }
            }
            if (showStartPicker) {
                DatePickerDialog(
                    initialDate = startDate ?: System.currentTimeMillis(),
                    onDateSelected = { date ->
                        onDateFilter(date, endDate)
                        showStartPicker = false
                    },
                    onDismissRequest = { showStartPicker = false }
                )
            }
            if (showEndPicker) {
                DatePickerDialog(
                    initialDate = endDate ?: System.currentTimeMillis(),
                    onDateSelected = { date ->
                        onDateFilter(startDate, date)
                        showEndPicker = false
                    },
                    onDismissRequest = { showEndPicker = false }
                )
            }
            Divider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFE0E0E0))
            // إحصائيات بشكل بطاقات صغيرة مع أيقونات
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = cardPadding / 2, vertical = cardPadding / 2),
                horizontalArrangement = Arrangement.spacedBy(cardPadding)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(statCardCorner),
                    elevation = CardDefaults.cardElevation(1.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF7FE))
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = statCardPaddingV, horizontal = statCardPaddingH),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF1976D2), modifier = Modifier.size(statIconSize))
                        Spacer(Modifier.width(cardPadding / 2))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(transactions.size.toString(), color = Color(0xFF1976D2), fontSize = statFont, fontWeight = FontWeight.Bold)
                            Text("إجمالي القيود", color = Color(0xFF666666), fontSize = statLabelFont)
                        }
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(statCardCorner),
                    elevation = CardDefaults.cardElevation(1.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F9F1))
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = statCardPaddingV, horizontal = statCardPaddingH),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF43A047), modifier = Modifier.size(statIconSize))
                        Spacer(Modifier.width(cardPadding / 2))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(transactions.sumOf { it.getAmount() }.toString(), color = Color(0xFF43A047), fontSize = statFont, fontWeight = FontWeight.Bold)
                            Text("إجمالي المبالغ", color = Color(0xFF666666), fontSize = statLabelFont)
                        }
                    }
                }
            }
            Divider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFE0E0E0))
            // قائمة المعاملات
            if (transactions.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("لا توجد معاملات للفلاتر المختارة", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                Column(Modifier.fillMaxWidth()) {
                    transactions.forEachIndexed { idx, transaction ->
                        TransactionCard(
                            transaction = transaction,
                            accounts = accounts,
                            onDelete = { onDelete(transaction) },
                            onEdit = { onEdit(transaction) },
                            onWhatsApp = { onWhatsApp(transaction) },
                            onSms = { onSms(transaction) }, // <-- Fix: pass transaction
                            modifier = Modifier
                                .padding(bottom = 6.dp)
                                .height(screenHeight * 0.18f),
                            searchQuery = searchQuery // تمرير البحث
                        )
                        if (idx < transactions.lastIndex) {
                            Divider(modifier = Modifier.padding(horizontal = 8.dp), color = Color(0xFFE0E0E0))
                        }
                    }
                    Spacer(modifier = Modifier.height(screenHeight * 0.09f))
                }
            }
        }
        // زر إضافة عائم (FAB)
        FloatingActionButton(
            onClick = onAddClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
                .size(56.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "إضافة معاملة")
        }
    }
}

// DatePickerDialog Composable (بسيط)
@Composable
fun DatePickerDialog(initialDate: Long, onDateSelected: (Long) -> Unit, onDismissRequest: () -> Unit) {
    val calendar = remember { Calendar.getInstance().apply { timeInMillis = initialDate } }
    val context = LocalContext.current
    val datePickerDialog = remember {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                onDateSelected(calendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }
    LaunchedEffect(Unit) {
        datePickerDialog.setOnDismissListener { onDismissRequest() } // <-- Fix: no parameter
        datePickerDialog.show()
    }
} 
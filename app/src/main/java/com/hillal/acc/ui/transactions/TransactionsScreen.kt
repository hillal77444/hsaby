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
    val screenHeight = configuration.screenHeightDp.dp
    val cardHeight = screenHeight * 0.15f // 15% من ارتفاع الشاشة
    val bottomSpacer = screenHeight * 0.09f // 9% من ارتفاع الشاشة
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FB))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = bottomSpacer, start = 8.dp, end = 8.dp)
        ) {
            // واجهة الفلاتر
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                shape = RoundedCornerShape(10.dp),
                elevation = CardDefaults.cardElevation(1.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AccountPickerField(
                            label = "الحساب",
                            accounts = accounts,
                            transactions = transactions,
                            balancesMap = balancesMap,
                            selectedAccount = selectedAccount,
                            onAccountSelected = onAccountFilter,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = { showStartPicker = true },
                            modifier = Modifier.height(40.dp).defaultMinSize(minWidth = 60.dp)
                        ) {
                            Text(startDate?.let { dateFormat.format(Date(it)) } ?: "من", fontSize = 14.sp)
                        }
                        OutlinedButton(
                            onClick = { showEndPicker = true },
                            modifier = Modifier.height(40.dp).defaultMinSize(minWidth = 60.dp)
                        ) {
                            Text(endDate?.let { dateFormat.format(Date(it)) } ?: "إلى", fontSize = 14.sp)
                        }
                    }
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearch,
                        label = { Text("بحث في الوصف", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
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
            // إحصائيات
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(0.5.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F9F1))
                ) {
                    Column(Modifier.fillMaxSize().padding(vertical = 6.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(transactions.size.toString(), color = MaterialTheme.colorScheme.primary, fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Text("إجمالي القيود", color = Color(0xFF666666), fontSize = 12.sp)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(0.5.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3E8FD))
                ) {
                    Column(Modifier.fillMaxSize().padding(vertical = 6.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(transactions.sumOf { it.getAmount() }.toString(), color = MaterialTheme.colorScheme.primary, fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Text("إجمالي المبالغ", color = Color(0xFF666666), fontSize = 12.sp)
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
                                .height(cardHeight)
                        )
                        if (idx < transactions.lastIndex) {
                            Divider(modifier = Modifier.padding(horizontal = 8.dp), color = Color(0xFFE0E0E0))
                        }
                    }
                    Spacer(modifier = Modifier.height(bottomSpacer))
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
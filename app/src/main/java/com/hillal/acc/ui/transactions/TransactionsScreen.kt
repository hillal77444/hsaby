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
    onSms: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
    accounts: List<Account> = emptyList(),
    onAccountFilter: (Account?) -> Unit = {},
    startDate: Long? = null,
    endDate: Long? = null,
    onDateFilter: (Long?, Long?) -> Unit = { _, _ -> },
    searchQuery: String = "",
    onSearch: (String) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 80.dp) // مساحة للزر العائم
        ) {
            // خلفية علوية بتدرج لوني عصري
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                            )
                        )
                    )
            )
            // دائرة الشعار مع Glow
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .align(Alignment.CenterHorizontally)
                    .offset(y = (-35).dp)
                    .shadow(16.dp, CircleShape, clip = false)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 12.dp,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_transactions),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(16.dp).fillMaxSize()
                    )
                }
            }
            // العنوان تحت الشعار
            Text(
                text = "عرض وإدارة جميع المعاملات المالية",
                color = Color.White,
                fontSize = 15.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .offset(y = (-20).dp)
            )
            // واجهة الفلاتر
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AccountPickerField(
                            label = "الحساب",
                            accounts = accounts,
                            transactions = transactions,
                            balancesMap = emptyMap(),
                            selectedAccount = null,
                            onAccountSelected = onAccountFilter,
                            modifier = Modifier.weight(1f)
                        )
                        val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale("ar")) }
                        OutlinedButton(onClick = {
                            // يمكنك استدعاء DatePicker هنا
                        }) {
                            Text(startDate?.let { dateFormat.format(Date(it)) } ?: "من")
                        }
                        OutlinedButton(onClick = {
                            // يمكنك استدعاء DatePicker هنا
                        }) {
                            Text(endDate?.let { dateFormat.format(Date(it)) } ?: "إلى")
                        }
                    }
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearch,
                        label = { Text("بحث في الوصف") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            }
            // إحصائيات
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F9F1))
                ) {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(transactions.size.toString(), color = MaterialTheme.colorScheme.primary, fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Text("إجمالي القيود", color = Color(0xFF666666), fontSize = 12.sp)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3E8FD))
                ) {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(transactions.sumOf { it.getAmount() }.toString(), color = MaterialTheme.colorScheme.primary, fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Text("إجمالي المبالغ", color = Color(0xFF666666), fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            // قائمة المعاملات
            if (transactions.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("لا توجد معاملات للفلاتر المختارة", color = Color.Gray)
                }
            } else {
                Column(Modifier.fillMaxWidth()) {
                    transactions.forEach { transaction ->
                        TransactionCard(
                            transaction = transaction,
                            accounts = accounts,
                            onDelete = { onDelete(transaction) },
                            onEdit = { onEdit(transaction) },
                            onWhatsApp = { onWhatsApp(transaction) },
                            onSms = { onSms(transaction) },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
        // زر إضافة عائم (FAB)
        FloatingActionButton(
            onClick = onAddClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 24.dp)
                .size(64.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "إضافة معاملة")
        }
    }
} 
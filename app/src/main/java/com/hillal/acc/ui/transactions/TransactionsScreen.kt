package com.hillal.acc.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerClick
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.hillal.acc.R
import com.hillal.acc.data.model.Transaction
import androidx.compose.ui.graphics.Offset
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SimpleDateFormat
import androidx.compose.ui.date.DatePickerDialog
import androidx.compose.ui.date.Calendar
import androidx.compose.ui.date.Date
import androidx.compose.ui.state.mutableStateOf
import androidx.compose.ui.date.remember
import androidx.compose.ui.date.Locale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.animateItemPlacement

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
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        val maxHeight = maxHeight
        val maxWidth = maxWidth
        val density = LocalDensity.current
        val headerHeight = maxHeight * 0.22f
        val fabSize = 64.dp
        val fabMargin = 24.dp
        val statsCardHeight = maxHeight * 0.09f
        val filterCardHeight = maxHeight * 0.18f
        val listPaddingBottom = fabSize + fabMargin + 16.dp

        // خلفية علوية بتدرج لوني عصري
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
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
                .size(headerHeight * 0.45f)
                .align(Alignment.TopCenter)
                .offset(y = headerHeight * 0.22f)
                .shadow(16.dp, CircleShape, clip = false)
                .zIndex(2f)
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
                .align(Alignment.TopCenter)
                .offset(y = headerHeight * 0.7f)
                .zIndex(2f)
        )

        // محتوى الشاشة (فلترة، إحصائيات، قائمة)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = headerHeight * 0.95f, bottom = listPaddingBottom)
                .zIndex(1f)
        ) {
            // واجهة الفلاتر في الأعلى
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // فلتر الحساب
                    var expanded by remember { mutableStateOf(false) }
                    var selectedAccount by remember { mutableStateOf<Account?>(null) }
                    // استبدل قائمة الحسابات المنسدلة التقليدية بـ AccountPickerField:
                    AccountPickerField(
                        label = "الحساب",
                        accounts = accounts,
                        transactions = transactions,
                        balancesMap = emptyMap(), // إذا كان لديك خريطة أرصدة مررها هنا
                        selectedAccount = selectedAccount,
                        onAccountSelected = { account ->
                            selectedAccount = account
                            onAccountFilter(account)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    // فلتر التاريخ
                    var showStartPicker by remember { mutableStateOf(false) }
                    var showEndPicker by remember { mutableStateOf(false) }
                    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale("ar")) }
                    OutlinedButton(onClick = { showStartPicker = true }) {
                        Text(startDate?.let { dateFormat.format(Date(it)) } ?: "من")
                    }
                    OutlinedButton(onClick = { showEndPicker = true }) {
                        Text(endDate?.let { dateFormat.format(Date(it)) } ?: "إلى")
                    }
                    if (showStartPicker) {
                        DatePickerDialog(
                            LocalContext.current,
                            { _, year, month, dayOfMonth ->
                                val cal = Calendar.getInstance()
                                cal.set(year, month, dayOfMonth)
                                onDateFilter(cal.timeInMillis, endDate)
                                showStartPicker = false
                            },
                            Calendar.getInstance().get(Calendar.YEAR),
                            Calendar.getInstance().get(Calendar.MONTH),
                            Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                    if (showEndPicker) {
                        DatePickerDialog(
                            LocalContext.current,
                            { _, year, month, dayOfMonth ->
                                val cal = Calendar.getInstance()
                                cal.set(year, month, dayOfMonth)
                                onDateFilter(startDate, cal.timeInMillis)
                                showEndPicker = false
                            },
                            Calendar.getInstance().get(Calendar.YEAR),
                            Calendar.getInstance().get(Calendar.MONTH),
                            Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                }
                // مربع البحث
                var search by remember { mutableStateOf(searchQuery) }
                OutlinedTextField(
                    value = search,
                    onValueChange = {
                        search = it
                        onSearch(it)
                    },
                    label = { Text("بحث في الوصف") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            // احسب الإحصائيات من قائمة المعاملات الحالية
            val totalCount = transactions.size
            val totalAmount = transactions.sumOf { it.getAmount() }

            // بطاقتا الإحصائيات تظهران دائمًا
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(statsCardHeight)
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
                        Text(totalCount.toString(), color = MaterialTheme.colorScheme.primary, fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
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
                        Text(totalAmount.toString(), color = MaterialTheme.colorScheme.primary, fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Text("إجمالي المبالغ", color = Color(0xFF666666), fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            // البطاقات الإضافية (فلترة، إحصائيات، ...)
            val isSearching = search.isNotEmpty()

            if (!isSearching) {
                // البطاقات الإضافية (فلترة، إحصائيات، ...)
                // واجهة الفلاتر في الأعلى
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // فلتر الحساب
                        var expanded by remember { mutableStateOf(false) }
                        var selectedAccount by remember { mutableStateOf<Account?>(null) }
                        // استبدل قائمة الحسابات المنسدلة التقليدية بـ AccountPickerField:
                        AccountPickerField(
                            label = "الحساب",
                            accounts = accounts,
                            transactions = transactions,
                            balancesMap = emptyMap(), // إذا كان لديك خريطة أرصدة مررها هنا
                            selectedAccount = selectedAccount,
                            onAccountSelected = { account ->
                                selectedAccount = account
                                onAccountFilter(account)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        // فلتر التاريخ
                        var showStartPicker by remember { mutableStateOf(false) }
                        var showEndPicker by remember { mutableStateOf(false) }
                        val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale("ar")) }
                        OutlinedButton(onClick = { showStartPicker = true }) {
                            Text(startDate?.let { dateFormat.format(Date(it)) } ?: "من")
                        }
                        OutlinedButton(onClick = { showEndPicker = true }) {
                            Text(endDate?.let { dateFormat.format(Date(it)) } ?: "إلى")
                        }
                        if (showStartPicker) {
                            DatePickerDialog(
                                LocalContext.current,
                                { _, year, month, dayOfMonth ->
                                    val cal = Calendar.getInstance()
                                    cal.set(year, month, dayOfMonth)
                                    onDateFilter(cal.timeInMillis, endDate)
                                    showStartPicker = false
                                },
                                Calendar.getInstance().get(Calendar.YEAR),
                                Calendar.getInstance().get(Calendar.MONTH),
                                Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        if (showEndPicker) {
                            DatePickerDialog(
                                LocalContext.current,
                                { _, year, month, dayOfMonth ->
                                    val cal = Calendar.getInstance()
                                    cal.set(year, month, dayOfMonth)
                                    onDateFilter(startDate, cal.timeInMillis)
                                    showEndPicker = false
                                },
                                Calendar.getInstance().get(Calendar.YEAR),
                                Calendar.getInstance().get(Calendar.MONTH),
                                Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                    }
                    // مربع البحث
                    var search by remember { mutableStateOf(searchQuery) }
                    OutlinedTextField(
                        value = search,
                        onValueChange = {
                            search = it
                            onSearch(it)
                        },
                        label = { Text("بحث في الوصف") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                // بطاقتا الإحصائيات
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(statsCardHeight)
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
                            Text("0", color = MaterialTheme.colorScheme.primary, fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
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
                            Text("0", color = MaterialTheme.colorScheme.primary, fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            Text("إجمالي المبالغ", color = Color(0xFF666666), fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            // قائمة المعاملات تظهر دائمًا
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 8.dp, top = 0.dp, start = 8.dp, end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(transactions, key = { _, tx -> tx.getId() }) { index, transaction ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(400)) ,
                        exit = fadeOut(animationSpec = tween(300))
                    ) {
                        TransactionCard(
                            transaction = transaction,
                            onDelete = { onDelete(transaction) },
                            onEdit = { onEdit(transaction) },
                            onWhatsApp = { onWhatsApp(transaction) },
                            onSms = { onSms(transaction) },
                            modifier = Modifier.animateItemPlacement()
                        )
                    }
                }
            }
        }
        // زر إضافة عائم (FAB) مثبت في الأسفل مع margin كافٍ
        FloatingActionButton(
            onClick = onAddClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = fabMargin, bottom = fabMargin)
                .size(fabSize)
                .zIndex(3f),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "إضافة معاملة")
        }
    }
}

@Composable
fun TransactionCard(
    transaction: Transaction,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onWhatsApp: () -> Unit,
    onSms: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDebit = transaction.getType()?.lowercase() == "debit" || transaction.getType() == "عليه"
    val gradient = if (isDebit) {
        Brush.linearGradient(
            colors = listOf(Color(0xFFFF5252), Color(0xFFFF8A80)), // أحمر قوي للمدين
            start = Offset(0f, 0f),
            end = Offset(400f, 400f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFF43EA7D), Color(0xFF1CBF4F)), // أخضر قوي للدائن
            start = Offset(0f, 0f),
            end = Offset(400f, 400f)
        )
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    // المبلغ
                    Text(
                        text = "${transaction.getAmount()} ${transaction.getCurrency()}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color.White
                    )
                    // اسم الحساب + أيقونة معلومات
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = transaction.getAccountName() ?: "--",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(2.dp))
                // التاريخ + أيقونة ساعة
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = transaction.getDateString() ?: "--",
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(2.dp))
                // الوصف
                Text(
                    text = transaction.getDescription() ?: "",
                    fontSize = 15.sp,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                )
                // الأزرار
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ActionCircleButton(icon = Icons.Default.Delete, borderColor = Color.Red, onClick = onDelete)
                    ActionCircleButton(icon = Icons.Default.Edit, borderColor = Color(0xFF1976D2), onClick = onEdit)
                    ActionCircleButton(painter = painterResource(id = R.drawable.ic_sms), borderColor = Color(0xFF1976D2), onClick = onSms)
                    ActionCircleButton(painter = painterResource(id = R.drawable.ic_whatsapp), borderColor = Color(0xFF25D366), onClick = onWhatsApp)
                }
            }
        }
    }
}

@Composable
fun ActionCircleButton(
    icon: ImageVector? = null,
    painter: Painter? = null,
    borderColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .border(2.dp, borderColor, CircleShape)
            .background(Color.White, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = borderColor, modifier = Modifier.size(22.dp))
        } else if (painter != null) {
            Icon(painter = painter, contentDescription = null, tint = borderColor, modifier = Modifier.size(22.dp))
        }
    }
} 
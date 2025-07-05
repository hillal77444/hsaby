package com.hillal.acc.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.clickable

@Composable
fun DashboardHeader(
    userName: String,
    onEditProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // أيقونة أو شعار التطبيق (يمكن تخصيصه لاحقاً)
        Spacer(modifier = Modifier.height(24.dp))
        // عبارة الترحيب
        Text(
            text = "مرحباً، $userName!",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        // بطاقة اسم المستخدم مع زر التعديل
        Card(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onEditProfile) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "تعديل الملف الشخصي"
                    )
                }
            }
        }
    }
}

@Composable
fun StatisticsCard(
    value: String,
    label: String,
    icon: @Composable () -> Unit,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor, contentColor = contentColor),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            icon()
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge)
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun StatisticsRow(
    totalAccounts: String,
    totalCreditors: String,
    totalDebtors: String,
    totalBalance: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatisticsCard(
            value = totalAccounts,
            label = "عدد الحسابات",
            icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
            backgroundColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        StatisticsCard(
            value = totalCreditors,
            label = "إجمالي لكم",
            icon = { Icon(Icons.Default.ArrowUpward, contentDescription = null) },
            backgroundColor = Color(0xFFE8F5E9), // لون أخضر فاتح
            contentColor = Color(0xFF22C55E),
            modifier = Modifier.weight(1f)
        )
        StatisticsCard(
            value = totalDebtors,
            label = "إجمالي عليكم",
            icon = { Icon(Icons.Default.ArrowDownward, contentDescription = null) },
            backgroundColor = Color(0xFFFFEBEE), // لون أحمر فاتح
            contentColor = Color(0xFFF44336),
            modifier = Modifier.weight(1f)
        )
        StatisticsCard(
            value = totalBalance,
            label = "الرصيد الكلي",
            icon = { Icon(Icons.Default.Money, contentDescription = null) },
            backgroundColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun QuickActionsRow(
    onAddTransaction: () -> Unit,
    onAddAccount: () -> Unit,
    onShowReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onAddTransaction,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("إضافة قيد")
        }
        Button(
            onClick = onAddAccount,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))
        ) {
            Icon(Icons.Default.AddCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("إضافة حساب")
        }
        Button(
            onClick = onShowReport,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
        ) {
            Icon(Icons.Default.ReceiptLong, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("كشف الحساب")
        }
    }
}

@Composable
fun ShortcutCard(
    label: String,
    icon: @Composable () -> Unit,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                color = color,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ShortcutsGrid(
    onAccounts: () -> Unit,
    onTransactions: () -> Unit,
    onReports: () -> Unit,
    onDebts: () -> Unit,
    onExchange: () -> Unit,
    onTransfer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shortcuts = listOf(
        Triple("الحسابات", Color(0xFF152FD9), Icons.Default.AccountCircle),
        Triple("المعاملات", Color(0xFFF59E42), Icons.Default.ListAlt),
        Triple("التقارير", Color(0xFF22C55E), Icons.Default.Assessment),
        Triple("متابعة الديون", Color(0xFFF44336), Icons.Default.ArrowDownward),
        Triple("صرف العملات", Color(0xFF1976D2), Icons.Default.CurrencyExchange),
        Triple("تحويل بين الحسابات", Color(0xFF1976D2), Icons.Default.SyncAlt)
    )
    val actions = listOf(onAccounts, onTransactions, onReports, onDebts, onExchange, onTransfer)
    Column(modifier = modifier) {
        for (row in 0 until 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (col in 0 until 3) {
                    val index = row * 3 + col
                    if (index < shortcuts.size) {
                        val (label, color, icon) = shortcuts[index]
                        ShortcutCard(
                            label = label,
                            icon = { Icon(icon, contentDescription = null, tint = color) },
                            color = color,
                            onClick = actions[index],
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun DashboardScreen(
    userName: String? = "اسم المستخدم",
    onEditProfile: () -> Unit = {},
    totalAccounts: String = "0",
    totalCreditors: String = "0",
    totalDebtors: String = "0",
    totalBalance: String = "0 يمني",
    onAddTransaction: () -> Unit = {},
    onAddAccount: () -> Unit = {},
    onShowReport: () -> Unit = {},
    onAccounts: () -> Unit = {},
    onTransactions: () -> Unit = {},
    onReports: () -> Unit = {},
    onDebts: () -> Unit = {},
    onExchange: () -> Unit = {},
    onTransfer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DashboardHeader(userName = userName ?: "اسم المستخدم", onEditProfile = onEditProfile)
        Spacer(modifier = Modifier.height(16.dp))
        StatisticsRow(
            totalAccounts = totalAccounts,
            totalCreditors = totalCreditors,
            totalDebtors = totalDebtors,
            totalBalance = totalBalance
        )
        Spacer(modifier = Modifier.height(16.dp))
        QuickActionsRow(
            onAddTransaction = onAddTransaction,
            onAddAccount = onAddAccount,
            onShowReport = onShowReport
        )
        Spacer(modifier = Modifier.height(16.dp))
        ShortcutsGrid(
            onAccounts = onAccounts,
            onTransactions = onTransactions,
            onReports = onReports,
            onDebts = onDebts,
            onExchange = onExchange,
            onTransfer = onTransfer
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    DashboardScreen()
} 
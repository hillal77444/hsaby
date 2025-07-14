package com.hillal.acc.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hillal.acc.R
import com.hillal.acc.data.model.Transaction
import com.hillal.acc.viewmodel.TransactionViewModel
import java.util.Locale
import com.hillal.acc.ui.theme.LocalAppDimensions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalContext
import com.hillal.acc.util.PreferencesManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import android.content.Context
import androidx.compose.runtime.remember
import java.text.SimpleDateFormat
import java.util.*

fun formatSessionExpiry(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)
        val date = parser.parse(raw)
        val formatter = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH)
        // النص عربي، الأرقام إنجليزي
        "تاريخ انتهاء الجلسة: ${formatter.format(date!!)}"
    } catch (e: Exception) {
        // fallback: أظهر النص كما هو
        "تاريخ انتهاء الجلسة: $raw"
    }
}

fun isSubscriptionExpired(raw: String?): Boolean {
    if (raw.isNullOrBlank()) return false
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)
        val expiry = parser.parse(raw)
        val now = Date()
        expiry != null && now.after(expiry)
    } catch (e: Exception) {
        false
    }
}

fun formatSubscriptionExpiry(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)
        val date = parser.parse(raw)
        val formatter = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH)
        formatter.format(date!!)
    } catch (e: Exception) {
        raw
    }
}

@Composable
fun ReportsScreen(
    transactionViewModel: TransactionViewModel,
    onAccountStatementClick: () -> Unit,
    onAccountsSummaryClick: () -> Unit,
    onCashboxStatementClick: () -> Unit
) {
    val dimens = LocalAppDimensions.current
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val transactions by transactionViewModel.getAllTransactions().observeAsState(emptyList())
    val totalDebit = transactions.filter { it.getType() == "debit" }.sumOf { it.getAmount() }
    val totalCredit = transactions.filter { it.getType() == "credit" }.sumOf { it.getAmount() }
    val netBalance = totalCredit - totalDebit
    val count = transactions.size
    val sum = transactions.sumOf { it.getAmount() }
    val avg = if (count > 0) sum / count else 0.0
    val scrollState = rememberScrollState()

    // إضافة استدعاء sessionExpiry من PreferencesManager
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val sessionExpiry = remember { preferencesManager.getSessionExpiry() }
    val isExpired = isSubscriptionExpired(sessionExpiry)
    val expiryDateStr = formatSubscriptionExpiry(sessionExpiry)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(scrollState)
            .padding(bottom = 56.dp)
    ) {
        // بطاقة الاشتراك المميزة
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isExpired) Color(0xFFFFCDD2) /* أحمر فاتح */ else Color(0xFFB3E5FC) /* أزرق فاتح */
            )
        ) {
            Column(
                Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = "تاريخ انتهاء الاشتراك",
                        tint = if (isExpired) Color.Red else Color(0xFF1976D2),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isExpired) "انتهى الاشتراك: $expiryDateStr" else "تاريخ انتهاء الاشتراك: $expiryDateStr",
                        fontSize = 16.sp,
                        color = if (isExpired) Color.Red else Color(0xFF1976D2),
                        fontWeight = FontWeight.Bold
                    )
                }
                if (isExpired) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "يجب تجديد الاشتراك للاستمتاع بمزايا المزامنة",
                        fontSize = 13.sp,
                        color = Color.Red.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
        // البطاقة العلوية الجذابة
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = dimens.spacingMedium, bottom = dimens.spacingSmall / 2) // تقليل الهوامش
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .height(dimens.cardHeight)
                    .align(Alignment.Center),
                shape = RoundedCornerShape(dimens.cardCorner * 1.2f),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = colors.primary)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = dimens.spacingMedium, vertical = dimens.spacingSmall / 2),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(dimens.iconSize * 2f)
                            .background(colors.onPrimary.copy(alpha = 0.10f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_statement),
                            contentDescription = null,
                            tint = colors.onPrimary,
                            modifier = Modifier.size(dimens.iconSize * 1.1f)
                        )
                    }
                    Spacer(Modifier.width(dimens.spacingMedium))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "التقارير المالية",
                            style = typography.headlineSmall.copy(color = colors.onPrimary, fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "ملخص الحسابات والمعاملات",
                            style = typography.bodyMedium.copy(color = colors.onPrimary.copy(alpha = 0.85f)),
                            maxLines = 1
                        )
                    }
                }
            }
        }
        // بطاقة الأزرار
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.spacingMedium, vertical = dimens.spacingSmall / 2),
            shape = RoundedCornerShape(dimens.cardCorner),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Column(
                Modifier.padding(dimens.spacingSmall / 1.5f)
            ) {
                Text(
                    text = "التقارير المتاحة",
                    style = typography.bodyLarge.copy(color = colors.primary, fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = dimens.spacingSmall / 2)
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimens.spacingSmall / 2)
                ) {
                    Button(
                        onClick = onAccountsSummaryClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.secondary,
                            contentColor = colors.onSecondary
                        ),
                        shape = RoundedCornerShape(dimens.cardCorner)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_summary),
                            contentDescription = null,
                            modifier = Modifier.size(dimens.iconSize * 0.8f)
                        )
                        Spacer(Modifier.width(dimens.spacingSmall / 2))
                        Text(
                            "تقرير الأرصدة",
                            style = typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Button(
                        onClick = onAccountStatementClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary
                        ),
                        shape = RoundedCornerShape(dimens.cardCorner)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_statement),
                            contentDescription = null,
                            modifier = Modifier.size(dimens.iconSize * 0.8f)
                        )
                        Spacer(Modifier.width(dimens.spacingSmall / 2))
                        Text(
                            "كشف الحساب",
                            style = typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
                Spacer(Modifier.height(dimens.spacingSmall / 2))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimens.spacingSmall / 2)
                ) {
                    Button(
                        onClick = onCashboxStatementClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primaryContainer,
                            contentColor = colors.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(dimens.cardCorner)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_wallet),
                            contentDescription = null,
                            modifier = Modifier.size(dimens.iconSize * 0.8f)
                        )
                        Spacer(Modifier.width(dimens.spacingSmall / 2))
                        Text(
                            "كشف الصندوق",
                            style = typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
        // بطاقة الإحصائيات المحسنة
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.spacingMedium, vertical = dimens.spacingSmall / 2),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Column(
                Modifier.padding(dimens.spacingMedium / 1.5f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_summary),
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(dimens.iconSize)
                    )
                    Spacer(Modifier.width(dimens.spacingSmall))
                    Text(
                        text = "إحصائيات عامة",
                        fontSize = dimens.bodyFont,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                }
                Spacer(Modifier.height(dimens.spacingSmall))
                // الإحصائيات الأولى
                Row(
                    Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        label = "إجمالي المدينين", 
                        value = totalDebit,
                        color = colors.error
                    )
                    StatItem(
                        label = "إجمالي الدائنين", 
                        value = totalCredit,
                        color = colors.secondary
                    )
                    StatItem(
                        label = "الرصيد", 
                        value = netBalance,
                        color = colors.primary
                    )
                }
                Spacer(Modifier.height(dimens.spacingSmall))
                // الإحصائيات الثانية
                Row(
                    Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        label = "عدد المعاملات", 
                        value = count.toDouble(), 
                        isInt = true,
                        color = colors.tertiary
                    )
                    StatItem(
                        label = "متوسط المعاملة", 
                        value = avg,
                        color = colors.secondary
                    )
                }
            }
        }
        // بطاقة إضافية للمعلومات
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.spacingMedium, vertical = dimens.spacingSmall / 2),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(
                containerColor = colors.primary.copy(alpha = 0.04f)
            )
        ) {
            Column(Modifier.padding(dimens.spacingMedium / 1.5f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_info),
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(dimens.iconSize)
                    )
                    Spacer(Modifier.width(dimens.spacingSmall))
                    Text(
                        text = "يمكنك الوصول إلى جميع التقارير المالية من خلال الأزرار أعلاه",
                        fontSize = dimens.bodyFont * 0.9f,
                        color = colors.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun StatItem(
    label: String, 
    value: Double, 
    isInt: Boolean = false,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val dimens = LocalAppDimensions.current
    val colors = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = dimens.spacingSmall / 2)
    ) {
        Text(
            text = if (isInt) value.toInt().toString() else String.format(Locale.ENGLISH, "%.2f", value),
            fontSize = dimens.statFont * 0.9f,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = dimens.statLabelFont * 0.85f,
            color = colors.onSurface.copy(alpha = 0.65f),
            modifier = Modifier.padding(top = dimens.spacingSmall / 2)
        )
    }
} 
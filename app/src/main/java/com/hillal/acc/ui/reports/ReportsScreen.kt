package com.hillal.acc.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ListAlt
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
import com.hillal.acc.ui.theme.LocalResponsiveDimensions

@Composable
fun ReportsScreen(
    transactionViewModel: TransactionViewModel,
    onAccountStatementClick: () -> Unit,
    onAccountsSummaryClick: () -> Unit,
    onCashboxStatementClick: () -> Unit
) {
    val dimensions = LocalResponsiveDimensions.current
    val transactions by transactionViewModel.getAllTransactions().observeAsState(emptyList())
    val totalDebit = transactions.filter { it.getType() == "debit" }.sumOf { it.getAmount() }
    val totalCredit = transactions.filter { it.getType() == "credit" }.sumOf { it.getAmount() }
    val netBalance = totalCredit - totalDebit
    val count = transactions.size
    val sum = transactions.sumOf { it.getAmount() }
    val avg = if (count > 0) sum / count else 0.0
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
    ) {
        // رأس الصفحة
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensions.cardHeight)
                .background(MaterialTheme.colorScheme.primary)
        ) {}
        Card(
            modifier = Modifier
                .size(dimensions.cardHeight * 0.57f)
                .offset(y = (-dimensions.cardHeight * 0.29f))
                .align(Alignment.CenterHorizontally),
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.cardElevation(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_statement),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(dimensions.iconSize * 2)
                )
            }
        }
        Text(
            text = "التقارير المالية",
            fontSize = dimensions.titleFont,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
            text = "ملخص الحسابات والمعاملات المالية",
            fontSize = dimensions.bodyFont,
            color = Color(0xFF666666),
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = dimensions.spacingSmall)
        )
        // بطاقة الأزرار
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensions.spacingLarge, vertical = dimensions.spacingSmall),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(6.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.padding(dimensions.spacingMedium)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)) {
                    Button(
                        onClick = onAccountsSummaryClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009688), contentColor = Color.White),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_summary),
                            contentDescription = null,
                            modifier = Modifier.size(dimensions.iconSize)
                        )
                        Spacer(Modifier.width(dimensions.spacingSmall))
                        Text("تقرير ارصدة الحسابات", fontSize = dimensions.bodyFont)
                    }
                    Button(
                        onClick = onAccountStatementClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5), contentColor = Color.White),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_statement),
                            contentDescription = null,
                            modifier = Modifier.size(dimensions.iconSize)
                        )
                        Spacer(Modifier.width(dimensions.spacingSmall))
                        Text("كشف الحساب", fontSize = dimensions.bodyFont)
                    }
                }
                Spacer(Modifier.height(dimensions.spacingSmall))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)) {
                    Button(
                        onClick = onCashboxStatementClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2), contentColor = Color.White),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_wallet),
                            contentDescription = null,
                            modifier = Modifier.size(dimensions.iconSize)
                        )
                        Spacer(Modifier.width(dimensions.spacingSmall))
                        Text("تقرير ارصدة الصناديق", fontSize = dimensions.bodyFont)
                    }
                }
            }
        }
        // بطاقة الإحصائيات
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensions.spacingLarge, vertical = dimensions.spacingSmall),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(3.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.padding(dimensions.spacingMedium)) {
                Text(
                    text = "اجمالي الارصده لجميع العملات",
                    fontSize = dimensions.bodyFont,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(dimensions.spacingSmall))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatItem(label = "إجمالي المدينين", value = totalDebit)
                    StatItem(label = "إجمالي الدائنين", value = totalCredit)
                    StatItem(label = "الرصيد", value = netBalance)
                }
                Spacer(Modifier.height(dimensions.spacingSmall))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatItem(label = "عدد المعاملات", value = count.toDouble(), isInt = true)
                    StatItem(label = "متوسط المعاملة", value = avg)
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: Double, isInt: Boolean = false) {
    val dimensions = LocalResponsiveDimensions.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (isInt) value.toInt().toString() else String.format(Locale.ENGLISH, "%.2f", value),
            fontWeight = FontWeight.Bold,
            fontSize = dimensions.statFont,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = dimensions.statLabelFont,
            color = Color(0xFF666666)
        )
    }
} 
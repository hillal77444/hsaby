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
        // رأس الصفحة المحسن
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensions.cardHeight * 0.18f) // تقليل الارتفاع بشكل أكبر
                .background(
                    MaterialTheme.colorScheme.primary
                )
        ) {
            // نمط خلفية جميل
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color(0xFF1976D2).copy(alpha = 0.1f)
                    )
            )
        }
        
        // البطاقة العلوية المحسنة
        Card(
            modifier = Modifier
                .size(dimensions.cardHeight * 0.25f) // تقليل الحجم
                .offset(y = (-dimensions.cardHeight * 0.02f)) // إزاحة رأسية بسيطة جداً
                .align(Alignment.CenterHorizontally),
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.cardElevation(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Box(
                contentAlignment = Alignment.Center, 
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                    )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_statement),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(dimensions.iconSize * 1.5f) // تقليل حجم الأيقونة
                )
            }
        }
        
        // النصوص المحسنة
        Column(
            modifier = Modifier.padding(horizontal = dimensions.spacingLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "التقارير المالية",
                fontSize = dimensions.titleFont * 0.9f, // تقليل حجم الخط
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = dimensions.spacingSmall)
            )
            Text(
                text = "ملخص الحسابات والمعاملات المالية",
                fontSize = dimensions.bodyFont * 0.85f,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(bottom = dimensions.spacingMedium)
            )
        }
        
        // بطاقة الأزرار المحسنة
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensions.spacingLarge, vertical = dimensions.spacingSmall),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                Modifier.padding(dimensions.spacingMedium)
            ) {
                Text(
                    text = "التقارير المتاحة",
                    fontSize = dimensions.bodyFont,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = dimensions.spacingSmall)
                )
                
                Row(
                    Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
                ) {
                    Button(
                        onClick = onAccountsSummaryClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF009688), 
                            contentColor = Color.White
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_summary),
                            contentDescription = null,
                            modifier = Modifier.size(dimensions.iconSize * 0.8f)
                        )
                        Spacer(Modifier.width(dimensions.spacingSmall))
                        Text(
                            "تقرير ارصدة الحسابات", 
                            fontSize = dimensions.bodyFont * 0.9f
                        )
                    }
                    Button(
                        onClick = onAccountStatementClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3F51B5), 
                            contentColor = Color.White
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_statement),
                            contentDescription = null,
                            modifier = Modifier.size(dimensions.iconSize * 0.8f)
                        )
                        Spacer(Modifier.width(dimensions.spacingSmall))
                        Text(
                            "كشف الحساب", 
                            fontSize = dimensions.bodyFont * 0.9f
                        )
                    }
                }
                Spacer(Modifier.height(dimensions.spacingSmall))
                Row(
                    Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
                ) {
                    Button(
                        onClick = onCashboxStatementClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1976D2), 
                            contentColor = Color.White
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_wallet),
                            contentDescription = null,
                            modifier = Modifier.size(dimensions.iconSize * 0.8f)
                        )
                        Spacer(Modifier.width(dimensions.spacingSmall))
                        Text(
                            "تقرير ارصدة الصناديق", 
                            fontSize = dimensions.bodyFont * 0.9f
                        )
                    }
                }
            }
        }
        
        // بطاقة الإحصائيات المحسنة
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensions.spacingLarge, vertical = dimensions.spacingSmall),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(6.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                Modifier.padding(dimensions.spacingMedium)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_summary),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(dimensions.iconSize)
                    )
                    Spacer(Modifier.width(dimensions.spacingSmall))
                    Text(
                        text = "إحصائيات عامة",
                        fontSize = dimensions.bodyFont,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(Modifier.height(dimensions.spacingMedium))
                
                // الإحصائيات الأولى
                Row(
                    Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        label = "إجمالي المدينين", 
                        value = totalDebit,
                        color = Color(0xFFE57373)
                    )
                    StatItem(
                        label = "إجمالي الدائنين", 
                        value = totalCredit,
                        color = Color(0xFF81C784)
                    )
                    StatItem(
                        label = "الرصيد", 
                        value = netBalance,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(Modifier.height(dimensions.spacingMedium))
                
                // الإحصائيات الثانية
                Row(
                    Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        label = "عدد المعاملات", 
                        value = count.toDouble(), 
                        isInt = true,
                        color = Color(0xFF64B5F6)
                    )
                    StatItem(
                        label = "متوسط المعاملة", 
                        value = avg,
                        color = Color(0xFFFFB74D)
                    )
                }
            }
        }
        
        // بطاقة إضافية للمعلومات
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensions.spacingLarge, vertical = dimensions.spacingSmall),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
            )
        ) {
            Row(
                modifier = Modifier.padding(dimensions.spacingMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_info),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(dimensions.iconSize)
                )
                Spacer(Modifier.width(dimensions.spacingSmall))
                Text(
                    text = "يمكنك الوصول إلى جميع التقارير المالية من خلال الأزرار أعلاه",
                    fontSize = dimensions.bodyFont * 0.9f,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
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
    val dimensions = LocalResponsiveDimensions.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = dimensions.spacingSmall)
    ) {
        Text(
            text = if (isInt) value.toInt().toString() else String.format(Locale.ENGLISH, "%.2f", value),
            fontWeight = FontWeight.Bold,
            fontSize = dimensions.statFont * 0.9f,
            color = color
        )
        Text(
            text = label,
            fontSize = dimensions.statLabelFont * 0.85f,
            color = Color(0xFF666666),
            modifier = Modifier.padding(top = dimensions.spacingSmall)
        )
    }
} 
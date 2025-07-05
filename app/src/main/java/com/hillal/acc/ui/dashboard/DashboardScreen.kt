package com.hillal.acc.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hillal.acc.R
import com.hillal.acc.data.model.ServerAppUpdateInfo
import com.hillal.acc.ui.dashboard.DashboardViewModel
import kotlin.math.roundToInt
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    userName: String,
    onEditProfile: () -> Unit,
    onAddAccount: () -> Unit,
    onAddTransaction: () -> Unit,
    onReport: () -> Unit,
    onAccounts: () -> Unit,
    onTransactions: () -> Unit,
    onReports: () -> Unit,
    onDebts: () -> Unit,
    onTransfer: () -> Unit,
    onExchange: () -> Unit
) {
    val totalDebtors by viewModel.totalDebtors.observeAsState()
    val totalCreditors by viewModel.totalCreditors.observeAsState()
    val accounts by viewModel.accounts.observeAsState()
    val netBalance by viewModel.netBalance.observeAsState()
    val scrollState = rememberScrollState()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7F7))
    ) {
        val maxW = maxWidth
        val maxH = maxHeight
        // نسب ديناميكية
        val padding = maxW * 0.03f
        val cardHeight = maxH * 0.09f
        val statIconSize = maxW * 0.07f
        val statFontSize = maxW.value * 0.045f
        val statLabelFontSize = maxW.value * 0.032f
        val headerHeight = maxH * 0.19f
        val logoSize = maxW * 0.18f
        val logoOffset = logoSize * 0.45f
        val welcomeFontSize = maxW.value * 0.045f
        val userCardFontSize = maxW.value * 0.04f
        val userCardPadding = maxW * 0.05f
        val actionButtonHeight = maxH * 0.07f
        val gridCardHeight = maxH * 0.13f
        val gridIconSize = maxW * 0.07f
        val gridFontSize = maxW.value * 0.038f
        val gridSubFontSize = maxW.value * 0.03f
        val verticalSpace = maxH * 0.015f

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // رأس الشاشة
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
                    .background(Color(0xFF2196F3)),
                contentAlignment = Alignment.TopCenter
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = padding, start = padding, end = padding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_menu),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(statIconSize)
                    )
                    Text(
                        text = "مالي برو",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        fontSize = welcomeFontSize.sp
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_sync),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(statIconSize)
                    )
                }
                // شعار التطبيق
                Box(
                    modifier = Modifier
                        .size(logoSize)
                        .align(Alignment.BottomCenter)
                        .offset(y = logoOffset),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher),
                        contentDescription = "Logo",
                        modifier = Modifier.size(logoSize * 0.7f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(logoOffset * 0.7f))
            // عبارة ترحيبية
            Text(
                text = "مرحباً، اسم المستخدم!",
                color = Color(0xFF2196F3),
                style = MaterialTheme.typography.titleMedium,
                fontSize = welcomeFontSize.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            // بطاقة المستخدم مع زر التعديل
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = padding, vertical = verticalSpace),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = userCardPadding, vertical = userCardPadding * 0.5f)
                ) {
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF2196F3),
                        fontSize = userCardFontSize.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onEditProfile) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_edit),
                            contentDescription = "تعديل الملف",
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(statIconSize)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(verticalSpace))
            // بطاقات الإحصائيات الأربع
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = padding),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatCardOld(
                    icon = R.drawable.ic_money,
                    value = "${netBalance?.toInt() ?: 0} يمني",
                    label = "الرصيد الأكبر",
                    subLabel = "الرصيد الأكبر",
                    color = Color(0xFFB2F2E5),
                    iconTint = Color(0xFF009688),
                    valueColor = Color(0xFF009688),
                    iconSize = statIconSize,
                    valueFontSize = statFontSize.sp,
                    labelFontSize = statLabelFontSize.sp,
                    modifier = Modifier.weight(1f).height(cardHeight)
                )
                StatCardOld(
                    icon = R.drawable.ic_arrow_downward,
                    value = "${totalDebtors?.toInt() ?: 0}",
                    label = "إجمالي عليكم",
                    subLabel = "إجمالي عليكم",
                    color = Color(0xFFFFF3E0),
                    iconTint = Color(0xFFFF9800),
                    valueColor = Color(0xFFFF9800),
                    iconSize = statIconSize,
                    valueFontSize = statFontSize.sp,
                    labelFontSize = statLabelFontSize.sp,
                    modifier = Modifier.weight(1f).height(cardHeight)
                )
                StatCardOld(
                    icon = R.drawable.ic_arrow_upward,
                    value = "${totalCreditors?.toInt() ?: 0}",
                    label = "إجمالي لكم",
                    subLabel = "إجمالي لكم",
                    color = Color(0xFFE8F5E9),
                    iconTint = Color(0xFF4CAF50),
                    valueColor = Color(0xFF4CAF50),
                    iconSize = statIconSize,
                    valueFontSize = statFontSize.sp,
                    labelFontSize = statLabelFontSize.sp,
                    modifier = Modifier.weight(1f).height(cardHeight)
                )
                StatCardOld(
                    icon = R.drawable.ic_accounts,
                    value = "${accounts?.size ?: 0}",
                    label = "عدد الحسابات",
                    subLabel = "عدد الحسابات",
                    color = Color(0xFFE3F2FD),
                    iconTint = Color(0xFF1976D2),
                    valueColor = Color(0xFF1976D2),
                    iconSize = statIconSize,
                    valueFontSize = statFontSize.sp,
                    labelFontSize = statLabelFontSize.sp,
                    modifier = Modifier.weight(1f).height(cardHeight)
                )
            }
            Spacer(modifier = Modifier.height(verticalSpace * 1.5f))
            // أزرار الإجراءات الثلاثة
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = padding),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(
                    text = "إضافة قيد",
                    icon = R.drawable.ic_add,
                    height = actionButtonHeight,
                    backgroundColor = Color(0xFF1976D2),
                    fontSize = gridFontSize.sp,
                    iconSize = gridIconSize,
                    onClick = onAddTransaction,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    text = "إضافة حساب",
                    icon = R.drawable.ic_add_account,
                    height = actionButtonHeight,
                    backgroundColor = Color(0xFF388E3C),
                    fontSize = gridFontSize.sp,
                    iconSize = gridIconSize,
                    onClick = onAddAccount,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    text = "كشف الحساب",
                    icon = R.drawable.ic_statement,
                    height = actionButtonHeight,
                    backgroundColor = Color(0xFFFF9800),
                    fontSize = gridFontSize.sp,
                    iconSize = gridIconSize,
                    onClick = onReport,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(verticalSpace * 2))
            // شبكة البطاقات السفلية
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = padding),
                verticalArrangement = Arrangement.spacedBy(verticalSpace)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(verticalSpace)
                ) {
                    GridCardOld(
                        icon = R.drawable.ic_reports,
                        title = "التقارير",
                        subtitle = "عرض التقارير",
                        backgroundColor = Color(0xFFE8F5E9),
                        iconTint = Color(0xFF388E3C),
                        fontSize = gridFontSize.sp,
                        subFontSize = gridSubFontSize.sp,
                        iconSize = gridIconSize,
                        onClick = onReports,
                        modifier = Modifier.weight(1f).height(gridCardHeight)
                    )
                    GridCardOld(
                        icon = R.drawable.ic_transactions,
                        title = "المعاملات",
                        subtitle = "سجل المعاملات",
                        backgroundColor = Color(0xFFE3F2FD),
                        iconTint = Color(0xFF1976D2),
                        fontSize = gridFontSize.sp,
                        subFontSize = gridSubFontSize.sp,
                        iconSize = gridIconSize,
                        onClick = onTransactions,
                        modifier = Modifier.weight(1f).height(gridCardHeight)
                    )
                    GridCardOld(
                        icon = R.drawable.ic_accounts,
                        title = "الحسابات",
                        subtitle = "إدارة الحسابات",
                        backgroundColor = Color(0xFFF3E5F5),
                        iconTint = Color(0xFF8E24AA),
                        fontSize = gridFontSize.sp,
                        subFontSize = gridSubFontSize.sp,
                        iconSize = gridIconSize,
                        onClick = onAccounts,
                        modifier = Modifier.weight(1f).height(gridCardHeight)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(verticalSpace)
                ) {
                    GridCardOld(
                        icon = R.drawable.ic_sync_alt,
                        title = "تحويل بين الحسابات",
                        subtitle = "تحويل الأموال",
                        backgroundColor = Color(0xFFFFF3E0),
                        iconTint = Color(0xFFFF9800),
                        fontSize = gridFontSize.sp,
                        subFontSize = gridSubFontSize.sp,
                        iconSize = gridIconSize,
                        onClick = onTransfer,
                        modifier = Modifier.weight(1f).height(gridCardHeight)
                    )
                    GridCardOld(
                        icon = R.drawable.ic_currency_exchange,
                        title = "صرف العملات",
                        subtitle = "صرف العملات",
                        backgroundColor = Color(0xFFE1F5FE),
                        iconTint = Color(0xFF0288D1),
                        fontSize = gridFontSize.sp,
                        subFontSize = gridSubFontSize.sp,
                        iconSize = gridIconSize,
                        onClick = onExchange,
                        modifier = Modifier.weight(1f).height(gridCardHeight)
                    )
                    GridCardOld(
                        icon = R.drawable.ic_summary,
                        title = "متابعة الديون",
                        subtitle = "متابعة الديون",
                        backgroundColor = Color(0xFFFFEBEE),
                        iconTint = Color(0xFFD32F2F),
                        fontSize = gridFontSize.sp,
                        subFontSize = gridSubFontSize.sp,
                        iconSize = gridIconSize,
                        onClick = onDebts,
                        modifier = Modifier.weight(1f).height(gridCardHeight)
                    )
                }
            }
            Spacer(modifier = Modifier.height(verticalSpace * 2))
        }
    }
}

@Composable
fun StatCardOld(
    icon: Int,
    value: String,
    label: String,
    subLabel: String,
    color: Color,
    iconTint: Color,
    valueColor: Color,
    iconSize: Dp,
    valueFontSize: TextUnit,
    labelFontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(iconSize)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = valueColor,
                    fontSize = valueFontSize
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = valueColor,
                fontSize = labelFontSize,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun ActionButton(
    text: String,
    icon: Int,
    height: Dp,
    backgroundColor: Color,
    fontSize: TextUnit,
    iconSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(height),
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor)
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, color = Color.White, fontSize = fontSize)
    }
}

@Composable
fun GridCardOld(
    icon: Int,
    title: String,
    subtitle: String,
    backgroundColor: Color,
    iconTint: Color,
    fontSize: TextUnit,
    subFontSize: TextUnit,
    iconSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(iconSize)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = iconTint,
                fontSize = fontSize,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = iconTint.copy(alpha = 0.7f),
                fontSize = subFontSize
            )
        }
    }
} 
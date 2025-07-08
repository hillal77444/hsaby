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
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.systemBarsPadding

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

    // نسب ديناميكية موحدة مع شاشات الدخول/التسجيل
    val configuration = LocalConfiguration.current
    val screenWidth = with(LocalDensity.current) { configuration.screenWidthDp.dp }
    val screenHeight = with(LocalDensity.current) { configuration.screenHeightDp.dp }
    val minSide = minOf(screenWidth, screenHeight)
    val blueHeight = screenHeight * 0.12f
    val logoSize = minSide * 0.13f
    val cardCorner = minSide * 0.045f
    val cardPadding = minSide * 0.018f
    val fontTitle = (minSide.value / 22).sp
    val fontField = (minSide.value / 28).sp
    val fontSmall = (minSide.value / 44).sp
    val iconSize = minSide * 0.055f
    val marginSmall = minSide * 0.004f
    val marginMedium = minSide * 0.012f
    val marginLarge = minSide * 0.018f
    val statCardHeight = minSide * 0.09f
    val actionButtonHeight = minSide * 0.055f
    val gridCardHeight = minSide * 0.10f
    val verticalSpace = minSide * 0.009f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7F7))
            .navigationBarsPadding() // يضيف padding سفلي تلقائي حسب النظام
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // المستطيل الأزرق العلوي
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(blueHeight)
                    .background(Color(0xFF2196F3))
            )
            // الشعار متداخل مع البطاقة
            Box(
                modifier = Modifier
                    .size(logoSize)
                    .offset(y = -logoSize / 2)
                    .background(Color.White, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher),
                    contentDescription = "Logo",
                    modifier = Modifier.size(logoSize * 0.8f)
                )
            }
            // عبارة ترحيب
            Text(
                text = "مرحباً، $userName!",
                color = Color(0xFF2196F3),
                fontWeight = FontWeight.Bold,
                fontSize = fontTitle,
                modifier = Modifier.padding(top = marginSmall, bottom = marginMedium)
            )
            // بطاقة المستخدم مع زر التعديل
            Card(
                shape = RoundedCornerShape(cardCorner),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(cardPadding)
                ) {
                    Text(
                        text = userName,
                        color = Color(0xFF152FD9),
                        fontWeight = FontWeight.Bold,
                        fontSize = fontField,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onEditProfile) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_edit),
                            contentDescription = "تعديل الملف",
                            tint = Color(0xFF152FD9),
                            modifier = Modifier.size(iconSize * 1.2f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(marginLarge))
            // بطاقات الإحصائيات الأربع
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.92f),
                horizontalArrangement = Arrangement.spacedBy(marginSmall)
            ) {
                StatCardOld(
                    icon = R.drawable.ic_money,
                    value = "${netBalance?.toInt() ?: 0} يمني",
                    label = "الرصيد الأكبر",
                    subLabel = "الرصيد الأكبر",
                    color = Color(0xFFB2F2E5),
                    iconTint = Color(0xFF009688),
                    valueColor = Color(0xFF009688),
                    valueFontSize = fontField,
                    labelFontSize = fontSmall,
                    modifier = Modifier.weight(1f).height(statCardHeight)
                )
                StatCardOld(
                    icon = R.drawable.ic_arrow_downward,
                    value = "${totalDebtors?.toInt() ?: 0}",
                    label = "إجمالي عليكم",
                    subLabel = "إجمالي عليكم",
                    color = Color(0xFFFFF3E0),
                    iconTint = Color(0xFFFF9800),
                    valueColor = Color(0xFFFF9800),
                    valueFontSize = fontField,
                    labelFontSize = fontSmall,
                    modifier = Modifier.weight(1f).height(statCardHeight)
                )
                StatCardOld(
                    icon = R.drawable.ic_arrow_upward,
                    value = "${totalCreditors?.toInt() ?: 0}",
                    label = "إجمالي لكم",
                    subLabel = "إجمالي لكم",
                    color = Color(0xFFE8F5E9),
                    iconTint = Color(0xFF4CAF50),
                    valueColor = Color(0xFF4CAF50),
                    valueFontSize = fontField,
                    labelFontSize = fontSmall,
                    modifier = Modifier.weight(1f).height(statCardHeight)
                )
                StatCardOld(
                    icon = R.drawable.ic_accounts,
                    value = "${accounts?.size ?: 0}",
                    label = "عدد الحسابات",
                    subLabel = "عدد الحسابات",
                    color = Color(0xFFE3F2FD),
                    iconTint = Color(0xFF1976D2),
                    valueColor = Color(0xFF1976D2),
                    valueFontSize = fontField,
                    labelFontSize = fontSmall,
                    modifier = Modifier.weight(1f).height(statCardHeight)
                )
            }
            Spacer(modifier = Modifier.height(marginLarge))
            // أزرار الإجراءات الثلاثة
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.92f),
                horizontalArrangement = Arrangement.spacedBy(marginMedium)
            ) {
                ActionButton(
                    text = "إضافة قيد",
                    icon = R.drawable.ic_add,
                    height = actionButtonHeight,
                    backgroundColor = Color(0xFF1976D2),
                    fontSize = fontField,
                    onClick = onAddTransaction,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    text = "إضافة حساب",
                    icon = R.drawable.ic_add_account,
                    height = actionButtonHeight,
                    backgroundColor = Color(0xFF388E3C),
                    fontSize = fontField,
                    onClick = onAddAccount,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    text = "كشف الحساب",
                    icon = R.drawable.ic_statement,
                    height = actionButtonHeight,
                    backgroundColor = Color(0xFFFF9800),
                    fontSize = fontField,
                    onClick = onReport,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(marginLarge))
            // شبكة البطاقات الشبكية (Grid) 3 أعمدة × صفين
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f),
                verticalArrangement = Arrangement.spacedBy(marginMedium)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(marginMedium)
                ) {
                    GridCardOld(
                        icon = R.drawable.ic_accounts,
                        title = "الحسابات",
                        onClick = onAccounts,
                        modifier = Modifier.weight(1f).height(gridCardHeight)
                    )
                    GridCardOld(
                        icon = R.drawable.ic_transactions,
                        title = "المعاملات",
                        onClick = onTransactions,
                        modifier = Modifier.weight(1f).height(gridCardHeight)
                    )
                    GridCardOld(
                        icon = R.drawable.ic_reports,
                        title = "التقارير",
                        onClick = onReports,
                        modifier = Modifier.weight(1f).height(gridCardHeight)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(marginMedium)
                ) {
                    GridCardOld(
                        icon = R.drawable.ic_arrow_downward,
                        title = "متابعة الديون",
                        onClick = onDebts,
                        modifier = Modifier.weight(1f).height(gridCardHeight)
                    )
                    GridCardOld(
                        icon = R.drawable.ic_currency_exchange,
                        title = "صرف العملات",
                        onClick = onExchange,
                        modifier = Modifier.weight(1f).height(gridCardHeight)
                    )
                    GridCardOld(
                        icon = R.drawable.ic_sync_alt,
                        title = "تحويل بين الحسابات",
                        onClick = onTransfer,
                        modifier = Modifier.weight(1f).height(gridCardHeight)
                    )
                }
            }
            Spacer(modifier = Modifier.height(marginLarge))
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
    valueFontSize: TextUnit,
    labelFontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidth = with(LocalDensity.current) { configuration.screenWidthDp.dp }
    val iconSize = screenWidth * 0.07f
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidth = with(LocalDensity.current) { configuration.screenWidthDp.dp }
    val iconSize = screenWidth * 0.07f
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidth = with(LocalDensity.current) { configuration.screenWidthDp.dp }
    val iconSize = screenWidth * 0.07f
    Card(
        shape = RoundedCornerShape(14.dp),
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onClick() }
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = Color(0xFF1976D2),
                modifier = Modifier.size(iconSize)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1976D2),
                fontSize = (iconSize * 0.9f).value.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
} 
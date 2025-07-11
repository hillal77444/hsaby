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
import com.hillal.acc.ui.theme.AppTheme
import com.hillal.acc.ui.theme.LocalAppDimensions

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
    AppTheme {
        val totalDebtors by viewModel.totalDebtors.observeAsState()
        val totalCreditors by viewModel.totalCreditors.observeAsState()
        val accounts by viewModel.accounts.observeAsState()
        val netBalance by viewModel.netBalance.observeAsState()
        val scrollState = rememberScrollState()

        val configuration = LocalConfiguration.current
        val dimens = LocalAppDimensions.current
        val colors = MaterialTheme.colorScheme
        val typography = MaterialTheme.typography

        val screenWidth = configuration.screenWidthDp.toFloat().dp
        val screenHeight = configuration.screenHeightDp.toFloat().dp
        val minSide = minOf(screenWidth, screenHeight)
        val maxSide = maxOf(screenWidth, screenHeight)
        val blueHeight = screenHeight * 0.10f // أصغر
        val logoSize = minSide * 0.16f        // أصغر
        val cardCorner = dimens.cardCorner
        val cardPadding = dimens.spacingMedium
        val fontTitle = typography.headlineMedium.fontSize
        val fontField = typography.bodyLarge.fontSize
        val fontSmall = typography.bodyMedium.fontSize
        val iconSize = dimens.iconSize
        val marginSmall = dimens.spacingSmall
        val marginMedium = dimens.spacingMedium
        val marginLarge = dimens.spacingLarge
        val statCardHeight = maxSide * 0.10f
        val actionButtonHeight = maxSide * 0.07f
        val gridCardHeight = maxSide * 0.11f
        val verticalSpace = minSide * 0.012f

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
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
                        .background(colors.primary)
                )
                // الشعار متداخل مع البطاقة
                Box(
                    modifier = Modifier
                        .size(logoSize)
                        .offset(y = -logoSize / 3) // تراكب أقل
                        .background(colors.surface, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher),
                        contentDescription = "Logo",
                        modifier = Modifier.size(logoSize * 0.8f)
                    )
                }
                // لا يوجد Spacer بين الشعار والرسالة الترحيبية
                // عبارة ترحيب
                Text(
                    text = "مرحباً، $userName!",
                    color = colors.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontTitle,
                    modifier = Modifier.padding(top = 2.dp, bottom = marginMedium),
                    style = typography.headlineMedium
                )
                // بطاقة المستخدم مع زر التعديل
                Card(
                    shape = RoundedCornerShape(cardCorner),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .wrapContentHeight(),
                    colors = CardDefaults.cardColors(containerColor = colors.surface)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(cardPadding)
                    ) {
                        Text(
                            text = userName,
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = fontField,
                            modifier = Modifier.weight(1f),
                            style = typography.bodyLarge
                        )
                        IconButton(onClick = onEditProfile) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_edit),
                                contentDescription = "تعديل الملف",
                                tint = colors.primary,
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
                        label = "الرصيد",
                        subLabel = "الرصيد",
                        color = Color(0xFFB2F2E5),
                        iconTint = Color(0xFF009688),
                        valueColor = Color(0xFF009688),
                        valueFontSize = fontSmall,
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
                        valueFontSize = fontSmall,
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
                        valueFontSize = fontSmall,
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
                        valueFontSize = fontSmall,
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
                        backgroundColor = colors.primary,
                        fontSize = fontSmall,
                        onClick = onAddTransaction,
                        modifier = Modifier.weight(1f)
                    )
                    ActionButton(
                        text = "إضافة حساب",
                        icon = R.drawable.ic_add_account,
                        height = actionButtonHeight,
                        backgroundColor = Color(0xFF388E3C),
                        fontSize = fontSmall,
                        onClick = onAddAccount,
                        modifier = Modifier.weight(1f)
                    )
                    ActionButton(
                        text = "كشف الحساب",
                        icon = R.drawable.ic_statement,
                        height = actionButtonHeight,
                        backgroundColor = Color(0xFFFF9800),
                        fontSize = fontSmall,
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
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
        contentPadding = PaddingValues(0.dp) // تصفير الهوامش الداخلية
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
        Spacer(modifier = Modifier.width(4.dp)) // تصغير المسافة بين الأيقونة والنص
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
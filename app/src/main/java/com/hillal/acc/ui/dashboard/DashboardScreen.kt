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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.blur
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.systemBarsPadding
import com.hillal.acc.ui.theme.AppTheme
import com.hillal.acc.ui.theme.LocalAppDimensions
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale // لعرض الشعار بشكل أنيق داخل الدائرة
import androidx.compose.foundation.border

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
        val fontSmall = dimens.fontSmall
        val iconSize = dimens.iconSize
        val marginSmall = dimens.spacingSmall
        val marginMedium = dimens.spacingMedium / 2 // نصف القيمة السابقة
        val marginLarge = dimens.spacingLarge / 2   // نصف القيمة السابقة
        val statCardHeight = maxSide * 0.10f
        val actionButtonHeight = maxSide * 0.07f
        val gridCardHeight = maxSide * 0.11f
        val verticalSpace = minSide * 0.012f

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF2196F3), Color.White)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(blueHeight * 0.5f))
                // الشعار دائري مع ظل ويمكن النقر عليه لتغيير الشعار
                val context = LocalContext.current
                val sharedPreferences = context.getSharedPreferences("report_header_prefs", android.content.Context.MODE_PRIVATE)
                val logoPath = sharedPreferences.getString("logo_path", null)
                var logoBitmap: android.graphics.Bitmap? = null
                if (!logoPath.isNullOrEmpty()) {
                    val file = java.io.File(context.filesDir, logoPath)
                    if (file.exists()) {
                        logoBitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                    }
                }
                Box(
                    modifier = Modifier
                        .size(logoSize)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.85f))
                        .shadow(12.dp, CircleShape)
                        .clickable {
                            val intent = android.content.Intent(context, com.hillal.acc.ui.ReportHeaderSettingsActivity::class.java)
                            context.startActivity(intent)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // إطار حول الدائرة (اختياري)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(4.dp, Color.LightGray, CircleShape)
                    ) {
                        if (logoBitmap != null) {
                            Image(
                                bitmap = logoBitmap.asImageBitmap(),
                                contentDescription = "Logo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.mipmap.ic_launcher_round),
                                contentDescription = "Logo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // عبارة ترحيب
                Text(
                    text = "مرحباً، $userName!",
                    color = colors.onPrimary,
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
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                // بطاقات الإحصائيات الزجاجية
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.92f),
                    horizontalArrangement = Arrangement.spacedBy(marginSmall)
                ) {
                    GlassStatCard(
                        icon = R.drawable.ic_accounts,
                        value = "${accounts?.size ?: 0}",
                        label = "عدد الحسابات",
                        iconTint = Color(0xFF1976D2),
                        valueColor = Color(0xFF1976D2),
                        valueFontSize = fontSmall * 1.1f,
                        labelFontSize = fontSmall * 0.95f,
                        modifier = Modifier.weight(1f).height(statCardHeight)
                    )
                    GlassStatCard(
                        icon = R.drawable.ic_arrow_upward,
                        value = "${totalCreditors?.toInt() ?: 0}",
                        label = "إجمالي لكم",
                        iconTint = Color(0xFF4CAF50),
                        valueColor = Color(0xFF4CAF50),
                        valueFontSize = fontSmall * 1.0f,
                        labelFontSize = fontSmall * 0.95f,
                        modifier = Modifier.weight(1f).height(statCardHeight)
                    )
                    GlassStatCard(
                        icon = R.drawable.ic_arrow_downward,
                        value = "${totalDebtors?.toInt() ?: 0}",
                        label = "إجمالي عليكم",
                        iconTint = Color(0xFFFF9800),
                        valueColor = Color(0xFFFF9800),
                        valueFontSize = fontSmall * 1.0f,
                        labelFontSize = fontSmall * 0.95f,
                        modifier = Modifier.weight(1f).height(statCardHeight)
                    )
                    GlassStatCard(
                        icon = R.drawable.ic_money,
                        value = "${netBalance?.toInt() ?: 0} يمني",
                        label = "الرصيد",
                        iconTint = Color(0xFF009688),
                        valueColor = Color(0xFF009688),
                        valueFontSize = fontSmall * 1.1f,
                        labelFontSize = fontSmall * 1.0f,
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
                            icon = R.drawable.ic_menu_home, // استبدل R.drawable.ic_grid بـ R.drawable.ic_menu_home
                            title = "جميع الخدمات",
                            onClick = { onTransfer() }, // استبدل onTransfer بمنطق التنقل إلى AllServices
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
        Spacer(modifier = Modifier.width(3.dp)) // تصغير المسافة بين الأيقونة والنص
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
                fontSize = (iconSize * 0.7f).value.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun GlassStatCard(
    icon: Int,
    value: String,
    label: String,
    iconTint: Color,
    valueColor: Color,
    valueFontSize: TextUnit? = null, // اجعلها اختيارية
    labelFontSize: TextUnit? = null, // اجعلها اختيارية
    modifier: Modifier = Modifier
) {
    val dimens = LocalAppDimensions.current
    val iconSize = dimens.iconSize
    // احسب الحجم تلقائياً إذا لم يتم تمريره
    val autoValueFontSize = (iconSize.value * 0.9f).sp
    val autoLabelFontSize = (iconSize.value * 0.7f).sp
    val usedValueFontSize = valueFontSize ?: autoValueFontSize
    val usedLabelFontSize = labelFontSize ?: autoLabelFontSize
    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.78f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dimens.fieldHorizontalPadding, vertical = dimens.spacingSmall),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(iconSize)
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = valueColor,
                fontSize = usedValueFontSize // استخدم الحجم المحسوب أو المرسل
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = valueColor,
                fontSize = usedLabelFontSize, // استخدم الحجم المحسوب أو المرسل
                modifier = Modifier.padding(top = 1.dp)
            )
        }
    }
} 
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
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val maxW = maxWidth
        val maxH = maxHeight
        val isTablet = maxW > 600.dp
        val contentPadding = if (isTablet) 32.dp else 12.dp
        val cardHeight = if (isTablet) 120.dp else 90.dp
        val statIconSize = if (isTablet) 48.dp else 32.dp
        val statFontSize = if (isTablet) 28.sp else 20.sp
        val statLabelFontSize = if (isTablet) 18.sp else 13.sp
        val headerHeight = if (isTablet) maxH * 0.18f else maxH * 0.13f
        val logoSize = if (isTablet) 140.dp else 96.dp
        val logoOffset = if (isTablet) (-logoSize/2.5f) else (-logoSize/2)
        val welcomeFontSize = if (isTablet) 28.sp else 20.sp
        val userCardFontSize = if (isTablet) 22.sp else 16.sp
        val userCardPadding = if (isTablet) 28.dp else 16.dp
        val actionButtonHeight = if (isTablet) 64.dp else 48.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp)
        ) {
            // رأس منحني علوي
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
                    .background(MaterialTheme.colorScheme.primary)
            ) {}

            // أيقونة دائرية أو شعار التطبيق
            Box(
                modifier = Modifier
                    .size(logoSize)
                    .offset(y = logoOffset)
                    .align(Alignment.CenterHorizontally)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher),
                    contentDescription = "Logo",
                    modifier = Modifier.size(logoSize * 0.75f)
                )
            }

            // عبارة ترحيبية
            Text(
                text = "مرحباً، $userName!",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = welcomeFontSize,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .offset(y = logoOffset/2)
            )

            // بطاقة اسم المستخدم مع زر التعديل
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = contentPadding, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(userCardPadding)
                ) {
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = userCardFontSize,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onEditProfile) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_edit),
                            contentDescription = "تعديل الملف",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // بطاقات الإحصائيات
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = contentPadding),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatCard(
                    icon = R.drawable.ic_accounts,
                    value = accounts?.size?.toString() ?: "0",
                    label = "عدد الحسابات",
                    color = MaterialTheme.colorScheme.primary,
                    iconSize = statIconSize,
                    valueFontSize = statFontSize,
                    labelFontSize = statLabelFontSize,
                    cardHeight = cardHeight,
                    onClick = onAccounts
                )
                StatCard(
                    icon = R.drawable.ic_arrow_upward,
                    value = totalCreditors?.roundToInt()?.toString() ?: "0",
                    label = "إجمالي لكم",
                    color = Color(0xFF4CAF50),
                    iconSize = statIconSize,
                    valueFontSize = statFontSize,
                    labelFontSize = statLabelFontSize,
                    cardHeight = cardHeight,
                    onClick = onTransactions
                )
                StatCard(
                    icon = R.drawable.ic_arrow_downward,
                    value = totalDebtors?.roundToInt()?.toString() ?: "0",
                    label = "إجمالي عليكم",
                    color = Color(0xFFF44336),
                    iconSize = statIconSize,
                    valueFontSize = statFontSize,
                    labelFontSize = statLabelFontSize,
                    cardHeight = cardHeight,
                    onClick = onTransactions
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // الرصيد الصافي
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = contentPadding, vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(userCardPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "الرصيد الصافي",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = userCardFontSize
                    )
                    Text(
                        text = "${netBalance?.roundToInt() ?: 0} يمني",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = statFontSize,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // بطاقات متابعة الديون والحسابات والمعاملات والتقارير
            DashboardGridCards(
                isTablet = isTablet,
                onAccounts = onAccounts,
                onTransactions = onTransactions,
                onReports = onReports,
                onDebts = onDebts,
                onTransfer = onTransfer,
                onExchange = onExchange
            )

            Spacer(modifier = Modifier.height(12.dp))

            // أزرار إضافة الحساب والمعاملة فقط
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = contentPadding),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionButton(
                    text = "إضافة حساب",
                    icon = R.drawable.ic_add_account,
                    height = actionButtonHeight,
                    onClick = onAddAccount
                )
                ActionButton(
                    text = "إضافة معاملة",
                    icon = R.drawable.ic_add_transaction,
                    height = actionButtonHeight,
                    onClick = onAddTransaction
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun StatCard(
    icon: Int,
    value: String,
    label: String,
    color: Color,
    iconSize: Dp,
    valueFontSize: TextUnit,
    labelFontSize: TextUnit,
    cardHeight: Dp,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 4.dp)
            .height(cardHeight)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(iconSize)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = color,
                fontSize = valueFontSize,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = labelFontSize,
                textAlign = TextAlign.Center
            )
        }
    }
}



@Composable
fun ActionButton(text: String, icon: Int, height: Dp, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 4.dp)
            .height(height),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, color = Color.White, fontSize = 16.sp)
    }
}

@Composable
fun DashboardGridCards(
    isTablet: Boolean,
    onAccounts: () -> Unit,
    onTransactions: () -> Unit,
    onReports: () -> Unit,
    onDebts: () -> Unit,
    onTransfer: () -> Unit,
    onExchange: () -> Unit
) {
    val cardHeight = if (isTablet) 120.dp else 100.dp
    val iconSize = if (isTablet) 32.dp else 24.dp
    val fontSize = if (isTablet) 16.sp else 14.sp

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // الصف الأول: الحسابات والمعاملات
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            GridCard(
                icon = R.drawable.ic_accounts,
                title = "الحسابات",
                subtitle = "إدارة الحسابات",
                color = MaterialTheme.colorScheme.primary,
                cardHeight = cardHeight,
                iconSize = iconSize,
                fontSize = fontSize,
                onClick = onAccounts
            )
            GridCard(
                icon = R.drawable.ic_transactions,
                title = "المعاملات",
                subtitle = "سجل المعاملات",
                color = Color(0xFF2196F3),
                cardHeight = cardHeight,
                iconSize = iconSize,
                fontSize = fontSize,
                onClick = onTransactions
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // الصف الثاني: التقارير والديون
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            GridCard(
                icon = R.drawable.ic_report,
                title = "التقارير",
                subtitle = "تقارير مالية",
                color = Color(0xFF9C27B0),
                cardHeight = cardHeight,
                iconSize = iconSize,
                fontSize = fontSize,
                onClick = onReports
            )
            GridCard(
                icon = R.drawable.ic_debts,
                title = "الديون",
                subtitle = "متابعة الديون",
                color = Color(0xFFFF9800),
                cardHeight = cardHeight,
                iconSize = iconSize,
                fontSize = fontSize,
                onClick = onDebts
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // الصف الثالث: التحويل والصرف
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            GridCard(
                icon = R.drawable.ic_transfer,
                title = "تحويل",
                subtitle = "تحويل الأموال",
                color = Color(0xFF4CAF50),
                cardHeight = cardHeight,
                iconSize = iconSize,
                fontSize = fontSize,
                onClick = onTransfer
            )
            GridCard(
                icon = R.drawable.ic_exchange,
                title = "صرف",
                subtitle = "صرف العملات",
                color = Color(0xFF607D8B),
                cardHeight = cardHeight,
                iconSize = iconSize,
                fontSize = fontSize,
                onClick = onExchange
            )
        }
    }
}

@Composable
fun GridCard(
    icon: Int,
    title: String,
    subtitle: String,
    color: Color,
    cardHeight: Dp,
    iconSize: Dp,
    fontSize: TextUnit,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 4.dp)
            .height(cardHeight)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(iconSize)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = color,
                fontSize = fontSize,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = fontSize * 0.8f,
                textAlign = TextAlign.Center
            )
        }
    }
} 
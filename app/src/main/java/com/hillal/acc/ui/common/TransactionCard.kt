package com.hillal.acc.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hillal.acc.R
import com.hillal.acc.data.model.Transaction
import java.text.SimpleDateFormat
import java.util.*
import com.hillal.acc.data.model.Account
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.blur

@Composable
fun TransactionCard(
    transaction: Transaction,
    accounts: List<Account>,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onWhatsApp: () -> Unit,
    onSms: () -> Unit,
    modifier: Modifier = Modifier,
    searchQuery: String = ""
) {
    val isDebit = transaction.getType()?.lowercase() == "debit" || transaction.getType() == "عليه"
    val gradient = if (isDebit) {
        Brush.linearGradient(
            colors = listOf(Color(0xFFFF5252).copy(alpha = 0.18f), Color(0xFFFF8A80).copy(alpha = 0.18f)),
            start = Offset(0f, 0f),
            end = Offset(400f, 400f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFF43EA7D).copy(alpha = 0.18f), Color(0xFF1CBF4F).copy(alpha = 0.18f)),
            start = Offset(0f, 0f),
            end = Offset(400f, 400f)
        )
    }
    val accountName = accounts.find { it.getId() == transaction.getAccountId() }?.getName() ?: "--"
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val dimens = com.hillal.acc.ui.theme.LocalAppDimensions.current
    val buttonSize = screenHeight * 0.045f
    val iconSize = screenHeight * 0.03f
    val cardHeight = screenHeight * 0.13f // بطاقة مضغوطة
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight)
            .shadow(6.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.55f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // طبقة الخلفية الزجاجية فقط
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(gradient)
                    .blur(10.dp)
            )
            // طبقة المحتوى الأمامي (النصوص والأزرار)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                // الصف الأول: أيقونة + اسم الحساب + المبلغ + الأزرار
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // دائرة أيقونة أو أول حرف من اسم الحساب
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                if (isDebit) Color(0xFFFF5252) else Color(0xFF43EA7D),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = accountName.take(1),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = accountName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF222222),
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${transaction.getAmount()} ${transaction.getCurrency()}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = if (isDebit) Color(0xFFFF5252) else Color(0xFF43EA7D),
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    // أزرار العمليات
                    ActionCircleButton(icon = Icons.Default.Delete, borderColor = Color.Red, onClick = onDelete, size = 32.dp, iconSize = 18.dp)
                    Spacer(Modifier.width(2.dp))
                    ActionCircleButton(icon = Icons.Default.Edit, borderColor = Color(0xFF1976D2), onClick = onEdit, size = 32.dp, iconSize = 18.dp)
                    Spacer(Modifier.width(2.dp))
                    ActionCircleButton(painter = painterResource(id = com.hillal.acc.R.drawable.ic_sms), borderColor = Color(0xFF1976D2), onClick = onSms, size = 32.dp, iconSize = 18.dp)
                    Spacer(Modifier.width(2.dp))
                    ActionCircleButton(painter = painterResource(id = com.hillal.acc.R.drawable.ic_whatsapp), borderColor = Color(0xFF25D366), onClick = onWhatsApp, size = 32.dp, iconSize = 18.dp)
                }
                Spacer(Modifier.height(2.dp))
                // الصف الثاني: الوصف (سطرين) + التاريخ
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HighlightedDescription(
                        description = transaction.getDescription() ?: "",
                        searchQuery = searchQuery,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color(0xFF888888), modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = transaction.getDateString(),
                            fontSize = 12.sp,
                            color = Color(0xFF888888)
                        )
                    }
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
    onClick: () -> Unit,
    size: Dp = 40.dp,
    iconSize: Dp = 22.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .border(2.dp, borderColor, CircleShape)
            .background(Color.White, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = borderColor, modifier = Modifier.size(iconSize))
        } else if (painter != null) {
            Icon(painter = painter, contentDescription = null, tint = borderColor, modifier = Modifier.size(iconSize))
        }
    }
}

fun Transaction.getDateString(): String {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return dateFormat.format(Date(this.getTransactionDate()))
}

@Composable
fun HighlightedDescription(
    description: String,
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    val maxLines = 2
    val fontSize = 14.sp
    if (searchQuery.isBlank()) {
        Text(
            text = description,
            fontSize = fontSize,
            color = Color(0xFF444444),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
        )
    } else {
        val index = description.indexOf(searchQuery, ignoreCase = true)
        if (index == -1) {
            Text(
                text = description,
                fontSize = fontSize,
                color = Color(0xFF444444),
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = modifier
            )
        } else {
            val before = description.substring(0, index)
            val match = description.substring(index, index + searchQuery.length)
            val after = description.substring(index + searchQuery.length)
            val highlighted = buildAnnotatedString {
                append(before)
                withStyle(SpanStyle(background = Color(0xFFFFF59D), color = Color.Black)) {
                    append(match)
                }
                append(after)
            }
            Text(
                text = highlighted,
                fontSize = fontSize,
                color = Color(0xFF444444),
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = modifier
            )
        }
    }
} 
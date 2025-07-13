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
            .shadow(12.dp, RoundedCornerShape(22.dp)), // ظل أقوى وحواف أعرض
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.70f)) // خلفية زجاجية أقوى
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // طبقة الخلفية الزجاجية فقط
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(gradient)
                    .blur(14.dp) // blur أقوى
            )
            // طبقة المحتوى الأمامي (النصوص والأزرار)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp) // padding أكبر
            ) {
                // الصف الأول: أيقونة + اسم الحساب + المبلغ
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // دائرة أيقونة أو أول حرف من اسم الحساب
                    Box(
                        modifier = Modifier
                            .size(30.dp)
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
                            fontSize = 17.sp
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = accountName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF222222),
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    val amountColor = if (isDebit) Color(0xFFD32F2F) else Color(0xFF388E3C)
                    // المبلغ مع ظل أنيق وخلفية شفافة خفيفة
                    Box(
                        modifier = Modifier
                            .shadow(4.dp, shape = RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.65f), shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${transaction.getAmount()} ${transaction.getCurrency()}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = amountColor,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
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
                    Spacer(Modifier.width(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color(0xFF888888), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = transaction.getDateString(),
                            fontSize = 13.sp,
                            color = Color(0xFF888888)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                // الصف الثالث: الأزرار في الأسفل موزعة أفقيًا
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ActionCircleButton(painter = painterResource(id = com.hillal.acc.R.drawable.ic_whatsapp), borderColor = Color(0xFF25D366), onClick = onWhatsApp, size = 34.dp, iconSize = 20.dp)
                    ActionCircleButton(painter = painterResource(id = com.hillal.acc.R.drawable.ic_sms), borderColor = Color(0xFF1976D2), onClick = onSms, size = 34.dp, iconSize = 20.dp)
                    ActionCircleButton(icon = Icons.Default.Delete, borderColor = Color.Red, onClick = onDelete, size = 34.dp, iconSize = 20.dp)
                    ActionCircleButton(icon = Icons.Default.Edit, borderColor = Color(0xFF1976D2), onClick = onEdit, size = 34.dp, iconSize = 20.dp)
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
    val maxChars = 80 // تقريباً ما يعادل سطرين بالعربي
    val fontSize = 14.sp
    if (searchQuery.isBlank()) {
        Text(
            text = description,
            fontSize = fontSize,
            color = Color(0xFF444444),
            maxLines = 2,
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
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = modifier
            )
        } else {
            // حدد بداية ونهاية الجزء المعروض بحيث يظهر التطابق في المنتصف تقريباً
            val contextLength = maxChars - searchQuery.length
            val beforeLen = contextLength / 2
            val afterLen = contextLength - beforeLen
            val start = maxOf(0, index - beforeLen)
            val end = minOf(description.length, index + searchQuery.length + afterLen)
            val shown = description.substring(start, end)
            val matchStart = shown.indexOf(searchQuery, ignoreCase = true)
            val matchEnd = matchStart + searchQuery.length
            val highlighted = buildAnnotatedString {
                if (start > 0) append("...")
                append(shown.substring(0, matchStart))
                withStyle(SpanStyle(background = Color(0xFFFFF59D), color = Color.Black)) {
                    append(shown.substring(matchStart, matchEnd))
                }
                append(shown.substring(matchEnd))
                if (end < description.length) append("...")
            }
            Text(
                text = highlighted,
                fontSize = fontSize,
                color = Color(0xFF444444),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = modifier
            )
        }
    }
} 
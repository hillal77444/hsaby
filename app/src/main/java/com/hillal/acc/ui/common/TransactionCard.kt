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
import androidx.compose.ui.unit.TextUnit

@Composable
fun TransactionCard(
    transaction: Transaction,
    accounts: List<Account>,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onWhatsApp: () -> Unit,
    onSms: () -> Unit,
    index: Int,
    modifier: Modifier = Modifier,
    searchQuery: String = ""
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    val isDebit = transaction.getType()?.lowercase() == "debit" || transaction.getType() == "عليه"
    val typeText = if (isDebit) "عليه" else "له"
    val typeColor = if (isDebit) Color(0xFFD32F2F) else Color(0xFF388E3C)
    val typeBgColor = if (isDebit) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
    val accountName = accounts.find { it.getId() == transaction.getAccountId() }?.getName() ?: "--"
    val description = transaction.getDescription() ?: ""
    val dateString = transaction.getDateString()
    val cardBgColor = if (index % 2 == 0) Color.White else Color(0xFFE3F6FB)

    // مقاسات نسبية
    val cardHeight = screenHeight * 0.15f
    val cardCorner = screenHeight * 0.025f
    val cardElevation = screenHeight * 0.008f
    val cardPaddingH = screenWidth * 0.02f
    val cardPaddingV = screenHeight * 0.008f
    val contentPadding = screenHeight * 0.012f
    val typeBoxWidth = screenWidth * 0.16f
    val typeBoxHeight = screenHeight * 0.05f
    val typeBoxCorner = screenHeight * 0.012f
    val typeFontSize = (screenHeight.value * 0.022f).sp
    val accountFontSize = (screenHeight.value * 0.024f).sp
    val dateFontSize = (screenHeight.value * 0.018f).sp
    val descFontSize = (screenHeight.value * 0.019f).sp
    val buttonSize = screenHeight * 0.06f
    val iconSize = screenHeight * 0.035f
    val buttonTextSize = (screenHeight.value * 0.017f).sp
    val buttonWidth = screenWidth * 0.18f
    val buttonSpacing = screenWidth * 0.01f
    val rowSpacing = screenHeight * 0.008f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight)
            .padding(vertical = cardPaddingV, horizontal = cardPaddingH),
        shape = RoundedCornerShape(cardCorner),
        elevation = CardDefaults.cardElevation(cardElevation),
        colors = CardDefaults.cardColors(containerColor = cardBgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding)
        ) {
            // الصف العلوي: نوع العملية | اسم الحساب | التاريخ
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // مربع نوع العملية
                Box(
                    modifier = Modifier
                        .width(typeBoxWidth)
                        .height(typeBoxHeight)
                        .background(typeBgColor, shape = RoundedCornerShape(typeBoxCorner))
                        .border(1.dp, typeColor, shape = RoundedCornerShape(typeBoxCorner)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = typeText,
                        color = typeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = typeFontSize
                    )
                }
                Spacer(Modifier.width(buttonSpacing))
                // اسم الحساب
                Text(
                    text = accountName,
                    fontWeight = FontWeight.Bold,
                    fontSize = accountFontSize,
                    color = Color(0xFF222222),
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                // التاريخ
                Text(
                    text = dateString,
                    fontSize = dateFontSize,
                    color = Color(0xFF888888),
                    modifier = Modifier.padding(start = buttonSpacing)
                )
            }
            Spacer(Modifier.height(rowSpacing))
            // الوصف
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HighlightedDescription(
                    description = description,
                    searchQuery = searchQuery,
                    modifier = Modifier.weight(1f),
                    fontSize = descFontSize
                )
            }
            Spacer(Modifier.height(rowSpacing * 2))
            // الأزرار الأربعة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionButton(
                    painter = painterResource(id = com.hillal.acc.R.drawable.ic_whatsapp),
                    label = "واتساب",
                    onClick = onWhatsApp,
                    color = Color(0xFF25D366),
                    size = buttonSize,
                    iconSize = iconSize,
                    textSize = buttonTextSize,
                    width = buttonWidth
                )
                ActionButton(
                    painter = painterResource(id = com.hillal.acc.R.drawable.ic_sms),
                    label = "SMS",
                    onClick = onSms,
                    color = Color(0xFF1976D2),
                    size = buttonSize,
                    iconSize = iconSize,
                    textSize = buttonTextSize,
                    width = buttonWidth
                )
                ActionButton(
                    icon = Icons.Default.Edit,
                    label = "تعديل",
                    onClick = onEdit,
                    color = Color(0xFF1976D2),
                    size = buttonSize,
                    iconSize = iconSize,
                    textSize = buttonTextSize,
                    width = buttonWidth
                )
                ActionButton(
                    icon = Icons.Default.Delete,
                    label = "حذف",
                    onClick = onDelete,
                    color = Color(0xFFD32F2F),
                    size = buttonSize,
                    iconSize = iconSize,
                    textSize = buttonTextSize,
                    width = buttonWidth
                )
            }
        }
    }
}

@Composable
fun ActionButton(
    icon: ImageVector? = null,
    painter: Painter? = null,
    label: String,
    onClick: () -> Unit,
    color: Color,
    size: Dp = 38.dp,
    iconSize: Dp = 22.dp,
    textSize: TextUnit = 12.sp,
    width: Dp = 64.dp
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(width)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .background(Color.White, CircleShape)
                .border(2.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(iconSize))
            } else if (painter != null) {
                Icon(painter = painter, contentDescription = null, tint = color, modifier = Modifier.size(iconSize))
            }
        }
        Spacer(Modifier.height(size * 0.08f))
        Text(
            text = label,
            fontSize = textSize,
            color = color,
            maxLines = 1
        )
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
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp
) {
    val maxChars = 80 // تقريباً ما يعادل سطرين بالعربي
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
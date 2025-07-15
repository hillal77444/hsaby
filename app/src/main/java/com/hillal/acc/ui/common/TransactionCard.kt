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
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextAlign

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
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth()
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        // هوامش صغيرة جداً
        val cardPaddingH = 4.dp
        val cardPaddingV = 2.dp
        val contentPadding = 3.dp
        val typeBoxWidth = screenWidth * 0.10f
        val typeBoxHeight = screenHeight * 0.18f
        val typeBoxCorner = screenHeight * 0.04f
        val buttonSpacing = 2.dp
        val rowSpacing = 2.dp
        val amountBoxPaddingH = 2.dp
        val amountBoxPaddingV = 2.dp
        val cardCorner = 6.dp

        // خطوط صغيرة جداً (70% من القيم السابقة تقريباً)
        val accountFontSize = 22.sp
        val typeFontSize = 18.sp
        val dateFontSize = 15.sp
        val descFontSize = 15.sp
        val buttonTextSize = 16.sp
        val iconSize = 13.dp
        val buttonWidth = screenWidth * 0.2f
        val buttonHeight = 46.dp

        val isDebit = transaction.getType()?.lowercase() == "debit" || transaction.getType() == "عليه"
        val typeText = if (isDebit) "عليه" else "له"
        val typeColor = if (isDebit) Color(0xFFD32F2F) else Color(0xFF388E3C)
        val typeBgColor = if (isDebit) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
        val accountName = accounts.find { it.getId() == transaction.getAccountId() }?.getName() ?: "--"
        val description = transaction.getDescription() ?: ""
        val dateString = transaction.getDateString()
        val cardBgColor = if (index % 2 == 0) Color.White else Color(0xFFE3F6FB)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = cardPaddingV, horizontal = cardPaddingH),
            shape = RoundedCornerShape(cardCorner),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(containerColor = cardBgColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding)
            ) {
                // الصف العلوي: نوع العملية | اسم الحساب | التاريخ | المبلغ
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
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    // التاريخ
                    Text(
                        text = dateString,
                        fontSize = dateFontSize,
                        color = Color(0xFF888888),
                        modifier = Modifier.padding(start = buttonSpacing)
                    )
                    Spacer(Modifier.width(buttonSpacing))
                    // المبلغ مع العملة بخلفية مميزة
                    val amountColor = if (isDebit) Color(0xFFD32F2F) else Color(0xFF388E3C)
                    val amountBg = if (isDebit) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                    Box(
                        modifier = Modifier
                            .background(amountBg.copy(alpha = 0.85f), shape = RoundedCornerShape(typeBoxCorner))
                            .border(1.dp, amountColor.copy(alpha = 0.5f), shape = RoundedCornerShape(typeBoxCorner))
                            .padding(horizontal = amountBoxPaddingH, vertical = amountBoxPaddingV),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${transaction.getAmount()} ${transaction.getCurrency()}",
                            fontWeight = FontWeight.Bold,
                            fontSize = typeFontSize,
                            color = amountColor
                        )
                    }
                }
                Spacer(Modifier.height(rowSpacing))
                // الوصف في منتصف البطاقة
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = rowSpacing),
                    contentAlignment = Alignment.Center
                ) {
                    HighlightedDescription(
                        description = description,
                        searchQuery = searchQuery,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = descFontSize
                    )
                }
                Spacer(Modifier.height(rowSpacing))
                // الأزرار الأربعة
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = rowSpacing),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ActionButton(
                        painter = painterResource(id = com.hillal.acc.R.drawable.ic_whatsapp),
                        label = "واتساب",
                        onClick = onWhatsApp,
                        color = Color(0xFF25D366),
                        width = buttonWidth,
                        height = buttonHeight,
                        iconSize = iconSize,
                        textSize = buttonTextSize
                    )
                    Spacer(Modifier.width(buttonSpacing))
                    ActionButton(
                        painter = painterResource(id = com.hillal.acc.R.drawable.ic_sms),
                        label = "SMS",
                        onClick = onSms,
                        color = Color(0xFF1976D2),
                        width = buttonWidth,
                        height = buttonHeight,
                        iconSize = iconSize,
                        textSize = buttonTextSize
                    )
                    Spacer(Modifier.width(buttonSpacing))
                    ActionButton(
                        icon = Icons.Default.Edit,
                        label = "تعديل",
                        onClick = onEdit,
                        color = Color(0xFF1976D2),
                        width = buttonWidth,
                        height = buttonHeight,
                        iconSize = iconSize,
                        textSize = buttonTextSize
                    )
                    Spacer(Modifier.width(buttonSpacing))
                    ActionButton(
                        icon = Icons.Default.Delete,
                        label = "حذف",
                        onClick = onDelete,
                        color = Color(0xFFD32F2F),
                        width = buttonWidth,
                        height = buttonHeight,
                        iconSize = iconSize,
                        textSize = buttonTextSize
                    )
                }
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
    width: Dp,
    height: Dp,
    iconSize: Dp,
    textSize: TextUnit
) {
    Surface(
        modifier = Modifier
            .width(width)
            .height(height)
            .padding(horizontal = 1.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.2.dp, color),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = height * 0.10f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(iconSize))
            } else if (painter != null) {
                Icon(painter = painter, contentDescription = null, tint = color, modifier = Modifier.size(iconSize))
            }
            Spacer(Modifier.height(height * 0.10f))
            Text(
                text = label,
                fontSize = textSize,
                color = color,
                maxLines = 1
            )
        }
    }
}

fun Transaction.getDateString(): String {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return dateFormat.format(Date(this.getTransactionDate()))
}

// حسّن الوصف ليكون أوضح
@Composable
fun HighlightedDescription(
    description: String,
    searchQuery: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 9.sp
) {
    val maxChars = 60 // أصغر قليلاً لتناسب التصميم الصغير
    if (searchQuery.isBlank()) {
        Text(
            text = description,
            fontSize = fontSize,
            color = Color(0xFF444444),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
            textAlign = TextAlign.Center
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
                modifier = modifier,
                textAlign = TextAlign.Center
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
                modifier = modifier,
                textAlign = TextAlign.Center
            )
        }
    }
} 
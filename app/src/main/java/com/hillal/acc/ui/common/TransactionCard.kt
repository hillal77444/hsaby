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
        val configuration = LocalConfiguration.current
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        val isDebit = transaction.getType()?.lowercase() == "debit" || transaction.getType() == "عليه"
        val typeText = if (isDebit) "عليه" else "له"
        val typeColor = if (isDebit) Color(0xFFD32F2F) else Color(0xFF388E3C)
        val typeBgColor = if (isDebit) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
        val accountName = accounts.find { it.getId() == transaction.getAccountId() }?.getName() ?: "--"
        val description = transaction.getDescription() ?: ""
        val dateString = transaction.getDateString()
        val cardBgColor = if (index % 2 == 0) Color.White else Color(0xFFE3F6FB)

        // تكيف ديناميكي حسب طول اسم الحساب
        val accountNameLength = accountName.length
        val dynamicAccountFontSize = when {
            accountNameLength > 22 -> screenHeight.value * 0.11f
            accountNameLength > 16 -> screenHeight.value * 0.13f
            accountNameLength > 12 -> screenHeight.value * 0.15f
            else -> screenHeight.value * 0.18f
        }.sp
        val dynamicTypeFontSize = (screenHeight.value * 0.13f).sp
        val dynamicDateFontSize = (screenHeight.value * 0.11f).sp
        val dynamicDescFontSize = (screenHeight.value * 0.12f).sp
        val dynamicButtonTextSize = (screenHeight.value * 0.10f).sp
        val dynamicIconSize = screenHeight * 0.22f
        val dynamicButtonWidth = screenWidth * 0.22f
        val dynamicButtonHeight = screenHeight * 0.62f
        val dynamicTypeBoxWidth = screenWidth * 0.18f
        val dynamicTypeBoxHeight = screenHeight * 0.38f
        val dynamicTypeBoxCorner = screenHeight * 0.09f
        val dynamicAmountFontSize = (screenHeight.value * 0.13f).sp
        val dynamicAmountBoxPaddingH = screenWidth * 0.04f
        val dynamicAmountBoxPaddingV = screenHeight * 0.04f
        val dynamicRowSpacing = screenHeight * 0.08f
        val dynamicCardCorner = screenHeight * 0.13f
        val dynamicCardPaddingH = screenWidth * 0.04f
        val dynamicCardPaddingV = screenHeight * 0.04f
        val dynamicContentPadding = screenHeight * 0.04f
        val dynamicButtonSpacing = screenWidth * 0.01f

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp, max = 350.dp)
                .padding(vertical = dynamicCardPaddingV, horizontal = dynamicCardPaddingH),
            shape = RoundedCornerShape(dynamicCardCorner),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(containerColor = cardBgColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dynamicContentPadding)
            ) {
                // الصف العلوي: نوع العملية | اسم الحساب | التاريخ | المبلغ
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // مربع نوع العملية
                    Box(
                        modifier = Modifier
                            .width(dynamicTypeBoxWidth)
                            .height(dynamicTypeBoxHeight)
                            .background(typeBgColor, shape = RoundedCornerShape(dynamicTypeBoxCorner))
                            .border(1.dp, typeColor, shape = RoundedCornerShape(dynamicTypeBoxCorner)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = typeText,
                            color = typeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = dynamicTypeFontSize
                        )
                    }
                    Spacer(Modifier.width(dynamicButtonSpacing))
                    // اسم الحساب
                    Text(
                        text = accountName,
                        fontWeight = FontWeight.Bold,
                        fontSize = dynamicAccountFontSize,
                        color = Color(0xFF222222),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    // التاريخ
                    Text(
                        text = dateString,
                        fontSize = dynamicDateFontSize,
                        color = Color(0xFF888888),
                        modifier = Modifier.padding(start = dynamicButtonSpacing)
                    )
                    Spacer(Modifier.width(dynamicButtonSpacing))
                    // المبلغ مع العملة بخلفية مميزة
                    val amountColor = if (isDebit) Color(0xFFD32F2F) else Color(0xFF388E3C)
                    val amountBg = if (isDebit) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                    Box(
                        modifier = Modifier
                            .background(amountBg.copy(alpha = 0.85f), shape = RoundedCornerShape(dynamicTypeBoxCorner))
                            .border(1.dp, amountColor.copy(alpha = 0.5f), shape = RoundedCornerShape(dynamicTypeBoxCorner))
                            .padding(horizontal = dynamicAmountBoxPaddingH, vertical = dynamicAmountBoxPaddingV),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${transaction.getAmount()} ${transaction.getCurrency()}",
                            fontWeight = FontWeight.Bold,
                            fontSize = dynamicAmountFontSize,
                            color = amountColor
                        )
                    }
                }
                Spacer(Modifier.height(dynamicRowSpacing))
                // الوصف في المنتصف
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    HighlightedDescription(
                        description = description,
                        searchQuery = searchQuery,
                        modifier = Modifier.weight(1f),
                        fontSize = dynamicDescFontSize
                    )
                }
                Spacer(Modifier.height(dynamicRowSpacing))
                // الأزرار الأربعة
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = dynamicRowSpacing),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ActionButton(
                        painter = painterResource(id = com.hillal.acc.R.drawable.ic_whatsapp),
                        label = "واتساب",
                        onClick = onWhatsApp,
                        color = Color(0xFF25D366),
                        width = dynamicButtonWidth,
                        height = dynamicButtonHeight,
                        iconSize = dynamicIconSize,
                        textSize = dynamicButtonTextSize
                    )
                    Spacer(Modifier.width(dynamicButtonSpacing))
                    ActionButton(
                        painter = painterResource(id = com.hillal.acc.R.drawable.ic_sms),
                        label = "SMS",
                        onClick = onSms,
                        color = Color(0xFF1976D2),
                        width = dynamicButtonWidth,
                        height = dynamicButtonHeight,
                        iconSize = dynamicIconSize,
                        textSize = dynamicButtonTextSize
                    )
                    Spacer(Modifier.width(dynamicButtonSpacing))
                    ActionButton(
                        icon = Icons.Default.Edit,
                        label = "تعديل",
                        onClick = onEdit,
                        color = Color(0xFF1976D2),
                        width = dynamicButtonWidth,
                        height = dynamicButtonHeight,
                        iconSize = dynamicIconSize,
                        textSize = dynamicButtonTextSize
                    )
                    Spacer(Modifier.width(dynamicButtonSpacing))
                    ActionButton(
                        icon = Icons.Default.Delete,
                        label = "حذف",
                        onClick = onDelete,
                        color = Color(0xFFD32F2F),
                        width = dynamicButtonWidth,
                        height = dynamicButtonHeight,
                        iconSize = dynamicIconSize,
                        textSize = dynamicButtonTextSize
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
    fontSize: TextUnit = 15.sp
) {
    val maxChars = 80
    if (searchQuery.isBlank()) {
        Text(
            text = description,
            fontSize = fontSize,
            color = Color(0xFF444444),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier.padding(horizontal = 6.dp)
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
                modifier = modifier.padding(horizontal = 6.dp)
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
                modifier = modifier.padding(horizontal = 6.dp)
            )
        }
    }
} 
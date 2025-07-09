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

@Composable
fun TransactionCard(
    transaction: Transaction,
    accounts: List<Account>,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onWhatsApp: () -> Unit,
    onSms: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDebit = transaction.getType()?.lowercase() == "debit" || transaction.getType() == "عليه"
    val gradient = if (isDebit) {
        Brush.linearGradient(
            colors = listOf(Color(0xFFFF5252), Color(0xFFFF8A80)),
            start = Offset(0f, 0f),
            end = Offset(400f, 400f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFF43EA7D), Color(0xFF1CBF4F)),
            start = Offset(0f, 0f),
            end = Offset(400f, 400f)
        )
    }
    val accountName = accounts.find { it.getId() == transaction.getAccountId() }?.getName() ?: "--"
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val buttonSize = screenHeight * 0.06f // زيادة الحجم: 6% من ارتفاع الشاشة
    val iconSize = screenHeight * 0.035f // زيادة الحجم: 3.5% من ارتفاع الشاشة
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // السطر الأول: اسم الحساب والمبلغ
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = accountName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White,
                        maxLines = 1
                    )
                    Text(
                        text = "${transaction.getAmount()} ${transaction.getCurrency()}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(2.dp))
                // السطر الثاني: الوصف والتاريخ
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HighlightedDescription(
                        description = transaction.getDescription() ?: "",
                        searchQuery = searchQuery,
                        modifier = Modifier.weight(1f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.White, modifier = Modifier.size(iconSize))
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = transaction.getDateString(),
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Spacer(Modifier.weight(1f)) // يدفع الأزرار للأسفل دائماً
            }
            // الأزرار دائماً في الأسفل
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionCircleButton(icon = Icons.Default.Delete, borderColor = Color.Red, onClick = onDelete, size = buttonSize, iconSize = iconSize)
                ActionCircleButton(icon = Icons.Default.Edit, borderColor = Color(0xFF1976D2), onClick = onEdit, size = buttonSize, iconSize = iconSize)
                ActionCircleButton(painter = painterResource(id = R.drawable.ic_sms), borderColor = Color(0xFF1976D2), onClick = onSms, size = buttonSize, iconSize = iconSize)
                ActionCircleButton(painter = painterResource(id = R.drawable.ic_whatsapp), borderColor = Color(0xFF25D366), onClick = onWhatsApp, size = buttonSize, iconSize = iconSize)
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
    if (searchQuery.isBlank()) {
        Text(
            text = description,
            fontSize = 14.sp,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
        )
    } else {
        val index = description.indexOf(searchQuery, ignoreCase = true)
        if (index == -1) {
            // لا يوجد تطابق، اعرض أول سطرين عادي
            Text(
                text = description,
                fontSize = 14.sp,
                color = Color.White,
                maxLines = 2,
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
                fontSize = 14.sp,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = modifier
            )
        }
    }
} 
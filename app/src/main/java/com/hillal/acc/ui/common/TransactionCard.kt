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
    val buttonSize = screenHeight * 0.033f // 3.3% من ارتفاع الشاشة (مثلاً ~26dp على شاشة 800dp)
    val iconSize = screenHeight * 0.018f // 1.8% من ارتفاع الشاشة
    Card(
        modifier = modifier
            .fillMaxWidth()
            // لا تضع .height هنا، بل مررها من TransactionsScreen
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
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // اسم الحساب بشكل بارز في الأعلى
                Text(
                    text = accountName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    // المبلغ
                    Text(
                        text = "${transaction.getAmount()} ${transaction.getCurrency()}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = Color.White
                    )
                    // التاريخ + أيقونة ساعة
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
                Spacer(Modifier.height(2.dp))
                // الوصف
                Text(
                    text = transaction.getDescription() ?: "",
                    fontSize = 14.sp,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                )
                // الأزرار
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
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
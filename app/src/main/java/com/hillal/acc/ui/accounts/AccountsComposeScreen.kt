package com.hillal.acc.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.Observer
import com.hillal.acc.R
import com.hillal.acc.data.model.Account
import java.util.*
import com.hillal.acc.ui.theme.AppTheme
import com.hillal.acc.ui.theme.LocalAppDimensions
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.res.painterResource
import com.hillal.acc.ui.theme.success
import com.hillal.acc.ui.theme.successContainer
import androidx.compose.foundation.shadow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsComposeScreen(
    viewModel: AccountViewModel,
    onNavigateToAddAccount: () -> Unit,
    onNavigateToEditAccount: (Long) -> Unit,
    onNavigateToAccountDetails: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    AppTheme {
        val dimens = LocalAppDimensions.current
        val colors = MaterialTheme.colorScheme
        val typography = MaterialTheme.typography

        val accounts by viewModel.allAccounts.observeAsState(initial = emptyList())
        val accountBalances = remember(accounts) {
            val balances = mutableStateMapOf<Long, Double>()
            accounts.forEach { account: Account ->
                balances[account.id] = account.balance
            }
            balances
        }
        var searchQuery by remember { mutableStateOf("") }
        var isAscendingSort by remember { mutableStateOf(true) }
        var currentSortType by remember { mutableStateOf("balance") }

        // حساب الإحصائيات
        val totalAccounts = accounts.size
        val activeAccounts = accounts.count { account: Account -> account.isWhatsappEnabled() }

        // البحث في الحسابات
        val filteredAccounts = remember(accounts, searchQuery) {
            if (searchQuery.isEmpty()) {
                accounts
            } else {
                accounts.filter { account: Account ->
                    account.name.contains(searchQuery, ignoreCase = true) ||
                    account.phoneNumber.contains(searchQuery, ignoreCase = true)
                }
            }
        }

        // ترتيب الحسابات
        val sortedAccounts = remember(filteredAccounts, currentSortType, isAscendingSort, accountBalances) {
            when (currentSortType) {
                "balance" -> {
                    if (isAscendingSort) {
                        filteredAccounts.sortedBy { account: Account -> accountBalances[account.id] ?: 0.0 }
                    } else {
                        filteredAccounts.sortedByDescending { account: Account -> accountBalances[account.id] ?: 0.0 }
                    }
                }
                "name" -> {
                    if (isAscendingSort) {
                        filteredAccounts.sortedBy { account: Account -> account.name }
                    } else {
                        filteredAccounts.sortedByDescending { account: Account -> account.name }
                    }
                }
                "number" -> {
                    if (isAscendingSort) {
                        filteredAccounts.sortedBy { account: Account -> account.serverId }
                    } else {
                        filteredAccounts.sortedByDescending { account: Account -> account.serverId }
                    }
                }
                "date" -> {
                    if (isAscendingSort) {
                        filteredAccounts.sortedBy { account: Account -> account.createdAt }
                    } else {
                        filteredAccounts.sortedByDescending { account: Account -> account.createdAt }
                    }
                }
                else -> filteredAccounts
            }
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // رأس الصفحة الجديد
                AccountsHeader(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    totalCount = filteredAccounts.size,
                    onFilterClick = { /* TODO: فلترة */ }
                )

                // قائمة الحسابات
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = dimens.spacingLarge),
                    verticalArrangement = Arrangement.spacedBy(dimens.spacingMedium)
                ) {
                    items(
                        items = sortedAccounts,
                        key = { account -> account.id }
                    ) { account ->
                        AccountItemModern(
                            account = account,
                            balance = accountBalances[account.id] ?: 0.0,
                            onWhatsAppToggle = { isEnabled ->
                                account.setWhatsappEnabled(isEnabled)
                                account.setUpdatedAt(System.currentTimeMillis())
                                viewModel.updateAccount(account)
                            },
                            onEditClick = { onNavigateToEditAccount(account.id) },
                            onItemClick = { onNavigateToAccountDetails(account.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }

            // زر إضافة حساب عائم
            FloatingActionButton(
                onClick = onNavigateToAddAccount,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(dimens.spacingLarge),
                containerColor = colors.primary,
                contentColor = colors.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "إضافة حساب",
                    modifier = Modifier.size(dimens.iconSize * 1.2f)
                )
            }
        }
    }
}

@Composable
private fun AccountsHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    totalCount: Int,
    onFilterClick: () -> Unit
) {
    val dimens = LocalAppDimensions.current
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(bottom = dimens.spacingMedium)
            .shadow(2.dp, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
    ) {
        // شريط العنوان والأزرار
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.spacingLarge, vertical = dimens.spacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* TODO: تحديث */ }) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "تحديث", tint = colors.primary)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "إدارة النقاط والفروع",
                color = colors.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { /* TODO: رجوع */ }) {
                Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "رجوع", tint = colors.primary)
            }
        }
        // شريط البحث والفلتر
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.spacingLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(colors.primary, shape = CircleShape)
                    .clickable { onFilterClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.FilterList, contentDescription = "فلترة", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(dimens.spacingSmall))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("الاسم | رقم الحساب | الموبايل", color = colors.onSurfaceVariant) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.onSurfaceVariant,
                    cursorColor = colors.primary
                ),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium
            )
        }
        // عدد النتائج
        Text(
            text = "${totalCount} :العدد",
            color = colors.onSurfaceVariant,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = dimens.spacingLarge, top = 4.dp)
        )
    }
}

@Composable
private fun AccountItemModern(
    account: Account,
    balance: Double,
    onWhatsAppToggle: (Boolean) -> Unit,
    onEditClick: () -> Unit,
    onItemClick: () -> Unit
) {
    val dimens = LocalAppDimensions.current
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onItemClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // يسار: سويتش واتساب + زر تعديل
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = account.isWhatsappEnabled(),
                        onCheckedChange = onWhatsAppToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.success,
                            checkedTrackColor = colors.successContainer,
                            uncheckedThumbColor = colors.onSurfaceVariant,
                            uncheckedTrackColor = colors.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("واتساب", fontSize = 12.sp, color = colors.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onEditClick,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "تعديل",
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تعديل", fontSize = 13.sp, color = Color.White)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            // يمين: بيانات الحساب وصورة شخصية
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // صورة شخصية دائرية
                Card(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = colors.primary.copy(alpha = 0.13f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = colors.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                // بيانات الحساب
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = account.name,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary,
                        fontSize = 17.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = colors.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = account.phoneNumber,
                            fontSize = 13.sp,
                            color = colors.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = colors.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (account.serverId > 0) "رقم الحساب: ${account.serverId}" else "رقم: غير محدد",
                            fontSize = 13.sp,
                            color = colors.onSurfaceVariant,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                    Text(
                        text = if (balance < 0) {
                            String.format(Locale.US, "عليه %,d يمني", kotlin.math.abs(balance.toLong()))
                        } else {
                            String.format(Locale.US, "له %,d يمني", balance.toLong())
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (balance < 0) colors.error else colors.success,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
} 
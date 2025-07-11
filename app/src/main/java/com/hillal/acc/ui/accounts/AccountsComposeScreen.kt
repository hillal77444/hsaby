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

        // الحصول على الأرصدة - سيتم تحديثها تلقائياً عند تغيير الحسابات
        // accountBalances سيتم تحديثها من خلال observeAsState

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background)
                .navigationBarsPadding()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(dimens.spacingLarge),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingLarge)
            ) {
                // Header Section
                item {
                    HeaderSection()
                }

                // Search and Filter Section
                item {
                    SearchAndFilterSection(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        currentSortType = currentSortType,
                        onSortTypeChange = {
                            currentSortType = it
                            isAscendingSort = !isAscendingSort
                        },
                        onFilterClick = { /* TODO: Implement filter */ }
                    )
                }

                // Statistics Section
                item {
                    StatisticsSection(
                        totalAccounts = totalAccounts,
                        activeAccounts = activeAccounts
                    )
                }

                // Accounts List
                items(
                    items = sortedAccounts,
                    key = { account -> account.id }
                ) { account ->
                    AccountItem(
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

                // Bottom spacing for FAB
                item {
                    Spacer(modifier = Modifier.height(dimens.cardCorner * 2))
                }
            }

            // Floating Action Button
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
                    contentDescription = stringResource(R.string.add_account),
                    modifier = Modifier.size(dimens.iconSize * 1.2f)
                )
            }
        }
    }
}

@Composable
private fun HeaderSection() {
    val dimens = LocalAppDimensions.current
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo Circle
        Card(
            modifier = Modifier
                .size(dimens.cardCorner * 3.2f)
                .padding(top = dimens.spacingLarge),
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(dimens.iconSize * 1.3f),
                    tint = colors.primary
                )
            }
        }

        // Title with Icon
        Row(
            modifier = Modifier.padding(top = dimens.spacingLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(dimens.iconSize),
                tint = colors.primary
            )
            Spacer(modifier = Modifier.width(dimens.spacingSmall))
            Text(
                text = "إدارة الحسابات",
                fontSize = typography.headlineMedium.fontSize,
                fontWeight = FontWeight.Bold,
                color = colors.primary,
                style = typography.headlineMedium
            )
        }

        // Description
        Text(
            text = "عرض وإدارة جميع الحسابات",
            fontSize = typography.bodyMedium.fontSize,
            color = colors.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = dimens.spacingSmall, bottom = dimens.spacingLarge),
            style = typography.bodyMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchAndFilterSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    currentSortType: String,
    onSortTypeChange: (String) -> Unit,
    onFilterClick: () -> Unit
) {
    val dimens = LocalAppDimensions.current
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimens.cardCorner),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(dimens.spacingLarge),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingMedium)
        ) {
            // Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("البحث في الحسابات...", fontSize = typography.bodyMedium.fontSize) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(dimens.iconSize)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.onSurfaceVariant
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = typography.bodyMedium.fontSize)
            )

            // Filter and Sort Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimens.spacingSmall)
            ) {
                Button(
                    onClick = onFilterClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.surfaceVariant,
                        contentColor = colors.primary
                    ),
                    shape = RoundedCornerShape(dimens.cardCorner)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(dimens.iconSize * 0.7f)
                    )
                    Spacer(modifier = Modifier.width(dimens.spacingSmall))
                    Text("تصفية", fontSize = typography.bodyMedium.fontSize)
                }

                Button(
                    onClick = {
                        val nextSortType = when (currentSortType) {
                            "balance" -> "name"
                            "name" -> "number"
                            "number" -> "date"
                            "date" -> "balance"
                            else -> "balance"
                        }
                        onSortTypeChange(nextSortType)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.surfaceVariant,
                        contentColor = colors.primary
                    ),
                    shape = RoundedCornerShape(dimens.cardCorner)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = null,
                        modifier = Modifier.size(dimens.iconSize * 0.7f)
                    )
                    Spacer(modifier = Modifier.width(dimens.spacingSmall))
                    Text(
                        when (currentSortType) {
                            "balance" -> "ترتيب (الرصيد)"
                            "name" -> "ترتيب (الاسم)"
                            "number" -> "ترتيب (الرقم)"
                            "date" -> "ترتيب (التاريخ)"
                            else -> "ترتيب (الرصيد)"
                        },
                        fontSize = typography.bodyMedium.fontSize
                    )
                }
            }
        }
    }
}

@Composable
private fun StatisticsSection(
    totalAccounts: Int,
    activeAccounts: Int
) {
    val dimens = LocalAppDimensions.current
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimens.spacingSmall)
    ) {
        // Total Accounts Card
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(dimens.cardCorner),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(dimens.spacingLarge),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = totalAccounts.toString(),
                    fontSize = typography.headlineSmall.fontSize,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )
                Text(
                    text = "إجمالي الحسابات",
                    fontSize = typography.bodySmall.fontSize,
                    color = colors.onSurfaceVariant
                )
            }
        }

        // Active Accounts Card
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(dimens.cardCorner),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(dimens.spacingLarge),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = activeAccounts.toString(),
                    fontSize = typography.headlineSmall.fontSize,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )
                Text(
                    text = "الحسابات النشطة",
                    fontSize = typography.bodySmall.fontSize,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AccountItem(
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
            .clickable { onItemClick() },
        shape = RoundedCornerShape(dimens.cardCorner),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(dimens.spacingLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Account Icon
            Card(
                modifier = Modifier.size(dimens.iconSize * 2),
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(dimens.iconSize),
                        tint = colors.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(dimens.spacingMedium))

            // Account Information
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = account.name,
                    fontSize = typography.bodyMedium.fontSize,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = dimens.spacingSmall)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(dimens.iconSize * 0.7f),
                        tint = colors.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(dimens.spacingSmall))
                    Text(
                        text = account.phoneNumber,
                        fontSize = typography.bodySmall.fontSize,
                        color = colors.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = dimens.spacingSmall)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(dimens.iconSize * 0.7f),
                        tint = colors.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(dimens.spacingSmall))
                    Text(
                        text = if (account.serverId > 0) "رقم الحساب: ${account.serverId}" else "رقم: غير محدد",
                        fontSize = typography.bodySmall.fontSize,
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
                    fontSize = typography.bodyMedium.fontSize,
                    fontWeight = FontWeight.Bold,
                    color = if (balance < 0) colors.error else colors.success,
                    modifier = Modifier.padding(top = dimens.spacingSmall)
                )
            }

            // Action Buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // WhatsApp Switch
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (account.isWhatsappEnabled()) {
                        Box(
                            modifier = Modifier
                                .size(dimens.iconSize * 1.1f)
                                .background(
                                    color = colors.success,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_whatsapp),
                                contentDescription = "واتساب",
                                tint = colors.onPrimary,
                                modifier = Modifier.size(dimens.iconSize * 0.7f)
                            )
                        }
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_whatsapp),
                            contentDescription = "واتساب",
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(dimens.iconSize * 0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.width(dimens.spacingSmall))
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
                }

                Spacer(modifier = Modifier.height(dimens.spacingSmall))

                // Edit Button
                Button(
                    onClick = onEditClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary
                    ),
                    shape = RoundedCornerShape(dimens.cardCorner),
                    modifier = Modifier.height(dimens.iconSize * 1.5f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(dimens.iconSize * 0.7f)
                    )
                    Spacer(modifier = Modifier.width(dimens.spacingSmall))
                    Text(
                        text = "تعديل",
                        fontSize = typography.bodySmall.fontSize
                    )
                }
            }
        }
    }
} 
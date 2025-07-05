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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hillal.acc.R
import com.hillal.acc.data.model.Account
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsComposeScreen(
    viewModel: AccountViewModel,
    onNavigateToAddAccount: () -> Unit,
    onNavigateToEditAccount: (Long) -> Unit,
    onNavigateToAccountDetails: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    ResponsiveAccountsTheme {
        val spacing = ResponsiveSpacing()
        val padding = ResponsivePadding()
        
        val accounts by viewModel.allAccounts.collectAsStateWithLifecycle(initialValue = emptyList())
        val accountBalances = remember { mutableStateMapOf<Long, Double>() }
        var searchQuery by remember { mutableStateOf("") }
        var isAscendingSort by remember { mutableStateOf(true) }
        var currentSortType by remember { mutableStateOf("balance") }
        
        // حساب الإحصائيات
        val totalAccounts = accounts.size
        val activeAccounts = accounts.count { it.isWhatsappEnabled }
        
        // البحث في الحسابات
        val filteredAccounts = remember(accounts, searchQuery) {
            if (searchQuery.isEmpty()) {
                accounts
            } else {
                accounts.filter { account ->
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
                        filteredAccounts.sortedBy { accountBalances[it.id] ?: 0.0 }
                    } else {
                        filteredAccounts.sortedByDescending { accountBalances[it.id] ?: 0.0 }
                    }
                }
                "name" -> {
                    if (isAscendingSort) {
                        filteredAccounts.sortedBy { it.name }
                    } else {
                        filteredAccounts.sortedByDescending { it.name }
                    }
                }
                "number" -> {
                    if (isAscendingSort) {
                        filteredAccounts.sortedBy { it.serverId }
                    } else {
                        filteredAccounts.sortedByDescending { it.serverId }
                    }
                }
                "date" -> {
                    if (isAscendingSort) {
                        filteredAccounts.sortedBy { it.createdAt }
                    } else {
                        filteredAccounts.sortedByDescending { it.createdAt }
                    }
                }
                else -> filteredAccounts
            }
        }
        
        // الحصول على الأرصدة
        LaunchedEffect(accounts) {
            accounts.forEach { account ->
                viewModel.getAccountBalanceYemeni(account.id).collect { balance ->
                    accountBalances[account.id] = balance ?: 0.0
                }
            }
        }
        
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding.large),
                verticalArrangement = Arrangement.spacedBy(spacing.large)
            ) {
                // Header Section
                item {
                    HeaderSection(spacing = spacing, padding = padding)
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
                        onFilterClick = { /* TODO: Implement filter */ },
                        spacing = spacing,
                        padding = padding
                    )
                }
                
                // Statistics Section
                item {
                    StatisticsSection(
                        totalAccounts = totalAccounts,
                        activeAccounts = activeAccounts,
                        spacing = spacing,
                        padding = padding
                    )
                }
                
                // Accounts List
                items(sortedAccounts) { account ->
                    AccountItem(
                        account = account,
                        balance = accountBalances[account.id] ?: 0.0,
                        onWhatsAppToggle = { isEnabled ->
                            account.isWhatsappEnabled = isEnabled
                            account.updatedAt = System.currentTimeMillis()
                            viewModel.updateAccount(account)
                        },
                        onEditClick = { onNavigateToEditAccount(account.id) },
                        onItemClick = { onNavigateToAccountDetails(account.id) },
                        spacing = spacing,
                        padding = padding
                    )
                }
                
                // Bottom spacing for FAB
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
            
            // Floating Action Button
            FloatingActionButton(
                onClick = onNavigateToAddAccount,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(padding.large),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_account)
                )
            }
        }
    }
}

@Composable
private fun HeaderSection(
    spacing: ResponsiveSpacingValues,
    padding: ResponsivePaddingValues
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo Circle
        Card(
            modifier = Modifier
                .size(64.dp)
                .padding(top = spacing.xl),
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color(0xFF152FD9)
                )
            }
        }
        
        // Title with Icon
        Row(
            modifier = Modifier.padding(top = spacing.large),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color(0xFF152FD9)
            )
            Spacer(modifier = Modifier.width(spacing.small))
            Text(
                text = "إدارة الحسابات",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF152FD9)
            )
        }
        
        // Description
        Text(
            text = "عرض وإدارة جميع الحسابات",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.small, bottom = spacing.large)
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
    onFilterClick: () -> Unit,
    spacing: ResponsiveSpacingValues,
    padding: ResponsivePaddingValues
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(padding.large),
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            // Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("البحث في الحسابات...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF152FD9)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF152FD9),
                    unfocusedBorderColor = Color(0xFF666666)
                )
            )
            
            // Filter and Sort Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                Button(
                    onClick = onFilterClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF3F4F6),
                        contentColor = Color(0xFF152FD9)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(spacing.small))
                    Text("تصفية")
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
                        containerColor = Color(0xFFF3F4F6),
                        contentColor = Color(0xFF152FD9)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(spacing.small))
                    Text(
                        when (currentSortType) {
                            "balance" -> "ترتيب (الرصيد)"
                            "name" -> "ترتيب (الاسم)"
                            "number" -> "ترتيب (الرقم)"
                            "date" -> "ترتيب (التاريخ)"
                            else -> "ترتيب (الرصيد)"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatisticsSection(
    totalAccounts: Int,
    activeAccounts: Int,
    spacing: ResponsiveSpacingValues,
    padding: ResponsivePaddingValues
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.small)
    ) {
        // Total Accounts Card
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(padding.large),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = totalAccounts.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
                Text(
                    text = "إجمالي الحسابات",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666)
                )
            }
        }
        
        // Active Accounts Card
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(padding.large),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = activeAccounts.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
                Text(
                    text = "الحسابات النشطة",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666)
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
    onItemClick: () -> Unit,
    spacing: ResponsiveSpacingValues,
    padding: ResponsivePaddingValues
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(padding.large),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Account Icon
            Card(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFF152FD9)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(spacing.medium))
            
            // Account Information
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF152FD9)
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = spacing.small)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF666666)
                    )
                    Spacer(modifier = Modifier.width(spacing.small))
                    Text(
                        text = account.phoneNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666)
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = spacing.small)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF666666)
                    )
                    Spacer(modifier = Modifier.width(spacing.small))
                    Text(
                        text = if (account.serverId > 0) "رقم الحساب: ${account.serverId}" else "رقم: غير محدد",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF999999),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
                
                Text(
                    text = if (balance < 0) {
                        String.format(Locale.US, "عليه %,d يمني", kotlin.math.abs(balance.toLong()))
                    } else {
                        String.format(Locale.US, "له %,d يمني", balance.toLong())
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (balance < 0) Color(0xFFE53E3E) else Color(0xFF22C55E),
                    modifier = Modifier.padding(top = spacing.small)
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
                    Text(
                        text = "واتساب",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666)
                    )
                    Spacer(modifier = Modifier.width(spacing.small))
                    Switch(
                        checked = account.isWhatsappEnabled,
                        onCheckedChange = onWhatsAppToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = if (account.isWhatsappEnabled) Color(0xFF22C55E) else Color(0xFF666666),
                            checkedTrackColor = if (account.isWhatsappEnabled) Color(0xFF22C55E) else Color(0xFFE0E0E0),
                            uncheckedThumbColor = Color(0xFF666666),
                            uncheckedTrackColor = Color(0xFFE0E0E0)
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(spacing.small))
                
                // Edit Button
                Button(
                    onClick = onEditClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF152FD9)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(spacing.small))
                    Text(
                        text = "تعديل",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
} 
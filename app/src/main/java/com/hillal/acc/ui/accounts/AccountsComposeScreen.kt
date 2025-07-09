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
import com.hillal.acc.ui.theme.ProvideResponsiveDimensions
import com.hillal.acc.ui.theme.LocalResponsiveDimensions
import com.hillal.acc.ui.accounts.ResponsiveAccountsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsComposeScreen(
    viewModel: AccountViewModel,
    onNavigateToAddAccount: () -> Unit,
    onNavigateToEditAccount: (Long) -> Unit,
    onNavigateToAccountDetails: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    ProvideResponsiveDimensions {
        ResponsiveAccountsTheme {
            val dimensions = LocalResponsiveDimensions.current
            
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
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(dimensions.spacingLarge),
                    verticalArrangement = Arrangement.spacedBy(dimensions.spacingLarge)
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
                        Spacer(modifier = Modifier.height(dimensions.cardHeight * 0.5f))
                    }
                }
                
                // Floating Action Button
                FloatingActionButton(
                    onClick = onNavigateToAddAccount,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(dimensions.spacingLarge),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_account),
                        modifier = Modifier.size(dimensions.iconSize * 1.2f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderSection() {
    val dimensions = LocalResponsiveDimensions.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo Circle
        Card(
            modifier = Modifier
                .size(dimensions.cardHeight * 0.53f)
                .padding(top = dimensions.spacingLarge),
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
                    modifier = Modifier.size(dimensions.iconSize * 1.3f),
                    tint = Color(0xFF152FD9)
                )
            }
        }
        
        // Title with Icon
        Row(
            modifier = Modifier.padding(top = dimensions.spacingLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(dimensions.iconSize),
                tint = Color(0xFF152FD9)
            )
            Spacer(modifier = Modifier.width(dimensions.spacingSmall))
            Text(
                text = "إدارة الحسابات",
                fontSize = dimensions.titleFont,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF152FD9)
            )
        }
        
        // Description
        Text(
            text = "عرض وإدارة جميع الحسابات",
            fontSize = dimensions.bodyFont,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = dimensions.spacingSmall, bottom = dimensions.spacingLarge)
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
    val dimensions = LocalResponsiveDimensions.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensions.cardCorner),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(dimensions.spacingLarge),
            verticalArrangement = Arrangement.spacedBy(dimensions.spacingMedium)
        ) {
            // Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("البحث في الحسابات...", fontSize = dimensions.bodyFont) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF152FD9),
                        modifier = Modifier.size(dimensions.iconSize)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF152FD9),
                    unfocusedBorderColor = Color(0xFF666666)
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = dimensions.bodyFont)
            )
            
            // Filter and Sort Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
            ) {
                Button(
                    onClick = onFilterClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF3F4F6),
                        contentColor = Color(0xFF152FD9)
                    ),
                    shape = RoundedCornerShape(dimensions.cardCorner)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(dimensions.iconSize * 0.7f)
                    )
                    Spacer(modifier = Modifier.width(dimensions.spacingSmall))
                    Text("تصفية", fontSize = dimensions.bodyFont)
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
                    shape = RoundedCornerShape(dimensions.cardCorner)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = null,
                        modifier = Modifier.size(dimensions.iconSize * 0.7f)
                    )
                    Spacer(modifier = Modifier.width(dimensions.spacingSmall))
                    Text(
                        when (currentSortType) {
                            "balance" -> "ترتيب (الرصيد)"
                            "name" -> "ترتيب (الاسم)"
                            "number" -> "ترتيب (الرقم)"
                            "date" -> "ترتيب (التاريخ)"
                            else -> "ترتيب (الرصيد)"
                        },
                        fontSize = dimensions.bodyFont
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
    val dimensions = LocalResponsiveDimensions.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
    ) {
        // Total Accounts Card
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(dimensions.cardCorner),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(dimensions.spacingLarge),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = totalAccounts.toString(),
                    fontSize = dimensions.statFont,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
                Text(
                    text = "إجمالي الحسابات",
                    fontSize = dimensions.statLabelFont,
                    color = Color(0xFF666666)
                )
            }
        }
        
        // Active Accounts Card
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(dimensions.cardCorner),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(dimensions.spacingLarge),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = activeAccounts.toString(),
                    fontSize = dimensions.statFont,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
                Text(
                    text = "الحسابات النشطة",
                    fontSize = dimensions.statLabelFont,
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
    onItemClick: () -> Unit
) {
    val dimensions = LocalResponsiveDimensions.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() },
        shape = RoundedCornerShape(dimensions.cardCorner),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(dimensions.spacingLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Account Icon
            Card(
                modifier = Modifier.size(dimensions.iconSize * 2),
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
                        modifier = Modifier.size(dimensions.iconSize),
                        tint = Color(0xFF152FD9)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(dimensions.spacingMedium))
            
            // Account Information
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = account.name,
                    fontSize = dimensions.bodyFont,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF152FD9)
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = dimensions.spacingSmall)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(dimensions.iconSize * 0.7f),
                        tint = Color(0xFF666666)
                    )
                    Spacer(modifier = Modifier.width(dimensions.spacingSmall))
                    Text(
                        text = account.phoneNumber,
                        fontSize = dimensions.statLabelFont,
                        color = Color(0xFF666666)
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = dimensions.spacingSmall)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(dimensions.iconSize * 0.7f),
                        tint = Color(0xFF666666)
                    )
                    Spacer(modifier = Modifier.width(dimensions.spacingSmall))
                    Text(
                        text = if (account.serverId > 0) "رقم الحساب: ${account.serverId}" else "رقم: غير محدد",
                        fontSize = dimensions.statLabelFont,
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
                    fontSize = dimensions.bodyFont,
                    fontWeight = FontWeight.Bold,
                    color = if (balance < 0) Color(0xFFE53E3E) else Color(0xFF22C55E),
                    modifier = Modifier.padding(top = dimensions.spacingSmall)
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
                        fontSize = dimensions.statLabelFont,
                        color = Color(0xFF666666)
                    )
                    Spacer(modifier = Modifier.width(dimensions.spacingSmall))
                    Switch(
                        checked = account.isWhatsappEnabled(),
                        onCheckedChange = onWhatsAppToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = if (account.isWhatsappEnabled()) Color(0xFF22C55E) else Color(0xFF666666),
                            checkedTrackColor = if (account.isWhatsappEnabled()) Color(0xFF22C55E) else Color(0xFFE0E0E0),
                            uncheckedThumbColor = Color(0xFF666666),
                            uncheckedTrackColor = Color(0xFFE0E0E0)
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(dimensions.spacingSmall))
                
                // Edit Button
                Button(
                    onClick = onEditClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF152FD9)
                    ),
                    shape = RoundedCornerShape(dimensions.cardCorner),
                    modifier = Modifier.height(dimensions.iconSize * 1.5f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(dimensions.iconSize * 0.7f)
                    )
                    Spacer(modifier = Modifier.width(dimensions.spacingSmall))
                    Text(
                        text = "تعديل",
                        fontSize = dimensions.statLabelFont
                    )
                }
            }
        }
    }
} 
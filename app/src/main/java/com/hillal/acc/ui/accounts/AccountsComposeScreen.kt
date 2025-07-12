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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hillal.acc.R
import com.hillal.acc.data.model.Account
import com.hillal.acc.ui.theme.AppTheme
import com.hillal.acc.ui.theme.LocalAppDimensions
import com.hillal.acc.ui.theme.success
import com.hillal.acc.ui.theme.successContainer
import java.util.*
import androidx.compose.ui.draw.scale

@Composable
fun AccountsAppBar(
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    TopAppBar(
        title = { Text("إدارة الحسابات", color = Color(0xFF1976D2), fontWeight = FontWeight.Bold, fontSize = 22.sp) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color(0xFF1976D2))
            }
        },
        actions = {
            IconButton(onClick = onRefreshClick) {
                Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = Color(0xFF1976D2))
            }
        }
    )
}

@Composable
fun AccountsCustomHeader(
    title: String,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    val blue = Color(0xFF1976D2)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp, start = 12.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // زر التحديث الدائري
        IconButton(
            onClick = onRefreshClick,
            modifier = Modifier
                .size(40.dp)
                .background(blue.copy(alpha = 0.08f), shape = CircleShape)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = blue)
        }
        Spacer(modifier = Modifier.weight(1f))
        // العنوان في المنتصف
        Text(
            text = title,
            color = blue,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier
                .align(Alignment.CenterVertically)
        )
        Spacer(modifier = Modifier.weight(1f))
        // زر الرجوع الدائري
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .size(40.dp)
                .background(blue.copy(alpha = 0.08f), shape = CircleShape)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = blue)
        }
    }
}

@Composable
fun AccountsSearchAndFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortType: String,
    sortOptions: List<Pair<String, String>>,
    filterMenuExpanded: Boolean,
    onFilterClick: () -> Unit,
    onFilterSelect: (String) -> Unit,
    onFilterDismiss: () -> Unit
) {
    val blue = Color(0xFF1976D2)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // زر الفلترة الدائري
        Box {
            IconButton(
                onClick = onFilterClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(blue.copy(alpha = 0.12f), shape = CircleShape)
            ) {
                Icon(Icons.Default.FilterList, contentDescription = "فلترة", tint = blue)
            }
            DropdownMenu(
                expanded = filterMenuExpanded,
                onDismissRequest = onFilterDismiss
            ) {
                sortOptions.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = { onFilterSelect(value) },
                        leadingIcon = {
                            if (value == sortType) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = blue)
                            }
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        // حقل البحث
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text("الاسم | رقم الحساب | الموبايل", color = Color(0xFFB0B0B0))
            },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFB0B0B0), modifier = Modifier.size(20.dp))
            },
            modifier = Modifier
                .weight(1f)
                .height(44.dp),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = blue,
                unfocusedBorderColor = Color(0xFFF3F4F6),
                cursorColor = blue,
                unfocusedContainerColor = Color(0xFFF3F4F6),
                focusedContainerColor = Color(0xFFF3F4F6)
            ),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium
        )
    }
}

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
            accounts.forEach { account ->
                balances[account.id] = 0.0
            }
            balances
        }
        var searchQuery by remember { mutableStateOf("") }
        var sortType by remember { mutableStateOf("balance_desc") }
        val sortOptions = listOf(
            "balance_desc" to "الرصيد الأكبر",
            "balance_asc" to "الرصيد الأصغر",
            "name" to "الاسم",
            "number" to "رقم الحساب"
        )
        var filterMenuExpanded by remember { mutableStateOf(false) }

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
        val sortedAccounts = remember(filteredAccounts, sortType, accountBalances) {
            when (sortType) {
                "balance_desc" -> filteredAccounts.sortedByDescending { account: Account -> accountBalances[account.id] ?: 0.0 }
                "balance_asc" -> filteredAccounts.sortedBy { account: Account -> accountBalances[account.id] ?: 0.0 }
                "name" -> filteredAccounts.sortedBy { account: Account -> account.name }
                "number" -> filteredAccounts.sortedBy { account: Account -> account.serverId }
                else -> filteredAccounts
            }
        }

        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onNavigateToAddAccount,
                    modifier = Modifier.padding(dimens.spacingLarge),
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "إضافة حساب",
                        modifier = Modifier.size(dimens.iconSize * 1.2f)
                    )
                }
            },
            containerColor = colors.background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // الهيدر المخصص
                AccountsCustomHeader(
                    title = "إدارة النقاط والفروع", // يمكنك تغيير النص هنا
                    onBackClick = { /* TODO: رجوع */ },
                    onRefreshClick = { /* TODO: تحديث */ }
                )
                // شريط البحث والفلترة
                AccountsSearchAndFilterBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    sortType = sortType,
                    sortOptions = sortOptions,
                    filterMenuExpanded = filterMenuExpanded,
                    onFilterClick = { filterMenuExpanded = true },
                    onFilterSelect = {
                        sortType = it
                        filterMenuExpanded = false
                    },
                    onFilterDismiss = { filterMenuExpanded = false }
                )
                // عدد النتائج
                Text(
                    text = "العدد: ${filteredAccounts.size}",
                    color = Color(0xFF888888),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 32.dp, top = 2.dp, bottom = 4.dp)
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
                            viewModel = viewModel,
                            accountBalances = accountBalances,
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
        }
    }
}

@Composable
private fun AccountItemModern(
    account: Account,
    viewModel: AccountViewModel,
    accountBalances: MutableMap<Long, Double>,
    onWhatsAppToggle: (Boolean) -> Unit,
    onEditClick: () -> Unit,
    onItemClick: () -> Unit
) {
    val dimens = LocalAppDimensions.current
    val blue = Color(0xFF1976D2)
    val green = Color(0xFF43A047)
    val red = Color(0xFFD32F2F)
    val gray = Color(0xFF666666)
    val lightGray = Color(0xFF999999)
    val balanceYemeni by viewModel.getAccountBalanceYemeni(account.id ?: 0L).observeAsState(0.0)
    LaunchedEffect(balanceYemeni) { accountBalances[account.id ?: 0L] = balanceYemeni ?: 0.0 }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .shadow(6.dp, RoundedCornerShape(18.dp))
            .clickable { onItemClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // العمود الرئيسي - معلومات الحساب
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // اسم الحساب
                Text(
                    text = account.name,
                    fontWeight = FontWeight.Bold,
                    color = blue,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                // رقم الهاتف
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = gray
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = account.phoneNumber,
                        fontSize = 13.sp,
                        color = gray
                    )
                }
                
                Spacer(modifier = Modifier.height(3.dp))
                
                // رقم الحساب
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = gray
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (account.serverId > 0) "رقم الحساب: ${account.serverId}" else "رقم: غير محدد",
                        fontSize = 12.sp,
                        color = lightGray,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // أيقونة شخص
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = gray
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "شخص",
                        fontSize = 12.sp,
                        color = gray
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // الرصيد
                Text(
                    text = if ((balanceYemeni ?: 0.0) < 0) {
                        String.format(Locale.US, "عليه %,d يمني", kotlin.math.abs((balanceYemeni ?: 0.0).toLong()))
                    } else {
                        String.format(Locale.US, "له %,d يمني", (balanceYemeni ?: 0.0).toLong())
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if ((balanceYemeni ?: 0.0) < 0) red else green,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // المربع الجانبي - أزرار التحكم
            Card(
                modifier = Modifier
                    .padding(start = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // سويتش واتساب
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = account.isWhatsappEnabled(),
                            onCheckedChange = onWhatsAppToggle,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF25D366),
                                checkedTrackColor = Color(0xFF25D366).copy(alpha = 0.3f),
                                uncheckedThumbColor = gray,
                                uncheckedTrackColor = gray.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.scale(0.8f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "واتساب",
                            fontSize = 12.sp,
                            color = gray
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // زر التعديل
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .size(48.dp)
                            .background(blue, shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "تعديل",
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
} 
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
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomAppBar(
    title: String = "إدارة الحسابات",
    onBackClick: () -> Unit = {},
    onRefreshClick: () -> Unit = {},
    useMaterial3: Boolean = true
) {
    val blue = Color(0xFF1976D2)
    if (useMaterial3) {
        TopAppBar(
            title = { Text(title, color = blue, fontWeight = FontWeight.Bold, fontSize = 22.sp) }
            // تمت إزالة الأزرار الجانبية
        )
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp, start = 12.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = title,
                color = blue,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun ModernSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "ابحث...",
    showClear: Boolean = true,
    onClear: (() -> Unit)? = null
) {
    val blue = Color(0xFF1976D2)
    val shadowColor = Color(0x22000000)
    val iconSize = 20.dp
    val cardCorner = 13.dp
    val textFieldHeight = 44.dp
    val textFieldFontSize = 15.sp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(cardCorner), ambientColor = shadowColor, spotColor = shadowColor)
            .background(Color.White, shape = RoundedCornerShape(cardCorner)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // زر الفلترة
        IconButton(
            onClick = onFilterClick,
            modifier = Modifier
                .size(iconSize + 8.dp)
                .background(blue.copy(alpha = 0.10f), shape = CircleShape)
        ) {
            Icon(Icons.Default.FilterList, contentDescription = "فلترة", tint = blue, modifier = Modifier.size(iconSize))
        }
        Spacer(modifier = Modifier.width(0.dp))
        // مربع البحث
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    placeholder,
                    color = Color(0xFFB0B0B0),
                    fontSize = textFieldFontSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = blue, modifier = Modifier.size(iconSize))
            },
            trailingIcon = {
                if (showClear && value.isNotEmpty() && onClear != null) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Close, contentDescription = "مسح", tint = Color(0xFFB0B0B0), modifier = Modifier.size(iconSize - 2.dp))
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(cardCorner),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = blue,
                unfocusedBorderColor = Color(0xFFF3F4F6),
                cursorColor = blue,
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White
            ),
            singleLine = true,
            maxLines = 1,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = textFieldFontSize, color = Color.Black)
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
        val accountBalances by viewModel.getAllAccountsBalancesYemeniMap().observeAsState(initial = emptyMap())
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
        val sortedAccounts = when (sortType) {
            "balance_desc" -> filteredAccounts.sortedByDescending { account ->
                accountBalances[account.id] ?: 0.0
            }
            "balance_asc" -> filteredAccounts.sortedBy { account ->
                accountBalances[account.id] ?: 0.0
            }
            "name" -> filteredAccounts.sortedBy { it.name }
            "number" -> filteredAccounts.sortedBy { it.serverId }
            else -> filteredAccounts
        }

        Scaffold(
            topBar = {
                val dimens = LocalAppDimensions.current
                val colors = MaterialTheme.colorScheme
                val typography = MaterialTheme.typography
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimens.spacingLarge, vertical = dimens.spacingMedium),
                    shape = RoundedCornerShape(dimens.cardCorner),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(dimens.spacingMedium),
                        verticalArrangement = Arrangement.spacedBy(dimens.spacingSmall)
                    ) {
                        Text(
                            text = "إدارة الحسابات",
                            style = typography.headlineMedium,
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = dimens.spacingSmall / 2)
                        )
                        ModernSearchBar(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            onFilterClick = { filterMenuExpanded = true },
                            onClear = { searchQuery = "" },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                DropdownMenu(
                    expanded = filterMenuExpanded,
                    onDismissRequest = { filterMenuExpanded = false }
                ) {
                    sortOptions.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            onClick = {
                                sortType = value
                                filterMenuExpanded = false
                            },
                            leadingIcon = {
                                if (value == sortType) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = colors.primary)
                                }
                            }
                        )
                    }
                }
            },
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
                Text(
                    text = "العدد: ${filteredAccounts.size}",
                    color = Color(0xFF888888),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 32.dp, top = 2.dp, bottom = 4.dp)
                )
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
                            accountBalance = accountBalances[account.id] ?: 0.0,
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
    accountBalance: Double,
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
    // احذف LaunchedEffect خارج Composable
    // واجعل الرصيد يتم جمعه فقط داخل Composable
    // لأنه يتم عرضه في البطاقة
    // val balanceYemeni by viewModel.getAccountBalanceYemeni(account.id ?: 0L).observeAsState(0.0)
    // LaunchedEffect(balanceYemeni) { accountBalances[account.id ?: 0L] = balanceYemeni ?: 0.0 }
    
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
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // الرصيد
                Text(
                    text = if ((accountBalance) < 0) {
                        String.format(Locale.US, "عليه %,d يمني", kotlin.math.abs((accountBalance).toLong()))
                    } else {
                        String.format(Locale.US, "له %,d يمني", (accountBalance).toLong())
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if ((accountBalance) < 0) red else green,
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
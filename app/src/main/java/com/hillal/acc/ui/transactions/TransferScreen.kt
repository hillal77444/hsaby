package com.hillal.acc.ui.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hillal.acc.data.model.Account
import com.hillal.acc.data.entities.Cashbox
import com.hillal.acc.ui.common.AccountPickerField
import com.hillal.acc.ui.common.CashboxPickerField
import com.hillal.acc.ui.theme.LocalAppDimensions
import com.hillal.acc.ui.theme.AppTheme
import com.hillal.acc.R
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    accounts: List<Account>,
    cashboxes: List<Cashbox>,
    currencies: List<String>,
    transactions: List<com.hillal.acc.data.model.Transaction>,
    balancesMap: Map<Long, Map<String, Double>>,
    onAddCashbox: (String) -> Unit,
    onTransfer: (fromAccount: Account, toAccount: Account, cashbox: Cashbox, currency: String, amount: String, notes: String) -> Unit
) {
    AppTheme {
        val dimens = LocalAppDimensions.current
        val colors = MaterialTheme.colorScheme
        val typography = MaterialTheme.typography
        val haptic = LocalHapticFeedback.current
        
        var fromAccount by remember { mutableStateOf<Account?>(null) }
        var toAccount by remember { mutableStateOf<Account?>(null) }
        var selectedCashbox by remember { mutableStateOf<Cashbox?>(null) }
        var selectedCurrency by remember { mutableStateOf(currencies.firstOrNull() ?: "") }
        var amount by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var successMessage by remember { mutableStateOf<String?>(null) }
        var showCurrencyMenu by remember { mutableStateOf(false) }
        var isTransferring by remember { mutableStateOf(false) }
        
        val scrollState = rememberScrollState()
        
        // Animations
        val headerScale by animateFloatAsState(
            targetValue = if (fromAccount != null && toAccount != null) 1.05f else 1f,
            animationSpec = tween(durationMillis = 300)
        )
        
        val cardElevation by animateDpAsState(
            targetValue = if (isTransferring) 8.dp else 4.dp,
            animationSpec = tween(durationMillis = 200)
        )
        
        val buttonScale by animateFloatAsState(
            targetValue = if (fromAccount != null && toAccount != null && selectedCashbox != null && amount.isNotBlank()) 1.02f else 1f,
            animationSpec = tween(durationMillis = 200)
        )

        Surface(
            color = colors.background,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = dimens.spacingMedium)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingSmall) // Reduced spacing
            ) {
                Spacer(Modifier.height(dimens.spacingMedium)) // Reduced top spacing
                
                // Compact Header
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer(scaleX = headerScale, scaleY = headerScale),
                    shape = RoundedCornerShape(dimens.cardCorner),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        colors.primary.copy(alpha = 0.1f),
                                        colors.primary.copy(alpha = 0.05f)
                                    )
                                )
                            )
                            .padding(dimens.spacingSmall) // Reduced padding
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_accounts),
                                contentDescription = null,
                                modifier = Modifier.size(dimens.iconSize),
                                tint = colors.primary
                            )
                            Spacer(Modifier.width(dimens.spacingSmall))
                            Text(
                                text = "تحويل بين الحسابات",
                                style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colors.primary
                            )
                        }
                    }
                }
                
                // Card: Accounts (Horizontal Layout)
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(durationMillis = 600)
                    ) + fadeIn(animationSpec = tween(durationMillis = 600))
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = cardElevation,
                                shape = RoundedCornerShape(dimens.cardCorner)
                            ),
                        shape = RoundedCornerShape(dimens.cardCorner),
                        colors = CardDefaults.cardColors(containerColor = colors.surface)
                    ) {
                        Column(Modifier.padding(dimens.spacingMedium)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = dimens.spacingSmall)
                            ) {
                                Icon(
                                    Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(dimens.iconSize * 0.8f)
                                )
                                Spacer(Modifier.width(dimens.spacingSmall))
                                Text(
                                    "الحسابات",
                                    style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colors.primary
                                )
                            }
                            
                            // Horizontal layout for accounts
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(dimens.spacingSmall)
                            ) {
                                // From Account
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("من:", style = typography.bodySmall, color = colors.onSurface)
                                    AccountPickerField(
                                        label = "اختر الحساب",
                                        accounts = accounts,
                                        transactions = transactions,
                                        balancesMap = balancesMap,
                                        selectedAccount = fromAccount,
                                        onAccountSelected = { 
                                            fromAccount = it
                                            haptic.performHapticFeedback(HapticFeedbackType.SelectionChange)
                                        }
                                    )
                                }
                                
                                // To Account
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("إلى:", style = typography.bodySmall, color = colors.onSurface)
                                    AccountPickerField(
                                        label = "اختر الحساب",
                                        accounts = accounts,
                                        transactions = transactions,
                                        balancesMap = balancesMap,
                                        selectedAccount = toAccount,
                                        onAccountSelected = { 
                                            toAccount = it
                                            haptic.performHapticFeedback(HapticFeedbackType.SelectionChange)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Card: Settings (Horizontal Layout)
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(durationMillis = 800)
                    ) + fadeIn(animationSpec = tween(durationMillis = 800))
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = cardElevation,
                                shape = RoundedCornerShape(dimens.cardCorner)
                            ),
                        shape = RoundedCornerShape(dimens.cardCorner),
                        colors = CardDefaults.cardColors(containerColor = colors.surface)
                    ) {
                        Column(Modifier.padding(dimens.spacingMedium)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = dimens.spacingSmall)
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = colors.secondary,
                                    modifier = Modifier.size(dimens.iconSize * 0.8f)
                                )
                                Spacer(Modifier.width(dimens.spacingSmall))
                                Text(
                                    "الإعدادات",
                                    style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colors.secondary
                                )
                            }
                            
                            // Horizontal layout for settings
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(dimens.spacingSmall)
                            ) {
                                // Cashbox
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("صندوق:", style = typography.bodySmall, color = colors.onSurface)
                                    CashboxPickerField(
                                        cashboxes = cashboxes,
                                        selectedCashbox = selectedCashbox,
                                        onCashboxSelected = { 
                                            selectedCashbox = it
                                            haptic.performHapticFeedback(HapticFeedbackType.SelectionChange)
                                        },
                                        onAddCashbox = onAddCashbox
                                    )
                                }
                                
                                // Currency
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("عملة:", style = typography.bodySmall, color = colors.onSurface)
                                    ExposedDropdownMenuBox(
                                        expanded = showCurrencyMenu,
                                        onExpandedChange = { 
                                            showCurrencyMenu = !showCurrencyMenu
                                            haptic.performHapticFeedback(HapticFeedbackType.SelectionChange)
                                        }
                                    ) {
                                        OutlinedTextField(
                                            value = selectedCurrency,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("اختر العملة") },
                                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCurrencyMenu) },
                                            colors = androidx.compose.material3.TextFieldDefaults.outlinedTextFieldColors(
                                                containerColor = colors.background,
                                                focusedBorderColor = colors.primary,
                                                unfocusedBorderColor = colors.outline
                                            )
                                        )
                                        DropdownMenu(
                                            expanded = showCurrencyMenu,
                                            onDismissRequest = { showCurrencyMenu = false }
                                        ) {
                                            currencies.forEach { currency ->
                                                DropdownMenuItem(
                                                    text = { Text(currency) },
                                                    onClick = {
                                                        selectedCurrency = currency
                                                        showCurrencyMenu = false
                                                        haptic.performHapticFeedback(HapticFeedbackType.SelectionChange)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Card: Amount (Full Width)
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(durationMillis = 1000)
                    ) + fadeIn(animationSpec = tween(durationMillis = 1000))
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = cardElevation,
                                shape = RoundedCornerShape(dimens.cardCorner)
                            ),
                        shape = RoundedCornerShape(dimens.cardCorner),
                        colors = CardDefaults.cardColors(containerColor = colors.surface)
                    ) {
                        Column(Modifier.padding(dimens.spacingMedium)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = dimens.spacingSmall)
                            ) {
                                Icon(
                                    Icons.Default.AttachMoney,
                                    contentDescription = null,
                                    tint = colors.tertiary,
                                    modifier = Modifier.size(dimens.iconSize * 0.8f)
                                )
                                Spacer(Modifier.width(dimens.spacingSmall))
                                Text(
                                    "المبلغ",
                                    style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colors.tertiary
                                )
                            }
                            OutlinedTextField(
                                value = amount,
                                onValueChange = { amount = it },
                                label = { Text("أدخل المبلغ") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = androidx.compose.material3.TextFieldDefaults.outlinedTextFieldColors(
                                    containerColor = colors.background,
                                    focusedBorderColor = colors.primary,
                                    unfocusedBorderColor = colors.outline
                                )
                            )
                        }
                    }
                }
                
                // Card: Details (Notes Only)
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(durationMillis = 1200)
                    ) + fadeIn(animationSpec = tween(durationMillis = 1200))
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = cardElevation,
                                shape = RoundedCornerShape(dimens.cardCorner)
                            ),
                        shape = RoundedCornerShape(dimens.cardCorner),
                        colors = CardDefaults.cardColors(containerColor = colors.surface)
                    ) {
                        Column(Modifier.padding(dimens.spacingMedium)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = dimens.spacingSmall)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = colors.tertiary,
                                    modifier = Modifier.size(dimens.iconSize * 0.8f)
                                )
                                Spacer(Modifier.width(dimens.spacingSmall))
                                Text(
                                    "التفاصيل",
                                    style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colors.tertiary
                                )
                            }
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("ملاحظات (اختياري)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = false,
                                maxLines = 2,
                                colors = androidx.compose.material3.TextFieldDefaults.outlinedTextFieldColors(
                                    containerColor = colors.background,
                                    focusedBorderColor = colors.primary,
                                    unfocusedBorderColor = colors.outline
                                )
                            )
                        }
                    }
                }
                
                // Animated success message
                AnimatedVisibility(
                    visible = successMessage != null,
                    enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(durationMillis = 500)
                    ) + fadeIn(animationSpec = tween(durationMillis = 500)),
                    exit = slideOutVertically(
                        targetOffsetY = { -it },
                        animationSpec = tween(durationMillis = 300)
                    ) + fadeOut(animationSpec = tween(durationMillis = 300))
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = colors.successContainer),
                        shape = RoundedCornerShape(dimens.cardCorner)
                    ) {
                        Row(
                            modifier = Modifier.padding(dimens.spacingMedium),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = colors.success,
                                modifier = Modifier.size(dimens.iconSize)
                            )
                            Spacer(Modifier.width(dimens.spacingSmall))
                            Text(
                                successMessage ?: "",
                                color = colors.success,
                                style = typography.bodyMedium
                            )
                        }
                    }
                }
                
                // Animated error message
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(durationMillis = 500)
                    ) + fadeIn(animationSpec = tween(durationMillis = 500)),
                    exit = slideOutVertically(
                        targetOffsetY = { -it },
                        animationSpec = tween(durationMillis = 300)
                    ) + fadeOut(animationSpec = tween(durationMillis = 300))
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = colors.errorContainer),
                        shape = RoundedCornerShape(dimens.cardCorner)
                    ) {
                        Row(
                            modifier = Modifier.padding(dimens.spacingMedium),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = colors.error,
                                modifier = Modifier.size(dimens.iconSize)
                            )
                            Spacer(Modifier.width(dimens.spacingSmall))
                            Text(
                                errorMessage ?: "",
                                color = colors.error,
                                style = typography.bodyMedium
                            )
                        }
                    }
                }
                
                Spacer(Modifier.weight(1f))
                
                // Enhanced transfer button with loading state
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (fromAccount == null || toAccount == null || selectedCashbox == null || amount.isBlank() || selectedCurrency.isBlank()) {
                            errorMessage = "يرجى تعبئة جميع الحقول المطلوبة"
                            successMessage = null
                        } else {
                            isTransferring = true
                            errorMessage = null
                            successMessage = "تم التحويل بنجاح!"
                            onTransfer(fromAccount!!, toAccount!!, selectedCashbox!!, selectedCurrency, amount, notes)
                            
                            // Simulate loading
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                delay(2000)
                                isTransferring = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimens.buttonHeight)
                        .graphicsLayer(scaleX = buttonScale, scaleY = buttonScale),
                    shape = RoundedCornerShape(dimens.buttonCorner),
                    enabled = !isTransferring
                ) {
                    if (isTransferring) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(dimens.iconSize * 0.8f),
                            color = colors.onPrimary
                        )
                        Spacer(Modifier.width(dimens.spacingSmall))
                    }
                    Text(
                        if (isTransferring) "جاري التحويل..." else "تحويل",
                        style = typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                
                Spacer(Modifier.height(dimens.spacingMedium)) // Reduced bottom spacing
            }
        }
    }
} 
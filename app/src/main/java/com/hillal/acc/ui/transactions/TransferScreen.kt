package com.hillal.acc.ui.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.hillal.acc.ui.accounts.AccountPickerField
import com.hillal.acc.ui.cashbox.CashboxPickerField
import com.hillal.acc.ui.theme.LocalAppDimensions

@Composable
fun TransferScreen(
    accounts: List<AccountUiModel>,
    cashboxes: List<CashboxUiModel>,
    currencies: List<String>,
    onTransfer: (fromAccount: AccountUiModel, toAccount: AccountUiModel, cashbox: CashboxUiModel, currency: String, amount: String, notes: String) -> Unit
) {
    val dimens = LocalAppDimensions.current
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    var fromAccount by remember { mutableStateOf<AccountUiModel?>(null) }
    var toAccount by remember { mutableStateOf<AccountUiModel?>(null) }
    var selectedCashbox by remember { mutableStateOf<CashboxUiModel?>(null) }
    var selectedCurrency by remember { mutableStateOf(currencies.firstOrNull() ?: "") }
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showCurrencyMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimens.spacingMedium)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(dimens.spacingSmall)
    ) {
        Spacer(Modifier.height(dimens.spacingLarge))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(Modifier.padding(dimens.spacingMedium)) {
                Text("من الحساب", style = typography.bodySmall)
                AccountPickerField(
                    accounts = accounts,
                    selectedAccount = fromAccount,
                    onAccountSelected = { fromAccount = it },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(dimens.spacingSmall))
                Text("إلى الحساب", style = typography.bodySmall)
                AccountPickerField(
                    accounts = accounts,
                    selectedAccount = toAccount,
                    onAccountSelected = { toAccount = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(Modifier.padding(dimens.spacingMedium)) {
                Text("اختر الصندوق", style = typography.bodySmall)
                CashboxPickerField(
                    cashboxes = cashboxes,
                    selectedCashbox = selectedCashbox,
                    onCashboxSelected = { selectedCashbox = it },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(dimens.spacingSmall))
                Text("اختر العملة", style = typography.bodySmall)
                ExposedDropdownMenuBox(
                    expanded = showCurrencyMenu,
                    onExpandedChange = { showCurrencyMenu = !showCurrencyMenu }
                ) {
                    OutlinedTextField(
                        value = selectedCurrency,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("العملة") },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCurrencyMenu) }
                    )
                    ExposedDropdownMenu(
                        expanded = showCurrencyMenu,
                        onDismissRequest = { showCurrencyMenu = false }
                    ) {
                        currencies.forEach { currency ->
                            DropdownMenuItem(
                                text = { Text(currency) },
                                onClick = {
                                    selectedCurrency = currency
                                    showCurrencyMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(Modifier.padding(dimens.spacingMedium)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("المبلغ") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(dimens.spacingSmall))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات (اختياري)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 2
                )
            }
        }
        if (errorMessage != null) {
            Text(errorMessage!!, color = colors.error, style = typography.bodyMedium)
        }
        if (successMessage != null) {
            Text(successMessage!!, color = colors.success, style = typography.bodyMedium)
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                if (fromAccount == null || toAccount == null || selectedCashbox == null || amount.isBlank() || selectedCurrency.isBlank()) {
                    errorMessage = "يرجى تعبئة جميع الحقول المطلوبة"
                    successMessage = null
                } else {
                    errorMessage = null
                    successMessage = "تم التحويل بنجاح!"
                    onTransfer(fromAccount!!, toAccount!!, selectedCashbox!!, selectedCurrency, amount, notes)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(dimens.buttonHeight),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("تحويل", style = typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
        }
        Spacer(Modifier.height(dimens.spacingLarge))
    }
}

// ملاحظة: يجب تعريف AccountUiModel وCashboxUiModel أو استخدام النماذج الفعلية لديك 
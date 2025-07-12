package com.hillal.acc.ui.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.hillal.acc.data.model.Account
import com.hillal.acc.data.entities.Cashbox
import com.hillal.acc.ui.common.AccountPickerField
import com.hillal.acc.ui.common.CashboxPickerField
import com.hillal.acc.ui.theme.LocalAppDimensions

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
    val dimens = LocalAppDimensions.current
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    var fromAccount by remember { mutableStateOf<Account?>(null) }
    var toAccount by remember { mutableStateOf<Account?>(null) }
    var selectedCashbox by remember { mutableStateOf<Cashbox?>(null) }
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
                    label = "من الحساب",
                    accounts = accounts,
                    transactions = transactions,
                    balancesMap = balancesMap,
                    selectedAccount = fromAccount,
                    onAccountSelected = { fromAccount = it }
                )
                Spacer(Modifier.height(dimens.spacingSmall))
                Text("إلى الحساب", style = typography.bodySmall)
                AccountPickerField(
                    label = "إلى الحساب",
                    accounts = accounts,
                    transactions = transactions,
                    balancesMap = balancesMap,
                    selectedAccount = toAccount,
                    onAccountSelected = { toAccount = it }
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
                    onAddCashbox = onAddCashbox
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
            Text(successMessage!!, color = colors.primary, style = typography.bodyMedium)
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
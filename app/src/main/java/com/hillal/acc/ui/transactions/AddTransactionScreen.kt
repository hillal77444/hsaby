 package com.hillal.acc.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hillal.acc.R
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hillal.acc.data.entities.Cashbox
import com.hillal.acc.data.model.Account
import com.hillal.acc.data.model.Transaction
import com.hillal.acc.ui.transactions.TransactionsViewModel
import com.hillal.acc.viewmodel.AccountViewModel
import com.hillal.acc.viewmodel.CashboxViewModel
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    accounts: List<Account>,
    cashboxes: List<Cashbox>,
    suggestions: List<String>,
    selectedAccountId: Long,
    selectedCashboxId: Long,
    accountBalancesMap: Map<Long, Map<String, Double>>,
    onAccountSelected: (Long) -> Unit,
    onCashboxSelected: (Long) -> Unit,
    onAddCashbox: (String?) -> Unit,
    onSuggestionSelected: (String) -> Unit,
    onSaveCredit: (accountId: Long, amount: Double, description: String, notes: String, currency: String, date: Long, cashboxId: Long) -> Unit,
    onSaveDebit: (accountId: Long, amount: Double, description: String, notes: String, currency: String, date: Long, cashboxId: Long) -> Unit,
    onCancel: () -> Unit
) {
    var selectedAccount by remember { mutableStateOf(accounts.find { it.id == selectedAccountId }) }
    var selectedCashbox by remember { mutableStateOf(cashboxes.find { it.id == selectedCashboxId }) }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var selectedCurrency by remember { mutableStateOf("ريال يمني") }
    var description by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showAccountDropdown by remember { mutableStateOf(false) }
    var showCashboxDropdown by remember { mutableStateOf(false) }
    var showSuggestions by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }
    var accountSearch by remember { mutableStateOf("") }

    if (showDatePicker) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("تم") }
            },
            title = { Text("اختر التاريخ") },
            text = {
                // يمكنك هنا بناء Picker بسيط (مثلاً 3 DropDowns لليوم/الشهر/السنة)
                // أو استخدام مكتبة خارجية إذا أردت
                // مثال مبسط:
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // اليوم
                    var day by remember { mutableStateOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) }
                    var month by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) }
                    var year by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
                    // ... عناصر اختيار اليوم/الشهر/السنة
                    // عند التأكيد: date = "$year-$month-$day"
                }
            }
        )
    }

    val configuration = LocalConfiguration.current
    val screenWidth = with(LocalDensity.current) { configuration.screenWidthDp.dp }
    val screenHeight = with(LocalDensity.current) { configuration.screenHeightDp.dp }
    val minSide = minOf(screenWidth, screenHeight)
    val maxSide = maxOf(screenWidth, screenHeight)
    val cardCorner = minSide * 0.045f
    val cardPadding = minSide * 0.018f
    val fontTitle = (minSide.value / 22).sp
    val fontField = (minSide.value / 28).sp
    val fontSmall = (minSide.value / 44).sp
    val iconSize = minSide * 0.055f
    val marginSmall = minSide * 0.004f
    val marginMedium = minSide * 0.012f
    val marginLarge = minSide * 0.018f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7F7))
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Color(0xFF2196F3)),
        )
        Box(
            modifier = Modifier
                .size(72.dp)
                .offset(y = (-36).dp)
                .background(Color.White, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_add_circle),
                contentDescription = null,
                tint = Color(0xFF1976D2),
                modifier = Modifier.size(48.dp)
            )
        }
        Text(
            text = "إضافة معاملة جديدة",
            fontSize = fontTitle,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1976D2),
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "يرجى تعبئة بيانات المعاملة بدقة",
            fontSize = fontSmall,
            color = Color(0xFF666666),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        // Card
        Card(
            shape = RoundedCornerShape(cardCorner),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = cardPadding, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Row: الحساب والصندوق
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // حقل الحساب
                    OutlinedTextField(
                        value = selectedAccount?.getName() ?: "",
                        onValueChange = { },
                        label = { Text("اختر الحساب") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .clickable { showAccountSheet = true },
                        singleLine = true,
                        readOnly = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1976D2),
                            unfocusedBorderColor = Color(0xFF1976D2)
                        )
                    )
                    if (showAccountSheet) {
                        ModalBottomSheet(onDismissRequest = { showAccountSheet = false }) {
                            Column(Modifier.padding(16.dp)) {
                                OutlinedTextField(
                                    value = accountSearch,
                                    onValueChange = { accountSearch = it },
                                    label = { Text("بحث عن حساب") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                val filteredAccounts = if (accountSearch.isBlank()) accounts else accounts.filter { it.getName().contains(accountSearch, ignoreCase = true) }
                                if (filteredAccounts.isEmpty()) {
                                    Text("لا توجد حسابات مطابقة", color = Color.Gray, modifier = Modifier.padding(8.dp))
                                } else {
                                    filteredAccounts.forEach { account ->
                                        val balances = accountBalancesMap[account.id] ?: emptyMap()
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedAccount = account
                                                    onAccountSelected(account.id)
                                                    showAccountSheet = false
                                                    accountSearch = ""
                                                }
                                                .padding(vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(account.getName(), fontWeight = FontWeight.Medium)
                                                if (balances.isNotEmpty()) {
                                                    Text(
                                                        balances.entries.joinToString(" | ") { (currency, value) -> "الرصيد: ${value.toLong()} $currency" },
                                                        color = Color(0xFF1976D2), fontSize = 13.sp
                                                    )
                                                }
                                                if (!account.getNotes().isNullOrBlank()) {
                                                    Text(account.getNotes()!!, color = Color.Gray, fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = selectedCashbox?.name ?: "",
                        onValueChange = { },
                        label = { Text("اختر الصندوق") },
                        leadingIcon = { Icon(Icons.Default.Save, contentDescription = null) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        readOnly = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1976D2),
                            unfocusedBorderColor = Color(0xFF1976D2)
                        )
                    )
                }
                Spacer(Modifier.height(12.dp))
                DropdownMenuBox(
                    label = "اختر الحساب",
                    items = accounts,
                    selectedItem = selectedAccount,
                    onItemSelected = { selectedAccount = it },
                    itemLabel = { it?.getName() ?: "" }
                )
                Spacer(Modifier.height(8.dp))
                DropdownMenuBox(
                    label = "اختر الصندوق",
                    items = cashboxes,
                    selectedItem = selectedCashbox,
                    onItemSelected = { selectedCashbox = it },
                    itemLabel = { it?.name ?: "" }
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("المبلغ") },
                    leadingIcon = { Icon(Icons.Default.Money, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(0.92f)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = date,
                    onValueChange = {},
                    label = { Text("التاريخ") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                    singleLine = true,
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(0.92f).clickable { showDatePicker = true }
                )
                Spacer(Modifier.height(12.dp))
                // العملة
                Text("العملة", fontSize = fontField, color = Color(0xFF1976D2), modifier = Modifier.padding(bottom = 4.dp))
                Row(Modifier.fillMaxWidth(0.92f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CurrencyRadioButton("ريال يمني", selectedCurrency) { selectedCurrency = it }
                    CurrencyRadioButton("ريال سعودي", selectedCurrency) { selectedCurrency = it }
                    CurrencyRadioButton("دولار أمريكي", selectedCurrency) { selectedCurrency = it }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        showSuggestions = it.isNotBlank() && suggestions.isNotEmpty()
                    },
                    label = { Text("البيان") },
                    singleLine = false,
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(0.92f)
                )
                DropdownMenu(
                    expanded = showSuggestions,
                    onDismissRequest = { showSuggestions = false }
                ) {
                    suggestions.filter { it.contains(description) }.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(suggestion) },
                            onClick = {
                                description = suggestion
                                onSuggestionSelected(suggestion)
                                showSuggestions = false
                            }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات (اختياري)") },
                    singleLine = false,
                    minLines = 1,
                    modifier = Modifier.fillMaxWidth(0.92f)
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.92f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            errorMessage = null
                            successMessage = null
                            if (selectedAccount == null) {
                                errorMessage = "الرجاء اختيار الحساب"
                                return@Button
                            }
                            if (amount.isBlank() || amount.toDoubleOrNull() == null) {
                                errorMessage = "الرجاء إدخال مبلغ صحيح"
                                return@Button
                            }
                            val dateMillis = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)?.time ?: System.currentTimeMillis()
                            onSaveCredit(
                                selectedAccount!!.id,
                                amount.toDouble(),
                                description,
                                notes,
                                selectedCurrency,
                                dateMillis,
                                selectedCashbox?.id ?: -1L
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Icon(painterResource(id = R.drawable.ic_arrow_downward), contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(4.dp))
                        Text("له", color = Color.White)
                    }
                    Button(
                        onClick = {
                            errorMessage = null
                            successMessage = null
                            if (selectedAccount == null) {
                                errorMessage = "الرجاء اختيار الحساب"
                                return@Button
                            }
                            if (amount.isBlank() || amount.toDoubleOrNull() == null) {
                                errorMessage = "الرجاء إدخال مبلغ صحيح"
                                return@Button
                            }
                            val dateMillis = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)?.time ?: System.currentTimeMillis()
                            onSaveDebit(
                                selectedAccount!!.id,
                                amount.toDouble(),
                                description,
                                notes,
                                selectedCurrency,
                                dateMillis,
                                selectedCashbox?.id ?: -1L
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                    ) {
                        Icon(painterResource(id = R.drawable.ic_arrow_upward), contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(4.dp))
                        Text("عليه", color = Color.White)
                    }
                    OutlinedButton(
                        onClick = { onCancel() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF1976D2))
                        Spacer(Modifier.width(4.dp))
                        Text("إلغاء", color = Color(0xFF1976D2))
                    }
                }
                errorMessage?.let {
                    Text(it, color = Color.Red, modifier = Modifier.padding(8.dp))
                }
                successMessage?.let {
                    Text(it, color = Color(0xFF4CAF50), modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}

@Composable
fun <T> DropdownMenuBox(
    label: String,
    items: List<T>,
    selectedItem: T?,
    onItemSelected: (T?) -> Unit,
    itemLabel: (T?) -> String
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = itemLabel(selectedItem),
            onValueChange = { },
            label = { Text(label) },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            trailingIcon = {
                Icon(Icons.Default.Person, contentDescription = null)
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(itemLabel(item)) },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun CurrencyRadioButton(text: String, selected: String, onSelect: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onSelect(text) }
    ) {
        RadioButton(
            selected = selected == text,
            onClick = { onSelect(text) },
            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF1976D2))
        )
        Text(text, color = Color(0xFF1976D2), fontSize = 14.sp)
    }
} 
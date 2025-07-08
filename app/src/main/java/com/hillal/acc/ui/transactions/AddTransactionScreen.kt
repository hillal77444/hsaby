package com.hillal.acc.ui.transactions

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hillal.acc.R
import com.hillal.acc.data.entities.Cashbox
import com.hillal.acc.data.model.Account
import com.hillal.acc.data.model.Transaction
import com.hillal.acc.data.preferences.UserPreferences
import com.hillal.acc.data.repository.TransactionRepository
import com.hillal.acc.ui.cashbox.AddCashboxDialog
import com.hillal.acc.ui.common.AccountPickerField
import com.hillal.acc.ui.common.CashboxPickerField
import com.hillal.acc.ui.transactions.TransactionsViewModel
import com.hillal.acc.viewmodel.AccountViewModel
import com.hillal.acc.viewmodel.CashboxViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Notes
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward

@Composable
fun AddTransactionScreen(
    navController: NavController,
    transactionsViewModel: TransactionsViewModel = viewModel(),
    accountViewModel: AccountViewModel = viewModel(),
    cashboxViewModel: CashboxViewModel = viewModel(),
    transactionRepository: TransactionRepository,
    userPreferences: UserPreferences = UserPreferences(LocalContext.current)
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // State variables
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var selectedAccountId by remember { mutableStateOf(-1L) }
    var selectedCashbox by remember { mutableStateOf<Cashbox?>(null) }
    var selectedCashboxId by remember { mutableStateOf(-1L) }
    var mainCashboxId by remember { mutableStateOf(-1L) }
    var allAccounts by remember { mutableStateOf(listOf<Account>()) }
    var allCashboxes by remember { mutableStateOf(listOf<Cashbox>()) }
    var allTransactions by remember { mutableStateOf(listOf<Transaction>()) }
    var accountBalancesMap by remember { mutableStateOf(mapOf<Long, Map<String, Double>>()) }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf(userPreferences.getUserName()) }
    var currency by remember { mutableStateOf(context.getString(R.string.currency_yer)) }
    var date by remember { mutableStateOf(Calendar.getInstance().timeInMillis) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAccountPicker by remember { mutableStateOf(false) }
    var showCashboxDialog by remember { mutableStateOf(false) }
    var isDialogShown by remember { mutableStateOf(false) }
    var lastSavedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var lastSavedAccount by remember { mutableStateOf<Account?>(null) }
    var lastSavedBalance by remember { mutableStateOf(0.0) }
    var suggestions by remember { mutableStateOf(listOf<String>()) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale("ar")) }

    // Observers
    LaunchedEffect(Unit) {
        accountViewModel.getAllAccounts().observe(lifecycleOwner) { accounts ->
            if (accounts != null) {
                // ترتيب الحسابات حسب عدد المعاملات
                val accountTransactionCount = mutableMapOf<Long, Int>()
                for (transaction in allTransactions) {
                    val accountId = transaction.getAccountId()
                    accountTransactionCount[accountId] = accountTransactionCount.getOrDefault(accountId, 0) + 1
                }
                val sortedAccounts = accounts.filterNotNull().sortedByDescending { account ->
                    accountTransactionCount.getOrDefault(account.getId(), 0)
                }
                allAccounts = sortedAccounts
            }
        }
        cashboxViewModel.getAllCashboxes().observe(lifecycleOwner) { cashboxes ->
            if (cashboxes != null) {
                allCashboxes = cashboxes.filterNotNull()
                if (allCashboxes.isNotEmpty()) {
                    mainCashboxId = allCashboxes[0].id
                    if (selectedCashboxId == -1L) {
                        selectedCashbox = allCashboxes[0]
                        selectedCashboxId = allCashboxes[0].id
                    }
                }
            }
        }
        transactionsViewModel.accountBalancesMap.observe(lifecycleOwner) { balancesMap ->
            if (balancesMap != null) {
                accountBalancesMap = balancesMap.filterKeys { it != null }.mapKeys { it.key!! } as Map<Long, Map<String, Double>>
            }
        }
        transactionsViewModel.getTransactions().observe(lifecycleOwner) { txs ->
            if (txs != null) allTransactions = txs
        }
    }

    // Suggestions
    fun loadAccountSuggestions(accountId: Long) {
        val prefs = context.getSharedPreferences("suggestions", Context.MODE_PRIVATE)
        val set = prefs.getStringSet("descriptions_" + accountId, setOf()) ?: setOf()
        suggestions = set.filterNotNull()
    }

    // Save Transaction
    fun saveTransaction(isDebit: Boolean) {
        if (selectedAccountId == -1L) {
            Toast.makeText(context, "الرجاء اختيار الحساب", Toast.LENGTH_SHORT).show()
            return
        }
        if (amount.isEmpty()) {
            Toast.makeText(context, context.getString(R.string.error_amount_required), Toast.LENGTH_SHORT).show()
            return
        }
        val amountDouble = amount.toDoubleOrNull()
        if (amountDouble == null) {
            Toast.makeText(context, context.getString(R.string.error_invalid_amount), Toast.LENGTH_SHORT).show()
            return
        }
        accountViewModel.getAccountById(selectedAccountId).observe(lifecycleOwner) { account ->
            if (account != null) {
                val transaction = Transaction(
                    selectedAccountId,
                    amountDouble,
                    if (isDebit) "debit" else "credit",
                    description,
                    currency
                )
                transaction.id = System.currentTimeMillis()
                transaction.setNotes(notes)
                transaction.setTransactionDate(date)
                transaction.setUpdatedAt(System.currentTimeMillis())
                transaction.setServerId(-1)
                transaction.setWhatsappEnabled(account.isWhatsappEnabled())
                // Cashbox logic
                val cashboxIdToSave = when {
                    selectedCashboxId != -1L -> selectedCashboxId
                    mainCashboxId != -1L -> mainCashboxId
                    allCashboxes.isNotEmpty() -> allCashboxes[0].id
                    else -> -1L
                }
                transaction.setCashboxId(cashboxIdToSave)
                transactionsViewModel.insertTransaction(transaction)
                lastSavedTransaction = transaction
                lastSavedAccount = account
                // Get balance
                transactionRepository.getBalanceUntilDate(selectedAccountId, transaction.getTransactionDate(), currency)
                    .observe(lifecycleOwner) { balance ->
                        lastSavedBalance = balance ?: 0.0
                        isDialogShown = true
                    }
                // Save suggestion
                val desc = description.trim()
                if (desc.isNotEmpty()) {
                    val prefs = context.getSharedPreferences("suggestions", Context.MODE_PRIVATE)
                    val key = "descriptions_" + selectedAccountId
                    val oldSet = prefs.getStringSet(key, setOf()) ?: setOf()
                    val newSet = oldSet.toMutableSet()
                    newSet.add(desc)
                    prefs.edit().putStringSet(key, newSet).apply()
                    loadAccountSuggestions(selectedAccountId)
                }
            }
        }
    }

    // UI
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val isLargeScreen = screenWidth > 500.dp
        val horizontalPadding = if (isLargeScreen) 48.dp else 16.dp
        val verticalSpacing = if (isLargeScreen) 20.dp else 8.dp
        val fieldFontSize = if (isLargeScreen) 18.sp else 15.sp
        val labelFontSize = if (isLargeScreen) 16.sp else 13.sp
        val buttonHeight = if (isLargeScreen) 56.dp else 48.dp
        val buttonFontSize = if (isLargeScreen) 18.sp else 15.sp
        val buttonShape = RoundedCornerShape(if (isLargeScreen) 16.dp else 8.dp)
        val buttonArrangement = if (isLargeScreen) Arrangement.spacedBy(24.dp) else Arrangement.spacedBy(8.dp)
        val scrollState = rememberScrollState()
        val infoColor = Color(0xFF1976D2).copy(alpha = 0.15f)
        val primaryColor = MaterialTheme.colorScheme.primary
        val cardShape = RoundedCornerShape(20.dp)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .imePadding()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header background (info color with scale)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(infoColor, Color.White),
                            startY = 0f,
                            endY = 400f
                        )
                    )
                    .graphicsLayer {
                        scaleX = 2.2f
                        scaleY = 1.2f
                    }
            )
            // Icon Card
            Card(
                modifier = Modifier
                    .size(72.dp)
                    .offset(y = (-100).dp)
                    .align(CenterHorizontally),
                shape = RoundedCornerShape(36.dp), // Changed from CircleShape to RoundedCornerShape
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add_circle),
                        contentDescription = null,
                        tint = Color(0xFF1976D2),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            // Title
            Text(
                text = "إضافة معاملة جديدة",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = Color.White,
                modifier = Modifier.align(CenterHorizontally).offset(y = (-90).dp)
            )
            // Subtitle
            Text(
                text = "يرجى تعبئة بيانات المعاملة بدقة",
                fontSize = 14.sp,
                color = Color(0xFF666666),
                modifier = Modifier.align(CenterHorizontally).offset(y = (-90).dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Main Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .shadow(4.dp, cardShape),
                shape = cardShape,
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Row: حساب وصندوق
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) {
                            AccountPickerField(
                                accounts = allAccounts,
                                transactions = allTransactions,
                                balancesMap = accountBalancesMap,
                                selectedAccount = selectedAccount,
                                onAccountSelected = { account ->
                                    selectedAccount = account
                                    selectedAccountId = account.getId()
                                    loadAccountSuggestions(selectedAccountId)
                                }
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            CashboxPickerField(
                                cashboxes = allCashboxes,
                                selectedCashbox = allCashboxes.find { it.id == selectedCashboxId },
                                onCashboxSelected = { cashbox ->
                                    selectedCashboxId = cashbox.id
                                },
                                onAddCashbox = { name ->
                                    CashboxHelper.addCashboxToServer(
                                        context, cashboxViewModel, name,
                                        object : CashboxHelper.CashboxCallback {
                                            override fun onSuccess(cashbox: Cashbox?) {
                                                selectedCashboxId = cashbox?.id ?: -1L
                                                CashboxHelper.showSuccessMessage(context, "تم إضافة الصندوق بنجاح")
                                            }
                                            override fun onError(error: String?) {
                                                CashboxHelper.showErrorMessage(context, error)
                                            }
                                        })
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(verticalSpacing))
                    // Row: مبلغ وتاريخ
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            label = { Text("المبلغ", fontSize = labelFontSize) },
                            modifier = Modifier.weight(1f),
                            leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                            textStyle = LocalTextStyle.current.copy(fontSize = fieldFontSize)
                        )
                        OutlinedTextField(
                            value = dateFormat.format(Date(date)),
                            onValueChange = {},
                            label = { Text("التاريخ", fontSize = labelFontSize) },
                            modifier = Modifier.weight(1f).clickable { showDatePicker = true },
                            enabled = false,
                            readOnly = true,
                            leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                            textStyle = LocalTextStyle.current.copy(fontSize = fieldFontSize)
                        )
                    }
                    Spacer(modifier = Modifier.height(verticalSpacing))
                    // Description with suggestions
                    var expandedSuggestions by remember { mutableStateOf(false) }
                    var descriptionState by rememberSaveable { mutableStateOf("") }
                    Box {
                        OutlinedTextField(
                            value = descriptionState,
                            onValueChange = {
                                descriptionState = it
                                description = it
                                expandedSuggestions = suggestions.any { s -> s.startsWith(it) } && it.isNotEmpty()
                            },
                            label = { Text("البيان", fontSize = labelFontSize) },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                            textStyle = LocalTextStyle.current.copy(fontSize = fieldFontSize),
                            trailingIcon = {
                                if (suggestions.isNotEmpty()) {
                                    IconButton(onClick = { expandedSuggestions = !expandedSuggestions }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = expandedSuggestions && suggestions.isNotEmpty(),
                            onDismissRequest = { expandedSuggestions = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            suggestions.filter { it.startsWith(descriptionState) && it != descriptionState }.take(5).forEach { suggestion ->
                                DropdownMenuItem(
                                    text = { Text(suggestion, fontSize = fieldFontSize) },
                                    onClick = {
                                        descriptionState = suggestion
                                        description = suggestion
                                        expandedSuggestions = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(verticalSpacing))
                    // Notes
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات", fontSize = labelFontSize) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                        textStyle = LocalTextStyle.current.copy(fontSize = fieldFontSize)
                    )
                    Spacer(modifier = Modifier.height(verticalSpacing))
                    // Currency Radio Buttons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = currency == context.getString(R.string.currency_yer),
                            onClick = { currency = context.getString(R.string.currency_yer) }
                        )
                        Text("ريال يمني", fontSize = labelFontSize)
                        Spacer(modifier = Modifier.width(8.dp))
                        RadioButton(
                            selected = currency == context.getString(R.string.currency_sar),
                            onClick = { currency = context.getString(R.string.currency_sar) }
                        )
                        Text("ريال سعودي", fontSize = labelFontSize)
                        Spacer(modifier = Modifier.width(8.dp))
                        RadioButton(
                            selected = currency == context.getString(R.string.currency_usd),
                            onClick = { currency = context.getString(R.string.currency_usd) }
                        )
                        Text("دولار", fontSize = labelFontSize)
                    }
                }
            }
            // Action Buttons في الأسفل
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = buttonArrangement
            ) {
                Button(
                    onClick = { saveTransaction(false) },
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight),
                    shape = buttonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50), contentColor = Color.White)
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("له", fontSize = buttonFontSize)
                }
                Button(
                    onClick = { saveTransaction(true) },
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight),
                    shape = buttonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336), contentColor = Color.White)
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("عليه", fontSize = buttonFontSize)
                }
                Button(
                    onClick = { navController.navigateUp() },
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight),
                    shape = buttonShape
                ) {
                    Text("إلغاء", fontSize = buttonFontSize)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val calendar = Calendar.getInstance().apply { timeInMillis = date }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                date = calendar.timeInMillis
                showDatePicker = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Account Picker BottomSheet
    AccountPickerBottomSheetCompose(
        show = showAccountPicker,
        accounts = allAccounts,
        transactions = allTransactions,
        balancesMap = accountBalancesMap,
        onAccountSelected = { account ->
            selectedAccount = account
            selectedAccountId = account.getId()
            loadAccountSuggestions(selectedAccountId)
            showAccountPicker = false
        },
        onDismiss = { showAccountPicker = false }
    )

    // Cashbox Dialog
    AddCashboxDialogCompose(
        show = showCashboxDialog,
        onCashboxAdded = { name ->
            CashboxHelper.addCashboxToServer(
                context, cashboxViewModel, name,
                object : CashboxHelper.CashboxCallback {
                    override fun onSuccess(cashbox: Cashbox?) {
                        selectedCashbox = cashbox
                        selectedCashboxId = cashbox?.id ?: -1L
                        CashboxHelper.showSuccessMessage(context, "تم إضافة الصندوق بنجاح")
                        showCashboxDialog = false
                    }
                    override fun onError(error: String?) {
                        CashboxHelper.showErrorMessage(context, error)
                    }
                })
        },
        onDismiss = { showCashboxDialog = false }
    )

    // Success Dialog
    if (isDialogShown) {
        AlertDialog(
            onDismissRequest = { isDialogShown = false },
            title = { Text("تمت إضافة المعاملة بنجاح") },
            text = {
                Column {
                    Text("هل ترغب بإرسال إشعار؟")
                }
            },
            confirmButton = {
                Row {
                    Button(onClick = {
                        // إرسال واتساب
                        lastSavedAccount?.let { account ->
                            lastSavedTransaction?.let { transaction ->
                                val phone = account.getPhoneNumber()
                                if (!phone.isNullOrEmpty()) {
                                    val msg = NotificationUtils.buildWhatsAppMessage(
                                        context,
                                        account.getName(),
                                        transaction,
                                        lastSavedBalance,
                                        transaction.getType()
                                    )
                                    NotificationUtils.sendWhatsAppMessage(context, phone, msg)
                                } else {
                                    Toast.makeText(context, "رقم الهاتف غير متوفر", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }) { Text("واتساب") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        // إرسال SMS
                        lastSavedAccount?.let { account ->
                            lastSavedTransaction?.let { transaction ->
                                val phone = account.getPhoneNumber()
                                if (!phone.isNullOrEmpty()) {
                                    val type = transaction.getType()
                                    val amountStr = String.format(Locale.US, "%.0f", transaction.getAmount())
                                    val balanceStr = String.format(Locale.US, "%.0f", abs(lastSavedBalance))
                                    val currency = transaction.getCurrency()
                                    val typeText = if (type.equals("credit", true) || type == "له") "لكم" else "عليكم"
                                    val balanceText = if (lastSavedBalance >= 0) "الرصيد لكم " else "الرصيد عليكم "
                                    val message = "حسابكم لدينا:\n" +
                                            typeText + " " + amountStr + " " + currency + "\n" +
                                            transaction.getDescription() + "\n" +
                                            balanceText + balanceStr + " " + currency
                                    NotificationUtils.sendSmsMessage(context, phone, message)
                                } else {
                                    Toast.makeText(context, "رقم الهاتف غير متوفر", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }) { Text("SMS") }
                }
            },
            dismissButton = {
                Row {
                    Button(onClick = {
                        isDialogShown = false
                        // إعادة تعيين الحقول
                        amount = ""
                        description = ""
                        date = System.currentTimeMillis()
                    }) {
                        Text("إضافة قيد آخر")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        isDialogShown = false
                        navController.navigateUp()
                    }) {
                        Text("خروج")
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountPickerBottomSheetCompose(
    show: Boolean,
    accounts: List<Account>,
    transactions: List<Transaction>,
    balancesMap: Map<Long, Map<String, Double>>,
    onAccountSelected: (Account) -> Unit,
    onDismiss: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    val filteredAccounts = if (search.isBlank()) accounts else accounts.filter { it.getName()?.contains(search) == true }
    if (show) {
        ModalBottomSheet(onDismissRequest = onDismiss) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(text = "اختر الحساب", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("بحث") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Divider()
                LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    items(filteredAccounts) { account ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    onAccountSelected(account)
                                    onDismiss()
                                },
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(account.getName() ?: "", fontWeight = FontWeight.Bold)
                                val balance = balancesMap[account.getId()]?.values?.sum() ?: 0.0
                                Text("الرصيد: ${balance}", fontSize = 14.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("إغلاق")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCashboxDialogCompose(
    show: Boolean,
    onCashboxAdded: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("إضافة صندوق جديد") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            error = false
                        },
                        label = { Text("اسم الصندوق") },
                        isError = error,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (error) {
                        Text("يرجى إدخال اسم الصندوق", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isBlank()) {
                        error = true
                    } else {
                        onCashboxAdded(name.trim())
                        name = ""
                        error = false
                        onDismiss()
                    }
                }) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                Button(onClick = {
                    name = ""
                    error = false
                    onDismiss()
                }) {
                    Text("إلغاء")
                }
            }
        )
    }
} 
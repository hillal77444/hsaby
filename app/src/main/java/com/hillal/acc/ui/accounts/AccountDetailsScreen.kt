package com.hillal.acc.ui.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hillal.acc.data.model.Account
import com.hillal.acc.data.model.Transaction
import com.hillal.acc.ui.common.TransactionCard
import com.hillal.acc.ui.accounts.AccountViewModel
import com.hillal.acc.ui.transactions.TransactionViewModel
import androidx.compose.runtime.livedata.observeAsState
import android.os.Bundle
import com.hillal.acc.R
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import com.hillal.acc.ui.transactions.NotificationUtils
import java.util.Locale
import kotlin.math.abs
import com.hillal.acc.data.remote.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.net.ConnectivityManager
import android.content.Context
import androidx.compose.foundation.layout.navigationBarsPadding

@Composable
fun AccountDetailsScreen(
    accountId: Long,
    navController: NavController,
    accountViewModel: AccountViewModel = viewModel(),
    transactionViewModel: TransactionViewModel = viewModel()
) {
    val context = LocalContext.current
    val account by accountViewModel.getAccountById(accountId).observeAsState()
    val transactionsLive = transactionViewModel.getTransactionsForAccount(accountId)
    val transactions: List<Transaction> = transactionsLive?.observeAsState(emptyList())?.value?.filterNotNull() ?: emptyList()
    var searchQuery by remember { mutableStateOf("") }

    // العملات المتوفرة
    val currencies = remember(transactions) {
        transactions.map { it.getCurrency()?.trim() ?: "يمني" }.distinct().ifEmpty { listOf("يمني") }
    }
    var selectedCurrency by remember { mutableStateOf("") }
    // عند أول بناء للشاشة، اختر "يمني" إذا موجود، أو أول عملة
    LaunchedEffect(currencies) {
        selectedCurrency = currencies.find { it == "يمني" } ?: currencies.firstOrNull() ?: "يمني"
    }

    // متغيرات حالة الحذف
    var showDeleteDialog by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    // الفلترة حسب العملة والبحث
    val filteredTransactions = transactions
        .filter { (it.getCurrency()?.trim() ?: "يمني") == selectedCurrency }
        .filter { it.getDescription()?.contains(searchQuery, ignoreCase = true) == true || searchQuery.isBlank() }

    val coroutineScope = rememberCoroutineScope()
    var showProgress by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF6F8FB) // خلفية ناعمة
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 0.dp)
        ) {
            // شريط علوي ثابت
            item {
                Surface(
                    tonalElevation = 2.dp,
                    shadowElevation = 4.dp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = account?.getName() ?: "تفاصيل الحساب",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(vertical = 2.dp, horizontal = 8.dp)
                        )
                    }
                }
            }
            // أزرار العملات
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    currencies.forEach { currency ->
                        val isSelected = selectedCurrency == currency
                        Button(
                            onClick = { selectedCurrency = currency },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFF0F0F0),
                                contentColor = if (isSelected) Color.White else Color.Black
                            ),
                            shape = MaterialTheme.shapes.small,
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = if (isSelected) 2.dp else 0.dp),
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .height(34.dp)
                        ) {
                            Text(currency, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            // شريط البحث بهامش خفيف جداً
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("بحث في الوصف") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "مسح البحث")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 0.dp)
                )
            }
            // قائمة المعاملات أو رسالة عدم وجود معاملات
            if (filteredTransactions.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("لا توجد معاملات لهذا الحساب", color = Color.Gray)
                    }
                }
            } else {
                itemsIndexed(filteredTransactions) { idx, transaction ->
                    val balanceLive = transactionViewModel.getBalanceUntilTransaction(
                        accountId = transaction.getAccountId(),
                        transactionDate = transaction.getTransactionDate(),
                        transactionId = transaction.getId(),
                        currency = transaction.getCurrency() ?: "يمني"
                    )
                    val balance by balanceLive.observeAsState(0.0)
                    TransactionCard(
                        transaction = transaction,
                        accounts = listOfNotNull(account),
                        onDelete = {
                            transactionToDelete = transaction
                            showDeleteDialog = true
                        },
                        onEdit = {
                            val args = Bundle().apply { putLong("transactionId", transaction.getId()) }
                            navController.navigate(R.id.editTransactionFragment, args)
                        },
                        onWhatsApp = {
                            val acc = listOfNotNull(account).find { it.getId() == transaction.getAccountId() }
                            val phone = acc?.getPhoneNumber()
                            if (!phone.isNullOrBlank()) {
                                val balance = balance
                                val message = NotificationUtils.buildWhatsAppMessage(
                                    context = context,
                                    accountName = acc.getName() ?: "--",
                                    transaction = transaction,
                                    balance = balance,
                                    type = transaction.getType()
                                )
                                NotificationUtils.sendWhatsAppMessage(context, phone, message)
                                showDeleteDialog = false
                                transactionToDelete = null
                            }
                        },
                        onSms = {
                            val acc = listOfNotNull(account).find { it.getId() == transaction.getAccountId() }
                            val phone = acc?.getPhoneNumber()
                            if (!phone.isNullOrBlank()) {
                                val balance = balance
                                val type = transaction.getType()
                                val amountStr = String.format(Locale.US, "%.0f", transaction.getAmount())
                                val balanceStr = String.format(Locale.US, "%.0f", abs(balance))
                                val currency = transaction.getCurrency() ?: "يمني"
                                val typeText = if (type.equals("credit", true) || type == "له") "لكم" else "عليكم"
                                val balanceText = if (balance >= 0) "الرصيد لكم " else "الرصيد عليكم "
                                val message = "حسابكم لدينا:\n" +
                                        typeText + " " + amountStr + " " + currency + "\n" +
                                        (transaction.getDescription() ?: "") + "\n" +
                                        balanceText + balanceStr + " " + currency
                                NotificationUtils.sendSmsMessage(context, phone, message)
                                showDeleteDialog = false
                                transactionToDelete = null
                            }
                        },
                        index = idx,
                        modifier = Modifier.height(screenHeight * 0.20f),
                        searchQuery = searchQuery
                    )
                    if (idx < filteredTransactions.lastIndex) {
                        Divider(modifier = Modifier.padding(horizontal = 12.dp), color = Color(0xFFE0E0E0))
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(32.dp)) // مسافة أسفل القائمة
                }
            }
        }
        // مربع حوار الحذف يبقى خارج LazyColumn
        if (showDeleteDialog && transactionToDelete != null) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteDialog = false
                    transactionToDelete = null
                },
                title = { Text("حذف القيد") },
                text = { Text("هل أنت متأكد أنك تريد حذف هذا القيد؟") },
                confirmButton = {
                    TextButton(onClick = {
                        val transaction = transactionToDelete
                        if (transaction != null) {
                            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
                            val isNetworkAvailable = connectivityManager?.activeNetworkInfo?.isConnected == true
                            if (!isNetworkAvailable) {
                                android.widget.Toast.makeText(context, "يرجى الاتصال بالإنترنت لحذف القيد", android.widget.Toast.LENGTH_SHORT).show()
                                return@TextButton
                            }
                            val token = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE).getString("token", null)
                            if (token == null) {
                                android.widget.Toast.makeText(context, "يرجى تسجيل الدخول أولاً", android.widget.Toast.LENGTH_SHORT).show()
                                return@TextButton
                            }
                            showProgress = true
                            RetrofitClient.getApiService()
                                .deleteTransaction("Bearer $token", transaction.getServerId())
                                .enqueue(object : Callback<Void?> {
                                    override fun onResponse(call: Call<Void?>, response: Response<Void?>) {
                                        showProgress = false
                                        if (response.isSuccessful) {
                                            transactionViewModel.deleteTransaction(transaction)
                                            android.widget.Toast.makeText(context, "تم حذف القيد بنجاح", android.widget.Toast.LENGTH_SHORT).show()
                                            showDeleteDialog = false
                                            transactionToDelete = null
                                        } else {
                                            android.widget.Toast.makeText(context, "فشل في حذف القيد من السيرفر", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    override fun onFailure(call: Call<Void?>, t: Throwable) {
                                        showProgress = false
                                        android.widget.Toast.makeText(context, "خطأ في الاتصال بالسيرفر", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                })
                        }
                    }) { Text("نعم") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        transactionToDelete = null
                    }) {
                        Text("لا")
                    }
                }
            )
        }
        if (showProgress) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("جاري حذف القيد...") },
                text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } },
                confirmButton = {},
                dismissButton = {}
            )
        }
    }
    // عند إعادة بناء الشاشة
    LaunchedEffect(Unit) {
        showDeleteDialog = false
        transactionToDelete = null
    }
} 
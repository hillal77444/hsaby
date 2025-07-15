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

    // متغيرات حالة الحذف
    var showDeleteDialog by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    val filteredTransactions = if (searchQuery.isBlank()) {
        transactions
    } else {
        transactions.filter { it.getDescription()?.contains(searchQuery, ignoreCase = true) == true }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF6F8FB) // خلفية ناعمة
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // شريط علوي ثابت
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

            // شريط البحث
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
                    .padding(12.dp)
            )

            // قائمة المعاملات
            if (filteredTransactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا توجد معاملات لهذا الحساب", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 0.dp)
                ) {
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
                                    val message = NotificationUtils.buildWhatsAppMessage(
                                        context = context,
                                        accountName = acc.getName() ?: "--",
                                        transaction = transaction,
                                        balance = balance ?: 0.0,
                                        type = transaction.getType()
                                    )
                                    NotificationUtils.sendWhatsAppMessage(context, phone, message)
                                }
                            },
                            onSms = {
                                val acc = listOfNotNull(account).find { it.getId() == transaction.getAccountId() }
                                val phone = acc?.getPhoneNumber()
                                if (!phone.isNullOrBlank()) {
                                    val type = transaction.getType()
                                    val amountStr = String.format(Locale.US, "%.0f", transaction.getAmount())
                                    val balanceStr = String.format(Locale.US, "%.0f", abs(balance ?: 0.0))
                                    val currency = transaction.getCurrency() ?: "يمني"
                                    val typeText = if (type.equals("credit", true) || type == "له") "لكم" else "عليكم"
                                    val balanceText = if ((balance ?: 0.0) >= 0) "الرصيد لكم " else "الرصيد عليكم "
                                    val message = "حسابكم لدينا:\n" +
                                            typeText + " " + amountStr + " " + currency + "\n" +
                                            (transaction.getDescription() ?: "") + "\n" +
                                            balanceText + balanceStr + " " + currency
                                    NotificationUtils.sendSmsMessage(context, phone, message)
                                }
                            },
                            index = idx,
                            modifier = Modifier.height(screenHeight * 0.18f),
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
            // مربع حوار الحذف
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
                            transactionViewModel.deleteTransaction(transactionToDelete)
                            android.widget.Toast.makeText(context, "تم حذف القيد بنجاح", android.widget.Toast.LENGTH_SHORT).show()
                            showDeleteDialog = false
                            transactionToDelete = null
                        }) {
                            Text("نعم")
                        }
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
        }
    }
} 
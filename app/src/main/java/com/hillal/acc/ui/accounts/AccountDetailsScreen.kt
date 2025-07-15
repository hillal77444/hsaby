package com.hillal.acc.ui.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
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

    val filteredTransactions = if (searchQuery.isBlank()) {
        transactions
    } else {
        transactions.filter { it.getDescription()?.contains(searchQuery, ignoreCase = true) == true }
    }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        // شريط البحث في الأعلى
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
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        // اسم الحساب في الأعلى
        account?.let { acc ->
            Text(text = acc.getName() ?: "--", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
        }

        // قائمة المعاملات
        if (filteredTransactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا توجد معاملات لهذا الحساب", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(filteredTransactions) { idx, transaction ->
                    TransactionCard(
                        transaction = transaction,
                        accounts = account?.let { listOf(it) } ?: emptyList(),
                        onDelete = {
                            transactionViewModel.deleteTransaction(transaction)
                            // يمكنك إضافة Toast أو Snackbar هنا
                        },
                        onEdit = {
                            // انتقل إلى شاشة تعديل المعاملة
                            val args = Bundle().apply { putLong("transactionId", transaction.getId()) }
                            navController.navigate(R.id.editTransactionFragment, args)
                        },
                        onWhatsApp = {
                            // أرسل رسالة واتساب
                            val phone = account?.getPhoneNumber()
                            if (!phone.isNullOrBlank()) {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                                intent.data = android.net.Uri.parse("https://wa.me/$phone")
                                context.startActivity(intent)
                            }
                        },
                        onSms = {
                            // أرسل SMS
                            val phone = account?.getPhoneNumber()
                            if (!phone.isNullOrBlank()) {
                                val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO)
                                intent.data = android.net.Uri.parse("smsto:$phone")
                                context.startActivity(intent)
                            }
                        },
                        index = idx,
                        searchQuery = searchQuery
                    )
                }
            }
        }
    }
} 
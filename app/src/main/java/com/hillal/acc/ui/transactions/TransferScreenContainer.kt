package com.hillal.acc.ui.transactions

import android.app.Application
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.livedata.observeAsState
import com.hillal.acc.data.model.Account
import com.hillal.acc.data.entities.Cashbox
import com.hillal.acc.data.model.Transaction
import com.hillal.acc.ui.accounts.AccountViewModel
import com.hillal.acc.viewmodel.CashboxViewModel
import com.hillal.acc.ui.common.AccountPickerField
import com.hillal.acc.ui.common.CashboxPickerField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.rememberSnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import com.hillal.acc.R
import androidx.compose.ui.unit.dp

@Composable
fun TransferScreenContainer(
    accountViewModel: AccountViewModel = viewModel(),
    cashboxViewModel: CashboxViewModel = viewModel(),
    transactionsViewModel: TransactionsViewModel = viewModel()
) {
    val accounts by accountViewModel.allAccounts.observeAsState(emptyList())
    val cashboxes by cashboxViewModel.getAllCashboxes().observeAsState(emptyList())
    val transactions by transactionsViewModel.getTransactions().observeAsState(emptyList())
    val balancesMap by transactionsViewModel.accountBalancesMap.observeAsState(emptyMap())
    val currencies = stringArrayResource(id = R.array.currencies_array).toList()
    val snackbarHostState = rememberSnackbarHostState()
    var isLoading by remember { mutableStateOf(false) }

    // إضافة صندوق جديد
    fun addCashbox(name: String) {
        val newCashbox = Cashbox()
        newCashbox.name = name
        cashboxViewModel.insert(newCashbox)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Surface(modifier = Modifier.padding(padding)) {
            TransferScreen(
                accounts = accounts,
                cashboxes = cashboxes,
                currencies = currencies,
                transactions = transactions,
                balancesMap = balancesMap,
                onAddCashbox = { addCashbox(it) },
                onTransfer = { fromAccount, toAccount, cashbox, currency, amount, notes, onResult ->
                    isLoading = true
                    try {
                        val amountValue = amount.toDoubleOrNull()
                        if (amountValue == null || amountValue <= 0) {
                            onResult(false, "يرجى إدخال مبلغ صحيح")
                            isLoading = false
                            return@TransferScreen
                        }
                        if (fromAccount.getId() == toAccount.getId()) {
                            onResult(false, "لا يمكن اختيار نفس الحساب مرتين")
                            isLoading = false
                            return@TransferScreen
                        }
                        // إنشاء معاملة الخصم
                        val debitTx = Transaction()
                        debitTx.id = System.currentTimeMillis() * 1000000 + System.nanoTime() % 1000000
                        debitTx.setAccountId(fromAccount.getId())
                        debitTx.setAmount(amountValue)
                        debitTx.setCurrency(currency)
                        debitTx.setType("debit")
                        debitTx.setDescription("تحويل من ${fromAccount.getName()} إلى ${toAccount.getName()}${if (notes.isNotBlank()) " - $notes" else ""}")
                        debitTx.setWhatsappEnabled(fromAccount.isWhatsappEnabled())
                        debitTx.setCashboxId(cashbox.id)
                        // إنشاء معاملة الإضافة
                        val creditTx = Transaction()
                        creditTx.setAccountId(toAccount.getId())
                        creditTx.setAmount(amountValue)
                        creditTx.setCurrency(currency)
                        creditTx.setType("credit")
                        creditTx.setDescription("تحويل من ${fromAccount.getName()} إلى ${toAccount.getName()}${if (notes.isNotBlank()) " - $notes" else ""}")
                        creditTx.setWhatsappEnabled(toAccount.isWhatsappEnabled())
                        creditTx.setCashboxId(cashbox.id)
                        // إضافة المعاملتين
                        transactionsViewModel.insertTransaction(debitTx)
                        transactionsViewModel.insertTransaction(creditTx)
                        onResult(true, "تم التحويل بنجاح!")
                    } catch (e: Exception) {
                        onResult(false, "حدث خطأ أثناء التحويل")
                    }
                    isLoading = false
                }
            )
        }
    }
} 
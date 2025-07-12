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
import com.hillal.acc.ui.accounts.AccountViewModelFactory
import com.hillal.acc.viewmodel.CashboxViewModel
import com.hillal.acc.ui.common.AccountPickerField
import com.hillal.acc.ui.common.CashboxPickerField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import com.hillal.acc.R
import androidx.compose.ui.unit.dp
import com.hillal.acc.App
import androidx.compose.foundation.layout.imePadding
import androidx.navigation.NavController
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreenContainer(
    transactionsViewModel: TransactionsViewModel = viewModel(),
    navController: NavController? = null
) {
    val context = LocalContext.current
    val accountRepository = remember { (context.applicationContext as App).getAccountRepository() }
    val accountViewModel: AccountViewModel = viewModel(factory = AccountViewModelFactory(accountRepository))
    // If you have a similar factory for CashboxViewModel, use it here as well
    val cashboxViewModel: CashboxViewModel = viewModel()

    val accounts by accountViewModel.allAccounts.observeAsState(emptyList())
    val cashboxes by cashboxViewModel.getAllCashboxes().observeAsState(emptyList())
    val transactionsNullable by transactionsViewModel.getTransactions().observeAsState(emptyList())
    val transactions = transactionsNullable ?: emptyList()
    val balancesMapNullable by transactionsViewModel.accountBalancesMap.observeAsState(emptyMap())
    val balancesMap = (balancesMapNullable ?: emptyMap())
        .filterKeys { it != null }
        .mapKeys { it.key!! }
        .mapValues { entry ->
            (entry.value as? Map<String?, Double?>)
                ?.filterKeys { it != null }
                ?.mapKeys { it.key!! }
                ?.filterValues { it != null }
                ?.mapValues { it.value!! } ?: emptyMap()
        }
    val currencies = stringArrayResource(id = R.array.currencies_array).toList()
    var isLoading by remember { mutableStateOf(false) }

    // البيانات يتم تحميلها تلقائياً عبر LiveData

    // إضافة صندوق جديد
    fun addCashbox(name: String) {
        val newCashbox = Cashbox()
        newCashbox.name = name
        cashboxViewModel.insert(newCashbox)
    }

    Scaffold { padding ->
        Surface(modifier = Modifier.padding(padding).imePadding()) {
            TransferScreen(
                accounts = accounts,
                cashboxes = cashboxes,
                currencies = currencies,
                transactions = transactions,
                balancesMap = balancesMap,
                onAddCashbox = { addCashbox(it) },
                onNavigateBack = { navController?.navigateUp() },
                onTransfer = { fromAccount, toAccount, cashbox, currency, amount, notes ->
                    isLoading = true
                    try {
                        val amountValue = amount.toDoubleOrNull()
                        if (amountValue == null || amountValue <= 0) {
                            return@TransferScreen
                        }
                        if (fromAccount.getId() == toAccount.getId()) {
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
                        
                        // إظهار إشعار النجاح في الشاشة الرئيسية
                        Toast.makeText(context, "تم التحويل بنجاح! تم تحويل $amountValue $currency من ${fromAccount.getName()} إلى ${toAccount.getName()}", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "حدث خطأ أثناء التحويل: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                    isLoading = false
                }
            )
        }
    }
} 
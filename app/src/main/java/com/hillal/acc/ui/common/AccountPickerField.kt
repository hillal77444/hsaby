package com.hillal.acc.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hillal.acc.data.model.Account
import com.hillal.acc.data.model.Transaction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountPickerField(
    label: String = "الحساب",
    accounts: List<Account>,
    transactions: List<Transaction>,
    balancesMap: Map<Long, Map<String, Double>>,
    selectedAccount: Account?,
    onAccountSelected: (Account) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSheet by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = selectedAccount?.getName() ?: "",
        onValueChange = {},
        label = { Text(label) },
        modifier = modifier
            .fillMaxWidth()
            .clickable { showSheet = true },
        enabled = false,
        readOnly = true,
        trailingIcon = {
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
    )
    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }) {
            var search by remember { mutableStateOf("") }
            val filteredAccounts = if (search.isBlank()) accounts else accounts.filter { it.getName()?.contains(search) == true }
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(text = "اختر الحساب", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("بحث") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardActions = KeyboardActions(onDone = { /* Hide keyboard */ })
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
                                    showSheet = false
                                },
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(account.getName() ?: "", fontWeight = FontWeight.Bold)
                                // عرض الأرصدة حسب العملة
                                val balances = balancesMap[account.getId()] ?: emptyMap()
                                if (balances.isNotEmpty()) {
                                    balances.forEach { (currency, value) ->
                                        val isCredit = value >= 0
                                        val balanceText = if (isCredit) "الرصيد لكم " else "الرصيد عليكم "
                                        val absValue = kotlin.math.abs(value ?: 0.0)
                                        Text(
                                            "$balanceText${absValue.toLong()} $currency",
                                            fontSize = 13.sp,
                                            color = if (isCredit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                        )
                                    }
                                } else {
                                    Text("لا يوجد رصيد", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                                }
                                // عرض الملاحظات إن وجدت
                                val notes = account.getNotes()
                                if (!notes.isNullOrBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        notes,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.secondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { showSheet = false }, modifier = Modifier.align(Alignment.End)) {
                    Text("إغلاق")
                }
            }
        }
    }
} 
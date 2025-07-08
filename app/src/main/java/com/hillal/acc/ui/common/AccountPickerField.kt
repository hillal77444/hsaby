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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.AccountCircle

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
    var showDialog by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = selectedAccount?.getName() ?: "",
        onValueChange = {},
        label = { Text(label) },
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDialog = true },
        enabled = false,
        readOnly = true,
        trailingIcon = {
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
    )
    if (showDialog) {
        val configuration = LocalConfiguration.current
        val screenHeight = configuration.screenHeightDp.dp
        val dialogHeight = screenHeight * 0.6f
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("إغلاق") }
            },
            title = {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        text = "اختر الحساب",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    var search by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        label = { Text("بحث", color = MaterialTheme.colorScheme.secondary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.secondary,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    val filteredAccounts = if (search.isBlank()) accounts else accounts.filter { it.getName()?.contains(search) == true }
                    Box(
                        Modifier
                            .heightIn(max = dialogHeight)
                            .fillMaxWidth()
                    ) {
                        LazyColumn {
                            items(filteredAccounts) { account ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            onAccountSelected(account)
                                            showDialog = false
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(4.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        Modifier
                                            .padding(12.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.AccountCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(account.getName() ?: "", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            val balances = balancesMap[account.getId()] ?: emptyMap()
                                            if (balances.isNotEmpty()) {
                                                balances.forEach { (currency, value) ->
                                                    val isCredit = value >= 0
                                                    val balanceText = if (isCredit) "الرصيد لكم " else "الرصيد عليكم "
                                                    val absValue = kotlin.math.abs(value ?: 0.0)
                                                    Text(
                                                        "$balanceText${absValue.toLong()} $currency",
                                                        fontSize = 13.sp,
                                                        color = if (isCredit) Color(0xFF1976D2) else MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            } else {
                                                Text("لا يوجد رصيد", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                                            }
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
                        }
                    }
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }
} 
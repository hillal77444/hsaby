package com.hillal.acc.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hillal.acc.data.entities.Cashbox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashboxPickerField(
    label: String = "الصندوق",
    cashboxes: List<Cashbox>,
    selectedCashbox: Cashbox?,
    onCashboxSelected: (Cashbox) -> Unit,
    onAddCashbox: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = selectedCashbox?.name ?: "",
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
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("اختر الصندوق") },
            text = {
                Column {
                    cashboxes.forEach { cashbox ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onCashboxSelected(cashbox)
                                    showDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(cashbox.name ?: "", fontWeight = FontWeight.Bold)
                        }
                    }
                    Divider(Modifier.padding(vertical = 8.dp))
                    TextButton(onClick = {
                        showDialog = false
                        showAddDialog = true
                    }) {
                        Text("➕ إضافة صندوق جديد...")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text("إغلاق") }
            },
            dismissButton = {}
        )
    }
    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var error by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
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
                        onAddCashbox(name.trim())
                        name = ""
                        error = false
                        showAddDialog = false
                    }
                }) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                Button(onClick = {
                    name = ""
                    error = false
                    showAddDialog = false
                }) {
                    Text("إلغاء")
                }
            }
        )
    }
} 
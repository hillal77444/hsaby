package com.hillal.acc.ui.accounts

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.CircularProgressIndicator
import android.widget.Toast
import com.hillal.acc.R
import com.hillal.acc.data.model.Account
import java.util.*
import android.app.Activity
import androidx.compose.material.icons.filled.PersonAddAlt1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountComposeScreen(
    viewModel: AccountViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialAccount: Account? = null,
    onSave: ((Account) -> Unit)? = null
) {
    val context = LocalContext.current
    val spacing = ResponsiveSpacing()
    val padding = ResponsivePadding()
    
    // State variables
    var name by remember { mutableStateOf(initialAccount?.name ?: "") }
    var phone by remember { mutableStateOf(initialAccount?.phoneNumber ?: "") }
    var notes by remember { mutableStateOf(initialAccount?.notes ?: "") }
    var whatsappEnabled by remember { mutableStateOf(initialAccount?.isWhatsappEnabled ?: true) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    
    // Contact picker launcher (دقيق)
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            uri?.let {
                try {
                    val cursor: Cursor? = context.contentResolver.query(
                        it,
                        arrayOf(
                            ContactsContract.CommonDataKinds.Phone.NUMBER,
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                        ),
                        null,
                        null,
                        null
                    )
                    cursor?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val phoneNumber = cursor.getString(0)
                            val contactName = cursor.getString(1)
                            val cleanPhone = phoneNumber.replace(Regex("[^0-9+]"), "")
                            name = contactName
                            phone = cleanPhone
                            nameError = null
                            phoneError = null
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "حدث خطأ أثناء اختيار جهة الاتصال", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            contactPickerLauncher.launch(intent)
        } else {
            Toast.makeText(context, "يجب السماح بالوصول إلى جهات الاتصال لاختيار جهة اتصال", Toast.LENGTH_LONG).show()
        }
    }
    
    // Function to pick contact
    val pickContact = {
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED -> {
                val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                contactPickerLauncher.launch(intent)
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }
    }
    
    // Function to save or update account
    val saveAccount = fun() {
        // Reset errors
        nameError = null
        phoneError = null
        
        // Validation
        if (name.isEmpty()) {
            nameError = "الرجاء إدخال اسم الحساب"
            return
        }
        
        if (phone.isEmpty()) {
            phoneError = "الرجاء إدخال رقم الهاتف"
            return
        }
        
        // Check if phone number already exists (فقط عند الإضافة)
        if (initialAccount == null) {
            val existingAccount = viewModel.getAccountByPhoneNumber(phone)
            if (existingAccount != null) {
                phoneError = "رقم الهاتف موجود مسبقاً"
                return
            }
        }
        
        // Set saving state
        isSaving = true
        
        val account = if (initialAccount != null) {
            // تحديث حساب موجود
            initialAccount.name = name
            initialAccount.phoneNumber = phone
            initialAccount.notes = notes
            initialAccount.isWhatsappEnabled = whatsappEnabled
            initialAccount
        } else {
            // إنشاء حساب جديد
            Account(
                viewModel.generateUniqueAccountNumber(),
                name,
                100.0, // Opening balance
                phone,
                false // isDebtor
            ).apply {
                this.notes = notes
                this.setWhatsappEnabled(whatsappEnabled)
                this.serverId = -1
            }
        }
        
        if (onSave != null) {
            onSave(account)
            Toast.makeText(context, "تم تحديث الحساب بنجاح", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.insertAccount(account)
            Toast.makeText(context, "حساب جديد إضاف بنجاح", Toast.LENGTH_SHORT).show()
        }
        isSaving = false
        onNavigateBack()
    }
    
    ResponsiveAccountsTheme {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding.large)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(spacing.large)
            ) {
                // Header Section
                HeaderSection(spacing = spacing, padding = padding)
                
                // Form Section
                FormSection(
                    name = name,
                    onNameChange = { name = it },
                    nameError = nameError,
                    phone = phone,
                    onPhoneChange = { phone = it },
                    phoneError = phoneError,
                    notes = notes,
                    onNotesChange = { notes = it },
                    whatsappEnabled = whatsappEnabled,
                    onWhatsappEnabledChange = { whatsappEnabled = it },
                    onPickContact = pickContact,
                    spacing = spacing,
                    padding = padding
                )
                
                // Buttons Section
                ButtonsSection(
                    onSave = saveAccount,
                    onCancel = onNavigateBack,
                    isSaving = isSaving,
                    spacing = spacing,
                    padding = padding
                )
                
                // Bottom spacing
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun HeaderSection(
    spacing: ResponsiveSpacingValues,
    padding: ResponsivePaddingValues
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo Circle
        Card(
            modifier = Modifier
                .size(64.dp)
                .padding(top = spacing.xl),
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color(0xFF152FD9)
                )
            }
        }
        
        // Title with Icon
        Row(
            modifier = Modifier.padding(top = spacing.large),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color(0xFF152FD9)
            )
            Spacer(modifier = Modifier.width(spacing.small))
            Text(
                text = "إضافة حساب جديد",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF152FD9)
            )
        }
        
        // Description
        Text(
            text = "يرجى تعبئة بيانات الحساب بدقة",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.small, bottom = spacing.large)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormSection(
    name: String,
    onNameChange: (String) -> Unit,
    nameError: String?,
    phone: String,
    onPhoneChange: (String) -> Unit,
    phoneError: String?,
    notes: String,
    onNotesChange: (String) -> Unit,
    whatsappEnabled: Boolean,
    onWhatsappEnabledChange: (Boolean) -> Unit,
    onPickContact: () -> Unit,
    spacing: ResponsiveSpacingValues,
    padding: ResponsivePaddingValues
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(padding.large),
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            // Name Field
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("اسم الحساب") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF152FD9)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true,
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it) } },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF152FD9),
                    unfocusedBorderColor = Color(0xFF666666),
                    errorBorderColor = Color(0xFFE53E3E)
                )
            )
            
            // Phone Field with Contact Picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    label = { Text("رقم الهاتف") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = Color(0xFF152FD9)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    isError = phoneError != null,
                    supportingText = phoneError?.let { { Text(it) } },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF152FD9),
                        unfocusedBorderColor = Color(0xFF666666),
                        errorBorderColor = Color(0xFFE53E3E)
                    )
                )
                
                Button(
                    onClick = onPickContact,
                    modifier = Modifier.size(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF3F4F6),
                        contentColor = Color(0xFF152FD9)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PersonAddAlt1,
                        contentDescription = "اختيار من جهات الاتصال",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Notes Field
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = { Text("ملاحظات") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Note,
                        contentDescription = null,
                        tint = Color(0xFF152FD9)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF152FD9),
                    unfocusedBorderColor = Color(0xFF666666)
                )
            )
            
            // WhatsApp Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تفعيل واتساب",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF152FD9),
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = whatsappEnabled,
                    onCheckedChange = onWhatsappEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF22C55E),
                        checkedTrackColor = Color(0xFF22C55E),
                        uncheckedThumbColor = Color(0xFF666666),
                        uncheckedTrackColor = Color(0xFFE0E0E0)
                    )
                )
            }
            
            // WhatsApp description
            Text(
                text = "سيتم إرسال إشعارات واتساب للعميل عند إضافة معاملات جديدة",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666),
                modifier = Modifier.padding(start = spacing.medium)
            )
        }
    }
}

@Composable
private fun ButtonsSection(
    onSave: () -> Unit,
    onCancel: () -> Unit,
    isSaving: Boolean,
    spacing: ResponsiveSpacingValues,
    padding: ResponsivePaddingValues
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.small)
    ) {
        // Save Button
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF152FD9)
            ),
            shape = RoundedCornerShape(8.dp),
            enabled = !isSaving
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(spacing.small))
            Text(
                text = if (isSaving) "جاري الحفظ..." else "حفظ",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Cancel Button
        Button(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE53E3E)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Cancel,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(spacing.small))
            Text(
                text = "إلغاء",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
} 
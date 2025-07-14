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
import com.hillal.acc.ui.theme.AppTheme
import com.hillal.acc.ui.theme.LocalAppDimensions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountComposeScreen(
    viewModel: AccountViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialAccount: Account? = null,
    onSave: ((Account) -> Unit)? = null
) {
    AppTheme {
        val context = LocalContext.current
        val dimens = LocalAppDimensions.current
        val colors = MaterialTheme.colorScheme
        val typography = MaterialTheme.typography

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

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(dimens.spacingMedium)
                    .imePadding()
                    .padding(bottom = 56.dp),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingMedium)
            ) {
                // Header Section
                HeaderSection(dimens, colors, typography)
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
                    dimens = dimens,
                    colors = colors,
                    typography = typography
                )
                // Buttons Section
                ButtonsSection(
                    onSave = saveAccount,
                    onCancel = onNavigateBack,
                    isSaving = isSaving,
                    dimens = dimens,
                    colors = colors,
                    typography = typography
                )
                Spacer(modifier = Modifier.height(dimens.spacingMedium))
            }
        }
    }
}

@Composable
private fun HeaderSection(
    dimens: com.hillal.acc.ui.theme.AppDimensions,
    colors: ColorScheme,
    typography: Typography
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo Circle
        Card(
            modifier = Modifier
                .size(dimens.iconSize)
                .padding(top = dimens.spacingMedium),
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(dimens.iconSize * 0.7f),
                    tint = colors.primary
                )
            }
        }
        // Title with Icon
        Row(
            modifier = Modifier.padding(top = dimens.spacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = null,
                modifier = Modifier.size(dimens.iconSize * 0.7f),
                tint = colors.primary
            )
            Spacer(modifier = Modifier.width(dimens.spacingSmall / 2))
            Text(
                text = "إضافة حساب جديد",
                style = typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = colors.primary
            )
        }
        // Description
        Text(
            text = "يرجى تعبئة بيانات الحساب بدقة",
            style = typography.bodyMedium,
            color = colors.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = dimens.spacingSmall / 2, bottom = dimens.spacingMedium)
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
    dimens: com.hillal.acc.ui.theme.AppDimensions,
    colors: ColorScheme,
    typography: Typography
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimens.cardCorner),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(dimens.spacingSmall),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingSmall / 2)
        ) {
            // Name Field
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("اسم الحساب", fontSize = typography.bodyLarge.fontSize) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(dimens.iconSize)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true,
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it, color = colors.error, style = typography.bodySmall) } },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.onSurface,
                    errorBorderColor = colors.error
                ),
                textStyle = typography.bodyLarge
            )
            // Phone Field with Contact Picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimens.spacingSmall)
            ) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    label = { Text("رقم الهاتف", fontSize = typography.bodyLarge.fontSize) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(dimens.iconSize)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    isError = phoneError != null,
                    supportingText = phoneError?.let { { Text(it, color = colors.error, style = typography.bodySmall) } },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.onSurface,
                        errorBorderColor = colors.error
                    ),
                    textStyle = typography.bodyLarge
                )
                // زر جهات الاتصال بشكل أنيق
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .padding(top = 2.dp)
                ) {
                    Surface(
                        onClick = onPickContact,
                        shape = CircleShape,
                        color = colors.primaryContainer,
                        tonalElevation = 4.dp,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .size(dimens.iconSize * 1.4f)
                            .then(Modifier)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Contacts,
                                contentDescription = "اختيار من جهات الاتصال",
                                tint = colors.primary,
                                modifier = Modifier.size(dimens.iconSize)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "جهات الاتصال",
                        style = typography.labelLarge,
                        color = colors.primary,
                        fontWeight = FontWeight.Medium,
                        fontSize = typography.labelLarge.fontSize * 0.95f
                    )
                }
            }
            // Notes Field
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = { Text("ملاحظات", fontSize = typography.bodyLarge.fontSize) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Note,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(dimens.iconSize)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.onSurface
                ),
                textStyle = typography.bodyLarge
            )
            // WhatsApp Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تفعيل واتساب",
                    style = typography.bodyMedium,
                    color = colors.primary,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = whatsappEnabled,
                    onCheckedChange = onWhatsappEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.onPrimary,
                        checkedTrackColor = colors.secondary,
                        uncheckedThumbColor = colors.onSurface,
                        uncheckedTrackColor = colors.surfaceVariant
                    )
                )
            }
            // WhatsApp description
            Text(
                text = "سيتم إرسال إشعارات واتساب للعميل عند إضافة معاملات جديدة",
                style = typography.bodySmall,
                color = colors.onSurface,
                modifier = Modifier.padding(start = dimens.spacingSmall)
            )
        }
    }
}

@Composable
private fun ButtonsSection(
    onSave: () -> Unit,
    onCancel: () -> Unit,
    isSaving: Boolean,
    dimens: com.hillal.acc.ui.theme.AppDimensions,
    colors: ColorScheme,
    typography: Typography
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimens.spacingSmall)
    ) {
        // Save Button
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary
            ),
            shape = RoundedCornerShape(dimens.cardCorner / 2),
            enabled = !isSaving
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(dimens.iconSize * 0.7f),
                    color = colors.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(dimens.iconSize * 0.7f)
                )
            }
            Spacer(modifier = Modifier.width(dimens.spacingSmall))
            Text(
                text = if (isSaving) "جاري الحفظ..." else "حفظ",
                style = typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
        // Cancel Button
        Button(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.error
            ),
            shape = RoundedCornerShape(dimens.cardCorner / 2)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(dimens.iconSize * 0.7f)
            )
            Spacer(modifier = Modifier.width(dimens.spacingSmall))
            Text(
                text = "إلغاء",
                style = typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
} 
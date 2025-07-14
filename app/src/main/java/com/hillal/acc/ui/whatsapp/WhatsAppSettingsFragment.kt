package com.hillal.acc.ui.whatsapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.hillal.acc.ui.theme.AppTheme
import com.hillal.acc.ui.theme.LocalAppDimensions
import com.hillal.acc.R
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.fragment.findNavController
import androidx.compose.material3.ColorScheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.hillal.acc.util.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextFieldDefaults
import com.hillal.acc.data.remote.DataManager
import com.hillal.acc.data.room.AppDatabase

class WhatsAppSettingsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme {
                    WhatsAppSettingsScreen(onNavigate = { route ->
                        findNavController().navigate(route)
                    })
                }
            }
        }
    }
}

@Composable
fun WhatsAppSettingsScreen(onNavigate: (String) -> Unit) {
    val dimens = LocalAppDimensions.current
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val showDialog = remember { mutableStateOf(false) }
    val showChangeSessionDialog = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val sessionName = preferencesManager.getSessionName() ?: "غير محدد"
    var selectedSession by remember { mutableStateOf("admin_main") }
    var customSessionName by remember { mutableStateOf(sessionName) }
    var isSaving by remember { mutableStateOf(false) }
    val dataManager = remember { DataManager(context, AppDatabase.getInstance(context).accountDao(), AppDatabase.getInstance(context).transactionDao(), AppDatabase.getInstance(context).pendingOperationDao()) }
    val showLinkDialog = remember { mutableStateOf(false) }
    val showAdminMainWarning = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimens.spacingLarge, vertical = dimens.spacingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.height(dimens.spacingLarge))
        Icon(
            painter = painterResource(id = R.drawable.ic_whatsapp),
            contentDescription = "واتساب",
            tint = colors.primary,
            modifier = Modifier.size(dimens.iconSize * 2)
        )
        Spacer(Modifier.height(dimens.spacingMedium))
        Text(
            text = "إعدادات واتساب",
            style = typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = colors.primary)
        )
        Spacer(Modifier.height(dimens.spacingLarge))
        WhatsAppSettingCard(
            title = "تنسيق رسايل واتساب",
            iconRes = R.drawable.ic_edit,
            onClick = { showDialog.value = true },
            colors = colors,
            dimens = dimens,
            typography = typography
        )
        Spacer(Modifier.height(dimens.spacingMedium))
        WhatsAppSettingCard(
            title = "تغيير جلسة واتساب",
            iconRes = R.drawable.ic_accounts,
            onClick = { showChangeSessionDialog.value = true },
            colors = colors,
            dimens = dimens,
            typography = typography
        )
        Spacer(Modifier.height(dimens.spacingMedium))
        WhatsAppSettingCard(
            title = "ربط واتساب",
            iconRes = R.drawable.ic_sync_alt,
            onClick = {
                if (sessionName == "admin_main") {
                    showAdminMainWarning.value = true
                } else {
                    showLinkDialog.value = true
                }
            },
            colors = colors,
            dimens = dimens,
            typography = typography
        )
    }
    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = { showDialog.value = false },
            title = { Text("تنسيق رسايل واتساب") },
            text = { Text("سيتم تفعيلها قريباً") },
            confirmButton = {
                TextButton(onClick = { showDialog.value = false }) {
                    Text("حسناً")
                }
            }
        )
    }
    if (showChangeSessionDialog.value) {
        ChangeSessionDialog(
            currentSession = sessionName,
            onDismiss = { showChangeSessionDialog.value = false },
            onSave = { selected ->
                isSaving = true
                var newSession = selected
                if (selected == "custom") {
                    newSession = customSessionName
                    if (newSession == "admin_main") {
                        newSession = generateRandomSessionName(length = 12)
                        customSessionName = newSession
                    }
                }
                dataManager.updateSessionNameOnServer(newSession, object : DataManager.DataCallback {
                    override fun onSuccess() {
                        preferencesManager.saveSessionInfo(newSession, preferencesManager.getSessionExpiry())
                        Toast.makeText(context, "تم حفظ الجلسة بنجاح", Toast.LENGTH_SHORT).show()
                        isSaving = false
                        showChangeSessionDialog.value = false
                    }
                    override fun onError(error: String) {
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                        isSaving = false
                    }
                })
            },
            selectedSession = selectedSession,
            onSessionChange = { selectedSession = it },
            customSessionName = customSessionName,
            onCustomSessionChange = { customSessionName = it },
            isSaving = isSaving
        )
    }
    if (showLinkDialog.value) {
        val qrLink = "https://malyp.com/api/whatsapp/qr/$sessionName"
        AlertDialog(
            onDismissRequest = { showLinkDialog.value = false },
            title = { Text("ربط واتساب برقمك الخاص", style = typography.titleLarge, color = colors.primary) },
            text = {
                Column {
                    Text(
                        "ميزة ربط واتساب تتيح لك إرسال الرسائل من رقمك الخاص مباشرة لعملائك عبر التطبيق.",
                        style = typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = colors.onSurface)
                    )
                    Spacer(Modifier.height(dimens.spacingSmall))
                    Text(
                        "\u2022 افتح الرابط أدناه في متصفحك.\n" +
                        "\u2022 سيظهر لك رمز QR.\n" +
                        "\u2022 من تطبيق واتساب: الإعدادات > الأجهزة المرتبطة > ربط جهاز > ثم امسح الكود.\n\n" +
                        "رابط الربط:",
                        style = typography.bodyMedium.copy(color = colors.onSurface)
                    )
                    Spacer(Modifier.height(dimens.spacingSmall))
                    Text(qrLink, style = typography.bodyMedium.copy(color = colors.primary))
                }
            },
            confirmButton = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(qrLink))
                        context.startActivity(intent)
                        showLinkDialog.value = false
                    }) {
                        Text("فتح الرابط")
                    }
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("QR Link", qrLink)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "تم نسخ الرابط", Toast.LENGTH_SHORT).show()
                        showLinkDialog.value = false
                    }) {
                        Text("نسخ الرابط")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkDialog.value = false }) { Text("إلغاء") }
            }
        )
    }
    if (showAdminMainWarning.value) {
        AlertDialog(
            onDismissRequest = { showAdminMainWarning.value = false },
            title = { Text("تنبيه", style = typography.titleLarge, color = colors.error) },
            text = {
                Column {
                    Text(
                        "ميزة  إرسال الإشعارات من رقمك الخاص عبر واتساب\n" +
                        "يمكنك من خلال هذه الميزة ربط رقم واتساب الخاص بك بالتطبيق، بحيث يتم إرسال الرسائل والإشعارات مباشرة من رقمك إلى عملائك، مما يمنح رسائلك طابعًا شخصيًا واحترافيًا ويزيد من ثقة العملاء في التعامل معك.\n\n" +
                        "لا يمكنك استخدام الجلسة الافتراضية \ لإرسال الرسائل من رقمك الخاص.\nيرجى إنشاء جلسة خاصة بك أولاً قبل ربط واتساب.\nاذهب إلى 'تغيير جلسة واتساب' وأنشئ جلسة جديدة باسم خاص بك.",
                        style = typography.bodyMedium.copy(color = colors.onSurface)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAdminMainWarning.value = false }) { Text("حسنًا") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeSessionDialog(
    currentSession: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    selectedSession: String,
    onSessionChange: (String) -> Unit,
    customSessionName: String,
    onCustomSessionChange: (String) -> Unit,
    isSaving: Boolean
) {
    val dimens = LocalAppDimensions.current
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val options = listOf("جلسة النظام", "جلسة خاصة")
    val values = listOf("admin_main", customSessionName)
    var expanded by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(if (selectedSession == "admin_main") 0 else 1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تغيير جلسة واتساب", style = typography.titleLarge) },
        text = {
            Column {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    TextField(
                        value = options[selectedIndex],
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("اختيار الجلسة") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = TextFieldDefaults.textFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        options.forEachIndexed { index, option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selectedIndex = index
                                    expanded = false
                                    if (index == 0) {
                                        onSessionChange("admin_main")
                                    } else {
                                        // If the custom session is admin_main, generate random
                                        var custom = customSessionName
                                        if (custom == "admin_main") {
                                            custom = generateRandomSessionName(length = 12)
                                            onCustomSessionChange(custom)
                                        }
                                        onSessionChange("custom")
                                    }
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (!isSaving) onSave(if (selectedIndex == 0) "admin_main" else "custom") }) {
                Text(if (isSaving) "...جاري الحفظ" else "حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

fun generateRandomSessionName(length: Int = 12): String {
    val chars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    return (1..length).map { chars.random() }.joinToString("")
}

@Composable
fun WhatsAppSettingCard(
    title: String,
    iconRes: Int,
    onClick: () -> Unit,
    colors: ColorScheme,
    dimens: com.hillal.acc.ui.theme.AppDimensions,
    typography: androidx.compose.material3.Typography
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = dimens.spacingSmall),
        shape = RoundedCornerShape(dimens.cardCorner),
        elevation = CardDefaults.cardElevation(dimens.cardElevation),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(dimens.spacingMedium)
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                tint = colors.primary,
                modifier = Modifier.size(dimens.iconSize * 1.5f)
            )
            Spacer(Modifier.width(dimens.spacingMedium))
            Text(
                text = title,
                style = typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = colors.onSurface)
            )
        }
    }
} 
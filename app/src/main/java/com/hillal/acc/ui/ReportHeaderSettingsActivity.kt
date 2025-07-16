package com.hillal.acc.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.hillal.acc.R
import com.hillal.acc.ui.accounts.ResponsiveAccountsTheme
import com.hillal.acc.ui.theme.ProvideResponsiveDimensions
import java.io.InputStream
import androidx.activity.compose.rememberLauncherForActivityResult

class ReportHeaderSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProvideResponsiveDimensions {
                ResponsiveAccountsTheme {
                    ReportHeaderSettingsScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportHeaderSettingsScreen() {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("report_header_prefs", Context.MODE_PRIVATE)
    val snackbarHostState = remember { SnackbarHostState() }
    val dimensions = com.hillal.acc.ui.theme.LocalResponsiveDimensions.current

    var rightHeader by remember { mutableStateOf("") }
    var leftHeader by remember { mutableStateOf("") }
    var logoUri by remember { mutableStateOf<Uri?>(null) }
    var logoBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var showPermissionDenied by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showSavedMessage by remember { mutableStateOf(false) }

    // استرجاع البيانات المحفوظة عند أول تشغيل
    LaunchedEffect(Unit) {
        rightHeader = sharedPreferences.getString("right_header", "") ?: ""
        leftHeader = sharedPreferences.getString("left_header", "") ?: ""
        val logoUriString = sharedPreferences.getString("logo_uri", null)
        if (logoUriString != null) {
            logoUri = Uri.parse(logoUriString)
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(logoUri!!)
                logoBitmap = inputStream?.use { BitmapFactory.decodeStream(it) }
            } catch (_: Exception) {}
        }
    }

    // لاختيار صورة الشعار
    val pickLogoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            logoUri = uri
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                logoBitmap = inputStream?.use { BitmapFactory.decodeStream(it) }
            } catch (_: Exception) {}
        }
    }

    // منطق الصلاحيات
    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionLauncher = rememberLauncherForActivityResult(RequestPermission()) { isGranted ->
        if (isGranted) {
            pickLogoLauncher.launch("image/*")
        } else {
            showPermissionDenied = true
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "إعدادات ترويسة التقرير والشعار",
                        fontWeight = FontWeight.Bold,
                        fontSize = dimensions.titleFont
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { (context as? Activity)?.finish() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(padding)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(20.dp),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = rightHeader,
                        onValueChange = { rightHeader = it },
                        label = { Text("الترويسة اليمنى") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = leftHeader,
                        onValueChange = { leftHeader = it },
                        label = { Text("الترويسة اليسرى") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    Text(
                        "شعار التقرير:",
                        fontWeight = FontWeight.Bold,
                        fontSize = dimensions.bodyFont * 1.1f,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF5F5F5))
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable {
                                permissionLauncher.launch(permissionToRequest)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (logoBitmap != null) {
                            Image(
                                bitmap = logoBitmap!!.asImageBitmap(),
                                contentDescription = "الشعار",
                                modifier = Modifier.size(110.dp)
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "اختر شعار",
                                    modifier = Modifier.size(48.dp),
                                    tint = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "اضغط لاختيار الشعار",
                                    color = Color.Gray,
                                    fontSize = dimensions.bodyFont * 0.85f
                                )
                            }
                        }
                    }
                    if (showPermissionDenied) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "يجب منح صلاحية الوصول للصور لاختيار الشعار",
                            color = Color.Red,
                            fontWeight = FontWeight.Bold,
                            fontSize = dimensions.bodyFont * 0.9f
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = {
                            isSaving = true
                            val editor = sharedPreferences.edit()
                            editor.putString("right_header", rightHeader)
                            editor.putString("left_header", leftHeader)
                            editor.putString("logo_uri", logoUri?.toString())
                            editor.apply()
                            isSaving = false
                            showSavedMessage = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text("حفظ الإعدادات", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (showSavedMessage) {
                        LaunchedEffect(showSavedMessage) {
                            snackbarHostState.showSnackbar("تم حفظ الإعدادات بنجاح")
                            showSavedMessage = false
                            // إغلاق الصفحة بعد الحفظ
                            (context as? Activity)?.finish()
                        }
                    }
                }
            }
        }
    }
} 
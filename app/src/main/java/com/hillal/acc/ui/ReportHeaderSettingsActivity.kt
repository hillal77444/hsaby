package com.hillal.acc.ui

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hillal.acc.ui.theme.ProvideResponsiveDimensions
import com.hillal.acc.ui.accounts.ResponsiveAccountsTheme
import java.io.InputStream
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.yalantis.ucrop.UCrop
import java.io.File
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF


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

// دالة اقتصاص Bitmap إلى دائرة شفافة
fun cropToCircle(bitmap: android.graphics.Bitmap): android.graphics.Bitmap {
    val size = minOf(bitmap.width, bitmap.height)
    val output = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val rect = Rect(0, 0, size, size)
    val rectF = RectF(rect)
    canvas.drawARGB(0, 0, 0, 0)
    canvas.drawOval(rectF, paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(bitmap, null, rect, paint)
    return output
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
    var isSaving by remember { mutableStateOf(false) }
    var showSavedMessage by remember { mutableStateOf(false) }
    var logoPath by remember { mutableStateOf<String?>(null) }

    // استرجاع البيانات المحفوظة عند أول تشغيل
    LaunchedEffect(Unit) {
        rightHeader = sharedPreferences.getString("right_header", "") ?: ""
        leftHeader = sharedPreferences.getString("left_header", "") ?: ""
        val savedLogoPath = sharedPreferences.getString("logo_path", null)
        logoPath = savedLogoPath
        if (!savedLogoPath.isNullOrEmpty()) {
            try {
                val file = java.io.File(context.filesDir, savedLogoPath)
                if (file.exists()) {
                    logoBitmap = BitmapFactory.decodeFile(file.absolutePath)
                }
            } catch (_: Exception) {}
        }
    }

    // Launcher لاستقبال نتيجة الاقتصاص من uCrop
    val cropLogoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let { uri ->
                try {
                    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                    val bitmap = inputStream?.use { BitmapFactory.decodeStream(it) }
                    if (bitmap != null) {
                        val croppedBitmap = cropToCircle(bitmap)
                        logoBitmap = croppedBitmap
                        val file = File(context.filesDir, "logo.png")
                        val out = java.io.FileOutputStream(file)
                        croppedBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                        out.flush()
                        out.close()
                        logoPath = "logo.png"
                    }
                } catch (_: Exception) {}
            }
        }
    }

    // Launcher لاختيار الصورة من المعرض ثم إرسالها للاقتصاص الدائري عبر uCrop
    val pickLogoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val destUri = Uri.fromFile(File(context.cacheDir, "cropped_logo_${System.currentTimeMillis()}.png"))
            val uCrop = UCrop.of(it, destUri)
                .withAspectRatio(1f, 1f)
                .withOptions(UCrop.Options().apply {
                    setCircleDimmedLayer(true) // اقتصاص دائري
                    setShowCropFrame(false)
                    setShowCropGrid(false)
                })
            cropLogoLauncher.launch(uCrop.getIntent(context))
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
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
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
                                // مباشرة اختيار الصورة بدون صلاحية
                                pickLogoLauncher.launch("image/*")
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
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = {
                            isSaving = true
                            val editor = sharedPreferences.edit()
                            editor.putString("right_header", rightHeader)
                            editor.putString("left_header", leftHeader)
                            editor.putString("logo_path", logoPath)
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
                            Toast.makeText(context, "تم حفظ الإعدادات بنجاح", Toast.LENGTH_SHORT).show()
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
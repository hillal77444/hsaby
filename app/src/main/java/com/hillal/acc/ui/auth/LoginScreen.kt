package com.hillal.acc.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hillal.acc.R
import com.hillal.acc.ui.theme.AppTheme
import com.hillal.acc.ui.theme.LocalAppDimensions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.foundation.layout.navigationBarsPadding

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit,
    onRegisterClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onContactClick: () -> Unit,
    onAboutClick: () -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    AppTheme {
        val configuration = LocalConfiguration.current
        val scrollState = rememberScrollState()
        val dimens = LocalAppDimensions.current
        val colors = MaterialTheme.colorScheme
        val typography = MaterialTheme.typography

        var phone by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }
        var isLoading by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        // عند ظهور رسالة خطأ، أوقف التحميل تلقائياً
        LaunchedEffect(errorMessage) {
            if (errorMessage != null) {
                isLoading = false
            }
        }

        val screenWidth = configuration.screenWidthDp.toFloat().dp
        val screenHeight = configuration.screenHeightDp.toFloat().dp
        val blueHeight = screenHeight * 0.10f // أصغر
        val logoSize = screenWidth * 0.16f    // أصغر
        val cardCorner = dimens.cardCorner
        val cardPadding = dimens.spacingMedium
        val fieldHeight = screenHeight * 0.078f
        val buttonHeight = screenHeight * 0.055f
        val fontTitle = typography.headlineMedium.fontSize
        val fontField = typography.bodyLarge.fontSize
        val fontButton = typography.bodyLarge.fontSize
        val fontSmall = typography.bodyMedium.fontSize
        val iconSize = dimens.iconSize
        val marginSmall = dimens.spacingSmall
        val marginMedium = dimens.spacingMedium
        val marginLarge = dimens.spacingLarge

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // المستطيل الأزرق أعلى الشاشة
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(blueHeight)
                        .background(colors.primary)
                )
                // الشعار متداخل مع البطاقة
                Box(
                    modifier = Modifier
                        .size(logoSize)
                        .offset(y = -logoSize / 3) // تراكب أقل
                        .background(colors.surface, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher),
                        contentDescription = "Logo",
                        contentScale = ContentScale.Inside,
                        modifier = Modifier.size(logoSize * 0.8f)
                    )
                }
                Spacer(modifier = Modifier.height(marginSmall)) // مسافة واضحة وصغيرة بين الشعار والبطاقة
                // البطاقة البيضاء
                Card(
                    shape = RoundedCornerShape(cardCorner),
                    elevation = CardDefaults.cardElevation(12.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .wrapContentHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = cardPadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(marginMedium)
                    ) {
                        Text(
                            text = "تسجيل الدخول",
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = fontTitle,
                            modifier = Modifier.padding(bottom = marginSmall),
                            style = typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("رقم التلفون", fontSize = fontField) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Phone,
                                    contentDescription = null,
                                    modifier = Modifier.size(iconSize),
                                    tint = colors.primary
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(fieldHeight),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                containerColor = colors.surface,
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.onSurface,
                                cursorColor = colors.primary
                            ),
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("كلمة السر", fontSize = fontField) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(iconSize),
                                    tint = colors.primary
                                )
                            },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                val desc = if (passwordVisible) "إخفاء كلمة السر" else "إظهار كلمة السر"
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(imageVector = image, contentDescription = desc, modifier = Modifier.size(iconSize), tint = colors.primary)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(fieldHeight),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                containerColor = colors.surface,
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.onSurface,
                                cursorColor = colors.primary
                            ),
                        )
                        TextButton(
                            onClick = onForgotPasswordClick,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                text = "نسيت كلمة السر؟",
                                color = colors.primary,
                                fontSize = fontSmall,
                                style = typography.bodyMedium
                            )
                        }
                        if (isLoading) {
                            AlertDialog(
                                onDismissRequest = {},
                                title = { Text("جاري تسجيل الدخول...", style = typography.bodyLarge) },
                                text = { Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(); Spacer(Modifier.width(12.dp)); Text("يرجى الانتظار...", style = typography.bodyMedium) } },
                                confirmButton = {}
                            )
                        }
                        if (errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                color = colors.error,
                                fontSize = fontSmall,
                                modifier = Modifier.padding(top = marginSmall),
                                style = typography.bodyMedium
                            )
                        }
                        Button(
                            onClick = {
                                isLoading = true
                                errorMessage = null
                                onLoginClick(phone, password)
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = marginSmall)
                                .height(buttonHeight),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                            shape = RoundedCornerShape(cardCorner)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = colors.onPrimary, modifier = Modifier.size(fontButton.value.dp))
                            } else {
                                Text("دخول", color = colors.onPrimary, fontSize = fontButton, style = typography.bodyLarge)
                            }
                        }
                    }
                }
                Button(
                    onClick = onRegisterClick,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(top = marginMedium)
                        .height(buttonHeight),
                    shape = RoundedCornerShape(cardCorner),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text("إنشاء حساب جديد", color = colors.onPrimary, fontWeight = FontWeight.Bold, fontSize = fontButton, style = typography.bodyLarge)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(top = marginMedium, bottom = marginLarge),
                    horizontalArrangement = Arrangement.spacedBy(marginSmall, Alignment.CenterHorizontally)
                ) {
                    Button(
                        onClick = onPrivacyClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(buttonHeight),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = RoundedCornerShape(cardCorner),
                        contentPadding = PaddingValues(horizontal = marginSmall / 2, vertical = marginSmall / 3)
                    ) {
                        Text("سياسة الخصوصية", color = colors.onPrimary, fontSize = fontSmall, style = typography.bodyMedium)
                    }
                    Button(
                        onClick = onContactClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(buttonHeight),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = RoundedCornerShape(cardCorner),
                        contentPadding = PaddingValues(horizontal = marginSmall / 2, vertical = marginSmall / 3)
                    ) {
                        Text("أرقام التواصل", color = colors.onPrimary, fontSize = fontSmall, style = typography.bodyMedium)
                    }
                    Button(
                        onClick = onAboutClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(buttonHeight),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = RoundedCornerShape(cardCorner),
                        contentPadding = PaddingValues(horizontal = marginSmall / 2, vertical = marginSmall / 3)
                    ) {
                        Text("عن التطبيق", color = colors.onPrimary, fontSize = fontSmall, style = typography.bodyMedium)
                    }
                }
                Spacer(
                    modifier = Modifier
                        .height(marginLarge)
                        .navigationBarsPadding() // يضمن عدم التداخل مع شريط التنقل السفلي
                )
            }
        }
    }
} 
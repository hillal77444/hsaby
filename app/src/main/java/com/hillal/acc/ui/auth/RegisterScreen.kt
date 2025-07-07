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
import androidx.compose.material.icons.filled.Person
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegister: (displayName: String, phone: String, password: String, confirmPassword: String) -> Unit,
    onBackToLogin: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    val configuration = LocalConfiguration.current
    val screenWidth = with(LocalDensity.current) { configuration.screenWidthDp.dp }
    val screenHeight = with(LocalDensity.current) { configuration.screenHeightDp.dp }
    val scrollState = rememberScrollState()

    // نسب التصميم
    val blueHeight = screenHeight * 0.12f
    val logoSize = screenWidth * 0.22f
    val cardCorner = screenWidth * 0.07f
    val cardPadding = screenWidth * 0.028f
    val fieldHeight = screenHeight * 0.078f
    val buttonHeight = screenHeight * 0.055f
    val fontTitle = (screenWidth.value / 15).sp
    val fontField = (screenWidth.value / 22).sp
    val fontButton = (screenWidth.value / 22).sp
    val fontSmall = (screenWidth.value / 38).sp
    val iconSize = screenWidth * 0.07f
    val marginSmall = screenWidth * 0.007f
    val marginMedium = screenWidth * 0.018f
    val marginLarge = screenWidth * 0.035f

    var displayName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
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
                    .background(Color(0xFF2196F3))
            )
            // الشعار متداخل مع البطاقة
            Box(
                modifier = Modifier
                    .size(logoSize)
                    .offset(y = -logoSize / 2)
                    .background(Color.White, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher),
                    contentDescription = "Logo",
                    contentScale = ContentScale.Inside,
                    modifier = Modifier.size(logoSize * 0.8f)
                )
            }
            // البطاقة البيضاء
            Card(
                shape = RoundedCornerShape(cardCorner),
                elevation = CardDefaults.cardElevation(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
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
                        text = "إنشاء حساب جديد",
                        color = Color(0xFF3F51B5),
                        fontWeight = FontWeight.Bold,
                        fontSize = fontTitle,
                        modifier = Modifier.padding(bottom = marginSmall)
                    )
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("الاسم", fontSize = fontField) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(iconSize)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(fieldHeight),
                        colors = TextFieldDefaults.outlinedTextFieldColors(containerColor = Color.White),
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم الهاتف", fontSize = fontField) },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(iconSize)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(fieldHeight),
                        colors = TextFieldDefaults.outlinedTextFieldColors(containerColor = Color.White),
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("كلمة السر", fontSize = fontField) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(iconSize)) },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            val desc = if (passwordVisible) "إخفاء كلمة السر" else "إظهار كلمة السر"
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = desc, modifier = Modifier.size(iconSize))
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(fieldHeight),
                        colors = TextFieldDefaults.outlinedTextFieldColors(containerColor = Color.White),
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("تأكيد كلمة السر", fontSize = fontField) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(iconSize)) },
                        singleLine = true,
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            val desc = if (confirmPasswordVisible) "إخفاء كلمة السر" else "إظهار كلمة السر"
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(imageVector = image, contentDescription = desc, modifier = Modifier.size(iconSize))
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(fieldHeight),
                        colors = TextFieldDefaults.outlinedTextFieldColors(containerColor = Color.White),
                    )
                }
            }
            if (localError != null || errorMessage != null) {
                Text(
                    text = localError ?: errorMessage ?: "",
                    color = Color.Red,
                    fontSize = fontSmall,
                    modifier = Modifier.padding(top = marginSmall)
                )
            }
            // زر إنشاء حساب جديد
            Button(
                onClick = {
                    localError = when {
                        displayName.isEmpty() -> "الرجاء إدخال الاسم المستخدم في الإشعارات"
                        phone.isEmpty() -> "الرجاء إدخال رقم الهاتف"
                        password.isEmpty() -> "الرجاء إدخال كلمة المرور"
                        confirmPassword.isEmpty() -> "الرجاء تأكيد كلمة المرور"
                        password != confirmPassword -> "كلمة المرور غير متطابقة"
                        else -> null
                    }
                    if (localError == null) {
                        onRegister(displayName, phone, password, confirmPassword)
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(top = marginMedium)
                    .height(buttonHeight),
                shape = RoundedCornerShape(cardCorner),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF152FD9))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(fontButton.value.dp)
                    )
                } else {
                    Text("إنشاء حساب جديد", color = Color.White, fontWeight = FontWeight.Bold, fontSize = fontButton)
                }
            }
            // زر العودة لتسجيل الدخول
            Button(
                onClick = onBackToLogin,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(top = marginMedium, bottom = marginLarge)
                    .height(buttonHeight),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF152FD9)),
                shape = RoundedCornerShape(cardCorner)
            ) {
                Text("العودة لتسجيل الدخول", color = Color.White, fontSize = fontButton)
            }
        }
    }
} 
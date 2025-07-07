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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hillal.acc.R

@Composable
fun RegisterScreen(
    onRegister: (displayName: String, phone: String, password: String, confirmPassword: String) -> Unit,
    onBackToLogin: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFFFF))
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val scrollState = rememberScrollState()

        var displayName by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }
        var confirmPasswordVisible by remember { mutableStateOf(false) }
        var localError by remember { mutableStateOf<String?>(null) }

        // الدائرة العلوية
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(screenHeight * 0.25f)
                .graphicsLayer {
                    scaleX = 2.2f
                    scaleY = 1.2f
                }
                .background(Color(0xFF3F51B5))
                .align(Alignment.TopCenter)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = screenWidth * 0.04f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(screenHeight * 0.04f))
            // عنوان الصفحة
            Text(
                text = "إنشاء حساب جديد",
                color = Color.White,
                fontSize = (screenWidth.value / 15).sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .offset(y = (-screenHeight * 0.13f))
            )
            // الشعار الدائري
            Card(
                shape = CircleShape,
                elevation = CardDefaults.cardElevation(screenHeight * 0.008f),
                modifier = Modifier
                    .size(screenWidth * 0.25f)
                    .align(Alignment.CenterHorizontally)
                    .offset(y = (-screenHeight * 0.10f))
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher),
                    contentDescription = "Logo",
                    contentScale = ContentScale.Inside,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(screenHeight * 0.01f))
            // الحاوية البيضاء للحقول
            Card(
                shape = RoundedCornerShape(screenWidth * 0.04f),
                elevation = CardDefaults.cardElevation(screenHeight * 0.005f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = screenHeight * 0.01f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = screenWidth * 0.04f)
                ) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("الاسم ") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(screenHeight * 0.015f))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم الهاتف") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(screenHeight * 0.015f))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("كلمة السر") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            val desc = if (passwordVisible) "إخفاء كلمة السر" else "إظهار كلمة السر"
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = desc)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(screenHeight * 0.015f))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("تأكيد كلمة السر") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        singleLine = true,
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            val desc = if (confirmPasswordVisible) "إخفاء كلمة السر" else "إظهار كلمة السر"
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(imageVector = image, contentDescription = desc)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            if (localError != null || errorMessage != null) {
                Text(
                    text = localError ?: errorMessage ?: "",
                    color = Color.Red,
                    fontSize = (screenWidth.value / 28).sp,
                    modifier = Modifier.padding(top = screenHeight * 0.01f)
                )
            }
            // زر إنشاء حساب جديد
            Button(
                onClick = {
                    // تحقق محلي قبل الاستدعاء الخارجي
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
                    .fillMaxWidth()
                    .padding(
                        top = screenHeight * 0.02f,
                        start = screenWidth * 0.08f,
                        end = screenWidth * 0.08f
                    )
                    .height(screenHeight * 0.055f),
                shape = RoundedCornerShape(screenWidth * 0.03f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF152FD9))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size((screenWidth.value / 18).dp)
                    )
                } else {
                    Text("إنشاء حساب جديد", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            // زر العودة لتسجيل الدخول
            Button(
                onClick = onBackToLogin,
                modifier = Modifier
                    .padding(top = screenHeight * 0.015f)
                    .align(Alignment.CenterHorizontally),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF152FD9))
            ) {
                Text("العودة لتسجيل الدخول", color = Color.White)
            }
        }
    }
} 
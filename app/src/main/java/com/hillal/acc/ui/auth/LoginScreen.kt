package com.hillal.acc.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hillal.acc.R

@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit,
    onRegisterClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onContactClick: () -> Unit,
    onAboutClick: () -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F8F8))
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        var phone by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }

        // الدائرة البنفسجية العلوية
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
                .padding(horizontal = screenWidth * 0.04f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(screenHeight * 0.04f))
            // عنوان تسجيل الدخول
            Text(
                text = "تسجيل الدخول",
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
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم التلفون") },
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
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        onClick = onForgotPasswordClick,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = "نسيت كلمة السر؟",
                            color = Color(0xFF3F51B5),
                            fontSize = (screenWidth.value / 28).sp
                        )
                    }
                    Button(
                        onClick = { onLoginClick(phone, password) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = screenHeight * 0.015f)
                            .height(screenHeight * 0.055f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF152FD9))
                    ) {
                        Text("دخول", color = Color.White, fontSize = (screenWidth.value / 22).sp)
                    }
                }
            }
            // زر إنشاء حساب جديد
            Button(
                onClick = onRegisterClick,
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
                Text("إنشاء حساب جديد", color = Color.White, fontWeight = FontWeight.Bold)
            }
            // الأزرار السفلية
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = screenHeight * 0.03f,
                        bottom = screenHeight * 0.04f,
                        start = screenWidth * 0.02f,
                        end = screenWidth * 0.02f
                    ),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = onPrivacyClick,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = screenWidth * 0.01f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF152FD9))
                ) {
                    Text("سياسة الخصوصية", color = Color.White, fontSize = (screenWidth.value / 30).sp)
                }
                Button(
                    onClick = onContactClick,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = screenWidth * 0.01f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF152FD9))
                ) {
                    Text("ارقام التواصل", color = Color.White, fontSize = (screenWidth.value / 30).sp)
                }
                Button(
                    onClick = onAboutClick,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = screenWidth * 0.01f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF152FD9))
                ) {
                    Text("حول التطبيق", color = Color.White, fontSize = (screenWidth.value / 30).sp)
                }
            }
        }
    }
} 
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hillal.acc.R
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff

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
            .background(Color(0xFFFFFFFF))
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val scrollState = rememberScrollState()

        var phone by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }

        // أحجام ديناميكية مع حدود دنيا/قصوى
        val cardCorner = (screenWidth * 0.04f).coerceIn(12.dp, 32.dp)
        val cardPadding = (screenWidth * 0.04f).coerceIn(12.dp, 32.dp)
        val logoSize = (screenWidth * 0.22f).coerceIn(72.dp, 140.dp)
        val fieldHeight = (screenHeight * 0.065f).coerceIn(44.dp, 64.dp)
        val buttonHeight = (screenHeight * 0.055f).coerceIn(40.dp, 56.dp)
        val fontTitle = (screenWidth.value / 15).sp.coerceIn(18.sp, 32.sp)
        val fontField = (screenWidth.value / 22).sp.coerceIn(14.sp, 20.sp)
        val fontButton = (screenWidth.value / 22).sp.coerceIn(14.sp, 20.sp)
        val fontSmall = (screenWidth.value / 30).sp.coerceIn(10.sp, 16.sp)
        val iconSize = (screenWidth * 0.07f).coerceIn(20.dp, 32.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFFFFF))
        ) {
            // AppBar رسمي
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "تسجيل الدخول",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = fontTitle
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF3F51B5)),
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF3F51B5)
                )
            )
            // الدائرة البنفسجية والشعار
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((screenHeight * 0.18f).coerceIn(60.dp, 120.dp))
                    .background(Color(0xFF3F51B5))
            ) {
                Card(
                    shape = CircleShape,
                    elevation = CardDefaults.cardElevation(8.dp),
                    modifier = Modifier
                        .size(logoSize)
                        .align(Alignment.BottomCenter)
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher),
                        contentDescription = "Logo",
                        contentScale = ContentScale.Inside,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(modifier = Modifier.height((screenHeight * 0.01f).coerceIn(4.dp, 16.dp)))
            // الحاوية البيضاء للحقول
            Card(
                shape = RoundedCornerShape(cardCorner),
                elevation = CardDefaults.cardElevation(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = cardPadding, vertical = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = cardPadding)
                ) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم التلفون", fontSize = fontField) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(iconSize)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(fieldHeight)
                    )
                    Spacer(modifier = Modifier.height((screenHeight * 0.012f).coerceIn(4.dp, 12.dp)))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("كلمة السر", fontSize = fontField) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(iconSize)
                            )
                        },
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
                            .height(fieldHeight)
                    )
                    TextButton(
                        onClick = onForgotPasswordClick,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = "نسيت كلمة السر؟",
                            color = Color(0xFF3F51B5),
                            fontSize = fontSmall
                        )
                    }
                    Button(
                        onClick = { onLoginClick(phone, password) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = (screenHeight * 0.012f).coerceIn(4.dp, 12.dp))
                            .height(buttonHeight),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF152FD9)),
                        shape = RoundedCornerShape(cardCorner)
                    ) {
                        Text("دخول", color = Color.White, fontSize = fontButton)
                    }
                }
            }
            // زر إنشاء حساب جديد
            Button(
                onClick = onRegisterClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = (screenHeight * 0.018f).coerceIn(6.dp, 18.dp),
                        start = (screenWidth * 0.08f).coerceIn(16.dp, 40.dp),
                        end = (screenWidth * 0.08f).coerceIn(16.dp, 40.dp)
                    )
                    .height(buttonHeight),
                shape = RoundedCornerShape(cardCorner),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF152FD9))
            ) {
                Text("إنشاء حساب جديد", color = Color.White, fontWeight = FontWeight.Bold, fontSize = fontButton)
            }
            // الأزرار السفلية
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = (screenHeight * 0.02f).coerceIn(6.dp, 20.dp),
                        bottom = (screenHeight * 0.03f).coerceIn(8.dp, 28.dp),
                        start = (screenWidth * 0.02f).coerceIn(4.dp, 16.dp),
                        end = (screenWidth * 0.02f).coerceIn(4.dp, 16.dp)
                    ),
                horizontalArrangement = Arrangement.spacedBy((screenWidth * 0.01f).coerceIn(2.dp, 8.dp), Alignment.CenterHorizontally)
            ) {
                Button(
                    onClick = onPrivacyClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF152FD9)),
                    shape = RoundedCornerShape(cardCorner)
                ) {
                    Text("سياسة الخصوصية", color = Color.White, fontSize = fontSmall)
                }
                Button(
                    onClick = onContactClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF152FD9)),
                    shape = RoundedCornerShape(cardCorner)
                ) {
                    Text("ارقام التواصل", color = Color.White, fontSize = fontSmall)
                }
                Button(
                    onClick = onAboutClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF152FD9)),
                    shape = RoundedCornerShape(cardCorner)
                ) {
                    Text("حول التطبيق", color = Color.White, fontSize = fontSmall)
                }
            }
        }
    }
} 
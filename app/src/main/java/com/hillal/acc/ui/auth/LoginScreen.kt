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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
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
            .background(Color.White)
    ) {
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val scrollState = rememberScrollState()
        val density = LocalDensity.current

        var phone by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }

        // أحجام ديناميكية بنسب فقط (بدون حدود دنيا/قصوى)
        fun relW(f: Float) = (screenWidth * f)
        fun relH(f: Float) = (screenHeight * f)
        val cardCorner = relW(0.07f)
        val cardPadding = relW(0.028f)
        val logoSize = relW(0.22f)
        val fieldHeight = relH(0.078f) // 0.065 * 1.2 تقريبًا
        val buttonHeight = relH(0.055f)
        val fontTitle = (screenWidth.value / 15).sp
        val fontField = (screenWidth.value / 22).sp
        val fontButton = (screenWidth.value / 22).sp
        val fontSmall = (screenWidth.value / 38).sp
        val iconSize = relW(0.07f)
        val marginSmall = relW(0.007f)
        val marginMedium = relW(0.018f)
        val marginLarge = relW(0.035f)
        val fieldInnerPadding = PaddingValues(horizontal = relW(0.03f), vertical = relH(0.01f))
        val minButtonWidth = relW(0.18f)

        val blueHeight = relH(0.32f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(blueHeight)
                    .background(Color(0xFF2196F3))
            ) {
                // يمكن وضع أي محتوى أعلى الأزرق هنا إذا أردت
            }
            Box(
                modifier = Modifier
                    .offset(y = -logoSize / 2)
                    .size(logoSize)
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
            Spacer(modifier = Modifier.height(marginMedium))
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
                        text = "تسجيل الدخول",
                        color = Color(0xFF3F51B5),
                        fontWeight = FontWeight.Bold,
                        fontSize = fontTitle,
                        modifier = Modifier.padding(bottom = marginSmall)
                    )
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
                            .height(fieldHeight),
                        colors = TextFieldDefaults.outlinedTextFieldColors(containerColor = Color.White),
                    )
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
                            .height(fieldHeight),
                        colors = TextFieldDefaults.outlinedTextFieldColors(containerColor = Color.White),
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
                            .padding(top = marginSmall)
                            .height(buttonHeight),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF152FD9)),
                        shape = RoundedCornerShape(cardCorner)
                    ) {
                        Text("دخول", color = Color.White, fontSize = fontButton)
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF152FD9))
            ) {
                Text("إنشاء حساب جديد", color = Color.White, fontWeight = FontWeight.Bold, fontSize = fontButton)
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF152FD9)),
                    shape = RoundedCornerShape(cardCorner),
                    contentPadding = PaddingValues(horizontal = marginSmall / 2, vertical = marginSmall / 3)
                ) {
                    Text("سياسة الخصوصية", color = Color.White, fontSize = fontSmall)
                }
                Button(
                    onClick = onContactClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF152FD9)),
                    shape = RoundedCornerShape(cardCorner),
                    contentPadding = PaddingValues(horizontal = marginSmall / 2, vertical = marginSmall / 3)
                ) {
                    Text("أرقام التواصل", color = Color.White, fontSize = fontSmall)
                }
                Button(
                    onClick = onAboutClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF152FD9)),
                    shape = RoundedCornerShape(cardCorner),
                    contentPadding = PaddingValues(horizontal = marginSmall / 2, vertical = marginSmall / 3)
                ) {
                    Text("عن التطبيق", color = Color.White, fontSize = fontSmall)
                }
            }
        }
    }
} 
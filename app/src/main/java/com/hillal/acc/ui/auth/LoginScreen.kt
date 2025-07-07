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
            .background(Color(0xFFFFFFFF))
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

        // أحجام ديناميكية مع حدود دنيا/قصوى (كل شيء نسبي)
        fun relW(f: Float, min: Float, max: Float) = max(min, min((screenWidth * f).value, max)).dp
        fun relH(f: Float, min: Float, max: Float) = max(min, min((screenHeight * f).value, max)).dp
        val cardCorner = relW(0.07f, 18f, 40f)
        val cardPadding = relW(0.028f, 8f, 22f)
        val logoSize = relW(0.22f, 72f, 140f)
        val fieldHeight = (relH(0.065f, 44f, 64f) * 1.2f)
        val buttonHeight = relH(0.055f, 40f, 56f)
        val fontTitle = max(18f, min((screenWidth.value / 15), 32f)).sp
        val fontField = max(14f, min((screenWidth.value / 22), 20f)).sp
        val fontButton = max(14f, min((screenWidth.value / 22), 20f)).sp
        val fontSmall = max(8f, min((screenWidth.value / 38), 13f)).sp
        val iconSize = relW(0.07f, 20f, 32f)
        val marginSmall = relW(0.007f, 1f, 8f)
        val marginMedium = relW(0.018f, 4f, 14f)
        val marginLarge = relW(0.035f, 10f, 24f)
        val fieldInnerPadding = PaddingValues(horizontal = relW(0.03f, 8f, 20f), vertical = relH(0.01f, 4f, 12f))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .imePadding()
                .background(Color(0xFFFFFFFF))
        ) {
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
            Spacer(modifier = Modifier.height(marginMedium))
            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = marginLarge, vertical = marginMedium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = CircleShape,
                            elevation = CardDefaults.cardElevation(8.dp),
                            modifier = Modifier.size(logoSize)
                        ) {
                            Image(
                                painter = painterResource(id = R.mipmap.ic_launcher),
                                contentDescription = "Logo",
                                contentScale = ContentScale.Inside,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(marginLarge))
                    Column(
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        LoginFields(
                            phone = phone,
                            onPhoneChange = { phone = it },
                            password = password,
                            onPasswordChange = { password = it },
                            passwordVisible = passwordVisible,
                            onPasswordVisibleChange = { passwordVisible = it },
                            onForgotPasswordClick = onForgotPasswordClick,
                            onLoginClick = { onLoginClick(phone, password) },
                            onRegisterClick = onRegisterClick,
                            onPrivacyClick = onPrivacyClick,
                            onContactClick = onContactClick,
                            onAboutClick = onAboutClick,
                            cardCorner = cardCorner,
                            cardPadding = cardPadding,
                            fieldHeight = fieldHeight,
                            buttonHeight = buttonHeight,
                            fontField = fontField,
                            fontButton = fontButton,
                            fontSmall = fontSmall,
                            iconSize = iconSize,
                            fieldInnerPadding = fieldInnerPadding,
                            marginSmall = marginSmall,
                            marginMedium = marginMedium,
                            marginLarge = marginLarge
                        )
                    }
                }
            } else {
                // Box متداخل: البنفسجي + الدائرة + البطاقة البيضاء (تداخل أنيق)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(relH(0.18f, 60f, 120f) + (logoSize * 0.33f)) // يكفي ليظهر الشعار بالكامل
                        .background(Color(0xFF3F51B5))
                ) {
                    Card(
                        shape = CircleShape,
                        elevation = CardDefaults.cardElevation(12.dp),
                        modifier = Modifier
                            .size(logoSize)
                            .align(Alignment.BottomCenter)
                            .offset(y = logoSize * 0.33f) // تداخل أنيق (ثلث الدائرة)
                    ) {
                        Image(
                            painter = painterResource(id = R.mipmap.ic_launcher),
                            contentDescription = "Logo",
                            contentScale = ContentScale.Inside,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(-(logoSize * 0.33f)))
                LoginFields(
                    phone = phone,
                    onPhoneChange = { phone = it },
                    password = password,
                    onPasswordChange = { password = it },
                    passwordVisible = passwordVisible,
                    onPasswordVisibleChange = { passwordVisible = it },
                    onForgotPasswordClick = onForgotPasswordClick,
                    onLoginClick = { onLoginClick(phone, password) },
                    onRegisterClick = onRegisterClick,
                    onPrivacyClick = onPrivacyClick,
                    onContactClick = onContactClick,
                    onAboutClick = onAboutClick,
                    cardCorner = cardCorner,
                    cardPadding = cardPadding,
                    fieldHeight = fieldHeight,
                    buttonHeight = buttonHeight,
                    fontField = fontField,
                    fontButton = fontButton,
                    fontSmall = fontSmall,
                    iconSize = iconSize,
                    fieldInnerPadding = fieldInnerPadding,
                    marginSmall = marginSmall,
                    marginMedium = marginMedium,
                    marginLarge = marginLarge
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginFields(
    phone: String,
    onPhoneChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibleChange: (Boolean) -> Unit,
    onForgotPasswordClick: () -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onContactClick: () -> Unit,
    onAboutClick: () -> Unit,
    cardCorner: Dp,
    cardPadding: Dp,
    fieldHeight: Dp,
    buttonHeight: Dp,
    fontField: androidx.compose.ui.unit.TextUnit,
    fontButton: androidx.compose.ui.unit.TextUnit,
    fontSmall: androidx.compose.ui.unit.TextUnit,
    iconSize: Dp,
    fieldInnerPadding: PaddingValues,
    marginSmall: Dp,
    marginMedium: Dp,
    marginLarge: Dp
) {
    Card(
        shape = RoundedCornerShape(cardCorner),
        elevation = CardDefaults.cardElevation(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                onValueChange = onPhoneChange,
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
            Spacer(modifier = Modifier.height(marginSmall))
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
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
                    IconButton(onClick = { onPasswordVisibleChange(!passwordVisible) }) {
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
                onClick = onLoginClick,
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
            .fillMaxWidth()
            .padding(
                top = marginMedium,
                start = marginLarge,
                end = marginLarge
            )
            .height(buttonHeight),
        shape = RoundedCornerShape(cardCorner),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF152FD9))
    ) {
        Text("إنشاء حساب جديد", color = Color.White, fontWeight = FontWeight.Bold, fontSize = fontButton)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = marginMedium,
                bottom = marginLarge,
                start = marginMedium,
                end = marginMedium
            ),
        horizontalArrangement = Arrangement.spacedBy(marginSmall / 2, Alignment.CenterHorizontally)
    ) {
        Button(
            onClick = onPrivacyClick,
            modifier = Modifier
                .weight(1f)
                .height(buttonHeight)
                .widthIn(min = relW(0.18f, 60f, 100f)),
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
                .height(buttonHeight)
                .widthIn(min = relW(0.18f, 60f, 100f)),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF152FD9)),
            shape = RoundedCornerShape(cardCorner),
            contentPadding = PaddingValues(horizontal = marginSmall / 2, vertical = marginSmall / 3)
        ) {
            Text("ارقام التواصل", color = Color.White, fontSize = fontSmall)
        }
        Button(
            onClick = onAboutClick,
            modifier = Modifier
                .weight(1f)
                .height(buttonHeight)
                .widthIn(min = relW(0.18f, 60f, 100f)),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF152FD9)),
            shape = RoundedCornerShape(cardCorner),
            contentPadding = PaddingValues(horizontal = marginSmall / 2, vertical = marginSmall / 3)
        ) {
            Text("حول التطبيق", color = Color.White, fontSize = fontSmall)
        }
    }
} 
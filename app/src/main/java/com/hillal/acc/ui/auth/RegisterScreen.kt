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
import com.hillal.acc.ui.theme.AppTheme
import com.hillal.acc.ui.theme.LocalAppDimensions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shadow
import androidx.compose.foundation.layout.imePadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegister: (displayName: String, phone: String, password: String, confirmPassword: String) -> Unit,
    onBackToLogin: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    AppTheme {
        val configuration = LocalConfiguration.current
        val dimens = LocalAppDimensions.current
        val colors = MaterialTheme.colorScheme
        val typography = MaterialTheme.typography
        val scrollState = rememberScrollState()

        var displayName by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }
        var confirmPasswordVisible by remember { mutableStateOf(false) }
        var localError by remember { mutableStateOf<String?>(null) }

        val logoSize = dimens.logoSize
        val cardCorner = dimens.cardCorner
        val cardPadding = dimens.spacingMedium
        val fieldHeight = dimens.fieldHeight
        val buttonHeight = dimens.buttonHeight
        val fontTitle = typography.headlineMedium.fontSize
        val fontField = typography.bodyLarge.fontSize
        val fontButton = typography.bodyLarge.fontSize
        val fontSmall = dimens.fontSmall
        val iconSize = dimens.iconSize
        val iconSizeSmall = dimens.iconSizeSmall
        val marginSmall = dimens.spacingSmall
        val marginMedium = dimens.spacingMedium
        val marginLarge = dimens.spacingLarge
        val cardElevation = dimens.cardElevation
        val fieldCorner = dimens.cardCorner

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(colors.gradient1, colors.gradient2)
                    )
                )
                .navigationBarsPadding()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(marginMedium))
                // الشعار مع أنيميشن دخول خفيف
                androidx.compose.animation.AnimatedVisibility(visible = true, enter = androidx.compose.animation.fadeIn()) {
                    Box(
                        modifier = Modifier
                            .size(logoSize)
                            .background(colors.surface, shape = CircleShape)
                            .shadow(cardElevation, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.mipmap.ic_launcher),
                            contentDescription = "Logo",
                            contentScale = ContentScale.Inside,
                            modifier = Modifier.size(logoSize * 0.8f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(marginSmall))
                // البطاقة البيضاء
                Card(
                    shape = RoundedCornerShape(cardCorner),
                    elevation = CardDefaults.cardElevation(cardElevation),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    modifier = Modifier
                        .fillMaxWidth(0.97f)
                        .wrapContentHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = cardPadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(marginSmall)
                    ) {
                        Text(
                            text = "إنشاء حساب جديد",
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = fontTitle,
                            modifier = Modifier.padding(bottom = 2.dp),
                            style = typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("الاسم", fontSize = fontField) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(iconSize), tint = colors.primary) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(fieldHeight),
                            shape = RoundedCornerShape(fieldCorner),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                containerColor = colors.backgroundVariant,
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.outline,
                                cursorColor = colors.primary
                            ),
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("رقم الهاتف", fontSize = fontField) },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(iconSize), tint = colors.primary) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(fieldHeight),
                            shape = RoundedCornerShape(fieldCorner),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                containerColor = colors.backgroundVariant,
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.outline,
                                cursorColor = colors.primary
                            ),
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("كلمة السر", fontSize = fontField) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(iconSize), tint = colors.primary) },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                val desc = if (passwordVisible) "إخفاء كلمة السر" else "إظهار كلمة السر"
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(imageVector = image, contentDescription = desc, modifier = Modifier.size(iconSizeSmall), tint = colors.primary)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(fieldHeight),
                            shape = RoundedCornerShape(fieldCorner),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                containerColor = colors.backgroundVariant,
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.outline,
                                cursorColor = colors.primary
                            ),
                        )
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("تأكيد كلمة السر", fontSize = fontField) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(iconSize), tint = colors.primary) },
                            singleLine = true,
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                val image = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                val desc = if (confirmPasswordVisible) "إخفاء كلمة السر" else "إظهار كلمة السر"
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(imageVector = image, contentDescription = desc, modifier = Modifier.size(iconSizeSmall), tint = colors.primary)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(fieldHeight),
                            shape = RoundedCornerShape(fieldCorner),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                containerColor = colors.backgroundVariant,
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.outline,
                                cursorColor = colors.primary
                            ),
                        )
                    }
                }
                if (localError != null || errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = colors.errorContainer),
                        shape = RoundedCornerShape(fieldCorner),
                        modifier = Modifier.fillMaxWidth(0.97f).padding(vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(marginSmall)) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = colors.error, modifier = Modifier.size(iconSizeSmall))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = localError ?: errorMessage ?: "",
                                color = colors.error,
                                fontSize = fontSmall,
                                style = typography.bodyMedium
                            )
                        }
                    }
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
                        .fillMaxWidth(0.97f)
                        .padding(top = marginMedium)
                        .height(buttonHeight),
                    shape = RoundedCornerShape(cardCorner),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = colors.onPrimary, modifier = Modifier.size(fontButton.value.dp))
                    } else {
                        Text("تسجيل", color = colors.onPrimary, fontSize = fontButton, style = typography.bodyLarge)
                    }
                }
                OutlinedButton(
                    onClick = onBackToLogin,
                    modifier = Modifier
                        .fillMaxWidth(0.97f)
                        .padding(top = marginSmall)
                        .height(buttonHeight),
                    shape = RoundedCornerShape(cardCorner),
                    border = ButtonDefaults.outlinedButtonBorder,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(iconSizeSmall), tint = colors.primary)
                    Spacer(Modifier.width(4.dp))
                    Text("العودة لتسجيل الدخول", fontWeight = FontWeight.Bold, fontSize = fontButton, style = typography.bodyLarge)
                }
                Spacer(modifier = Modifier.height(marginSmall))
            }
        }
    }
} 
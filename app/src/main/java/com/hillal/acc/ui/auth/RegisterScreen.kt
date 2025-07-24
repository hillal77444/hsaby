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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.ui.graphics.graphicsLayer
import com.hillal.acc.ui.theme.gradient1
import com.hillal.acc.ui.theme.gradient2
import com.hillal.acc.ui.theme.backgroundVariant
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions

// دالة ترجمة رسائل الخطأ القادمة من الخادم
fun translateErrorMessage(serverMessage: String?): String {
    return when (serverMessage) {
        "اسم المستخدم موجود مسبقاً" -> "اسم المستخدم مستخدم بالفعل"
        "رقم الهاتف مسجل مسبقاً" -> "رقم الهاتف مستخدم بالفعل"
        "كلمة المرور غير متطابقة مع التأكيد" -> "تأكد من كتابة كلمة المرور بشكل صحيح"
        "كلمة المرور يجب أن تكون 6 أحرف على الأقل" -> "كلمة المرور قصيرة جداً"
        "الرجاء إدخال اسم المستخدم" -> "يرجى إدخال اسم المستخدم"
        "الرجاء إدخال رقم الهاتف" -> "يرجى إدخال رقم الهاتف"
        "الرجاء إدخال كلمة المرور" -> "يرجى إدخال كلمة المرور"
        "الرجاء إدخال تأكيد كلمة المرور" -> "يرجى تأكيد كلمة المرور"
        "حدث خطأ أثناء إنشاء الحساب" -> "حدث خطأ أثناء إنشاء الحساب، حاول لاحقاً"
        else -> serverMessage ?: "حدث خطأ غير متوقع"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegister: (displayName: String, phone: String, password: String, confirmPassword: String) -> Unit,
    onBackToLogin: () -> Unit,
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
        var isLoading by remember { mutableStateOf(false) }

        // عند ظهور رسالة الخطأ، أعد isLoading إلى false
        LaunchedEffect(errorMessage) {
            if (errorMessage != null) {
                isLoading = false
            }
        }

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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            try { colors.gradient1 } catch(_: Exception) { colors.primary },
                            try { colors.gradient2 } catch(_: Exception) { colors.secondary }
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .imePadding(), // نقلناها هنا فقط
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(marginMedium))
                // الشعار مع أنيميشن دخول خفيف (ScaleIn + FadeIn)
                androidx.compose.animation.AnimatedVisibility(
                    visible = true,
                    enter = scaleIn(initialScale = dimens.logoScaleIn, animationSpec = tween(600)) + fadeIn(animationSpec = tween(600))
                ) {
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
                            modifier = Modifier.size(logoSize * dimens.logoContentScale)
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
                        .fillMaxWidth(dimens.cardWidthRatio)
                        .wrapContentHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = dimens.spacingTiny), // تقليل الهامش الداخلي للبطاقة ليكون ربع القيمة
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(marginSmall)
                    ) {
                        Text(
                            text = "إنشاء حساب جديد",
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = fontTitle,
                            modifier = Modifier.padding(bottom = dimens.spacingTiny),
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
                                .height(66.dp),
                            shape = RoundedCornerShape(cardCorner),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                containerColor = colors.backgroundVariant,
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.outline,
                                cursorColor = colors.primary
                            ),
                        )
                        OutlinedTextField(
                            value = phone,
                            // عند كل تغيير، نقبل فقط الأرقام ونحذف أي رموز أو فراغات
                            onValueChange = { input ->
                                phone = input.filter { it.isDigit() } // يسمح فقط بالأرقام
                            },
                            label = { Text("رقم الهاتف", fontSize = fontField) },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(iconSize), tint = colors.primary) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), // يقبل أرقام فقط
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(66.dp),
                            shape = RoundedCornerShape(cardCorner),
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
                                .height(66.dp),
                            shape = RoundedCornerShape(cardCorner),
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
                                .height(66.dp),
                            shape = RoundedCornerShape(cardCorner),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                containerColor = colors.backgroundVariant,
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.outline,
                                cursorColor = colors.primary
                            ),
                        )
                    }
                }
                // بعد البطاقة البيضاء وقبل زر التسجيل
                if (localError != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = colors.errorContainer),
                        shape = RoundedCornerShape(cardCorner),
                        modifier = Modifier.fillMaxWidth(dimens.cardWidthRatio).padding(vertical = dimens.errorCardVerticalPadding)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(marginSmall)) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = colors.error, modifier = Modifier.size(iconSizeSmall))
                            Spacer(Modifier.width(dimens.iconSpacing))
                            Text(
                                text = localError ?: "",
                                color = colors.error,
                                fontSize = fontSmall,
                                style = typography.bodyMedium
                            )
                        }
                    }
                }
                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = colors.errorContainer),
                        shape = RoundedCornerShape(cardCorner),
                        modifier = Modifier.fillMaxWidth(dimens.cardWidthRatio).padding(vertical = dimens.errorCardVerticalPadding)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(marginSmall)) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = colors.error, modifier = Modifier.size(iconSizeSmall))
                            Spacer(Modifier.width(dimens.iconSpacing))
                            Text(
                                text = translateErrorMessage(errorMessage),
                                color = colors.error,
                                fontSize = fontSmall,
                                style = typography.bodyMedium
                            )
                        }
                    }
                }
                // زر تسجيل مع تأثير ضغط (scale) عند الضغط
                var registerPressed by remember { mutableStateOf(false) }
                Button(
                    onClick = {
                        registerPressed = true
                        localError = when {
                            displayName.isEmpty() -> "الرجاء إدخال الاسم المستخدم في الإشعارات"
                            phone.isEmpty() -> "الرجاء إدخال رقم الهاتف"
                            password.isEmpty() -> "الرجاء إدخال كلمة المرور"
                            confirmPassword.isEmpty() -> "الرجاء تأكيد كلمة المرور"
                            password.length < 6 -> "كلمة المرور يجب أن تكون 6 أحرف على الأقل"
                            password != confirmPassword -> "كلمة المرور غير متطابقة"
                            else -> null
                        }
                        if (localError == null) {
                            isLoading = true
                            onRegister(displayName, phone, password, confirmPassword)
                        }
                        registerPressed = false
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth(dimens.cardWidthRatio)
                        .padding(top = marginMedium)
                        .height(buttonHeight)
                        .graphicsLayer {
                            scaleX = if (registerPressed) dimens.cardWidthRatio else 1f
                            scaleY = if (registerPressed) dimens.cardWidthRatio else 1f
                        },
                    shape = RoundedCornerShape(cardCorner),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = colors.onPrimary, modifier = Modifier.size(dimens.iconSizeSmall))
                        Spacer(Modifier.width(dimens.buttonSpacing))
                        Text("جاري التسجيل...", color = colors.onPrimary, fontSize = fontButton, style = typography.bodyLarge)
                    } else {
                        Text("تسجيل", color = colors.onPrimary, fontSize = fontButton, style = typography.bodyLarge)
                    }
                }
                OutlinedButton(
                    onClick = onBackToLogin,
                    modifier = Modifier
                        .fillMaxWidth(dimens.cardWidthRatio)
                        .padding(top = marginSmall)
                        .height(buttonHeight),
                    shape = RoundedCornerShape(cardCorner),
                    border = ButtonDefaults.outlinedButtonBorder,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(iconSizeSmall), tint = colors.primary)
                    Spacer(Modifier.width(dimens.iconSpacing))
                    Text("العودة لتسجيل الدخول", fontWeight = FontWeight.Bold, fontSize = fontButton, style = typography.bodyLarge)
                }
                Spacer(modifier = Modifier.height(marginSmall))
            }
        }
    }
} 
package com.hillal.acc.ui.auth

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hillal.acc.R
import com.hillal.acc.ui.theme.AppTheme
import com.hillal.acc.ui.theme.LocalAppDimensions
import com.hillal.acc.ui.theme.backgroundVariant
import com.hillal.acc.ui.theme.gradient1
import com.hillal.acc.ui.theme.gradient2
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.graphics.graphicsLayer

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
        val screenWidth = configuration.screenWidthDp.dp
        val screenWidthValue = configuration.screenWidthDp.toFloat()
        val fontField = (screenWidthValue * 0.045f).sp // 4.5% من العرض
        val textFieldHeight = screenWidth * 0.08f // 8% من الارتفاع
        val iconSize = screenWidth * 0.065f // 6.5% من العرض
        val iconPadding = screenWidth * 0.02f // 2% من العرض
        val cardCorner = screenWidth * 0.04f // 4% من العرض
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

<<<<<<< HEAD
        val logoSize = LocalAppDimensions.current.logoSize
        val marginSmall = LocalAppDimensions.current.spacingSmall
        val marginMedium = LocalAppDimensions.current.spacingMedium
        val marginLarge = LocalAppDimensions.current.spacingLarge
        val cardElevation = LocalAppDimensions.current.cardElevation
=======
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
>>>>>>> parent of 3170ef2a7 (ؤرر)

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
                    enter = scaleIn(initialScale = 0.7f, animationSpec = tween(600)) + fadeIn(animationSpec = tween(600))
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
                            .padding(all = LocalAppDimensions.current.spacingSmall / 4), // تقليل الهامش الداخلي للبطاقة ليكون ربع القيمة
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(marginSmall)
                    ) {
                        Text(
                            text = "إنشاء حساب جديد",
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = typography.headlineMedium.fontSize,
                            modifier = Modifier.padding(bottom = 2.dp),
                            style = typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                        // Label يدوي لحقل الاسم
                        Text(
                            text = "الاسم",
                            fontSize = fontField,
                            color = colors.primary,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        BasicTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            modifier = Modifier
                                .fillMaxWidth()
<<<<<<< HEAD
                                .height(textFieldHeight)
                                .border(1.dp, colors.primary, RoundedCornerShape(cardCorner))
                                .background(colors.backgroundVariant, RoundedCornerShape(cardCorner)),
                            textStyle = typography.bodyLarge.copy(fontSize = fontField, lineHeight = fontField * 1.2, color = colors.onSurface),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .padding(0.dp)
                                        .padding(start = iconSize + iconPadding, end = iconPadding),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(iconSize).align(Alignment.CenterStart),
                                        tint = colors.primary
                                    )
                                    innerTextField()
                                }
                            }
=======
                                .height(fieldHeight),
                            shape = RoundedCornerShape(cardCorner),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                containerColor = colors.backgroundVariant,
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.outline,
                                cursorColor = colors.primary
                            ),
>>>>>>> parent of 3170ef2a7 (ؤرر)
                        )
                        // Label يدوي لحقل رقم الهاتف
                        Text(
                            text = "رقم الهاتف",
                            fontSize = fontField,
                            color = colors.primary,
                            modifier = Modifier.align(Alignment.Start).padding(top = marginSmall)
                        )
                        BasicTextField(
                            value = phone,
                            onValueChange = { phone = it },
<<<<<<< HEAD
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(textFieldHeight)
                                .border(1.dp, colors.primary, RoundedCornerShape(cardCorner))
                                .background(colors.backgroundVariant, RoundedCornerShape(cardCorner)),
                            textStyle = typography.bodyLarge.copy(fontSize = fontField, lineHeight = fontField * 1.2, color = colors.onSurface),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .padding(0.dp)
                                        .padding(start = iconSize + iconPadding, end = iconPadding),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Icon(
                                        Icons.Default.Phone,
                                        contentDescription = null,
                                        modifier = Modifier.size(iconSize).align(Alignment.CenterStart),
                                        tint = colors.primary
                                    )
                                    innerTextField()
                                }
                            }
=======
                            label = { Text("رقم الهاتف", fontSize = fontField) },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(iconSize), tint = colors.primary) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), // يقبل أرقام فقط
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(fieldHeight),
                            shape = RoundedCornerShape(cardCorner),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                containerColor = colors.backgroundVariant,
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.outline,
                                cursorColor = colors.primary
                            ),
>>>>>>> parent of 3170ef2a7 (ؤرر)
                        )
                        // Label يدوي لحقل كلمة السر
                        Text(
                            text = "كلمة السر",
                            fontSize = fontField,
                            color = colors.primary,
                            modifier = Modifier.align(Alignment.Start).padding(top = marginSmall)
                        )
                        BasicTextField(
                            value = password,
                            onValueChange = { password = it },
<<<<<<< HEAD
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(textFieldHeight)
                                .border(1.dp, colors.primary, RoundedCornerShape(cardCorner))
                                .background(colors.backgroundVariant, RoundedCornerShape(cardCorner)),
                            textStyle = typography.bodyLarge.copy(fontSize = fontField, lineHeight = fontField * 1.2, color = colors.onSurface),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .padding(0.dp)
                                        .padding(start = iconSize + iconPadding, end = iconSize + iconPadding),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(iconSize).align(Alignment.CenterStart),
                                        tint = colors.primary
                                    )
                                    innerTextField()
                                    IconButton(
                                        onClick = { passwordVisible = !passwordVisible },
                                        modifier = Modifier.align(Alignment.CenterEnd)
                                    ) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                            contentDescription = if (passwordVisible) "إخفاء كلمة السر" else "إظهار كلمة السر",
                                            modifier = Modifier.size(iconSize),
                                            tint = colors.primary
                                        )
                                    }
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
=======
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
                            shape = RoundedCornerShape(cardCorner),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                containerColor = colors.backgroundVariant,
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.outline,
                                cursorColor = colors.primary
                            ),
>>>>>>> parent of 3170ef2a7 (ؤرر)
                        )
                        // Label يدوي لحقل تأكيد كلمة السر
                        Text(
                            text = "تأكيد كلمة السر",
                            fontSize = fontField,
                            color = colors.primary,
                            modifier = Modifier.align(Alignment.Start).padding(top = marginSmall)
                        )
                        BasicTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
<<<<<<< HEAD
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(textFieldHeight)
                                .border(1.dp, colors.primary, RoundedCornerShape(cardCorner))
                                .background(colors.backgroundVariant, RoundedCornerShape(cardCorner)),
                            textStyle = typography.bodyLarge.copy(fontSize = fontField, lineHeight = fontField * 1.2, color = colors.onSurface),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .padding(0.dp)
                                        .padding(start = iconSize + iconPadding, end = iconSize + iconPadding),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(iconSize).align(Alignment.CenterStart),
                                        tint = colors.primary
                                    )
                                    innerTextField()
                                    IconButton(
                                        onClick = { confirmPasswordVisible = !confirmPasswordVisible },
                                        modifier = Modifier.align(Alignment.CenterEnd)
                                    ) {
                                        Icon(
                                            imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                            contentDescription = if (confirmPasswordVisible) "إخفاء كلمة السر" else "إظهار كلمة السر",
                                            modifier = Modifier.size(iconSize),
                                            tint = colors.primary
                                        )
                                    }
                                }
                            },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation()
=======
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
                            shape = RoundedCornerShape(cardCorner),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                containerColor = colors.backgroundVariant,
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.outline,
                                cursorColor = colors.primary
                            ),
>>>>>>> parent of 3170ef2a7 (ؤرر)
                        )
                    }
                }
                // بعد البطاقة البيضاء وقبل زر التسجيل
                if (localError != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = colors.errorContainer),
                        shape = RoundedCornerShape(cardCorner),
                        modifier = Modifier.fillMaxWidth(0.97f).padding(vertical = marginSmall)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(marginSmall)) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = colors.error, modifier = Modifier.size(iconSize.value.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = localError ?: "",
                                color = colors.error,
                                fontSize = (screenWidthValue * 0.03f).sp, // 3% من العرض
                                style = typography.bodyMedium
                            )
                        }
                    }
                }
                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = colors.errorContainer),
                        shape = RoundedCornerShape(cardCorner),
                        modifier = Modifier.fillMaxWidth(0.97f).padding(vertical = marginSmall)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(marginSmall)) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = colors.error, modifier = Modifier.size(iconSize.value.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = translateErrorMessage(errorMessage),
                                color = colors.error,
                                fontSize = (screenWidthValue * 0.03f).sp, // 3% من العرض
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
                        .fillMaxWidth(0.97f)
                        .padding(top = marginMedium)
                        .height(LocalAppDimensions.current.buttonHeight)
                        .graphicsLayer {
                            scaleX = if (registerPressed) 0.97f else 1f
                            scaleY = if (registerPressed) 0.97f else 1f
                        },
                    shape = RoundedCornerShape(cardCorner),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = colors.onPrimary, modifier = Modifier.size((fontField.value * 1.2f).dp))
                        Spacer(Modifier.width(8.dp))
                        Text("جاري التسجيل...", color = colors.onPrimary, fontSize = (fontField.value * 1.2f).sp, style = typography.bodyLarge)
                    } else {
                        Text("تسجيل", color = colors.onPrimary, fontSize = (fontField.value * 1.2f).sp, style = typography.bodyLarge)
                    }
                }
                OutlinedButton(
                    onClick = onBackToLogin,
                    modifier = Modifier
                        .fillMaxWidth(0.97f)
                        .padding(top = marginSmall)
                        .height(LocalAppDimensions.current.buttonHeight),
                    shape = RoundedCornerShape(cardCorner),
                    border = ButtonDefaults.outlinedButtonBorder,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(iconSize.value.dp), tint = colors.primary)
                    Spacer(Modifier.width(4.dp))
                    Text("العودة لتسجيل الدخول", fontWeight = FontWeight.Bold, fontSize = (fontField.value * 1.2f).sp, style = typography.bodyLarge)
                }
                Spacer(modifier = Modifier.height(marginSmall))
            }
        }
    }
} 
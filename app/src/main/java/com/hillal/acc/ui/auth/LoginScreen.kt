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
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.ui.ExperimentalComposeUiApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun LoginScreen(
    errorMessage: String?,
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
        
        // عند ظهور رسالة الخطأ، أعد isLoading إلى false
        LaunchedEffect(errorMessage) {
            if (errorMessage != null) {
                isLoading = false
            }
        }

        val screenWidth = configuration.screenWidthDp.dp
        val screenWidthValue = configuration.screenWidthDp.toFloat()
        val screenHeight = configuration.screenHeightDp.toFloat().dp
        val logoSize = dimens.logoSize
        val fontTitle = typography.headlineMedium.fontSize
        val fontField = (screenWidthValue * 0.045f).sp // 4.5% من العرض
        val textFieldHeight = screenHeight * 0.08f // 8% من الارتفاع
        val iconSize = screenWidth * 0.065f // 6.5% من العرض
        val iconPadding = screenWidth * 0.02f // 2% من العرض
        val cardCorner = screenWidth * 0.04f // 4% من العرض
        val fontButton = typography.bodyLarge.fontSize
        val fontSmall = typography.bodyMedium.fontSize
        val fontFieldPx = fontField.value
        val marginSmall = dimens.spacingSmall
        val marginMedium = dimens.spacingMedium
        val marginLarge = dimens.spacingLarge

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
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
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
                            .shadow(dimens.cardElevation, CircleShape),
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
                    elevation = CardDefaults.cardElevation(dimens.cardElevation),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    modifier = Modifier
                        .fillMaxWidth(0.97f)
                        .wrapContentHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = dimens.spacingSmall / 4), // تقليل الهامش الداخلي للبطاقة
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(marginSmall)
                    ) {
                        Text(
                            text = "تسجيل الدخول",
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = fontTitle,
                            modifier = Modifier.padding(bottom = 2.dp),
                            style = typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                        // Label يدوي لحقل رقم التلفون
                        Text(
                            text = "رقم التلفون",
                            fontSize = fontField,
                            color = colors.primary,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        BasicTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(textFieldHeight)
                                .border(1.dp, colors.primary, RoundedCornerShape(cardCorner))
                                .background(colors.background, RoundedCornerShape(cardCorner)),
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(textFieldHeight)
                                .border(1.dp, colors.primary, RoundedCornerShape(cardCorner))
                                .background(colors.background, RoundedCornerShape(cardCorner)),
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
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onForgotPasswordClick) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(dimens.iconSizeSmall), tint = colors.primary)
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    text = "نسيت كلمة السر؟",
                                    color = colors.primary,
                                    fontSize = fontSmall,
                                    style = typography.bodyMedium
                                )
                            }
                        }
                        // رسالة الخطأ داخل كارد صغيرة تحت الزر
                        if (errorMessage != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = colors.errorContainer),
                                shape = RoundedCornerShape(dimens.cardCorner),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(dimens.spacingSmall)) {
                                    Icon(Icons.Default.Error, contentDescription = null, tint = colors.error, modifier = Modifier.size(dimens.iconSizeSmall))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = errorMessage ?: "",
                                        color = colors.error,
                                        fontSize = dimens.fontSmall,
                                        style = typography.bodyMedium
                                    )
                                }
                            }
                        }
                        // زر الدخول مع مؤشر تحميل
                        Button(
                            onClick = {
                                isLoading = true
                                onLoginClick(phone, password)
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(screenWidth * 0.13f), // مثال: 13% من العرض
                            shape = RoundedCornerShape(dimens.buttonCorner),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = colors.onPrimary, modifier = Modifier.size(dimens.iconSizeSmall))
                                Spacer(Modifier.width(8.dp))
                                Text("جاري تسجيل الدخول...", color = colors.onPrimary)
                            } else {
                                Text("دخول", color = colors.onPrimary)
                            }
                        }
                        OutlinedButton(
                            onClick = onRegisterClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(screenWidth * 0.13f), // مثال: 13% من العرض
                            shape = RoundedCornerShape(dimens.buttonCorner),
                            border = ButtonDefaults.outlinedButtonBorder,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(dimens.iconSizeSmall), tint = colors.primary)
                            Spacer(Modifier.width(4.dp))
                            Text("إنشاء حساب جديد", fontWeight = FontWeight.Bold, fontSize = fontButton, style = typography.bodyLarge)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(marginSmall))
                // استبدال الروابط السريعة في الأسفل بـ Row واحد مع أزرار واضحة
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = marginSmall, start = marginSmall, end = marginSmall),
                    horizontalArrangement = Arrangement.spacedBy(marginSmall, Alignment.CenterHorizontally)
                ) {
                    OutlinedButton(
                        onClick = onPrivacyClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = colors.primary, modifier = Modifier.size(dimens.iconSizeSmall))
                        Spacer(Modifier.width(4.dp))
                        Text("سياسة الخصوصية", fontSize = dimens.fontSmall, color = colors.primary, fontWeight = FontWeight.Medium)
                    }
                    OutlinedButton(
                        onClick = onContactClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = colors.primary, modifier = Modifier.size(dimens.iconSizeSmall))
                        Spacer(Modifier.width(4.dp))
                        Text("تواصل معنا", fontSize = dimens.fontSmall, color = colors.primary, fontWeight = FontWeight.Medium)
                    }
                    OutlinedButton(
                        onClick = onAboutClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = colors.primary, modifier = Modifier.size(dimens.iconSizeSmall))
                        Spacer(Modifier.width(4.dp))
                        Text("حول التطبيق", fontSize = dimens.fontSmall, color = colors.primary, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(modifier = Modifier.height(marginSmall))
            }
        }
    }
} 
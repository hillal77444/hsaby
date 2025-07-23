package com.hillal.acc.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hillal.acc.ui.theme.LocalAppDimensions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.NavController
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import com.hillal.acc.R
import androidx.compose.foundation.background
import android.content.Intent
import com.hillal.acc.ui.ReportHeaderSettingsActivity
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.Color
import com.hillal.acc.ui.debts.DebtsWebViewActivity

// نموذج للصفحات المتوفرة
// يمكنك تعديل القائمة حسب الصفحات الفعلية

data class ServiceItem(val label: String, val iconRes: Int, val onClick: () -> Unit, val iconTint: Color? = null)

@Composable
fun AllServicesScreen(navController: NavController) {
    val dimens = LocalAppDimensions.current
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val columns = if (LocalConfiguration.current.screenWidthDp < 600) 3 else 4
    val context = LocalContext.current

    var showSubscriptionDialog by remember { mutableStateOf(false) }
    // نقل تعريف allServices إلى هنا ليكون navController متاحًا
    val allServices = listOf(
        ServiceItem("الحسابات", R.drawable.ic_accounts, onClick = { navController.navigate(R.id.navigation_accounts) }),
        ServiceItem("المعاملات", R.drawable.ic_transactions, onClick = { navController.navigate(R.id.transactionsFragment) }),
        ServiceItem("التقارير", R.drawable.ic_reports, onClick = { navController.navigate(R.id.navigation_reports) }),
        ServiceItem("متابعة الديون", R.drawable.ic_arrow_downward, onClick = {
            val intent = Intent(context, DebtsWebViewActivity::class.java)
            context.startActivity(intent)
        }),
        ServiceItem("صرف العملات", R.drawable.ic_currency_exchange, onClick = { navController.navigate(R.id.action_dashboard_to_exchange) }),
        ServiceItem("تحويل بين الحسابات", R.drawable.ic_sync_alt, onClick = { navController.navigate(R.id.transferFragment) }),
        ServiceItem("إعدادات واتساب", R.drawable.ic_whatsapp, onClick = { navController.navigate(R.id.whatsappSettingsFragment) }),
        ServiceItem(
            "إعدادات ترويسة التقرير والشعار",
            R.drawable.ic_reports, // أيقونة تقارير
            onClick = {
                context.startActivity(Intent(context, ReportHeaderSettingsActivity::class.java))
            }
        ),
        // بطاقة تجديد الاشتراك فقط
        ServiceItem(
            "تجديد الاشتراك",
            R.drawable.ic_wallet, // أيقونة محفظة أو اشتراك
            onClick = {
                showSubscriptionDialog = true
            },
            iconTint = colors.primary // لون أزرق
        )
    )

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.spacingMedium, vertical = dimens.spacingSmall),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Grid of service cards
            val chunked = allServices.chunked(columns)
            chunked.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimens.spacingSmall)
                ) {
                    rowItems.forEach { service ->
                        ServiceCard(service, modifier = Modifier.weight(1f))
                    }
                    // Fill empty cells if needed
                    if (rowItems.size < columns) {
                        repeat(columns - rowItems.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(dimens.spacingSmall))
            }
            Spacer(Modifier.height(dimens.spacingLarge))
        }
    }

    // بعد رسم الشبكة أو القائمة
    if (showSubscriptionDialog) {
        Dialog(onDismissRequest = { showSubscriptionDialog = false }) {
            Card(
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 200.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // يبدأ المحتوى من هنا بدون نص 'خدمات'
                    Icon(
                        painter = painterResource(id = R.drawable.ic_wallet),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "تجديد الاشتراك",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "استمتع بكامل مزايا التطبيق!",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Medium)
                    )
                    Spacer(Modifier.height(8.dp))
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "عند الاشتراك تحصل على جميع خدمات التطبيق:",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("ربط الإشعارات برقكم الخاص", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SyncAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("المزامنة مع الخادم واسترجاع بياناتك في أي وقت", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Devices, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("العمل على أكثر من جهاز في نفس الوقت", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("جميع خدمات التطبيق بدون قيود", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "سعر الاشتراك: 1000 ريال يمني (عملة قديمة) في الشهر",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = {
                                val url = "https://wa.me/967774447251?text=اشتراك تطبيق مالي برو"
                                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                context.startActivity(intent)
                                showSubscriptionDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("اشتراك", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { showSubscriptionDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("إلغاء", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceCard(service: ServiceItem, modifier: Modifier = Modifier) {
    val dimens = LocalAppDimensions.current
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { service.onClick() },
        shape = RoundedCornerShape(dimens.cardCorner),
        elevation = CardDefaults.cardElevation(dimens.cardElevation),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(dimens.spacingSmall),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = service.iconRes),
                contentDescription = service.label,
                modifier = Modifier.size(dimens.iconSize * 2),
                colorFilter = service.iconTint?.let { androidx.compose.ui.graphics.ColorFilter.tint(it) }
            )
            Spacer(Modifier.height(dimens.spacingSmall))
            Text(
                text = service.label,
                style = typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = colors.onSurface, fontSize = dimens.bodyFont / 1.5),
                maxLines = 2
            )
        }
    }
} 
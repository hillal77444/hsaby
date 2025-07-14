package com.hillal.acc.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

// نموذج للصفحات المتوفرة
// يمكنك تعديل القائمة حسب الصفحات الفعلية

data class ServiceItem(val label: String, val iconRes: Int, val onClick: () -> Unit)

@Composable
fun AllServicesScreen(navController: NavController) {
    val dimens = LocalAppDimensions.current
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val columns = if (LocalConfiguration.current.screenWidthDp < 600) 3 else 4

    // نقل تعريف allServices إلى هنا ليكون navController متاحًا
    val allServices = listOf(
        ServiceItem("الحسابات", R.drawable.ic_accounts, onClick = { navController.navigate("navigation_accounts") }),
        ServiceItem("المعاملات", R.drawable.ic_transactions, onClick = { navController.navigate("transactionsFragment") }),
        ServiceItem("التقارير", R.drawable.ic_reports, onClick = { navController.navigate("navigation_reports") }),
        ServiceItem("متابعة الديون", R.drawable.ic_arrow_downward, onClick = { navController.navigate("nav_summary") }),
        ServiceItem("صرف العملات", R.drawable.ic_currency_exchange, onClick = { navController.navigate("action_dashboard_to_exchange") }),
        ServiceItem("تحويل بين الحسابات", R.drawable.ic_sync_alt, onClick = { navController.navigate("transferFragment") }),
        ServiceItem("إعدادات واتساب", R.drawable.ic_whatsapp, onClick = { navController.navigate("whatsappSettingsFragment") })
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
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimens.spacingLarge, bottom = dimens.spacingSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع", tint = colors.primary, modifier = Modifier.size(dimens.iconSize))
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "الخدمات",
                    style = typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = colors.onBackground),
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { /* TODO: Refresh */ }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "تحديث", tint = colors.primary, modifier = Modifier.size(dimens.iconSize))
                }
                IconButton(onClick = { /* TODO: Search */ }) {
                    Icon(Icons.Filled.Search, contentDescription = "بحث", tint = colors.primary, modifier = Modifier.size(dimens.iconSize))
                }
            }
            Spacer(Modifier.height(dimens.spacingSmall))
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
                modifier = Modifier.size(dimens.iconSize * 2)
            )
            Spacer(Modifier.height(dimens.spacingSmall))
            Text(
                text = service.label,
                style = typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = colors.onSurface, fontSize = dimens.bodyFont),
                maxLines = 2
            )
        }
    }
} 
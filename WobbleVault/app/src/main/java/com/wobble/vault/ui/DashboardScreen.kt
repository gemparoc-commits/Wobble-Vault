package com.wobble.vault.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wobble.vault.data.api.WobbleApi
import com.wobble.vault.data.model.AuthUser
import com.wobble.vault.data.model.DashboardStats
import com.wobble.vault.ui.theme.BrandGreen
import com.wobble.vault.ui.theme.BrandInk
import com.wobble.vault.ui.theme.BrandLine
import com.wobble.vault.ui.theme.BrandMuted
import com.wobble.vault.ui.theme.BrandPaper
import com.wobble.vault.ui.theme.BrandRed
import com.wobble.vault.ui.theme.BrandRedDark
import com.wobble.vault.ui.theme.BrandRedSoft

private data class DashboardModule(val label: String, val icon: ImageVector, val description: String, val metric: String, val metricLabel: String, val route: String, val permission: String, val accent: Color)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(api: WobbleApi, user: AuthUser, onUnauthorized: () -> Unit, onOpenDrawer: () -> Unit, onNavigate: (String) -> Unit) {
    var stats by remember { mutableStateOf<DashboardStats?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var loadKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(loadKey) {
        loading = true
        val response = try { api.dashboardStats() } catch (_: Exception) { null }
        when {
            response?.isSuccessful == true && response.body() != null -> { stats = response.body(); loading = false }
            response?.code() == 401 -> onUnauthorized()
            else -> { error = "Could not reach the dashboard. Check your connection."; loading = false }
        }
    }
    val current = stats ?: DashboardStats()
    val modules = listOf(
        DashboardModule("Inventory", Icons.Filled.ShoppingCart, "Brands, sizes, quantities, and prices for every shoe in stock.", formatCount(current.totalInventoryItems), "Items", "inventory", "INVENTORY", BrandGreen),
        DashboardModule("Orders", Icons.Filled.ShoppingCart, "Customer orders, stock deductions, and order payments.", formatCount(current.activeOrders), "Active", "orders", "ORDERS", Color(0xFF237C79)),
        DashboardModule("Sales", Icons.Filled.Home, "Order payments, liquidations, and performance reporting.", formatCurrency(current.monthlyNetIncome), "Net this month", "sales", "SALES", Color(0xFF2D66B3)),
        DashboardModule("Sales Archive", Icons.Filled.Home, "Receipts, liquidations, and closed orders in one place.", formatCount(current.archivedOrders), "Archived", "sales-archive", "SALES_ARCHIVE", BrandMuted),
        DashboardModule("Accounts", Icons.Filled.AccountCircle, "User registration, roles, and per-page permissions.", "Open", "Team management", "accounts", "ACCOUNTS", Color(0xFF6B4F8A))
    )
    Scaffold(topBar = { TopAppBar(title = { Text("Dashboard", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onOpenDrawer) { Icon(Icons.Filled.Menu, "Open navigation") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandPaper, titleContentColor = BrandInk)) }) { padding ->
        when {
            error != null -> ErrorState(error!!) { loadKey++ }
            else -> LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item { DashboardHero(user, current, loading, onNavigate) }
                item { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Quick view", color = BrandInk, fontSize = 18.sp, fontWeight = FontWeight.Bold); modules.filter { user.hasPermission(it.permission) }.take(4).forEach { module -> SummaryCard(module, loading) { onNavigate(module.route) } } } }
                item { PerformancePanel(current, loading, onNavigate) }
                item { WatchlistPanel() }
                item { SectionCard("What matters in this project", "The core working areas that define Wobble Vault.") { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { modules.filter { user.hasPermission(it.permission) }.forEach { module -> ModuleCard(module, loading) { onNavigate(module.route) } } } } }
            }
        }
    }
}

@Composable
private fun DashboardHero(user: AuthUser, stats: DashboardStats, loading: Boolean, onNavigate: (String) -> Unit) {
    Surface(shape = RoundedCornerShape(26.dp), color = BrandPaper, border = BorderStroke(1.dp, BrandLine), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Surface(shape = RoundedCornerShape(999.dp), color = BrandRedSoft) { Text("OPERATIONS COMMAND CENTER", color = BrandRedDark, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)) }
            Spacer(Modifier.height(13.dp)); Text("Dashboard", color = BrandInk, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.2).sp); Spacer(Modifier.height(8.dp))
            Text("Welcome back, ${user.username.ifBlank { "there" }}. This view highlights the business areas that matter most: inventory stock, orders, sales, and account access.", color = BrandMuted, fontSize = 14.sp, lineHeight = 21.sp); Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { HeroMetric("Active orders", if (loading) "-" else formatCount(stats.activeOrders), "Orders currently being fulfilled.", Modifier.weight(1f)); HeroMetric("Low stock", if (loading) "-" else formatCount(stats.lowStockItems), "Pairs to review soon.", Modifier.weight(1f)); HeroMetric("Net this month", if (loading) "-" else formatCurrency(stats.monthlyNetIncome), "Sales minus liquidations.", Modifier.weight(1f)) }
            Spacer(Modifier.height(14.dp)); Button(onClick = { onNavigate("sales") }, colors = ButtonDefaults.buttonColors(containerColor = BrandRed), shape = RoundedCornerShape(11.dp), modifier = Modifier.fillMaxWidth()) { Text("Open sales report", fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun HeroMetric(label: String, value: String, hint: String, modifier: Modifier) { Column(modifier) { Text(label, color = BrandMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(4.dp)); Text(value, color = BrandInk, fontSize = 16.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(3.dp)); Text(hint, color = BrandMuted, fontSize = 9.sp, lineHeight = 12.sp) } }

@Composable
private fun SummaryCard(module: DashboardModule, loading: Boolean, onClick: () -> Unit) { Surface(onClick = onClick, shape = RoundedCornerShape(17.dp), color = BrandPaper, border = BorderStroke(1.dp, BrandLine), modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Surface(shape = CircleShape, color = module.accent.copy(alpha = .12f), modifier = Modifier.size(46.dp)) { BoxIcon(module.icon, module.accent) }; Spacer(Modifier.size(12.dp)); Column(Modifier.weight(1f)) { Text(module.label, color = BrandMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold); Text(if (loading) "-" else module.metric, color = BrandInk, fontSize = 21.sp, fontWeight = FontWeight.Bold); Text(module.metricLabel, color = BrandMuted, fontSize = 11.sp) }; Text("→", color = BrandMuted, fontSize = 20.sp) } } }

@Composable
private fun PerformancePanel(stats: DashboardStats, loading: Boolean, onNavigate: (String) -> Unit) { SectionCard("Business performance this month", "A compact report for orders, revenue, and stock direction.") { Surface(shape = RoundedCornerShape(14.dp), color = BrandRedSoft, modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(13.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) { PerformanceTotal("Sales income", stats.monthlySalesIncome, loading, Modifier.weight(1f)); PerformanceTotal("Liquidations", stats.monthlyLiquidation, loading, Modifier.weight(1f)); PerformanceTotal("Net result", stats.monthlyNetIncome, loading, Modifier.weight(1f)) } }; Spacer(Modifier.height(10.dp)); val items = listOf("Active orders" to "Open orders currently being fulfilled" to stats.activeOrders, "Archived orders" to "Completed orders moved to the archive" to stats.archivedOrders, "Cancelled orders" to "Cancelled orders with stock restored" to stats.cancelledOrders, "Low stock items" to "Pairs below the default stock threshold of 10" to stats.lowStockItems); items.forEach { (labelHint, value) -> val (label, hint) = labelHint; Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.SpaceBetween) { Column(Modifier.weight(1f)) { Text(label, color = BrandInk, fontWeight = FontWeight.SemiBold, fontSize = 13.sp); Text(hint, color = BrandMuted, fontSize = 11.sp) }; Text(if (loading) "-" else formatCount(value), color = BrandInk, fontSize = 18.sp, fontWeight = FontWeight.Bold) } }; Spacer(Modifier.height(5.dp)); Button(onClick = { onNavigate("sales") }, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = BrandInk), border = BorderStroke(1.dp, BrandLine), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) { Text("Open full sales workspace") } } }

@Composable
private fun PerformanceTotal(label: String, value: Double, loading: Boolean, modifier: Modifier) { Column(modifier) { Text(label, color = BrandMuted, fontSize = 10.sp); Text(if (loading) "-" else formatCurrency(value), color = BrandInk, fontSize = 14.sp, fontWeight = FontWeight.Bold) } }

@Composable
private fun WatchlistPanel() { SectionCard("Priority areas", "The pieces of the business the team should care about every day.") { listOf("Inventory control" to "Keep stock balanced and watch low inventory before it becomes a problem.", "Orders" to "Track active orders, capture payments, and archive or cancel when done.", "Sales and reporting" to "Monitor income, liquidations, and performance in one place.", "Accounts" to "Keep team access, roles, and page-level permissions organized.").forEach { (title, text) -> Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.Top) { Surface(shape = CircleShape, color = BrandRedSoft, modifier = Modifier.size(34.dp)) { BoxIcon(Icons.Filled.Home, BrandRed) }; Spacer(Modifier.size(10.dp)); Column { Text(title, color = BrandInk, fontWeight = FontWeight.SemiBold, fontSize = 13.sp); Text(text, color = BrandMuted, fontSize = 11.sp, lineHeight = 16.sp) } } } } }

@Composable
private fun ModuleCard(module: DashboardModule, loading: Boolean, onClick: () -> Unit) { Surface(onClick = onClick, shape = RoundedCornerShape(14.dp), color = module.accent.copy(alpha = .08f), border = BorderStroke(1.dp, module.accent.copy(alpha = .2f)), modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Surface(shape = CircleShape, color = module.accent.copy(alpha = .14f), modifier = Modifier.size(38.dp)) { BoxIcon(module.icon, module.accent) }; Spacer(Modifier.size(10.dp)); Column(Modifier.weight(1f)) { Text(module.label, color = BrandInk, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(module.description, color = BrandMuted, fontSize = 11.sp, lineHeight = 15.sp) }; Column(horizontalAlignment = Alignment.End) { Text(if (loading) "-" else module.metric, color = BrandInk, fontWeight = FontWeight.Bold, fontSize = 13.sp); Text(module.metricLabel, color = BrandMuted, fontSize = 10.sp) }; Spacer(Modifier.size(8.dp)); Text("→", color = BrandMuted, fontSize = 18.sp) } } }

@Composable
private fun BoxIcon(icon: ImageVector, tint: Color) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(21.dp)) } }

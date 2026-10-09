package com.wobble.vault.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wobble.vault.data.api.WobbleApi
import com.wobble.vault.data.model.AuthUser
import com.wobble.vault.data.model.CreateIncomeSourceRequest
import com.wobble.vault.data.model.IncomeSourceDto
import com.wobble.vault.ui.theme.BrandGreen
import com.wobble.vault.ui.theme.BrandInk
import com.wobble.vault.ui.theme.BrandLine
import com.wobble.vault.ui.theme.BrandMuted
import com.wobble.vault.ui.theme.BrandPaper
import com.wobble.vault.ui.theme.BrandRed
import com.wobble.vault.ui.theme.BrandRedDark
import com.wobble.vault.ui.theme.BrandRedSoft
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(api: WobbleApi, user: AuthUser, onBack: () -> Unit, onUnauthorized: () -> Unit, onOpenDrawer: () -> Unit) {
    var entries by remember { mutableStateOf<List<IncomeSourceDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var reload by remember { mutableIntStateOf(0) }
    var liquidationOpen by remember { mutableStateOf(false) }
    var detailTarget by remember { mutableStateOf<IncomeSourceDto?>(null) }
    var deleteTarget by remember { mutableStateOf<IncomeSourceDto?>(null) }
    var receiptsOpen by remember { mutableStateOf(false) }
    var liquidationsOpen by remember { mutableStateOf(false) }
    var reportStart by remember { mutableStateOf("") }
    var reportEnd by remember { mutableStateOf("") }
    var reportEntries by remember { mutableStateOf<List<IncomeSourceDto>?>(null) }
    var reportError by remember { mutableStateOf<String?>(null) }
    var reportLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val permitted = user.hasPermission("SALES")
    val canArchive = user.hasPermission("SALES_ARCHIVE")

    LaunchedEffect(reload, permitted) {
        if (!permitted) return@LaunchedEffect
        loading = true
        val response = try { api.income(0, 1000) } catch (_: Exception) { null }
        when {
            response?.isSuccessful == true -> { entries = response.body()?.content.orEmpty(); loading = false; error = null }
            response?.code() == 401 -> onUnauthorized()
            else -> { loading = false; error = "Could not load sales records. Check your connection." }
        }
    }

    val term = query.trim()
    val visible = entries.filter { entry -> term.isBlank() || listOf(entry.jobOrderNo, entry.customerName, entry.referenceNumber).any { it.orEmpty().contains(term, true) } }
    val receipts = entries.filterNot { it.isLiquidation() }
    val liquidations = entries.filter { it.isLiquidation() }
    val visibleReceipts = visible.filterNot { it.isLiquidation() }
    val visibleLiquidations = visible.filter { it.isLiquidation() }
    val salesTotal = receipts.sumOf { it.amount ?: 0.0 }
    val liquidationTotal = liquidations.sumOf { it.amount ?: 0.0 }
    val reportSource = reportEntries ?: entries

    Scaffold(topBar = {
        TopAppBar(title = { Text("Sales", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onOpenDrawer) { Icon(Icons.Filled.Menu, "Open navigation") } }, actions = { TextButton(onClick = { liquidationOpen = true }) { Text("Record liquidation", color = BrandRed) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandPaper, titleContentColor = BrandInk))
    }) { padding ->
        if (!permitted) Column(Modifier.fillMaxSize().padding(padding)) { AccessDenied() } else LazyColumn(Modifier.fillMaxSize().background(Color(0xFFF4F1EB)).padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { PageHeader("REVENUE", "Sales", "Track receipts from the store and FB Page, review liquidations, and report performance for any date range.") }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { StatPill("Sales total", "All receipts", formatPHP(salesTotal), Modifier.weight(1f)); StatPill("Liquidation total", "Withdrawals", formatPHP(liquidationTotal), Modifier.weight(1f)); StatPill("Net", "Sales minus liquidations", formatPHP(salesTotal - liquidationTotal), Modifier.weight(1f)) } }
            item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SectionCard("Source of income", modifier = Modifier.weight(1f)) { SalesDetailRow("Store", formatPHP(receipts.filter { it.shopType.equals("store", true) }.sumOf { it.amount ?: 0.0 })); SalesDetailRow("FB Page", formatPHP(receipts.filter { it.shopType.equals("online", true) }.sumOf { it.amount ?: 0.0 })) }
                SectionCard("Payment methods", modifier = Modifier.weight(1f)) { SalesDetailRow("Cash", formatPHP(receipts.filter { it.paymentMethod.equals("cash", true) }.sumOf { it.amount ?: 0.0 })); SalesDetailRow("Gcash", formatPHP(receipts.filter { it.paymentMethod.equals("gcash", true) }.sumOf { it.amount ?: 0.0 })) }
            } }
            item { OutlinedTextField(query, { query = it }, placeholder = { Text("Search by order number, customer, or reference") }, leadingIcon = { Icon(Icons.Filled.Search, null, tint = BrandMuted) }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            item { SectionHeaderWithAction("Receipts", "Payment entries from order checkouts.", "View all", visibleReceipts.size > 3) { receiptsOpen = true } }
            when {
                loading -> item { LoadingState() }
                error != null -> item { ErrorState(error!!) { reload++ } }
                visibleReceipts.isEmpty() -> item { EmptyState("No receipts match this search", "Payment entries appear here after order checkouts.") }
                else -> items(visibleReceipts.take(3), key = { it.id ?: "receipt-${it.referenceNumber}" }) { entry -> SaleCard(entry, { detailTarget = entry }, { deleteTarget = entry }, canArchive) }
            }
            item { SectionHeaderWithAction("Liquidations", "Cash withdrawals recorded with a liquidation reference.", "View all", visibleLiquidations.size > 3, Modifier.padding(top = 8.dp)) { liquidationsOpen = true } }
            if (!loading && error == null) {
                if (visibleLiquidations.isEmpty()) item { EmptyState("No liquidations match this search", "Record a liquidation to see it here.") }
                else items(visibleLiquidations.take(3), key = { it.id ?: "liquidation-${it.referenceNumber}" }) { entry -> SaleCard(entry, { detailTarget = entry }, { deleteTarget = entry }, canArchive) }
            }
            item { PerformanceReport(reportStart, { reportStart = it }, reportEnd, { reportEnd = it }, reportLoading, reportError, reportEntries != null, reportSource.filterNot { it.isLiquidation() }.sumOf { it.amount ?: 0.0 }, reportSource.filter { it.isLiquidation() }.sumOf { it.amount ?: 0.0 }) { start, end -> if (start.isBlank() || end.isBlank()) reportError = "Please select both a start and an end date." else if (start > end) reportError = "Start date must be on or before the end date." else scope.launch { reportLoading = true; reportError = null; val response = try { api.incomeDateRange(start, end) } catch (_: Exception) { null }; if (response?.isSuccessful == true) reportEntries = response.body().orEmpty() else if (response?.code() == 401) onUnauthorized() else reportError = "Could not generate the report."; reportLoading = false } } }
        }
    }
    if (liquidationOpen) LiquidationEditor(api, entries, { liquidationOpen = false }, { liquidationOpen = false; reload++ }, onUnauthorized)
    if (receiptsOpen) EntriesModal("All receipts", "Every payment entry matching the current search.", visibleReceipts, "No receipts match this search", { receiptsOpen = false }, { detailTarget = it }, { deleteTarget = it }, canArchive)
    if (liquidationsOpen) EntriesModal("All liquidations", "Every liquidation matching the current search.", visibleLiquidations, "No liquidations match this search", { liquidationsOpen = false }, { detailTarget = it }, { deleteTarget = it }, canArchive)
    detailTarget?.let { ReceiptDetails(it) { detailTarget = null } }
    deleteTarget?.let { entry -> ConfirmDialog("Delete record?", "Delete this ${if (entry.isLiquidation()) "liquidation" else "receipt"}?", "Delete", { deleteTarget = null; scope.launch { val response = entry.id?.let { api.deleteIncome(it) }; if (response?.code() == 401) onUnauthorized() else reload++ } }, { deleteTarget = null }) }
}

@Composable
private fun SectionHeaderWithAction(title: String, subtitle: String, actionLabel: String, showAction: Boolean, modifier: Modifier = Modifier, onAction: () -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, color = BrandInk, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = BrandMuted, fontSize = 12.sp) }
        if (showAction) TextButton(onClick = onAction) { Text(actionLabel, color = BrandRed, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
    }
}

@Composable
private fun EntriesModal(title: String, subtitle: String, entries: List<IncomeSourceDto>, emptyText: String, onDismiss: () -> Unit, onView: (IncomeSourceDto) -> Unit, onDelete: (IncomeSourceDto) -> Unit, canDelete: Boolean) {
    var modalQuery by remember { mutableStateOf("") }
    val term = modalQuery.trim()
    val filtered = entries.filter { entry -> term.isBlank() || listOf(entry.jobOrderNo, entry.customerName, entry.referenceNumber).any { it.orEmpty().contains(term, true) } }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title, color = BrandInk, fontWeight = FontWeight.Bold) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(subtitle, color = BrandMuted, fontSize = 12.sp)
            OutlinedTextField(modalQuery, { modalQuery = it }, placeholder = { Text("Search this list") }, leadingIcon = { Icon(Icons.Filled.Search, null, tint = BrandMuted) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            if (filtered.isEmpty()) EmptyState(emptyText, "Try a broader search term.") else filtered.forEach { entry -> SaleCard(entry, { onView(entry) }, { onDelete(entry) }, canDelete) }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}

@Composable
private fun SaleCard(entry: IncomeSourceDto, onOpen: () -> Unit, onDelete: () -> Unit, canDelete: Boolean) {
    Surface(shape = RoundedCornerShape(16.dp), color = BrandPaper, border = BorderStroke(1.dp, BrandLine), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Row(Modifier.weight(1f)) { Icon(Icons.Filled.Home, null, tint = if (entry.isLiquidation()) BrandRed else BrandGreen, modifier = Modifier.size(21.dp)); Spacer(Modifier.size(9.dp)); Column { Text(if (entry.isLiquidation()) "Liquidation" else "Payment receipt", color = BrandInk, fontWeight = FontWeight.Bold); Text(formatSlashDate(entry.incomeDate), color = BrandMuted, fontSize = 12.sp) } }; Text(formatPHP(entry.amount), color = if (entry.isLiquidation()) BrandRed else BrandGreen, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            if (entry.isLiquidation()) { SalesDetailRow("Remarks", entry.remarks ?: "Cash withdrawal"); SalesDetailRow("Payment method", formatPayment(entry.paymentMethod)); SalesDetailRow("Reference", entry.referenceNumber) }
            else { SalesDetailRow("Customer", entry.customerName ?: "Walk-in customer"); SalesDetailRow("Order no.", entry.jobOrderNo); SalesDetailRow("Shop", formatShop(entry.shopType)); SalesDetailRow("Payment method", formatPayment(entry.paymentMethod)); SalesDetailRow("Reference", entry.referenceNumber) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onOpen) { Text("View receipt", color = BrandRed) }; if (canDelete) IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Delete", tint = BrandRed) } }
        }
    }
}

@Composable
private fun PerformanceReport(start: String, onStartChange: (String) -> Unit, end: String, onEndChange: (String) -> Unit, loading: Boolean, error: String?, hasReport: Boolean, sales: Double, liquidations: Double, onGenerate: (String, String) -> Unit) { SectionCard("Performance report", "Compare sales against liquidations for a selected date range.") { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(start, onStartChange, label = { Text("Start date") }, singleLine = true, modifier = Modifier.weight(1f)); OutlinedTextField(end, onEndChange, label = { Text("End date") }, singleLine = true, modifier = Modifier.weight(1f)) }; Spacer(Modifier.height(8.dp)); Button(enabled = !loading, onClick = { onGenerate(start, end) }, colors = ButtonDefaults.buttonColors(containerColor = BrandRed), modifier = Modifier.fillMaxWidth()) { Text(if (loading) "Generating..." else "Generate report") }; Text(if (hasReport) "Showing selected date range." else "Showing all recorded entries.", color = BrandMuted, fontSize = 11.sp); error?.let { Text(it, color = BrandRed, fontSize = 12.sp) }; Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { StatPill("Sales total", "Report", formatPHP(sales), Modifier.weight(1f)); StatPill("Liquidations", "Report", formatPHP(liquidations), Modifier.weight(1f)); StatPill("Net", "Report", formatPHP(sales - liquidations), Modifier.weight(1f)) } } }

@Composable
private fun LiquidationEditor(api: WobbleApi, entries: List<IncomeSourceDto>, onDismiss: () -> Unit, onSaved: () -> Unit, onUnauthorized: () -> Unit) { var date by remember { mutableStateOf(todayISO()) }; var amount by remember { mutableStateOf("") }; var reason by remember { mutableStateOf("") }; var saving by remember { mutableStateOf(false) }; val scope = rememberCoroutineScope(); val reference = "LIQ-${date.replace("-", "")}-${(entries.size + 1).toString().padStart(3, '0')}"; AlertDialog(onDismissRequest = { if (!saving) onDismiss() }, title = { Text("Record Liquidation", fontWeight = FontWeight.Bold) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(date, { date = it }, label = { Text("Date") }, singleLine = true); OutlinedTextField(amount, { amount = it }, label = { Text("Amount") }, singleLine = true); OutlinedTextField(reason, { reason = it }, label = { Text("Reason") }, minLines = 2); OutlinedTextField(reference, {}, label = { Text("Reference number") }, readOnly = true, singleLine = true) } }, confirmButton = { Button(enabled = !saving && (amount.toDoubleOrNull() ?: 0.0) > 0 && reason.isNotBlank(), onClick = { saving = true; scope.launch { val response = api.createIncome(CreateIncomeSourceRequest("store", "cash", date, amount = amount.toDoubleOrNull() ?: 0.0, referenceNumber = reference, paymentCategory = "LIQUIDATION", remarks = reason.trim())); saving = false; if (response.isSuccessful) onSaved() else if (response.code() == 401) onUnauthorized() } }) { Text(if (saving) "Saving..." else "Save liquidation") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }) }

@Composable
private fun ReceiptDetails(entry: IncomeSourceDto, onDismiss: () -> Unit) { AlertDialog(onDismissRequest = onDismiss, title = { Text(if (entry.isLiquidation()) "Liquidation record" else "Payment receipt", fontWeight = FontWeight.Bold) }, text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) { Surface(shape = RoundedCornerShape(14.dp), color = BrandRedSoft, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) { Text(if (entry.isLiquidation()) "AMOUNT LIQUIDATED" else "AMOUNT RECEIVED", color = BrandRedDark, fontSize = 10.sp, fontWeight = FontWeight.Bold); Text(formatPHP(entry.amount), color = BrandInk, fontSize = 24.sp, fontWeight = FontWeight.Bold) } }; SalesDetailRow("Reference no.", entry.referenceNumber); SalesDetailRow("Date issued", formatSlashDate(entry.incomeDate)); SalesDetailRow("Order no.", entry.jobOrderNo); SalesDetailRow("Payment method", formatPayment(entry.paymentMethod)); SalesDetailRow("Customer", if (entry.isLiquidation()) "N/A" else entry.customerName ?: "Walk-in Customer"); SalesDetailRow("Shop", formatShop(entry.shopType)); SalesDetailRow("Category", if (entry.isLiquidation()) "Liquidation" else "Payment"); entry.remarks?.let { SalesDetailRow("Remarks", it) } } }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }) }

@Composable private fun SalesDetailRow(label: String, value: String?) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = BrandMuted, fontSize = 12.sp); Text(value ?: "-", color = BrandInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) } }
private fun formatShop(value: String?): String = when (value?.lowercase()) { "store" -> "Wobble Store"; "online" -> "FB Page"; else -> value ?: "-" }
private fun formatPayment(value: String?): String = when (value?.lowercase()) { "cash" -> "Cash"; "gcash" -> "Gcash"; else -> value ?: "-" }

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wobble.vault.data.api.WobbleApi
import com.wobble.vault.data.model.AuthUser
import com.wobble.vault.data.model.IncomeSourceDto
import com.wobble.vault.ui.theme.BrandGreen
import com.wobble.vault.ui.theme.BrandInk
import com.wobble.vault.ui.theme.BrandLine
import com.wobble.vault.ui.theme.BrandMuted
import com.wobble.vault.ui.theme.BrandPaper
import com.wobble.vault.ui.theme.BrandRed
import com.wobble.vault.ui.theme.BrandRedSoft
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesArchiveScreen(
    api: WobbleApi,
    user: AuthUser,
    onBack: () -> Unit,
    onUnauthorized: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    var entries by remember { mutableStateOf<List<IncomeSourceDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var reload by remember { mutableIntStateOf(0) }
    var deleteTarget by remember { mutableStateOf<IncomeSourceDto?>(null) }
    val scope = rememberCoroutineScope()
    val permitted = user.hasPermission("SALES_ARCHIVE")

    LaunchedEffect(reload, permitted) {
        if (!permitted) return@LaunchedEffect
        loading = true
        val response = try {
            api.income(0, 1000)
        } catch (_: Exception) {
            null
        }
        when {
            response?.isSuccessful == true -> {
                entries = response.body()?.content.orEmpty()
                loading = false
                error = null
            }
            response?.code() == 401 -> onUnauthorized()
            else -> {
                loading = false
                error = "Could not load archived sales records. Check your connection."
            }
        }
    }

    val term = query.trim()
    val filtered = entries.filter { entry ->
        term.isBlank() || listOf(entry.jobOrderNo, entry.referenceNumber, entry.customerName)
            .any { it.orEmpty().contains(term, ignoreCase = true) }
    }
    val receipts = filtered.filterNot { it.isLiquidation() }
    val liquidations = filtered.filter { it.isLiquidation() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sales Archive", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Open navigation")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandPaper,
                    titleContentColor = BrandInk
                )
            )
        }
    ) { padding ->
        if (!permitted) {
            Column(Modifier.fillMaxSize().padding(padding)) { AccessDenied() }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF4F1EB))
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    PageHeader(
                        "CLOSED RECORDS",
                        "Sales Archive",
                        "Review every receipt and liquidation, and permanently remove records that should no longer count."
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatPill("Total records", "All income sources", formatCount(entries.size.toLong()), Modifier.weight(1f))
                        StatPill("Receipts", "Sales & payments", formatCount(entries.count { !it.isLiquidation() }.toLong()), Modifier.weight(1f))
                        StatPill("Liquidations", "Withdrawals", formatCount(entries.count { it.isLiquidation() }.toLong()), Modifier.weight(1f))
                    }
                }
                item {
                    SectionCard(
                        "Record archive",
                        "Deleting a record removes it permanently from the Sales page totals."
                    ) {
                        SearchField(
                            query,
                            { query = it },
                            "Search by order number, customer, or reference"
                        )
                    }
                }
                if (loading) {
                    item { LoadingState() }
                } else if (error != null) {
                    item { ErrorState(error!!) { reload++ } }
                } else {
                    item {
                        Text("Receipts", color = BrandInk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Sales and payment entries from all shops.", color = BrandMuted, fontSize = 12.sp)
                    }
                    if (receipts.isEmpty()) {
                        item { EmptyState("No receipts match this search", "Sales and payment entries appear here after checkouts.") }
                    } else {
                        items(receipts, key = { it.id ?: "receipt-${it.referenceNumber}" }) { entry ->
                            ArchiveReceiptCard(entry, onDelete = { deleteTarget = entry })
                        }
                    }
                    item {
                        Column {
                            Text("Liquidations", color = BrandInk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Withdrawal entries (LIQ refs).", color = BrandMuted, fontSize = 12.sp)
                        }
                    }
                    if (liquidations.isEmpty()) {
                        item { EmptyState("No liquidations match this search", "Withdrawal entries appear here when recorded.") }
                    } else {
                        items(liquidations, key = { it.id ?: "liquidation-${it.referenceNumber}" }) { entry ->
                            ArchiveLiquidationCard(entry, onDelete = { deleteTarget = entry })
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { entry ->
        ConfirmDialog(
            "Delete record?",
            "This permanently removes the ${if (entry.isLiquidation()) "liquidation" else "receipt"} from Sales totals.",
            "Delete",
            onConfirm = {
                deleteTarget = null
                scope.launch {
                    val response = entry.id?.let { api.deleteIncome(it) }
                    if (response?.code() == 401) onUnauthorized() else reload++
                }
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun ArchiveReceiptCard(entry: IncomeSourceDto, onDelete: () -> Unit) {
    ArchiveCard(entry, onDelete) {
        ArchiveRow("Date", formatSlashDate(entry.incomeDate))
        ArchiveRow("Shop", archiveShop(entry.shopType))
        ArchiveRow("Payment", archivePayment(entry.paymentMethod))
        ArchiveRow("Order no.", entry.jobOrderNo)
        ArchiveRow("Customer", entry.customerName ?: "Walk-in customer")
        ArchiveRow("Amount", formatPHP(entry.amount), BrandGreen)
        ArchiveRow("Reference", entry.referenceNumber)
    }
}

@Composable
private fun ArchiveLiquidationCard(entry: IncomeSourceDto, onDelete: () -> Unit) {
    ArchiveCard(entry, onDelete) {
        ArchiveRow("Date", formatSlashDate(entry.incomeDate))
        ArchiveRow("Liquidation no.", entry.referenceNumber)
        ArchiveRow("Amount", formatPHP(entry.amount), BrandRed)
        ArchiveRow("Remarks", entry.remarks ?: "Cash withdrawal")
    }
}

@Composable
private fun ArchiveCard(entry: IncomeSourceDto, onDelete: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = BrandPaper,
        border = BorderStroke(1.dp, BrandLine),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.weight(1f)) {
                    Icon(
                        Icons.Filled.Home,
                        contentDescription = null,
                        tint = if (entry.isLiquidation()) BrandRed else BrandGreen,
                        modifier = Modifier.size(21.dp)
                    )
                    Spacer(Modifier.size(9.dp))
                    Column {
                        Text(
                            if (entry.isLiquidation()) "Liquidation" else "Payment receipt",
                            color = BrandInk,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (entry.isLiquidation()) "Withdrawal entry" else "Sales and payment entry",
                            color = BrandMuted,
                            fontSize = 12.sp
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete record", tint = BrandRed)
                }
            }
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = BrandRedSoft,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(10.dp)) { content() }
            }
        }
    }
}

@Composable
private fun ArchiveRow(label: String, value: String?, valueColor: Color = BrandInk) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = BrandMuted, fontSize = 12.sp)
        Text(value ?: "-", color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun archiveShop(value: String?): String = when (value?.lowercase()) {
    "store" -> "Wobble Store"
    "online" -> "FB Page"
    else -> value ?: "-"
}

private fun archivePayment(value: String?): String = when (value?.lowercase()) {
    "cash" -> "Cash"
    "gcash" -> "Gcash"
    else -> value ?: "-"
}

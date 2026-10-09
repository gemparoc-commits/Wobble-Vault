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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wobble.vault.data.api.WobbleApi
import com.wobble.vault.data.model.AuthUser
import com.wobble.vault.data.model.CreateOrderRequest
import com.wobble.vault.data.model.InventoryItem
import com.wobble.vault.data.model.OrderDto
import com.wobble.vault.data.model.OrderItemRequest
import com.wobble.vault.data.model.PaymentUpdateRequest
import com.wobble.vault.ui.theme.BrandGreen
import com.wobble.vault.ui.theme.BrandInk
import com.wobble.vault.ui.theme.BrandLine
import com.wobble.vault.ui.theme.BrandMuted
import com.wobble.vault.ui.theme.BrandPaper
import com.wobble.vault.ui.theme.BrandRed
import com.wobble.vault.ui.theme.BrandRedDark
import com.wobble.vault.ui.theme.BrandRedSoft
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class OrderLine(var inventoryId: String = "", var productName: String = "", var size: String = "", var unitPrice: String = "", var quantity: String = "1")

private fun OrderDto.outstanding(): Double = (balance ?: ((price ?: 0.0) - (payment ?: 0.0))).coerceAtLeast(0.0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(api: WobbleApi, user: AuthUser, initialStatus: String = "ALL", onBack: () -> Unit, onUnauthorized: () -> Unit, onOpenDrawer: () -> Unit) {
    var status by remember { mutableStateOf(initialStatus) }
    var orders by remember { mutableStateOf<List<OrderDto>>(emptyList()) }
    var inventory by remember { mutableStateOf<List<InventoryItem>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var editorOrder by remember { mutableStateOf<OrderDto?>(null) }
    var detailsOrder by remember { mutableStateOf<OrderDto?>(null) }
    var deleteOrder by remember { mutableStateOf<OrderDto?>(null) }
    var paymentOrder by remember { mutableStateOf<OrderDto?>(null) }
    val scope = rememberCoroutineScope()
    val permitted = user.hasPermission("ORDERS") || (initialStatus == "ARCHIVED" && user.hasPermission("SALES_ARCHIVE"))

    LaunchedEffect(status, reload, permitted) {
        if (!permitted) return@LaunchedEffect
        loading = true
        val serverStatus = if (initialStatus == "ARCHIVED") "ARCHIVED" else status.takeUnless { it == "ALL" || it == "WITH_BALANCE" }
        val response = try { api.orders(serverStatus, 0, 100) } catch (_: Exception) { null }
        when { response?.isSuccessful == true -> { orders = response.body()?.content.orEmpty(); error = null; loading = false }; response?.code() == 401 -> onUnauthorized(); else -> { error = "Could not load orders. Check your connection."; loading = false } }
    }
    LaunchedEffect(permitted) { if (permitted) { val response = try { api.inventory(0, 1000) } catch (_: Exception) { null }; if (response?.isSuccessful == true) inventory = response.body()?.content.orEmpty() } }

    val filtered = orders.filter { val term = query.trim().lowercase(); term.isBlank() || it.jobOrderNo.orEmpty().lowercase().contains(term) || it.customerName.orEmpty().lowercase().contains(term) }.filter { initialStatus == "ARCHIVED" || status != "WITH_BALANCE" || (it.status == "ACTIVE" && it.outstanding() > 0.0) }
    val pageSize = 10
    var page by remember { mutableIntStateOf(1) }
    val totalPages = maxOf(1, (filtered.size + pageSize - 1) / pageSize)
    val safePage = page.coerceIn(1, totalPages)
    val visible = filtered.drop((safePage - 1) * pageSize).take(pageSize)
    LaunchedEffect(status, query) { page = 1 }

    Scaffold(topBar = { TopAppBar(title = { Text(if (initialStatus == "ARCHIVED") "Sales Archive" else "Orders", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onOpenDrawer) { Icon(Icons.Filled.Menu, "Open navigation") } }, actions = { if (initialStatus != "ARCHIVED") TextButton(onClick = { editorOrder = OrderDto(status = "ACTIVE") }) { Text("New order", color = BrandRed, fontWeight = FontWeight.SemiBold) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandPaper, titleContentColor = BrandInk)) }) { padding ->
        if (!permitted) Column(Modifier.fillMaxSize().padding(padding)) { AccessDenied() } else Column(Modifier.fillMaxSize().background(Color(0xFFF4F1EB)).padding(padding)) {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { PageHeader(if (initialStatus == "ARCHIVED") "SALES HISTORY" else "FULFILLMENT", if (initialStatus == "ARCHIVED") "Sales archive" else "Orders", "Create orders from stock, take payments as they come in, and archive or cancel when finished.") }
                if (initialStatus != "ARCHIVED") item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("ALL" to "All", "WITH_BALANCE" to "With balance", "ARCHIVED" to "Archived", "CANCELLED" to "Cancelled").forEach { (key, label) -> FilterChip(selected = status == key, onClick = { status = key }, label = { Text(label) }) } } }
                item { OutlinedTextField(query, { query = it }, placeholder = { Text("Search by order no or customer") }, leadingIcon = { Icon(Icons.Filled.Search, null, tint = BrandMuted) }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { Text("Order overview", color = BrandInk, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                if (loading) {
                    item { BoxedLoading() }
                } else if (error != null) {
                    item { ErrorState(error!!) { reload++ } }
                } else if (visible.isEmpty()) {
                    item { EmptyState("No orders in this view", "Create a new order or switch tabs to see other statuses.") }
                } else {
                    items(visible, key = { it.id ?: it.jobOrderNo.orEmpty() }) { order ->
                        OrderCard(order, onView = { detailsOrder = order }, onEdit = { editorOrder = order }, onDelete = { deleteOrder = order }, onPay = { paymentOrder = order }, canEdit = order.status != "CANCELLED", canDelete = user.role == "ADMIN" && order.status != "CANCELLED", canPay = order.status == "ACTIVE" && order.outstanding() > 0.0)
                    }
                    item { Pagination(safePage, totalPages, { page = (safePage - 1).coerceAtLeast(1) }, { page = (safePage + 1).coerceAtMost(totalPages) }) }
                }
            }
        }
    }
    editorOrder?.let { order -> OrderEditor(api, order, inventory, onDismiss = { editorOrder = null }, onSaved = { editorOrder = null; reload++ }, onUnauthorized = onUnauthorized) }
    detailsOrder?.let { order -> OrderDetails(api, order, onDismiss = { detailsOrder = null }, onChanged = { detailsOrder = null; reload++ }, onUnauthorized = onUnauthorized) }
    deleteOrder?.let { order -> ConfirmDialog("Delete order?", "Delete ${order.jobOrderNo ?: "this order"}?", "Delete", onConfirm = { deleteOrder = null; scope.launch { val response = order.id?.let { api.deleteOrder(it) }; if (response?.code() == 401) onUnauthorized() else reload++ } }, onDismiss = { deleteOrder = null }) }
    paymentOrder?.let { order -> PaymentUpdateDialog(api, order, onDismiss = { paymentOrder = null }, onSaved = { paymentOrder = null; reload++ }, onUnauthorized = onUnauthorized) }
}

@Composable private fun BoxedLoading() { Box(Modifier.fillMaxWidth().height(220.dp)) { LoadingState() } }

@Composable
private fun OrderCard(order: OrderDto, onView: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onPay: () -> Unit, canEdit: Boolean, canDelete: Boolean, canPay: Boolean) {
    Surface(shape = RoundedCornerShape(16.dp), color = BrandPaper, border = BorderStroke(1.dp, BrandLine), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(order.jobOrderNo ?: "Order", color = BrandInk, fontWeight = FontWeight.Bold)
                    Text(order.customerName ?: "Walk-in", color = BrandMuted, fontSize = 13.sp)
                }
                Surface(shape = RoundedCornerShape(999.dp), color = if (order.status == "ACTIVE") Color(0xFFE4EEE7) else BrandRedSoft) {
                    Text(order.status.orEmpty(), color = if (order.status == "ACTIVE") BrandGreen else BrandRedDark, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
                }
            }
            Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatSlashDate(order.orderDate), color = BrandMuted, fontSize = 12.sp)
                Text(formatPHP(order.price), color = BrandRedDark, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onView) { Icon(Icons.Filled.Home, null, modifier = Modifier.size(16.dp)); Text(" View", fontSize = 12.sp) }
                if (canPay) TextButton(onClick = onPay) { Text("Payment update", fontSize = 12.sp, color = BrandRed, fontWeight = FontWeight.SemiBold) }
                if (canEdit) IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "Edit", tint = BrandRed) }
                if (canDelete) IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Delete", tint = BrandRed) }
            }
        }
    }
}

@Composable
private fun OrderEditor(api: WobbleApi, order: OrderDto, inventory: List<InventoryItem>, onDismiss: () -> Unit, onSaved: () -> Unit, onUnauthorized: () -> Unit) {
    var customer by remember { mutableStateOf(order.customerName.orEmpty()) }; var date by remember { mutableStateOf(order.orderDate?.take(10) ?: todayISO()) }; var discount by remember { mutableStateOf(order.discount?.toString() ?: "0") }; var payment by remember { mutableStateOf(order.payment?.toString() ?: "0") }; var paymentMethod by remember { mutableStateOf(order.paymentMethod ?: "cash") }; var shop by remember { mutableStateOf(order.shop ?: "store") }; var notes by remember { mutableStateOf(order.notes.orEmpty()) }; var productSearch by remember { mutableStateOf("") }; var lines by remember { mutableStateOf(order.items.map { OrderLine(it.inventoryId.orEmpty(), it.productName.orEmpty(), it.size.orEmpty(), it.unitPrice?.toString().orEmpty(), it.quantity?.toString() ?: "1") }.ifEmpty { listOf(OrderLine()) }) }; var validation by remember { mutableStateOf<String?>(null) }; var saving by remember { mutableStateOf(false) }
    val subtotal = lines.sumOf { (it.unitPrice.toDoubleOrNull() ?: 0.0) * (it.quantity.toDoubleOrNull() ?: 0.0) }; val discountValue = discount.toDoubleOrNull() ?: 0.0; val total = maxOf(subtotal - discountValue, 0.0); val paymentValue = payment.toDoubleOrNull() ?: 0.0; val balance = maxOf(total - paymentValue, 0.0); val matches = inventory.filter { val term = productSearch.trim(); term.isNotBlank() && (it.name.orEmpty().contains(term, true) || it.brand.orEmpty().contains(term, true)) }.take(5)
    AlertDialog(onDismissRequest = { if (!saving) onDismiss() }, title = { Text(if (order.id == null) "New Order" else "Edit Order ${order.jobOrderNo}", fontWeight = FontWeight.Bold) }, text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(customer, { customer = it }, label = { Text("Customer name") }, singleLine = true); OutlinedTextField(date, { date = it }, label = { Text("Order date") }, singleLine = true); Text("Products", color = BrandInk, fontWeight = FontWeight.Bold)
        OutlinedTextField(productSearch, { productSearch = it }, label = { Text("Search inventory to add") }, leadingIcon = { Icon(Icons.Filled.Search, null) }, singleLine = true)
        matches.forEach { item -> TextButton(onClick = { val index = lines.indexOfFirst { it.inventoryId.isBlank() }; val next = OrderLine(item.id.orEmpty(), item.name.orEmpty(), item.size.orEmpty(), item.price?.toString() ?: "", "1"); lines = if (index >= 0) lines.toMutableList().also { it[index] = next } else lines + next; productSearch = "" }) { Text("${item.brand} - ${item.name} ${item.size ?: ""} · stock ${item.quantity ?: 0}", color = BrandRed) } }
        lines.forEachIndexed { index, line -> SectionCard("Product ${index + 1}") { Text(if (line.productName.isBlank()) "No inventory item selected" else "${line.productName} ${line.size}", color = BrandInk, fontWeight = FontWeight.SemiBold); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(line.unitPrice, { value -> lines = lines.toMutableList().also { it[index] = line.copy(unitPrice = value) } }, label = { Text("Unit price") }, modifier = Modifier.weight(1f), singleLine = true); OutlinedTextField(line.quantity, { value -> lines = lines.toMutableList().also { it[index] = line.copy(quantity = value.filter(Char::isDigit)) } }, label = { Text("Qty") }, modifier = Modifier.weight(1f), singleLine = true) }; Text("Subtotal ${formatPHP((line.unitPrice.toDoubleOrNull() ?: 0.0) * (line.quantity.toDoubleOrNull() ?: 0.0))}", color = BrandMuted, fontSize = 12.sp); if (lines.size > 1) TextButton(onClick = { lines = lines.toMutableList().also { it.removeAt(index) } }) { Text("Remove product", color = BrandRed) } } }
        TextButton(onClick = { lines = lines + OrderLine() }) { Text("+ Add another product", color = BrandRed) }; Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(discount, { discount = it }, label = { Text("Discount") }, modifier = Modifier.weight(1f), singleLine = true); OutlinedTextField(payment, { payment = it }, label = { Text("Payment") }, modifier = Modifier.weight(1f), singleLine = true) }; OutlinedTextField(paymentMethod, { paymentMethod = it }, label = { Text("Payment method: cash or gcash") }, singleLine = true); OutlinedTextField(shop, { shop = it }, label = { Text("Shop: store or online") }, singleLine = true); OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }); SectionCard("Order totals") { DetailRow("Subtotal", formatPHP(subtotal)); DetailRow("Discount", "-${formatPHP(discountValue)}"); DetailRow("Total", formatPHP(total)); DetailRow("Payment", formatPHP(paymentValue)); DetailRow("Balance", formatPHP(balance)) }; validation?.let { Text(it, color = BrandRed, fontSize = 12.sp) }
    } }, confirmButton = { Button(enabled = !saving, onClick = { val requestedByItem = lines.filter { it.inventoryId.isNotBlank() }.groupBy({ it.inventoryId }, { it.quantity.toIntOrNull() ?: 0 }).mapValues { it.value.sum() }; val reservedByItem = order.items.filter { !it.inventoryId.isNullOrBlank() }.groupBy({ it.inventoryId!! }, { it.quantity ?: 0 }).mapValues { it.value.sum() }; val stockError = requestedByItem.entries.firstNotNullOfOrNull { (id, qty) -> val stock = inventory.firstOrNull { it.id == id }?.quantity ?: 0; val allowed = stock + (reservedByItem[id] ?: 0); if (qty > allowed) { val name = lines.firstOrNull { it.inventoryId == id }?.productName?.ifBlank { "this item" } ?: "this item"; "Only $allowed available for $name" } else null }; validation = when { customer.isBlank() -> "Customer name is required"; lines.any { it.inventoryId.isBlank() || (it.quantity.toIntOrNull() ?: 0) < 1 || (it.unitPrice.toDoubleOrNull() ?: 0.0) <= 0 } -> "Each product needs an inventory item, quantity, and price"; stockError != null -> stockError; total <= 0 -> "Order total must be greater than zero"; paymentValue > total -> "Payment cannot exceed the order total"; else -> null }; if (validation == null) { saving = true; kotlinx.coroutines.MainScope().launch { val request = CreateOrderRequest(customer.trim(), lines.map { OrderItemRequest(it.inventoryId, it.productName, it.size.ifBlank { null }, it.unitPrice.toDoubleOrNull() ?: 0.0, it.quantity.toIntOrNull() ?: 1) }, discountValue, total, paymentValue, paymentMethod.lowercase(), shop.lowercase(), date, notes.ifBlank { null }, order.status ?: "ACTIVE"); val response = order.id?.let { api.updateOrder(it, request) } ?: api.createOrder(request); saving = false; if (response.isSuccessful) onSaved() else if (response.code() == 401) onUnauthorized() } } }) { Text(if (saving) "Saving…" else if (order.id == null) "Create order" else "Update order") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun PaymentUpdateDialog(api: WobbleApi, order: OrderDto, onDismiss: () -> Unit, onSaved: () -> Unit, onUnauthorized: () -> Unit) {
    var amount by remember { mutableStateOf("") }; var method by remember { mutableStateOf((order.paymentMethod ?: "cash").lowercase().takeIf { it == "gcash" } ?: "cash") }; var validation by remember { mutableStateOf<String?>(null) }; var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val balance = order.outstanding()
    AlertDialog(onDismissRequest = { if (!saving) onDismiss() }, title = { Text("Payment update", color = BrandInk, fontWeight = FontWeight.Bold) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { DetailRow("Order", order.jobOrderNo); DetailRow("Total", formatPHP(order.price)); DetailRow("Paid so far", formatPHP(order.payment)); DetailRow("Outstanding balance", formatPHP(balance)); OutlinedTextField(amount, { amount = it }, label = { Text("Payment amount") }, singleLine = true); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("cash" to "Cash", "gcash" to "Gcash").forEach { (key, label) -> FilterChip(selected = method == key, onClick = { method = key }, label = { Text(label) }) } }; validation?.let { Text(it, color = BrandRed, fontSize = 12.sp) } } }, confirmButton = { Button(enabled = !saving, onClick = { val value = amount.toDoubleOrNull(); validation = when { value == null || value <= 0 -> "Enter a payment amount greater than zero"; (value * 100).roundToInt() > (balance * 100).roundToInt() -> "Payment cannot exceed the outstanding balance of ${formatPHP(balance)}"; else -> null }; if (validation == null) { saving = true; scope.launch { val response = try { order.id?.let { api.updatePayment(it, PaymentUpdateRequest(value ?: 0.0, method)) } } catch (_: Exception) { null }; saving = false; if (response?.isSuccessful == true) onSaved() else if (response?.code() == 401) onUnauthorized() else validation = "Could not record this payment. Check the amount and try again." } } }) { Text(if (saving) "Saving…" else "Record payment") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun OrderDetails(api: WobbleApi, order: OrderDto, onDismiss: () -> Unit, onChanged: () -> Unit, onUnauthorized: () -> Unit) {
    var saving by remember { mutableStateOf(false) }
    val nextStatuses = when (order.status) {
        "ACTIVE" -> listOf("ARCHIVED", "CANCELLED")
        "ARCHIVED" -> listOf("ACTIVE", "CANCELLED")
        else -> listOf("ACTIVE")
    }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Order ${order.jobOrderNo ?: "details"}", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(order.customerName ?: "Walk-in", color = BrandInk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("${formatSlashDate(order.orderDate)} · ${order.status}", color = BrandMuted)
                order.items.forEach { item ->
                    DetailRow("${item.productName} ${item.size ?: ""} x ${item.quantity ?: 0}", formatPHP((item.unitPrice ?: 0.0) * (item.quantity ?: 0)))
                }
                DetailRow("Discount", "-${formatPHP(order.discount)}")
                DetailRow("Total", formatPHP(order.price))
                DetailRow("Payment (${order.paymentMethod})", formatPHP(order.payment))
                DetailRow("Balance", formatPHP(order.balance))
                DetailRow("Shop", order.shop)
                order.notes?.let { DetailRow("Notes", it) }
                Text("Status actions", color = BrandInk, fontWeight = FontWeight.Bold)
                nextStatuses.forEach { next ->
                    Button(enabled = !saving, onClick = {
                        saving = true
                        kotlinx.coroutines.MainScope().launch {
                            val request = CreateOrderRequest(order.customerName, order.items.map { OrderItemRequest(it.inventoryId, it.productName, it.size, it.unitPrice, it.quantity) }, order.discount ?: 0.0, order.price ?: 0.0, order.payment ?: 0.0, order.paymentMethod ?: "cash", order.shop ?: "store", order.orderDate?.take(10) ?: todayISO(), order.notes, next)
                            val response = order.id?.let { api.updateOrder(it, request) }
                            saving = false
                            if (response?.isSuccessful == true) onChanged() else if (response?.code() == 401) onUnauthorized()
                        }
                    }) { Text(if (next == "ARCHIVED") "Hide / archive order" else if (next == "CANCELLED") "Order is cancelled" else "Restore order") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun DetailRow(label: String, value: String?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = BrandMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value ?: "-", color = BrandInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Pagination(page: Int, total: Int, previous: () -> Unit, next: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        TextButton(enabled = page > 1, onClick = previous) { Text("Previous") }
        Text("Page $page of $total", color = BrandMuted, modifier = Modifier.padding(top = 12.dp))
        TextButton(enabled = page < total, onClick = next) { Text("Next") }
    }
}

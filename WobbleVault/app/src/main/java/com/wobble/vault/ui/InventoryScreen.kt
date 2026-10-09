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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wobble.vault.data.api.WobbleApi
import com.wobble.vault.data.model.AuthUser
import com.wobble.vault.data.model.InventoryItem
import com.wobble.vault.data.model.InventoryRequest
import com.wobble.vault.ui.theme.BrandAmber
import com.wobble.vault.ui.theme.BrandGreen
import com.wobble.vault.ui.theme.BrandInk
import com.wobble.vault.ui.theme.BrandLine
import com.wobble.vault.ui.theme.BrandMuted
import com.wobble.vault.ui.theme.BrandPaper
import com.wobble.vault.ui.theme.BrandRed
import com.wobble.vault.ui.theme.BrandRedDark
import com.wobble.vault.ui.theme.BrandRedSoft
import kotlinx.coroutines.launch
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(api: WobbleApi, user: AuthUser?, onBack: () -> Unit, onUnauthorized: () -> Unit, onOpenDrawer: () -> Unit) {
    var inventory by remember { mutableStateOf<List<InventoryItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var searchTerm by remember { mutableStateOf("") }
    var sizeFilter by remember { mutableStateOf("") }
    var currentPage by remember { mutableIntStateOf(1) }
    var editorItem by remember { mutableStateOf<InventoryItem?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var detailsItem by remember { mutableStateOf<InventoryItem?>(null) }
    var deleteItem by remember { mutableStateOf<InventoryItem?>(null) }
    val permitted = user?.hasPermission("INVENTORY") == true
    val scope = rememberCoroutineScope()

    suspend fun saveInventory(item: InventoryItem, request: InventoryRequest): Response<InventoryItem>? {
        try {
            api.csrf()
        } catch (_: Exception) {
        }
        return try {
            item.id?.let { api.updateInventory(it, request) } ?: api.createInventory(request)
        } catch (_: Exception) {
            null
        }
    }

    LaunchedEffect(reload, permitted) {
        if (!permitted) return@LaunchedEffect
        loading = true
        error = null
        val response = try { api.inventory(0, 100) } catch (_: Exception) { null }
        when {
            response?.isSuccessful == true -> { inventory = response.body()?.content.orEmpty(); loading = false }
            response?.code() == 401 -> onUnauthorized()
            else -> { error = "Could not load inventory. Check your connection."; loading = false }
        }
    }

    val filtered = inventory.sortedWith(compareBy<InventoryItem> { it.brand.orEmpty().lowercase() }.thenBy { it.name.orEmpty().lowercase() }).filter { item ->
        val term = searchTerm.trim()
        val matchesSearch = term.isBlank() || item.name.orEmpty().contains(term, true) || item.brand.orEmpty().contains(term, true)
        val matchesSize = sizeFilter.isBlank() || item.size.orEmpty().equals(sizeFilter, true)
        matchesSearch && matchesSize
    }
    val pageSize = 10
    val totalPages = maxOf(1, (filtered.size + pageSize - 1) / pageSize)
    val safePage = currentPage.coerceIn(1, totalPages)
    val visible = filtered.drop((safePage - 1) * pageSize).take(pageSize)

    LaunchedEffect(searchTerm, sizeFilter) { currentPage = 1 }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Inventory", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onOpenDrawer) { Icon(Icons.Filled.Menu, "Open navigation") } }, actions = { TextButton(onClick = { saveError = null; editorItem = InventoryItem() }) { Text("Add shoe", color = BrandRed, fontWeight = FontWeight.SemiBold) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandPaper, titleContentColor = BrandInk))
    }) { padding ->
        if (!permitted) Column(Modifier.fillMaxSize().padding(padding)) { InventoryAccessDenied() } else Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding)) {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item { PageHeader("STOCK CONTROL", "Inventory", "Keep brands, sizes, quantities, and prices up to date for every shoe you carry.") }
                item { InventoryStats(inventory.size, visible.size, filtered.size) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(searchTerm, { searchTerm = it }, placeholder = { Text("Search by brand or name") }, leadingIcon = { Icon(Icons.Filled.Search, null, tint = BrandMuted) }, singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), colors = inventoryFieldColors())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(sizeFilter, { sizeFilter = it }, placeholder = { Text("Filter by size") }, singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f), colors = inventoryFieldColors())
                            if (sizeFilter.isNotBlank()) TextButton(onClick = { sizeFilter = "" }) { Text("Clear", color = BrandRed) }
                        }
                    }
                }
                item { Text("Inventory overview", color = BrandInk, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                if (loading) item { Box(Modifier.fillMaxWidth().height(220.dp)) { LoadingState() } }
                else if (error != null) item { ErrorState(error!!) { reload++ } }
                else if (visible.isEmpty()) item { EmptyState("No inventory items match your current filters", "Try a broader term or add a new item to refresh the catalog.") }
                else {
                    items(visible, key = { it.id ?: "${it.brand}-${it.name}-${it.size}" }) { item -> InventoryCard(item, onView = { detailsItem = item }, onEdit = { editorItem = item }, onDelete = { deleteItem = item }) }
                    item { Pagination(safePage, totalPages, { currentPage = (safePage - 1).coerceAtLeast(1) }, { currentPage = (safePage + 1).coerceAtMost(totalPages) }) }
                }
            }
        }
    }

    editorItem?.let { item -> InventoryEditor(item.id != null, item, saveError = saveError, onDismiss = { editorItem = null }, onSave = { request -> scope.launch { val response = saveInventory(item, request); if (response?.isSuccessful == true) { saveError = null; editorItem = null; reload++ } else if (response?.code() == 401) onUnauthorized() else { saveError = when (response?.code()) { 403 -> "The server rejected this save because the request was not permitted."; 400, 422 -> "The shoe details were rejected. Check the required fields and values."; else -> "Could not save this shoe${response?.code()?.let { " (HTTP $it)" } ?: ". Check your connection."}" } } } }) }
    detailsItem?.let { item -> InventoryDetails(item) { detailsItem = null } }
    deleteItem?.let { item -> ConfirmDialog("Delete shoe?", "This removes ${item.name.orEmpty()} from inventory.", "Delete", onConfirm = { deleteItem = null; scope.launch { val response = item.id?.let { api.deleteInventory(it) }; if (response?.code() == 401) onUnauthorized() else reload++ } }, onDismiss = { deleteItem = null }) }
}

@Composable
private fun InventoryStats(tracked: Int, visible: Int, filtered: Int) { Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { StatPill("Tracked items", "Loaded in roster", tracked.toString(), Modifier.weight(1f)); StatPill("Visible now", "On this page", visible.toString(), Modifier.weight(1f)); StatPill("Filtered results", "Matching search", filtered.toString(), Modifier.weight(1f)) } }

@Composable
private fun InventoryCard(item: InventoryItem, onView: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) { Surface(shape = RoundedCornerShape(16.dp), color = BrandPaper, border = BorderStroke(1.dp, BrandLine), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) { Column(Modifier.weight(1f)) { Text(item.brand ?: "Brand", color = BrandRed, fontSize = 11.sp, fontWeight = FontWeight.Bold); Text(item.name ?: "Unnamed item", color = BrandInk, fontSize = 16.sp, fontWeight = FontWeight.Bold); Text(listOfNotNull(item.size?.takeIf { it.isNotBlank() }?.let { "Size $it" }, item.gender, item.sizingSystem).joinToString(" · ").ifBlank { "No size details" }, color = BrandMuted, fontSize = 12.sp) }; Surface(shape = RoundedCornerShape(999.dp), color = if ((item.quantity ?: 0) <= 0) BrandRedSoft else Color(0xFFE4EEE7)) { Text("Qty ${item.quantity ?: 0}", color = if ((item.quantity ?: 0) <= 0) BrandRed else BrandGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) } }; Spacer(Modifier.height(8.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(formatCurrency(item.price), color = BrandRedDark, fontSize = 15.sp, fontWeight = FontWeight.Bold); Row { TextButton(onClick = onView) { Icon(Icons.Filled.Info, null, modifier = Modifier.size(16.dp)); Text(" View", fontSize = 12.sp) }; IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "Edit", tint = BrandRed) }; IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Delete", tint = BrandRed) } } } } } }

@Composable
private fun Pagination(page: Int, totalPages: Int, onPrevious: () -> Unit, onNext: () -> Unit) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { TextButton(enabled = page > 1, onClick = onPrevious) { Text("Previous") }; Text("Page $page of $totalPages", color = BrandMuted, fontSize = 12.sp); TextButton(enabled = page < totalPages, onClick = onNext) { Text("Next") } } }

@Composable
private fun InventoryEditor(editing: Boolean, item: InventoryItem, saveError: String?, onDismiss: () -> Unit, onSave: (InventoryRequest) -> Unit) {
    var brand by remember { mutableStateOf(item.brand.orEmpty()) }; var name by remember { mutableStateOf(item.name.orEmpty()) }; var gender by remember { mutableStateOf(item.gender.orEmpty()) }; var sizing by remember { mutableStateOf(item.sizingSystem.orEmpty()) }; var size by remember { mutableStateOf(item.size.orEmpty()) }; var quantity by remember { mutableStateOf(item.quantity?.toString().orEmpty()) }; var price by remember { mutableStateOf(item.price?.toString().orEmpty()) }; var notes by remember { mutableStateOf(item.notes.orEmpty()) }; var validation by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (editing) "Edit Shoe" else "New Shoe", color = BrandInk, fontWeight = FontWeight.Bold) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(brand, { brand = it }, label = { Text("Brand") }, singleLine = true); OutlinedTextField(name, { name = it }, label = { Text("Shoe name") }, singleLine = true); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(gender, { gender = it }, label = { Text("Gender") }, modifier = Modifier.weight(1f), singleLine = true); OutlinedTextField(sizing, { sizing = it }, label = { Text("Sizing system") }, modifier = Modifier.weight(1f), singleLine = true) }; Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(size, { size = it }, label = { Text("Size") }, modifier = Modifier.weight(1f), singleLine = true); OutlinedTextField(quantity, { quantity = it.filter(Char::isDigit) }, label = { Text("Quantity") }, modifier = Modifier.weight(1f), singleLine = true) }; OutlinedTextField(price, { price = it }, label = { Text("Price") }, singleLine = true); OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, minLines = 2); validation?.let { Text(it, color = BrandRed, fontSize = 12.sp) }; saveError?.let { Text(it, color = BrandRed, fontSize = 12.sp) } } }, confirmButton = { Button(onClick = { val qty = quantity.toIntOrNull(); val cost = price.toDoubleOrNull(); validation = when { brand.isBlank() -> "Brand is required"; name.isBlank() -> "Shoe name is required"; qty == null || qty < 0 -> "Quantity cannot be less than zero"; cost == null || cost <= 0 -> "Price must be greater than zero"; else -> null }; if (validation == null) onSave(InventoryRequest(brand.trim(), name.trim(), gender.trim().ifBlank { null }, sizing.trim().ifBlank { null }, size.trim().ifBlank { null }, qty, cost, notes.trim().ifBlank { null })) }) { Text(if (editing) "Update" else "Add") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun InventoryDetails(item: InventoryItem, onDismiss: () -> Unit) { AlertDialog(onDismissRequest = onDismiss, title = { Text("Item Details", color = BrandInk, fontWeight = FontWeight.Bold) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Surface(shape = RoundedCornerShape(12.dp), color = BrandRedSoft, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) { Text("INVENTORY ITEM", color = BrandRedDark, fontSize = 10.sp, fontWeight = FontWeight.Bold); Text(item.name ?: "-", color = BrandInk, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text(item.brand ?: "Brand", color = BrandMuted, fontSize = 12.sp) } }; DetailRow("Brand", item.brand); DetailRow("Size", item.size); DetailRow("Gender", item.gender); DetailRow("Sizing system", item.sizingSystem); DetailRow("Quantity", item.quantity?.toString()); DetailRow("Price", item.price?.let { formatPHP(it) }); DetailRow("Notes", item.notes); DetailRow("Date created", formatDateCreated(item.createdAt)) } }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }) }

@Composable
private fun DetailRow(label: String, value: String?) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = BrandMuted, fontSize = 12.sp); Text(value ?: "-", color = BrandInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) } }

@Composable
private fun inventoryFieldColors() = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandRed, unfocusedBorderColor = BrandLine, cursorColor = BrandRed, focusedTextColor = BrandInk, unfocusedTextColor = BrandInk, focusedPlaceholderColor = BrandMuted, unfocusedPlaceholderColor = BrandMuted, unfocusedContainerColor = BrandPaper)

@Composable
private fun InventoryAccessDenied() { Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(Icons.Filled.Lock, null, tint = BrandAmber, modifier = Modifier.size(34.dp)); Spacer(Modifier.height(12.dp)); Text("Access denied", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = BrandInk); Spacer(Modifier.height(6.dp)); Text("You don't have permission to view inventory.", fontSize = 13.sp, color = BrandMuted, textAlign = TextAlign.Center) } }

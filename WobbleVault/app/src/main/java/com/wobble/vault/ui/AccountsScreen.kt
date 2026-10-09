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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
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
import com.wobble.vault.data.model.CreateUserRequest
import com.wobble.vault.data.model.UserDto
import com.wobble.vault.ui.theme.BrandInk
import com.wobble.vault.ui.theme.BrandLine
import com.wobble.vault.ui.theme.BrandMuted
import com.wobble.vault.ui.theme.BrandPaper
import com.wobble.vault.ui.theme.BrandRed
import com.wobble.vault.ui.theme.BrandRedSoft
import kotlinx.coroutines.launch

private val accountPermissions = listOf("INVENTORY" to "Inventory", "ORDERS" to "Orders", "SALES" to "Sales", "SALES_ARCHIVE" to "Sales Archive", "ACCOUNTS" to "Accounts")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(api: WobbleApi, user: AuthUser, onBack: () -> Unit, onUnauthorized: () -> Unit, onOpenDrawer: () -> Unit) {
    var users by remember { mutableStateOf<List<UserDto>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var registerOpen by remember { mutableStateOf(false) }
    var permissionUser by remember { mutableStateOf<UserDto?>(null) }
    var deleteTarget by remember { mutableStateOf<UserDto?>(null) }
    val scope = rememberCoroutineScope()
    val permitted = user.hasPermission("ACCOUNTS")
    val isAdmin = user.role.equals("ADMIN", ignoreCase = true)

    LaunchedEffect(reload, permitted, isAdmin) {
        if (!permitted || !isAdmin) { loading = false; return@LaunchedEffect }
        loading = true
        val response = try { api.users(0, 100) } catch (_: Exception) { null }
        when {
            response?.isSuccessful == true -> { users = response.body()?.content.orEmpty(); loading = false; error = null }
            response?.code() == 401 -> onUnauthorized()
            else -> { loading = false; error = "Could not load accounts. Check your connection." }
        }
    }

    val term = query.trim()
    val visible = users.filter { account -> term.isBlank() || listOf(account.username, account.email, account.role).any { it.orEmpty().contains(term, true) } }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Accounts", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onOpenDrawer) { Icon(Icons.Filled.Menu, "Open navigation") } }, actions = { if (isAdmin) TextButton(onClick = { registerOpen = true }) { Text("Register Admin", color = BrandRed) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandPaper, titleContentColor = BrandInk))
    }) { padding ->
        when {
            !permitted -> Column(Modifier.fillMaxSize().padding(padding)) { AccessDenied() }
            !isAdmin -> Column(Modifier.fillMaxSize().padding(padding).padding(18.dp)) { PageHeader("PEOPLE & ACCESS", "Accounts", "You have permission to open this page, but account management controls are reserved for administrators.") }
            else -> LazyColumn(Modifier.fillMaxSize().background(Color(0xFFF4F1EB)).padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item { PageHeader("PEOPLE & ACCESS", "Accounts", "Keep your team accounts, roles, and page permissions clear and easy to manage.") }
                item { SectionCard("Accounts overview", "Quickly scan your roster, refine the list, and keep permissions aligned.") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        StatPill("Registered", "People in roster", formatCount(users.size.toLong()), Modifier.weight(1f))
                        StatPill("Admins", "Permission leaders", formatCount(users.count { it.role.equals("ADMIN", true) }.toLong()), Modifier.weight(1f))
                        StatPill("Visible now", "Matching filters", formatCount(visible.size.toLong()), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(14.dp)); SearchField(query, { query = it }, "Search accounts by name or email")
                } }
                when {
                    loading -> item { LoadingState() }
                    error != null -> item { ErrorState(error!!) { reload++ } }
                    visible.isEmpty() -> item { EmptyState("No accounts match this view yet", "Try adjusting your search or register an admin account.") }
                    else -> items(visible, key = { it.id ?: it.username.orEmpty() }) { account -> AccountCard(account, { permissionUser = account }, { deleteTarget = account }) }
                }
            }
        }
    }
    if (registerOpen) AccountEditor(api, { registerOpen = false }, { registerOpen = false; reload++ }, onUnauthorized)
    permissionUser?.let { account -> PermissionEditor(api, account, { permissionUser = null }, { permissionUser = null; reload++ }, onUnauthorized) }
    deleteTarget?.let { account -> ConfirmDialog("Delete account?", "This permanently removes ${account.username ?: "this account"} and its page access.", "Delete", onConfirm = { deleteTarget = null; scope.launch { val response = account.id?.let { api.deleteUser(it) }; if (response?.code() == 401) onUnauthorized() else reload++ } }, onDismiss = { deleteTarget = null }) }
}

@Composable
private fun AccountCard(account: UserDto, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = BrandPaper, border = BorderStroke(1.dp, BrandLine), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) { Text(account.username ?: "Account", color = BrandInk, fontSize = 17.sp, fontWeight = FontWeight.Bold); Text(account.email ?: "No email", color = BrandMuted, fontSize = 13.sp) }
                Text(account.role ?: "-", color = BrandRed, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp)); Text("Page access", color = BrandMuted, fontSize = 11.sp)
            Text(if (account.permissions.isEmpty()) "No page access" else account.permissions.joinToString { it.pageName }, color = BrandInk, fontSize = 13.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "Manage permissions", tint = BrandRed) }; IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Delete account", tint = BrandRed) } }
        }
    }
}

@Composable
private fun AccountEditor(api: WobbleApi, onDismiss: () -> Unit, onSaved: () -> Unit, onUnauthorized: () -> Unit) {
    var username by remember { mutableStateOf("") }; var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; var selected by remember { mutableStateOf(setOf<String>()) }; var saving by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }; val scope = rememberCoroutineScope()
    AlertDialog(onDismissRequest = { if (!saving) onDismiss() }, title = { Text("Register New Admin", fontWeight = FontWeight.Bold) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(username, { username = it }, label = { Text("Account name") }, singleLine = true); OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true); OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true)
        Text("Page viewing permissions", color = BrandInk, fontWeight = FontWeight.SemiBold); Text("Dashboard is always available. Select from the current sidebar pages below.", color = BrandMuted, fontSize = 12.sp)
        accountPermissions.forEach { (key, label) -> FilterChip(selected = key in selected, onClick = { selected = if (key in selected) selected - key else selected + key }, label = { Text(label) }) }; error?.let { Text(it, color = BrandRed, fontSize = 12.sp) }
    } }, confirmButton = { Button(enabled = !saving && username.trim().isNotBlank() && email.trim().isNotBlank() && password.length >= 6, onClick = { saving = true; error = null; scope.launch {
        val response = try { api.createUser(CreateUserRequest(username.trim(), email.trim(), password, "ADMIN")) } catch (_: Exception) { null }
        if (response?.isSuccessful == true) { val created = response.body(); if (created?.id != null) selected.forEach { api.grantPermission(created.id, it) }; onSaved() } else if (response?.code() == 401) onUnauthorized() else error = "Could not register this account."; saving = false
    } }, colors = ButtonDefaults.buttonColors(containerColor = BrandRed)) { Text(if (saving) "Saving..." else "Register Admin") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun PermissionEditor(api: WobbleApi, account: UserDto, onDismiss: () -> Unit, onSaved: () -> Unit, onUnauthorized: () -> Unit) {
    var selected by remember { mutableStateOf(account.permissions.map { it.pageName }.toSet()) }; var saving by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }; val scope = rememberCoroutineScope()
    AlertDialog(onDismissRequest = { if (!saving) onDismiss() }, title = { Text("Manage Permissions: ${account.username}", fontWeight = FontWeight.Bold) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Dashboard is always available. Update access for the current sidebar pages below.", color = BrandMuted, fontSize = 12.sp); accountPermissions.forEach { (key, label) -> FilterChip(selected = key in selected, onClick = { selected = if (key in selected) selected - key else selected + key }, label = { Text(label) }) }; error?.let { Text(it, color = BrandRed, fontSize = 12.sp) }
    } }, confirmButton = { Button(enabled = !saving, onClick = { saving = true; error = null; scope.launch {
        val current = account.permissions.map { it.pageName }.toSet(); val added = selected - current; val removed = current - selected; val responses = added.map { api.grantPermission(account.id.orEmpty(), it) } + removed.map { api.revokePermission(account.id.orEmpty(), it) }
        if (responses.any { it.code() == 401 }) onUnauthorized() else if (responses.any { !it.isSuccessful }) error = "Could not save all permissions." else onSaved(); saving = false
    } }, colors = ButtonDefaults.buttonColors(containerColor = BrandRed)) { Text(if (saving) "Saving..." else "Save Permissions") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

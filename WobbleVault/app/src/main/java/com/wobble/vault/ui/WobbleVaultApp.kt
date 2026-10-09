package com.wobble.vault.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wobble.vault.data.AuthRepository
import com.wobble.vault.R
import com.wobble.vault.data.api.WobbleApi
import com.wobble.vault.data.model.AuthUser
import com.wobble.vault.ui.theme.BrandInk
import com.wobble.vault.ui.theme.BrandMuted
import com.wobble.vault.ui.theme.BrandRed
import com.wobble.vault.ui.theme.BrandRedDark
import com.wobble.vault.ui.theme.BrandRedSoft
import kotlinx.coroutines.launch

private val InventoryBoxIcon = ImageVector.Builder("inventory_box", 24.dp, 24.dp, 24f, 24f).path(fill = SolidColor(Color.Black)) {
    moveTo(3f, 6f); lineTo(12f, 3f); lineTo(21f, 6f); lineTo(12f, 9f); close()
    moveTo(3f, 6f); lineTo(3f, 18f); lineTo(12f, 21f); lineTo(12f, 9f); close()
    moveTo(21f, 6f); lineTo(21f, 18f); lineTo(12f, 21f); lineTo(12f, 9f); close()
}.build()

private val MoneyIcon = ImageVector.Builder("money", 24.dp, 24.dp, 24f, 24f).path(fill = SolidColor(Color.Black)) {
    moveTo(12f, 2f); curveTo(6.5f, 2f, 2f, 6.5f, 2f, 12f); curveTo(2f, 17.5f, 6.5f, 22f, 12f, 22f); curveTo(17.5f, 22f, 22f, 17.5f, 22f, 12f); curveTo(22f, 6.5f, 17.5f, 2f, 12f, 2f); close()
    moveTo(13f, 5f); lineTo(13f, 7f); curveTo(15f, 7.2f, 16.5f, 8.3f, 16.5f, 10f); lineTo(13.5f, 10f); curveTo(13.5f, 9.3f, 13f, 9f, 12f, 9f); curveTo(11f, 9f, 10.5f, 9.4f, 10.5f, 10f); curveTo(10.5f, 10.6f, 11.2f, 10.8f, 12.7f, 11.1f); curveTo(15.2f, 11.6f, 16.8f, 12.6f, 16.8f, 14.8f); curveTo(16.8f, 16.7f, 15.3f, 18f, 13f, 18.3f); lineTo(13f, 20f); lineTo(11f, 20f); lineTo(11f, 18.3f); curveTo(8.7f, 18f, 7.3f, 16.8f, 7.2f, 14.8f); lineTo(10.2f, 14.8f); curveTo(10.3f, 15.6f, 10.9f, 16f, 12f, 16f); curveTo(13.1f, 16f, 13.8f, 15.6f, 13.8f, 14.9f); curveTo(13.8f, 14.3f, 13.1f, 14f, 11.7f, 13.7f); curveTo(9.2f, 13.2f, 7.7f, 12.1f, 7.7f, 10.1f); curveTo(7.7f, 8.4f, 9f, 7.3f, 11f, 7f); lineTo(11f, 5f); close()
}.build()

private val ArchiveDocumentIcon = ImageVector.Builder("archive_document", 24.dp, 24.dp, 24f, 24f).path(fill = SolidColor(Color.Black)) {
    moveTo(5f, 2f); lineTo(15f, 2f); lineTo(20f, 7f); lineTo(20f, 22f); lineTo(5f, 22f); close()
    moveTo(15f, 2f); lineTo(15f, 7f); lineTo(20f, 7f); close()
    moveTo(8f, 11f); lineTo(17f, 11f); lineTo(17f, 13f); lineTo(8f, 13f); close()
    moveTo(8f, 15f); lineTo(17f, 15f); lineTo(17f, 17f); lineTo(8f, 17f); close()
}.build()

@Composable
fun WobbleVaultApp(repo: AuthRepository, api: WobbleApi) {
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    var user by remember { mutableStateOf<AuthUser?>(null) }

    fun goToLogin() {
        user = null
        navController.navigate("login") {
            popUpTo(0) { inclusive = true }
        }
    }

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            LaunchedEffect(Unit) {
                when (val result = repo.restore()) {
                    is AuthRepository.AuthResult.Success -> {
                        user = result.user
                            navController.navigate("dashboard") {
                            popUpTo(0) { inclusive = true }
                        }
                    }

                    is AuthRepository.AuthResult.Failure -> goToLogin()
                }
            }
            SplashScreen()
        }

        composable("login") {
            LoginScreen(
                repo = repo,
                onLoggedIn = { loggedInUser ->
                    user = loggedInUser
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("menu") {
            val currentUser = user
            if (currentUser == null) {
                LaunchedEffect(Unit) { goToLogin() }
            } else {
                MenuScreen(
                    user = currentUser,
                    onOpen = { route -> navController.navigate(route) },
                    onLogout = {
                        scope.launch {
                            repo.logout()
                            goToLogin()
                        }
                    }
                )
            }
        }

        composable("dashboard") {
            val currentUser = user
            if (currentUser == null) {
                LaunchedEffect(Unit) { goToLogin() }
            } else {
                AuthenticatedFrame(currentUser, "dashboard", navController, scope, { goToLogin() }) { openDrawer ->
                    DashboardScreen(api, currentUser, onUnauthorized = { goToLogin() }, onOpenDrawer = openDrawer, onNavigate = { route -> navController.navigate(route) { launchSingleTop = true } })
                }
            }
        }

        composable("inventory") {
            val currentUser = user
            if (currentUser == null) {
                LaunchedEffect(Unit) { goToLogin() }
            } else {
                AuthenticatedFrame(currentUser, "inventory", navController, scope, { goToLogin() }) { openDrawer ->
                    InventoryScreen(api, currentUser, onBack = { navController.popBackStack() }, onUnauthorized = { goToLogin() }, onOpenDrawer = openDrawer)
                }
            }
        }

        composable("orders") {
            val currentUser = user
            if (currentUser == null) {
                LaunchedEffect(Unit) { goToLogin() }
            } else {
                AuthenticatedFrame(currentUser, "orders", navController, scope, { goToLogin() }) { openDrawer ->
                    OrdersScreen(api, currentUser, onBack = { navController.popBackStack() }, onUnauthorized = { goToLogin() }, onOpenDrawer = openDrawer)
                }
            }
        }

        composable("sales") {
            val currentUser = user
            if (currentUser == null) {
                LaunchedEffect(Unit) { goToLogin() }
            } else {
                AuthenticatedFrame(currentUser, "sales", navController, scope, { goToLogin() }) { openDrawer ->
                    SalesScreen(api, currentUser, onBack = { navController.popBackStack() }, onUnauthorized = { goToLogin() }, onOpenDrawer = openDrawer)
                }
            }
        }

        composable("sales-archive") {
            val currentUser = user
            if (currentUser == null) {
                LaunchedEffect(Unit) { goToLogin() }
            } else {
                AuthenticatedFrame(currentUser, "sales-archive", navController, scope, { goToLogin() }) { openDrawer ->
                    SalesArchiveScreen(api, currentUser, onBack = { navController.popBackStack() }, onUnauthorized = { goToLogin() }, onOpenDrawer = openDrawer)
                }
            }
        }

        composable("accounts") {
            val currentUser = user
            if (currentUser == null) {
                LaunchedEffect(Unit) { goToLogin() }
            } else {
                AuthenticatedFrame(currentUser, "accounts", navController, scope, { goToLogin() }) { openDrawer ->
                    AccountsScreen(api, currentUser, onBack = { navController.popBackStack() }, onUnauthorized = { goToLogin() }, onOpenDrawer = openDrawer)
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedFrame(
    user: AuthUser,
    currentRoute: String,
    navController: androidx.navigation.NavHostController,
    scope: kotlinx.coroutines.CoroutineScope,
    onLogout: () -> Unit,
    content: @Composable (openDrawer: () -> Unit) -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val openDrawer: () -> Unit = {
        scope.launch { drawerState.open() }
        Unit
    }
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Image(
                        painter = painterResource(R.drawable.wv_logo),
                        contentDescription = "Wobble Vault logo",
                        modifier = Modifier
                            .size(width = 150.dp, height = 112.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Text("Wobble Vault", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = BrandInk)
                    Text("Operations dashboard", fontSize = 12.sp, color = BrandMuted)
                    Spacer(Modifier.height(18.dp))
                    Text(user.username.ifBlank { user.email.orEmpty() }, fontWeight = FontWeight.SemiBold, color = BrandInk)
                    Text(user.role.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 12.sp, color = BrandMuted)
                }
                HorizontalDivider()
                DrawerItem("Dashboard", "dashboard", currentRoute, Icons.Filled.Home, user, navController, drawerState, scope)
                if (user.hasPermission("INVENTORY")) DrawerItem("Inventory", "inventory", currentRoute, InventoryBoxIcon, user, navController, drawerState, scope)
                if (user.hasPermission("ORDERS")) DrawerItem("Orders", "orders", currentRoute, Icons.Filled.ShoppingCart, user, navController, drawerState, scope)
                if (user.hasPermission("SALES")) DrawerItem("Sales", "sales", currentRoute, MoneyIcon, user, navController, drawerState, scope)
                if (user.hasPermission("SALES_ARCHIVE")) DrawerItem("Sales Archive", "sales-archive", currentRoute, ArchiveDocumentIcon, user, navController, drawerState, scope)
                if (user.hasPermission("ACCOUNTS")) DrawerItem("Accounts", "accounts", currentRoute, Icons.Filled.AccountCircle, user, navController, drawerState, scope)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onLogout, modifier = Modifier.padding(12.dp)) { Text("Log out", color = BrandRed) }
            }
        }
    ) { content(openDrawer) }
}

@Composable
private fun DrawerItem(
    label: String,
    route: String,
    currentRoute: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    user: AuthUser,
    navController: androidx.navigation.NavHostController,
    drawerState: androidx.compose.material3.DrawerState,
    scope: kotlinx.coroutines.CoroutineScope
) {
    NavigationDrawerItem(
        label = { Text(label) },
        selected = currentRoute == route,
        icon = { Icon(icon, contentDescription = null) },
        onClick = {
            scope.launch { drawerState.close() }
            if (currentRoute != route) navController.navigate(route) { launchSingleTop = true }
        },
        colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = BrandRedSoft, selectedTextColor = BrandRedDark)
    )
}

@Composable
private fun SplashScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.wv_logo),
            contentDescription = "Wobble Vault logo",
            modifier = Modifier
                .size(width = 220.dp, height = 164.dp)
                .clip(RoundedCornerShape(20.dp))
        )
        Spacer(Modifier.height(18.dp))
        CircularProgressIndicator(color = BrandRed)
        Spacer(Modifier.height(18.dp))
        Text("Wobble Vault", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = BrandInk)
        Spacer(Modifier.height(4.dp))
        Text("Authentic Shoe Store", fontSize = 13.sp, color = BrandMuted)
    }
}

package com.wobble.vault.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wobble.vault.data.model.AuthUser
import com.wobble.vault.ui.theme.BrandInk
import com.wobble.vault.ui.theme.BrandLine
import com.wobble.vault.ui.theme.BrandMuted
import com.wobble.vault.ui.theme.BrandPaper
import com.wobble.vault.ui.theme.BrandRedDark
import com.wobble.vault.ui.theme.BrandRedSoft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    user: AuthUser,
    onOpen: (String) -> Unit,
    onLogout: () -> Unit
) {
    var loggingOut by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wobble Vault", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandPaper,
                    titleContentColor = BrandInk
                ),
                actions = {
                    IconButton(
                        enabled = !loggingOut,
                        onClick = {
                            loggingOut = true
                            onLogout()
                        }
                    ) {
                        if (loggingOut) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = BrandRedDark
                            )
                        } else {
                            Icon(Icons.Filled.ExitToApp, contentDescription = "Log out")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Text("Welcome back,", color = BrandMuted, fontSize = 14.sp)
            Text(
                user.username.ifBlank { user.email.orEmpty() },
                color = BrandInk,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            Surface(
                color = BrandRedSoft,
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(
                    user.role.lowercase().replaceFirstChar { it.uppercase() },
                    color = BrandRedDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            MenuCard(
                icon = Icons.Filled.Home,
                title = "Dashboard",
                description = "Inventory value, orders and monthly income at a glance.",
                onClick = { onOpen("dashboard") }
            )
            Spacer(Modifier.height(12.dp))
            if (user.hasPermission("INVENTORY")) {
                MenuCard(
                    icon = Icons.Filled.ShoppingCart,
                    title = "Inventory",
                    description = "Browse the catalog: brands, sizes, quantities and prices.",
                    onClick = { onOpen("inventory") }
                )
                Spacer(Modifier.height(12.dp))
            }
            if (user.hasPermission("ORDERS")) {
                MenuCard(Icons.Filled.ShoppingCart, "Orders", "Create orders, update fulfillment status, and track balances.", { onOpen("orders") })
                Spacer(Modifier.height(12.dp))
            }
            if (user.hasPermission("SALES")) {
                MenuCard(Icons.Filled.Home, "Sales", "Review receipts, net income, and record liquidations.", { onOpen("sales") })
                Spacer(Modifier.height(12.dp))
            }
            if (user.hasPermission("SALES_ARCHIVE")) {
                MenuCard(Icons.Filled.ShoppingCart, "Sales Archive", "Review archived orders and payment records.", { onOpen("sales-archive") })
                Spacer(Modifier.height(12.dp))
            }
            if (user.hasPermission("ACCOUNTS")) {
                MenuCard(Icons.Filled.Home, "Accounts", "Manage admin accounts and page-level permissions.", { onOpen("accounts") })
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Authentic Shoe Store · Quality Products · Quality Service",
                color = BrandMuted,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MenuCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = BrandPaper,
        border = BorderStroke(1.dp, BrandLine),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(BrandRedSoft, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = BrandRedDark, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = BrandInk)
                Spacer(Modifier.height(3.dp))
                Text(description, fontSize = 13.sp, color = BrandMuted, lineHeight = 17.sp)
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = BrandMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

package com.wobble.vault.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wobble.vault.ui.theme.BrandInk
import com.wobble.vault.ui.theme.BrandLine
import com.wobble.vault.ui.theme.BrandMuted
import com.wobble.vault.ui.theme.BrandPaper
import com.wobble.vault.ui.theme.BrandRed

@Composable
fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = BrandRed)
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.Warning,
            contentDescription = null,
            tint = BrandRed,
            modifier = Modifier.size(36.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            message,
            color = BrandInk,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("  Try again", fontSize = 14.sp)
        }
    }
}

@Composable
fun AccessDenied() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.Warning,
            contentDescription = null,
            tint = BrandRed,
            modifier = Modifier.size(36.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text("Access restricted", color = BrandInk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "You do not have permission to view this area.",
            color = BrandMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EmptyState(title: String, text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = BrandInk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(text, color = BrandMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun PageHeader(
    eyebrow: String,
    title: String,
    subtitle: String,
    actions: (@Composable () -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(eyebrow, color = BrandRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(title, color = BrandInk, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = BrandMuted, fontSize = 13.sp, lineHeight = 18.sp)
        if (actions != null) {
            Spacer(Modifier.height(12.dp))
            actions()
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = BrandPaper,
        border = BorderStroke(1.dp, BrandLine),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = BrandInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = BrandMuted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun StatPill(label: String, sublabel: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = BrandPaper,
        border = BorderStroke(1.dp, BrandLine),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(label, color = BrandMuted, fontSize = 11.sp)
            Text(value, color = BrandInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(sublabel, color = BrandMuted, fontSize = 10.sp)
        }
    }
}

@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = BrandMuted, fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = BrandMuted) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BrandRed,
            unfocusedBorderColor = BrandLine,
            focusedTextColor = BrandInk,
            unfocusedTextColor = BrandInk
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun BrandButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = BrandRed,
            disabledContainerColor = BrandLine
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun BrandOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        border = BorderStroke(1.dp, BrandLine),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Text(text, fontSize = 14.sp, color = BrandInk, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BrandPaper,
        title = { Text(title, color = BrandInk, fontSize = 17.sp, fontWeight = FontWeight.Bold) },
        text = { Text(message, color = BrandMuted, fontSize = 14.sp, lineHeight = 20.sp) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = BrandRed, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = BrandMuted)
            }
        }
    )
}

@Composable
fun StatCard(
    label: String,
    value: String,
    valueColor: Color = BrandInk
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = BrandPaper,
        border = BorderStroke(1.dp, BrandLine),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, color = BrandMuted, fontSize = 12.sp, lineHeight = 16.sp)
            Spacer(Modifier.height(6.dp))
            Text(value, color = valueColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

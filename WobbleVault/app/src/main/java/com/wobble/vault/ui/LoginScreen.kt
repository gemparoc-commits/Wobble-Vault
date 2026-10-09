package com.wobble.vault.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wobble.vault.R
import com.wobble.vault.data.AuthRepository
import com.wobble.vault.data.model.AuthUser
import com.wobble.vault.ui.theme.BrandInk
import com.wobble.vault.ui.theme.BrandLine
import com.wobble.vault.ui.theme.BrandMuted
import com.wobble.vault.ui.theme.BrandPaper
import com.wobble.vault.ui.theme.BrandRed
import com.wobble.vault.ui.theme.BrandRedDark
import com.wobble.vault.ui.theme.BrandRedSoft
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    repo: AuthRepository,
    onLoggedIn: (AuthUser) -> Unit
) {
    val scope = rememberCoroutineScope()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BrandInk)
                .padding(horizontal = 28.dp, vertical = 36.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.wv_logo),
                contentDescription = "Wobble Vault logo",
                modifier = Modifier
                    .size(width = 170.dp, height = 124.dp)
                    .clip(RoundedCornerShape(18.dp))
            )
            Spacer(Modifier.height(20.dp))
            Text("Wobble Vault", color = Color(0xFFEAE6DF), fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))
            Spacer(
                Modifier
                    .size(width = 48.dp, height = 4.dp)
                    .background(BrandRed)
            )
            Spacer(Modifier.height(16.dp))
            Text("Authentic Shoe Store", color = Color(0xFFEAE6DF), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Manage inventory, orders, sales reports from one focused workspace.",
                color = Color(0xFFB8B2A9),
                fontSize = 13.sp
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Welcome back", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BrandInk)
            Text(
                "Sign in with your email to continue to the dashboard.",
                color = BrandMuted,
                fontSize = 14.sp
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; error = null },
                label = { Text("Email") },
                placeholder = { Text("Enter your email address") },
                singleLine = true,
                enabled = !loading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; error = null },
                label = { Text("Password") },
                placeholder = { Text("Enter your password") },
                singleLine = true,
                enabled = !loading,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors()
            )

            error?.let { message ->
                Surface(
                    color = BrandRedSoft,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        message,
                        color = BrandRedDark,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    )
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        loading = true
                        error = null
                        when (val result = repo.login(email.trim(), password)) {
                            is AuthRepository.AuthResult.Success -> onLoggedIn(result.user)
                            is AuthRepository.AuthResult.Failure -> {
                                error = result.message
                                loading = false
                            }
                        }
                    }
                },
                enabled = email.isNotBlank() && password.isNotBlank() && !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Sign In", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Text(
                "Quality Products · Quality Service · Negotiable Prices",
                color = BrandMuted,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BrandRed,
    unfocusedBorderColor = BrandLine,
    focusedLabelColor = BrandRedDark,
    unfocusedLabelColor = BrandMuted,
    cursorColor = BrandRed,
    focusedTextColor = BrandInk,
    unfocusedTextColor = BrandInk,
    focusedPlaceholderColor = BrandMuted,
    unfocusedPlaceholderColor = BrandMuted,
    unfocusedContainerColor = BrandPaper
)

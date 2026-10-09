package com.wobble.vault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.wobble.vault.data.AuthRepository
import com.wobble.vault.data.Network
import com.wobble.vault.data.TokenStore
import com.wobble.vault.ui.WobbleVaultApp
import com.wobble.vault.ui.theme.WobbleVaultTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = TokenStore(applicationContext)
        val api = Network.createApi(store)
        val repo = AuthRepository(api, store)
        setContent {
            WobbleVaultTheme {
                WobbleVaultApp(repo = repo, api = api)
            }
        }
    }
}

package com.example.feature

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.MyApp
import com.example.ui.navigation.VaultNavHost
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val factory = (application as MyApp).container.viewModelFactory
        setContent { MyApplicationTheme { VaultNavHost(factory) } }
    }
}

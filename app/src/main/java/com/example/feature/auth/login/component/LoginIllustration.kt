package com.example.feature.auth.login.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

@Composable
fun LoginIllustration() {
    Surface(shape = VaultStyle.Panel, color = Ink, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("THẺ CỦA TÔI", style = MaterialTheme.typography.labelSmall, color = Color(0xFFCBDED6))
                Icon(Icons.Outlined.CreditCard, null, Modifier.size(30.dp), tint = Amber)
            }
            Text("•••• •••• •••• 4826", color = Color.White, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Cá nhân", color = Color(0xFFE0EDE8), style = MaterialTheme.typography.bodySmall)
                Icon(Icons.Outlined.Lock, null, Modifier.size(18.dp), tint = Amber)
            }
        }
    }
}

package com.example.ui.component.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun VaultNavigation(selected: Int, onSelect: (Int) -> Unit) {
    val labels = listOf("Kho của tôi", "Tài chính", "Yêu thích", "Cài đặt")
    val icons = listOf(Icons.Outlined.Inventory2, Icons.Outlined.CreditCard, Icons.Outlined.StarOutline, Icons.Outlined.Tune)
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.surface, border = BorderStroke(1.dp, colors.outlineVariant), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 10.dp, vertical = 10.dp).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            labels.forEachIndexed { index, label ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(15.dp))
                    .background(if (selected == index) colors.primaryContainer else Color.Transparent)
                    .selectable(selected == index, role = Role.Tab, onClick = { onSelect(index) })
                    .padding(horizontal = 2.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(icons[index], null, Modifier.size(22.dp), tint = if (selected == index) colors.primary else colors.onSurfaceVariant)
                    Text(label, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified),
                        textAlign = TextAlign.Center, color = if (selected == index) colors.onPrimaryContainer else colors.onSurfaceVariant)
                }
            }
        }
    }
}

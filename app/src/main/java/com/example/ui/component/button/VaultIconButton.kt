package com.example.ui.component.button

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.example.ui.theme.VaultStyle

@Composable
fun VaultIconButton(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, filled: Boolean = false, tint: Color = MaterialTheme.colorScheme.onSurface) {
    Box(modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).clip(VaultStyle.Control)
        .background(if (filled) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(22.dp), tint = tint.copy(alpha = if (enabled) 1f else .35f))
    }
}

@Composable
fun FavoriteButton(checked: Boolean, label: String, enabled: Boolean = true, onToggle: () -> Unit) {
    Box(Modifier.size(48.dp).clip(VaultStyle.Control)
        .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = { onToggle() })
        .semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Icon(if (checked) Icons.Outlined.Star else Icons.Outlined.StarOutline, null, Modifier.size(22.dp),
            tint = if (checked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

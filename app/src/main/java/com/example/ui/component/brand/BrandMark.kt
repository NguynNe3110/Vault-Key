package com.example.ui.component.brand

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BrandMark(modifier: Modifier = Modifier, compact: Boolean = false) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(if (compact) 34.dp else 42.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Key, null, Modifier.size(if (compact) 19.dp else 24.dp), tint = MaterialTheme.colorScheme.onPrimary) }
        }
        Text("vaultkey", style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge)
    }
}

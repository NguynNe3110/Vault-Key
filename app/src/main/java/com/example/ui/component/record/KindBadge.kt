package com.example.ui.component.record

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.domain.model.RecordKind
import com.example.ui.icon.icon

@Composable
fun KindBadge(kind: RecordKind, small: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val financial = kind == RecordKind.CARD || kind == RecordKind.BANK
    Surface(shape = RoundedCornerShape(if (small) 12.dp else 15.dp), color = if (financial) colors.tertiaryContainer else colors.primaryContainer,
        modifier = Modifier.size(if (small) 40.dp else 48.dp)) {
        Box(contentAlignment = Alignment.Center) { Icon(kind.icon(), null, Modifier.size(if (small) 20.dp else 23.dp), tint = if (financial) colors.onTertiaryContainer else colors.onPrimaryContainer) }
    }
}

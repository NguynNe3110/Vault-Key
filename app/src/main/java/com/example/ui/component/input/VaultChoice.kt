package com.example.ui.component.input

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.example.ui.dialog.VaultDialog
import com.example.ui.theme.VaultStyle

@Composable
fun VaultChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier,
    count: Int? = null, icon: ImageVector? = null) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier = modifier.heightIn(min = 44.dp).selectable(selected, role = Role.Tab, onClick = onClick),
        color = if (selected) colors.primary else colors.surface,
        contentColor = if (selected) colors.onPrimary else colors.onSurfaceVariant,
        shape = RoundedCornerShape(12.dp), border = if (selected) null else BorderStroke(1.dp, colors.outlineVariant)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) Icon(icon, null, Modifier.size(17.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            if (count != null) Text(count.toString(), style = MaterialTheme.typography.labelMedium,
                color = if (selected) colors.onPrimary.copy(alpha = .75f) else colors.onSurfaceVariant)
        }
    }
}

@Composable
fun SelectionMark(selected: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.size(22.dp).border(if (selected) 0.dp else 1.5.dp,
        if (selected) Color.Transparent else colors.outline, CircleShape)
        .background(if (selected) colors.primary else Color.Transparent, CircleShape), contentAlignment = Alignment.Center) {
        if (selected) Icon(Icons.Outlined.Check, null, Modifier.size(15.dp), tint = colors.onPrimary)
    }
}

@Composable
fun SelectionRow(title: String, subtitle: String?, selected: Boolean, onSelect: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(VaultStyle.Control)
        .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
        .selectable(selected, role = Role.RadioButton, onClick = onSelect).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SelectionMark(selected)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (!subtitle.isNullOrBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun VaultToggle(checked: Boolean, label: String, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).toggleable(checked, role = Role.Switch, onValueChange = onChange)
        .semantics { contentDescription = label }, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Box(Modifier.width(44.dp).height(26.dp).background(if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape).padding(3.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart) {
            Box(Modifier.size(20.dp).background(if (checked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.surface, CircleShape))
        }
    }
}

@Composable
fun ChoiceField(label: String, value: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(onClick = { expanded = true }, shape = VaultStyle.Control, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), color = MaterialTheme.colorScheme.surface) {
            Row(Modifier.fillMaxWidth().heightIn(min = 54.dp).padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(value, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.ExpandMore, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (expanded) VaultDialog(label, { expanded = false }, footer = {}) {
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEach { option -> SelectionRow(option, null, value == option) { onSelect(option); expanded = false } }
        }
    }
}

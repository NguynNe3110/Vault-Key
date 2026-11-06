package com.example.ui.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.component.button.*
import com.example.ui.theme.VaultStyle

@Composable
fun VaultDialog(title: String, onClose: () -> Unit, footer: @Composable () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val maxHeight = (LocalConfiguration.current.screenHeightDp * .88f).dp
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.padding(20.dp).widthIn(max = 480.dp).fillMaxWidth().heightIn(max = maxHeight),
            shape = VaultStyle.Dialog, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
                footer()
            }
        }
    }
}

@Composable
fun DialogActions(onCancel: () -> Unit, onConfirm: () -> Unit, label: String, busy: Boolean = false, destructive: Boolean = false) {
    if (LocalDensity.current.fontScale > 1.3f) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            VaultButton(label, onConfirm, Modifier.fillMaxWidth(), busy = busy, tone = if (destructive) ButtonTone.Danger else ButtonTone.Primary)
            VaultButton("Hủy", onCancel, Modifier.fillMaxWidth(), tone = ButtonTone.Secondary, enabled = !busy)
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            VaultButton("Hủy", onCancel, Modifier.weight(1f), tone = ButtonTone.Secondary, enabled = !busy)
            VaultButton(label, onConfirm, Modifier.weight(1.2f), busy = busy, tone = if (destructive) ButtonTone.Danger else ButtonTone.Primary)
        }
    }
}

@Composable
fun VaultConfirmDialog(title: String, message: String, confirm: String, onClose: () -> Unit, onConfirm: () -> Unit, destructive: Boolean = false) {
    VaultDialog(title, onClose, footer = { DialogActions(onClose, onConfirm, confirm, destructive = destructive) }) {
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

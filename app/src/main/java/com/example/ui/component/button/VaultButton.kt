package com.example.ui.component.button

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.VaultStyle

enum class ButtonTone { Primary, Secondary, Quiet, Danger }

@Composable
fun VaultButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null,
    tone: ButtonTone = ButtonTone.Primary, enabled: Boolean = true, busy: Boolean = false,
    leading: (@Composable () -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    val background = when(tone) {
        ButtonTone.Primary -> colors.primary
        ButtonTone.Secondary -> colors.surfaceVariant
        ButtonTone.Quiet -> Color.Transparent
        ButtonTone.Danger -> colors.errorContainer
    }
    val foreground = when(tone) {
        ButtonTone.Primary -> colors.onPrimary
        ButtonTone.Danger -> colors.onErrorContainer
        else -> colors.onSurface
    }
    Surface(onClick = onClick, enabled = enabled && !busy, shape = VaultStyle.Control,
        color = if (enabled) background else colors.surfaceVariant,
        contentColor = if (enabled) foreground else colors.onSurfaceVariant.copy(alpha = .55f),
        modifier = modifier.heightIn(min = 52.dp)) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = foreground)
            else if (leading != null) leading()
            else if (icon != null) Icon(icon, null, Modifier.size(19.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        }
    }
}

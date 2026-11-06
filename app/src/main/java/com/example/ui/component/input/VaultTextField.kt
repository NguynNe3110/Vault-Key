package com.example.ui.component.input

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.theme.VaultStyle

@Composable
fun VaultField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier,
    placeholder: String = "", error: String? = null, hint: String? = null, singleLine: Boolean = true,
    minLines: Int = 1, maxLines: Int = 8, keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leading: (@Composable () -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val colors = MaterialTheme.colorScheme
    val outline by animateColorAsState(if (error != null) colors.error else if (focused) colors.primary else colors.outline, label = "fieldOutline")
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (label.isNotBlank()) Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        BasicTextField(value = value, onValueChange = onValueChange, modifier = modifier.fillMaxWidth().semantics {
            if (label.isNotBlank()) contentDescription = label
            if (error != null) this.error(error)
        }, textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
            singleLine = singleLine, minLines = minLines, maxLines = if (singleLine) 1 else maxLines,
            keyboardOptions = keyboardOptions, visualTransformation = visualTransformation,
            interactionSource = interaction, cursorBrush = SolidColor(colors.primary), decorationBox = { input ->
                Row(Modifier.fillMaxWidth().border(if (focused) 1.5.dp else 1.dp, outline, VaultStyle.Control)
                    .background(colors.surface, VaultStyle.Control).heightIn(min = if (minLines > 1) 100.dp else 54.dp)
                    .padding(start = 14.dp, end = if (trailing == null) 14.dp else 3.dp, top = if (minLines > 1) 14.dp else 3.dp, bottom = if (minLines > 1) 14.dp else 3.dp),
                    verticalAlignment = if (minLines > 1) Alignment.Top else Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    leading?.invoke()
                    Box(Modifier.weight(1f).padding(vertical = if (minLines > 1) 0.dp else 10.dp)) {
                        if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                        input()
                    }
                    trailing?.invoke()
                }
            })
        if (error != null) Text(error, style = MaterialTheme.typography.bodySmall, color = colors.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        else if (!hint.isNullOrBlank()) Text(hint, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }
}

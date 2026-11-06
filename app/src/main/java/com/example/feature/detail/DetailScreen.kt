package com.example.feature.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.domain.model.*
import com.example.ui.component.button.*
import com.example.ui.component.record.KindBadge
import com.example.ui.component.surface.*
import com.example.ui.dialog.VaultConfirmDialog
import com.example.ui.theme.VaultStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(item: AccountItem, level: SecurityLevel?, onDismiss: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background, shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
            Surface(color = MaterialTheme.colorScheme.outlineVariant, shape = VaultStyle.Control, modifier = Modifier.size(36.dp, 4.dp)) {}
        } }) {
        LazyColumn(Modifier.widthIn(max = 740.dp).fillMaxWidth().align(Alignment.CenterHorizontally), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    KindBadge(RecordKind.from(item.kind))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.title, style = MaterialTheme.typography.headlineSmall)
                        Text(item.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    VaultIconButton(Icons.Outlined.Close, "Đóng chi tiết", onDismiss)
                }
            }
            if (item.maskedIdentifier.isNotBlank()) item {
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(if (item.kind == "CARD") "Số thẻ" else "Số tài khoản", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(item.maskedIdentifier, style = MaterialTheme.typography.headlineSmall, fontFamily = FontFamily.Monospace)
                        }
                        VaultIconButton(if (copied == "number") Icons.Outlined.Check else Icons.Outlined.ContentCopy, "Sao chép số đã lưu", {
                            clipboard.setText(AnnotatedString(item.maskedIdentifier)); copied = "number"
                        })
                    }
                }
            }
            if (item.username.isNotBlank()) item {
                DetailValue(if (item.kind == "ACCOUNT") "Tên đăng nhập / email" else "Chủ sở hữu", item.username, copied == "username") {
                    clipboard.setText(AnnotatedString(item.username)); copied = "username"
                }
            }
            if (item.kind == "ACCOUNT") item {
                Panel {
                    Text(level?.name ?: "Mức ${item.securityLevelId}", style = MaterialTheme.typography.titleMedium)
                    Text(level?.formatDescription.orEmpty(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(item.customFields.toList(), key = { it.first }) { (label, value) ->
                DetailValue(label, value, copied == label) { clipboard.setText(AnnotatedString(value)); copied = label }
            }
            item { Text("Cập nhật ${SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale.forLanguageTag("vi")).format(Date(item.updatedAt))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    VaultButton("Chỉnh sửa thông tin", onEdit, Modifier.fillMaxWidth(), icon = Icons.Outlined.Edit)
                    VaultButton("Xóa mục này", { confirmDelete = true }, Modifier.fillMaxWidth(), tone = ButtonTone.Danger, icon = Icons.Outlined.DeleteOutline)
                }
            }
        }
    }
    if (confirmDelete) VaultConfirmDialog("Xóa thông tin?", "“${item.title}” sẽ được xóa khỏi kho của bạn.", "Xóa", { confirmDelete = false }, onDelete, destructive = true)
}

@Composable
private fun DetailValue(label: String, value: String, copied: Boolean, onCopy: () -> Unit) {
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyLarge)
            }
            VaultIconButton(if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy, if (copied) "Đã sao chép $label" else "Sao chép $label", onCopy)
        }
    }
}

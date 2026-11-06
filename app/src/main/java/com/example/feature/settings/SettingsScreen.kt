package com.example.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.CollectEffects
import com.example.domain.model.*
import com.example.ui.component.brand.BrandMark
import com.example.ui.component.button.*
import com.example.ui.component.input.*
import com.example.ui.component.surface.*
import com.example.ui.dialog.*
import com.example.ui.theme.VaultStyle

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onSignIn: () -> Unit, onSignOut: () -> Unit, onMessage: (String) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEffects(viewModel.effects) { when(it) { is SettingsUiEffect.Message -> onMessage(it.value); SettingsUiEffect.SignOut -> onSignOut() } }
    SettingsContent(state, viewModel::onEvent, onSignIn)
}

@Composable
fun SettingsContent(state: SettingsUiState, onEvent: (SettingsUiEvent) -> Unit, onSignIn: () -> Unit) {
    LazyColumn(Modifier.widthIn(max = 740.dp).fillMaxSize().testTag("settings_list"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        item { BrandMark(compact = true) }
        item { Text("Cài đặt", style = MaterialTheme.typography.headlineLarge) }
        item {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Surface(shape = VaultStyle.Control, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(48.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            if (state.user?.displayName.isNullOrBlank()) Icon(Icons.Outlined.PersonOutline, null, tint = MaterialTheme.colorScheme.primary)
                            else Text(state.user!!.displayName.take(1).uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(state.user?.displayName ?: "Kho trên thiết bị", style = MaterialTheme.typography.titleMedium)
                        if (state.user != null) Text(state.user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (state.user != null) Icon(when(state.cloud) {
                        CloudState.Current -> Icons.Outlined.CloudDone
                        CloudState.Updating -> Icons.Outlined.CloudSync
                        else -> Icons.Outlined.CloudOff
                    }, when(state.cloud) { CloudState.Current -> "Đã cập nhật"; CloudState.Updating -> "Đang cập nhật"; CloudState.Waiting -> "Chờ kết nối"; CloudState.Attention -> "Chưa cập nhật được kho" },
                        Modifier.size(22.dp), tint = if (state.cloud == CloudState.Attention) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (state.user == null) VaultButton("Đăng nhập Google", onSignIn, Modifier.fillMaxWidth(), tone = ButtonTone.Secondary, enabled = state.googleAvailable)
                else if (state.cloud == CloudState.Attention) {
                    GroupDivider()
                    Text("Chưa cập nhật được kho", style = MaterialTheme.typography.titleSmall)
                    Text("Thông tin mới vẫn được giữ trên máy.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    VaultButton("Thử lại", { onEvent(SettingsUiEvent.Retry) }, tone = ButtonTone.Secondary, icon = Icons.Outlined.Refresh)
                }
            }
        }
        item { SectionTitle("Quy ước mật khẩu") }
        item {
            GroupPanel {
                state.contents.levels.forEachIndexed { index, level ->
                    SettingRow(level.name, level.formatDescription, leading = {
                        Surface(shape = VaultStyle.Control, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(36.dp)) {
                            Box(contentAlignment = Alignment.Center) { Text(level.id.toString(), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer) }
                        }
                    }, onClick = { onEvent(SettingsUiEvent.EditLevel(level)) })
                    if (index < state.contents.levels.lastIndex) GroupDivider()
                }
                GroupDivider()
                Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Số mức", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    VaultIconButton(Icons.Outlined.Remove, "Giảm số mức mật khẩu", { onEvent(SettingsUiEvent.LevelCountChanged(state.contents.levels.size - 1)) }, enabled = state.contents.levels.size > 1)
                    Text(state.contents.levels.size.toString(), style = MaterialTheme.typography.titleMedium)
                    VaultIconButton(Icons.Outlined.Add, "Tăng số mức mật khẩu", { onEvent(SettingsUiEvent.LevelCountChanged(state.contents.levels.size + 1)) }, enabled = state.contents.levels.size < 10)
                }
            }
        }
        item { SectionTitle("Quy tắc che số", action = { VaultIconButton(Icons.Outlined.Add, "Tạo quy tắc che số", { onEvent(SettingsUiEvent.EditRule(null)) }, filled = true) }) }
        item {
            GroupPanel {
                state.contents.rules.forEachIndexed { index, rule ->
                    SettingRow(rule.name, MaskingEngine.mask("123456", rule), leading = {
                        Icon(Icons.Outlined.VisibilityOff, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                    }, monospace = true, onClick = { onEvent(SettingsUiEvent.EditRule(rule)) })
                    if (index < state.contents.rules.lastIndex) GroupDivider()
                }
            }
        }
        item { SectionTitle("Danh mục", action = { VaultIconButton(Icons.Outlined.Add, "Thêm danh mục", { onEvent(SettingsUiEvent.AddCategory) }, filled = true) }) }
        item {
            GroupPanel {
                state.categories.forEachIndexed { index, name ->
                    Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 6.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(7.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        val count = state.contents.accounts.count { it.category == name }
                        if (count > 0) Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        VaultIconButton(Icons.Outlined.Remove, "Xóa danh mục $name", { onEvent(SettingsUiEvent.DeleteCategory(name)) }, enabled = state.categories.size > 1)
                    }
                    if (index < state.categories.lastIndex) GroupDivider()
                }
            }
        }
        if (state.user != null) item {
            VaultButton("Đăng xuất", { onEvent(SettingsUiEvent.AskSignOut) }, Modifier.fillMaxWidth(), icon = Icons.Outlined.Logout, tone = ButtonTone.Secondary)
        }
        item { Text("vaultkey  ·  1.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    SettingsDialogs(state, onEvent)
}

@Composable
private fun SettingRow(title: String, subtitle: String?, leading: @Composable () -> Unit, monospace: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(horizontal = 18.dp, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        leading()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (!subtitle.isNullOrBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall,
                fontFamily = if (monospace) FontFamily.Monospace else MaterialTheme.typography.bodySmall.fontFamily, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Outlined.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsDialogs(state: SettingsUiState, onEvent: (SettingsUiEvent) -> Unit) {
    val close = { onEvent(SettingsUiEvent.CloseDialog) }
    state.editingRule?.let { rule ->
        val draft = rule.copy(name = state.name.ifBlank { "Mẫu" }, pattern = if (state.lastFour) "" else state.pattern, keepLast = if (state.lastFour) 4 else 0)
        val preview = if (draft.validate() == null) MaskingEngine.mask(state.sample, draft) else "—"
        VaultDialog(if (state.contents.rules.any { it.id == rule.id }) "Sửa quy tắc che số" else "Quy tắc che số mới", close,
            footer = { DialogActions(close, { onEvent(SettingsUiEvent.SaveRule) }, "Lưu quy tắc", busy = state.saving) }) {
            VaultField(state.name, { onEvent(SettingsUiEvent.NameChanged(it)) }, "Tên quy tắc", placeholder = "Ví dụ: Thẻ cá nhân")
            VaultToggle(state.lastFour, "Chỉ hiện 4 số cuối") { onEvent(SettingsUiEvent.LastFourChanged(it)) }
            if (!state.lastFour) VaultField(state.pattern, { onEvent(SettingsUiEvent.PatternChanged(it)) }, "Vị trí và ký tự thay thế", placeholder = "1=*, 5=%", hint = "Vị trí tính từ 1. Ví dụ: 1-4=* để che 4 số đầu.")
            VaultField(state.sample, { onEvent(SettingsUiEvent.SampleChanged(it)) }, "Số mẫu", placeholder = "123456")
            Surface(shape = VaultStyle.Control, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Xem trước", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(preview, style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            if (state.error != null) Text(state.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            if (state.contents.rules.any { it.id == rule.id } && state.contents.rules.size > 1)
                VaultButton("Xóa quy tắc", { onEvent(SettingsUiEvent.AskDeleteRule) }, Modifier.fillMaxWidth(), tone = ButtonTone.Danger, icon = Icons.Outlined.DeleteOutline)
        }
    }
    state.editingLevel?.let {
        VaultDialog("Quy ước mật khẩu", close, footer = { DialogActions(close, { onEvent(SettingsUiEvent.SaveLevel) }, "Lưu thay đổi", busy = state.saving) }) {
            VaultField(state.name, { onEvent(SettingsUiEvent.NameChanged(it)) }, "Tên mức")
            VaultField(state.description, { onEvent(SettingsUiEvent.DescriptionChanged(it)) }, "Quy ước gợi nhớ", singleLine = false, minLines = 3, placeholder = "Chữ hoa đầu + ký tự đặc biệt")
            if (state.error != null) Text(state.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
    if (state.addingCategory) VaultDialog("Danh mục mới", close, footer = { DialogActions(close, { onEvent(SettingsUiEvent.SaveCategory) }, "Thêm", busy = state.saving) }) {
        VaultField(state.name, { onEvent(SettingsUiEvent.NameChanged(it)) }, "Tên danh mục", placeholder = "Ví dụ: Gia đình", error = state.error)
    }
    state.deletingRule?.let { VaultConfirmDialog("Xóa quy tắc?", "Các số đã lưu vẫn giữ nguyên.", "Xóa", close, { onEvent(SettingsUiEvent.ConfirmDeleteRule) }, destructive = true) }
    state.reducingTo?.let { max -> VaultConfirmDialog("Giảm số mức?", "Các mục đang dùng mức cao hơn sẽ chuyển về Mức $max.", "Chuyển mức", close, { onEvent(SettingsUiEvent.ConfirmReduceLevels) }) }
    if (state.showSignOut) VaultConfirmDialog("Đăng xuất?", "Kho của bạn vẫn được giữ trên thiết bị.", "Đăng xuất", close, { onEvent(SettingsUiEvent.ConfirmSignOut) })
}

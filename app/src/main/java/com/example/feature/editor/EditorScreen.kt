package com.example.feature.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.CollectEffects
import com.example.domain.model.RecordKind
import com.example.ui.component.button.*
import com.example.ui.component.input.*
import com.example.ui.component.surface.*
import com.example.ui.dialog.VaultConfirmDialog
import com.example.ui.icon.*
import com.example.ui.theme.VaultStyle
import kotlinx.coroutines.launch

@Composable
fun EditorScreen(viewModel: EditorViewModel, id: String?, kind: RecordKind, onClose: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(id, kind) { viewModel.onEvent(EditorUiEvent.Open(id, kind)) }
    CollectEffects(viewModel.effects) { if (it is EditorUiEffect.Close) onClose() }
    EditorContent(state, viewModel::onEvent)
}

@Composable
fun EditorContent(state: EditorUiState, onEvent: (EditorUiEvent) -> Unit) {
    val titleFocus = remember { FocusRequester() }
    val numberFocus = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val financial = state.kind == RecordKind.CARD || state.kind == RecordKind.BANK
    BackHandler { onEvent(EditorUiEvent.Close) }
    Column(Modifier.widthIn(max = 740.dp).fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VaultIconButton(Icons.AutoMirrored.Outlined.ArrowBack, "Quay lại kho", { onEvent(EditorUiEvent.Close) }, enabled = !state.saving)
            Column(Modifier.weight(1f)) {
                Text(if (state.original == null) "Thêm mục" else "Chỉnh sửa", style = MaterialTheme.typography.titleLarge)
                Text(state.kind.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FavoriteButton(state.favorite, "Đánh dấu yêu thích", enabled = !state.saving) { onEvent(EditorUiEvent.ToggleFavorite) }
        }
        LazyColumn(Modifier.weight(1f).testTag("editor_list"), state = listState, contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            if (state.original == null) item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(RecordKind.entries) { kind -> VaultChip(kind.label, state.kind == kind, { onEvent(EditorUiEvent.KindChanged(kind)) }, icon = kind.icon()) }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    VaultField(state.title, { onEvent(EditorUiEvent.TitleChanged(it)) }, "Tên mục", modifier = Modifier.focusRequester(titleFocus).testTag("record_title"),
                        placeholder = when(state.kind) { RecordKind.ACCOUNT -> "Gmail cá nhân"; RecordKind.CARD -> "Visa chi tiêu"; RecordKind.BANK -> "Tài khoản nhận lương"; RecordKind.NOTE -> "Thông tin bảo hiểm" },
                        error = if (state.submitted && state.title.isBlank()) "Nhập tên mục." else null)
                    ChoiceField("Danh mục", state.category, (state.categories + state.category).filter { it.isNotBlank() }.distinct()) { onEvent(EditorUiEvent.CategoryChanged(it)) }
                    if (state.kind != RecordKind.NOTE) VaultField(state.username, { onEvent(EditorUiEvent.UsernameChanged(it)) },
                        if (state.kind == RecordKind.ACCOUNT) "Tên đăng nhập / email" else "Tên chủ sở hữu", placeholder = if (financial) "Tên in trên thẻ hoặc tài khoản" else "Email hoặc tên đăng nhập")
                }
            }
            if (financial) item {
                Panel {
                    SectionTitle(if (state.kind == RecordKind.CARD) "Thông tin thẻ" else "Thông tin ngân hàng")
                    VaultField(state.rawIdentifier, { onEvent(EditorUiEvent.NumberChanged(it)) }, if (state.kind == RecordKind.CARD) "Số thẻ" else "Số tài khoản",
                        modifier = Modifier.focusRequester(numberFocus).testTag("record_number"),
                        placeholder = if (state.original == null) "Nhập số để che" else "Để trống để giữ số đã lưu",
                        visualTransformation = if (state.numberVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        error = if (state.submitted) state.numberError else null,
                        trailing = { VaultIconButton(if (state.numberVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            if (state.numberVisible) "Ẩn số đang nhập" else "Hiện số đang nhập", { onEvent(EditorUiEvent.ToggleNumberVisibility) }) })
                    val rule = state.rules.find { it.id == state.ruleId }
                    ChoiceField("Quy tắc che số", rule?.name ?: "Chọn quy tắc", state.rules.map { it.name }) { name ->
                        state.rules.firstOrNull { it.name == name }?.let { onEvent(EditorUiEvent.RuleChanged(it.id)) }
                    }
                    Surface(shape = VaultStyle.Control, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.VisibilityOff, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("Số sẽ lưu", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Text(state.preview.ifBlank { "•••• •••• ••••" }, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.testTag("mask_preview"))
                        }
                    }
                    Text(if (state.original == null) "Phần đã che sẽ không thể khôi phục." else "Nhập lại số nếu muốn đổi quy tắc. Phần đã che không thể khôi phục.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (state.kind == RecordKind.ACCOUNT) item {
                Panel {
                    SectionTitle("Quy ước mật khẩu")
                    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.levels.forEach { level -> SelectionRow(level.name, level.formatDescription, state.levelId == level.id) { onEvent(EditorUiEvent.LevelChanged(level.id)) } }
                    }
                }
            }
            item {
                SectionTitle("Thông tin bổ sung", action = {
                    VaultIconButton(Icons.Outlined.Add, "Thêm trường thông tin", { onEvent(EditorUiEvent.AddField) }, filled = true)
                })
                if (state.submitted && state.fieldsError != null) Text(state.fieldsError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            items(state.fields, key = { it.id }) { field ->
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { VaultField(field.name, { onEvent(EditorUiEvent.FieldChanged(field.copy(name = it))) }, "Tên trường", placeholder = "Website, ghi chú…") }
                        VaultIconButton(Icons.Outlined.Close, "Xóa trường ${field.name}", { onEvent(EditorUiEvent.RemoveField(field.id)) })
                    }
                    VaultField(field.value, { onEvent(EditorUiEvent.FieldChanged(field.copy(value = it))) }, "Nội dung", placeholder = "Nhập thông tin", singleLine = false, minLines = if (state.kind == RecordKind.NOTE) 3 else 1)
                }
            }
            if (state.formError != null) item { Text(state.formError, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
        }
        Surface(color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            VaultButton(if (state.saving) "Đang lưu…" else "Lưu vào kho", {
                onEvent(EditorUiEvent.Save)
                if (state.title.isBlank() || state.numberError != null) scope.launch {
                    val basicIndex = if (state.original == null) 1 else 0
                    listState.scrollToItem(if (state.title.isBlank()) basicIndex else basicIndex + 1)
                    withFrameNanos { }
                    runCatching { if (state.title.isBlank()) titleFocus.requestFocus() else numberFocus.requestFocus() }
                }
            }, Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).testTag("save_record"), icon = Icons.Outlined.Check, busy = state.saving)
        }
    }
    if (state.discard) VaultConfirmDialog("Bỏ thay đổi?", "Thông tin vừa nhập chưa được lưu.", "Bỏ thay đổi",
        { onEvent(EditorUiEvent.KeepEditing) }, { onEvent(EditorUiEvent.ConfirmDiscard) }, destructive = true)
}

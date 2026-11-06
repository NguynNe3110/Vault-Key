package com.example.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.CollectEffects
import com.example.domain.model.*
import com.example.ui.component.brand.BrandMark
import com.example.ui.component.button.*
import com.example.ui.component.input.*
import com.example.ui.component.record.KindBadge
import com.example.ui.component.surface.*
import com.example.ui.dialog.VaultDialog
import com.example.ui.icon.*
import com.example.ui.theme.VaultStyle

@Composable
fun HomeScreen(viewModel: HomeViewModel, tab: Int, onAdd: () -> Unit, onDetail: (AccountItem) -> Unit,
    onSettings: () -> Unit, onMessage: (String) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(tab) { viewModel.onEvent(HomeUiEvent.TabChanged(tab)) }
    CollectEffects(viewModel.effects) { if (it is HomeUiEffect.Message) onMessage(it.value) }
    HomeContent(state, viewModel::onEvent, onAdd, onDetail, onSettings)
}

@Composable
fun HomeContent(state: HomeUiState, onEvent: (HomeUiEvent) -> Unit, onAdd: () -> Unit,
    onDetail: (AccountItem) -> Unit, onSettings: () -> Unit) {
    var categoryPicker by remember { mutableStateOf(false) }
    LazyVerticalGrid(columns = GridCells.Adaptive(320.dp), modifier = Modifier.widthIn(max = 1100.dp).fillMaxSize().testTag("vault_list"),
        contentPadding = PaddingValues(20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                BrandMark(Modifier.weight(1f), compact = true)
                VaultIconButton(Icons.Outlined.AccountCircle, "Tài khoản và cài đặt", onSettings)
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(when(state.tab) { 1 -> "Thẻ & ngân hàng"; 2 -> "Yêu thích"; else -> "Kho lưu trữ" }, style = MaterialTheme.typography.headlineLarge)
                    Text(if (state.query.isNotBlank() || state.kind != "ALL" || state.category != "Tất cả") "${state.records.size} kết quả" else "${state.total} mục",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                VaultButton("Thêm", onAdd, icon = Icons.Outlined.Add, modifier = Modifier.testTag("add_record"))
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) {
                    VaultField(state.query, { onEvent(HomeUiEvent.QueryChanged(it)) }, "", placeholder = "Tìm trong kho",
                        modifier = Modifier.testTag("vault_search").semantics { contentDescription = "Tìm trong kho" },
                        leading = { Icon(Icons.Outlined.Search, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        trailing = if (state.query.isNotEmpty()) ({ VaultIconButton(Icons.Outlined.Close, "Xóa tìm kiếm", { onEvent(HomeUiEvent.QueryChanged("")) }) }) else null)
                }
                Surface(shape = VaultStyle.Control, color = if (state.category != "Tất cả") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    VaultIconButton(Icons.Outlined.Tune, "Lọc danh mục", { categoryPicker = true }, modifier = Modifier.height(54.dp),
                        tint = if (state.category != "Tất cả") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 4.dp)) {
                item { VaultChip("Tất cả", state.kind == "ALL", { onEvent(HomeUiEvent.KindChanged("ALL")) }, count = state.total) }
                items(RecordKind.entries.filter { state.tab != 1 || it == RecordKind.CARD || it == RecordKind.BANK }) { kind ->
                    VaultChip(kind.shortLabel(), state.kind == kind.name, { onEvent(HomeUiEvent.KindChanged(kind.name)) }, count = state.counts[kind.name] ?: 0)
                }
            }
        }
        if (state.category != "Tất cả") item(span = { GridItemSpan(maxLineSpan) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(state.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                VaultIconButton(Icons.Outlined.Close, "Bỏ lọc danh mục", { onEvent(HomeUiEvent.CategoryChanged("Tất cả")) })
            }
        }
        if (state.records.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 44.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(shape = VaultStyle.Panel, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(72.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(if (state.total > 0) Icons.Outlined.SearchOff else if (state.tab == 2) Icons.Outlined.StarOutline else Icons.Outlined.FolderOpen, null,
                        Modifier.size(30.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                }
                Text(if (state.total > 0) "Chưa tìm thấy mục phù hợp" else if (state.tab == 2) "Chưa có mục yêu thích" else "Kho của bạn còn trống", style = MaterialTheme.typography.titleLarge)
                Text(if (state.total > 0) "Thử từ khóa khác hoặc bỏ bộ lọc." else if (state.tab == 2) "Đánh dấu ngôi sao để tìm lại nhanh hơn." else "Thêm thông tin đầu tiên của bạn.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (state.total > 0) VaultButton("Bỏ bộ lọc", { onEvent(HomeUiEvent.ClearFilters) }, tone = ButtonTone.Secondary)
                else if (state.tab != 2) VaultButton("Thêm mục đầu tiên", onAdd, icon = Icons.Outlined.Add)
            }
        }
        items(state.records, key = { it.id }) { item ->
            RecordCard(item, state.levels.find { it.id == item.securityLevelId }, { onDetail(item) }) { onEvent(HomeUiEvent.FavoriteClicked(item)) }
        }
    }
    if (categoryPicker) VaultDialog("Danh mục", { categoryPicker = false }, footer = {}) {
        (listOf("Tất cả") + state.categories).forEach { category ->
            SelectionRow(category, null, state.category == category) { onEvent(HomeUiEvent.CategoryChanged(category)); categoryPicker = false }
        }
    }
}

@Composable
fun RecordCard(item: AccountItem, level: SecurityLevel?, onClick: () -> Unit, onFavorite: () -> Unit) {
    val kind = RecordKind.from(item.kind)
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = VaultStyle.Panel,
        color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KindBadge(kind)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(item.username.ifBlank { kind.label }, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                FavoriteButton(item.isFavorite, if (item.isFavorite) "Bỏ yêu thích ${item.title}" else "Yêu thích ${item.title}", onToggle = onFavorite)
            }
            if (item.maskedIdentifier.isNotBlank()) {
                Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, VaultStyle.Control).padding(14.dp)) {
                    Text(item.maskedIdentifier, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleLarge)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(5.dp).background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape))
                Text(item.category, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (kind == RecordKind.ACCOUNT) Text(level?.name ?: "Mức ${item.securityLevelId}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

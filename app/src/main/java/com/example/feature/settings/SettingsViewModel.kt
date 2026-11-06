package com.example.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.*
import com.example.domain.repository.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class SettingsViewModel(private val vault: VaultRepository, auth: AuthRepository) : ViewModel() {
    private val form = MutableStateFlow(SettingsUiState())
    private val base = combine(vault.contents, vault.categories, auth.currentUser, vault.syncStatus, vault.isSyncing) { data, categories, user, status, syncing ->
        val cloud = when {
            syncing -> CloudState.Updating
            status == "Đã đồng bộ" -> CloudState.Current
            status.contains("PERMISSION_DENIED") || status.contains("FAILED_PRECONDITION") || status.contains("Firestore") || status.contains("hết hạn") -> CloudState.Attention
            else -> CloudState.Waiting
        }
        SettingsUiState(contents = data, categories = categories, user = user, cloud = cloud, googleAvailable = auth.isAvailable.value && auth.webClientId.isNotBlank())
    }
    val state = combine(base, form) { data, draft -> draft.copy(contents = data.contents, categories = data.categories,
        user = data.user, cloud = data.cloud, googleAvailable = data.googleAvailable) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())
    private val channel = Channel<SettingsUiEffect>(Channel.BUFFERED)
    val effects = channel.receiveAsFlow()
    private fun close() { form.value = SettingsUiState() }
    private fun runWrite(closeAfter: Boolean = false, action: suspend () -> Unit) {
        viewModelScope.launch {
            form.update { it.copy(saving = true, error = null) }
            try { action(); if (closeAfter) close() }
            catch(e: CancellationException) { throw e }
            catch(e: IllegalArgumentException) {
                if (closeAfter) form.update { it.copy(error = e.message ?: "Kiểm tra lại thông tin.") }
                else channel.send(SettingsUiEffect.Message(e.message ?: "Không thể thực hiện."))
            }
            catch(e: Exception) {
                if (closeAfter) form.update { it.copy(error = "Không thể lưu. Vui lòng thử lại.") }
                else channel.send(SettingsUiEffect.Message("Không thể thực hiện. Hãy thử lại."))
            }
            finally { form.update { it.copy(saving = false) } }
        }
    }
    fun onEvent(event: SettingsUiEvent) {
        if (state.value.saving) return
        val value = state.value
        when(event) {
            is SettingsUiEvent.EditRule -> {
                val rule = event.rule ?: MaskingRule(UUID.randomUUID().toString(), "", "1=*, 5=%")
                form.value = SettingsUiState(editingRule = rule, name = rule.name, pattern = rule.pattern, lastFour = rule.keepLast > 0)
            }
            is SettingsUiEvent.EditLevel -> form.value = SettingsUiState(editingLevel = event.level, name = event.level.name, description = event.level.formatDescription)
            is SettingsUiEvent.NameChanged -> form.update { it.copy(name = event.value, error = null) }
            is SettingsUiEvent.DescriptionChanged -> form.update { it.copy(description = event.value, error = null) }
            is SettingsUiEvent.PatternChanged -> form.update { it.copy(pattern = event.value, error = null) }
            is SettingsUiEvent.LastFourChanged -> form.update { it.copy(lastFour = event.value, error = null) }
            is SettingsUiEvent.SampleChanged -> form.update { it.copy(sample = event.value.filter(Char::isDigit).take(32)) }
            is SettingsUiEvent.DeleteCategory -> runWrite { vault.deleteCategory(event.name) }
            is SettingsUiEvent.LevelCountChanged -> if (event.count in 1..10) {
                if (value.contents.accounts.any { it.securityLevelId > event.count }) form.update { it.copy(reducingTo = event.count) }
                else runWrite { vault.setMaxLevel(event.count) }
            }
            SettingsUiEvent.CloseDialog -> close()
            SettingsUiEvent.AddCategory -> form.value = SettingsUiState(addingCategory = true)
            SettingsUiEvent.SaveCategory -> runWrite(closeAfter = true) { vault.addCategory(value.name) }
            SettingsUiEvent.SaveLevel -> value.editingLevel?.let { level -> runWrite(closeAfter = true) { vault.saveLevel(level.copy(name = value.name, formatDescription = value.description)) } }
            SettingsUiEvent.SaveRule -> value.editingRule?.let { original ->
                val rule = original.copy(name = value.name, pattern = if (value.lastFour) "" else value.pattern, keepLast = if (value.lastFour) 4 else 0)
                val error = rule.validate()
                if (error != null) form.update { it.copy(error = error) }
                else runWrite(closeAfter = true) { vault.saveRule(rule) }
            }
            SettingsUiEvent.AskDeleteRule -> form.value = SettingsUiState(deletingRule = value.editingRule)
            SettingsUiEvent.ConfirmDeleteRule -> value.deletingRule?.let { rule -> runWrite(closeAfter = true) { vault.deleteRule(rule) } }
            SettingsUiEvent.ConfirmReduceLevels -> value.reducingTo?.let { count -> runWrite(closeAfter = true) { vault.setMaxLevel(count) } }
            SettingsUiEvent.AskSignOut -> form.value = SettingsUiState(showSignOut = true)
            SettingsUiEvent.ConfirmSignOut -> { close(); channel.trySend(SettingsUiEffect.SignOut) }
            SettingsUiEvent.Retry -> vault.requestSync()
        }
    }
}

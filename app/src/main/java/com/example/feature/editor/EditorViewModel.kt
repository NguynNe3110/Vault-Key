package com.example.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.*
import com.example.domain.repository.VaultRepository
import com.example.domain.usecase.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EditorViewModel(private val vault: VaultRepository, private val saveRecord: SaveRecordUseCase) : ViewModel() {
    private val _state = MutableStateFlow(EditorUiState())
    val state = _state.asStateFlow()
    private val channel = Channel<EditorUiEffect>(Channel.BUFFERED)
    val effects = channel.receiveAsFlow()
    init {
        viewModelScope.launch {
            combine(vault.contents, vault.categories) { data, categories -> data to categories }.collect { (data, categories) ->
                update { it.copy(levels = data.levels, rules = data.rules, categories = categories) }
            }
        }
    }
    private fun template(kind: RecordKind) = when(kind) {
        RecordKind.ACCOUNT -> listOf(ExtraField(name = "Website"))
        RecordKind.CARD -> listOf(ExtraField(name = "Ngân hàng"), ExtraField(name = "Ngày hết hạn"))
        RecordKind.BANK -> listOf(ExtraField(name = "Ngân hàng"))
        RecordKind.NOTE -> listOf(ExtraField(name = "Ghi chú"))
    }
    private fun update(transform: (EditorUiState) -> EditorUiState) {
        val value = transform(_state.value)
        val rule = value.rules.find { it.id == value.ruleId }
        val number = value.kind == RecordKind.CARD || value.kind == RecordKind.BANK
        val preview = if (number && value.rawIdentifier.isNotBlank() && rule != null && rule.validate() == null)
            MaskingEngine.mask(value.rawIdentifier, rule) else value.original?.maskedIdentifier.orEmpty()
        val numberError = when {
            !number -> null
            value.rawIdentifier.isBlank() && value.original?.maskedIdentifier.isNullOrBlank() -> "Nhập số cần lưu."
            value.rawIdentifier.isNotBlank() && (rule == null || rule.validate() != null || !MaskingEngine.hidesAny(value.rawIdentifier, rule)) -> "Quy tắc cần che ít nhất một số."
            else -> null
        }
        val fields = value.fields.filter { it.name.isNotBlank() || it.value.isNotBlank() }
        val fieldError = when {
            fields.any { it.name.isBlank() } -> "Đặt tên cho trường đã nhập nội dung."
            fields.map { it.name.trim().lowercase() }.distinct().size != fields.size -> "Tên trường không được trùng nhau."
            else -> null
        }
        val old = value.original
        val dirty = value.title != old?.title.orEmpty() || value.username != old?.username.orEmpty() || value.rawIdentifier.isNotBlank() ||
            value.favorite != (old?.isFavorite ?: false) || (old != null && (value.category != old.category || value.levelId != old.securityLevelId)) ||
            value.fields.filter { it.value.isNotBlank() }.associate { it.name to it.value } != old?.customFields.orEmpty()
        _state.value = value.copy(preview = preview, numberError = numberError, fieldsError = fieldError, dirty = dirty)
    }
    fun onEvent(event: EditorUiEvent) {
        if (state.value.saving) return
        when(event) {
            is EditorUiEvent.Open -> {
                if (state.value.initialized) return
                val data = vault.contents.value
                val original = data.accounts.find { it.id == event.id }
                val kind = original?.let { RecordKind.from(it.kind) } ?: event.kind
                update { EditorUiState(initialized = true, original = original, kind = kind,
                    title = original?.title.orEmpty(), username = original?.username.orEmpty(),
                    category = original?.category ?: (if (kind == RecordKind.CARD || kind == RecordKind.BANK) "Tài chính" else vault.categories.value.firstOrNull().orEmpty()),
                    levelId = original?.securityLevelId ?: data.levels.firstOrNull()?.id ?: 1,
                    ruleId = original?.maskRuleId?.takeIf { id -> data.rules.any { it.id == id } } ?: data.rules.firstOrNull { it.id == "last-four" }?.id ?: data.rules.firstOrNull()?.id,
                    favorite = original?.isFavorite ?: false, fields = original?.customFields?.map { ExtraField(name = it.key, value = it.value) } ?: template(kind),
                    levels = data.levels, rules = data.rules, categories = vault.categories.value) }
            }
            is EditorUiEvent.TitleChanged -> update { it.copy(title = event.value, formError = null) }
            is EditorUiEvent.UsernameChanged -> update { it.copy(username = event.value, formError = null) }
            is EditorUiEvent.CategoryChanged -> update { it.copy(category = event.value) }
            is EditorUiEvent.LevelChanged -> update { it.copy(levelId = event.id) }
            is EditorUiEvent.RuleChanged -> update { it.copy(ruleId = event.id) }
            is EditorUiEvent.NumberChanged -> update { it.copy(rawIdentifier = event.value.filter(Char::isDigit).take(1000), formError = null) }
            is EditorUiEvent.KindChanged -> update { it.copy(kind = event.kind,
                fields = if (it.fields.all { field -> field.value.isBlank() }) template(event.kind) else it.fields,
                category = if (event.kind == RecordKind.CARD || event.kind == RecordKind.BANK) "Tài chính" else it.categories.firstOrNull().orEmpty()) }
            is EditorUiEvent.FieldChanged -> update { it.copy(fields = it.fields.map { field -> if (field.id == event.field.id) event.field else field }) }
            is EditorUiEvent.RemoveField -> update { it.copy(fields = it.fields.filterNot { field -> field.id == event.id }) }
            EditorUiEvent.AddField -> update { it.copy(fields = it.fields + ExtraField()) }
            EditorUiEvent.ToggleNumberVisibility -> update { it.copy(numberVisible = !it.numberVisible) }
            EditorUiEvent.ToggleFavorite -> update { it.copy(favorite = !it.favorite) }
            EditorUiEvent.KeepEditing -> update { it.copy(discard = false) }
            EditorUiEvent.Close -> if (state.value.dirty) update { it.copy(discard = true) } else channel.trySend(EditorUiEffect.Close)
            EditorUiEvent.ConfirmDiscard -> channel.trySend(EditorUiEffect.Close)
            EditorUiEvent.Save -> save()
        }
    }
    private fun save() {
        update { it.copy(submitted = true) }
        val input = state.value
        if (input.title.isBlank() || input.numberError != null || input.fieldsError != null) return
        viewModelScope.launch {
            update { it.copy(saving = true, formError = null) }
            try {
                saveRecord(RecordDraft(input.original, input.title, input.category,
                    if (input.kind == RecordKind.NOTE) "" else input.username, input.levelId, input.kind,
                    input.rawIdentifier, input.rules.find { it.id == input.ruleId },
                    input.fields.filter { it.name.isNotBlank() && it.value.isNotBlank() }.associate { it.name.trim() to it.value.trim() }, input.favorite))
                update { it.copy(rawIdentifier = "", submitted = false) }
                channel.send(EditorUiEffect.Close)
            } catch(e: CancellationException) { throw e }
            catch(e: IllegalArgumentException) { update { it.copy(formError = e.message ?: "Kiểm tra lại thông tin.") } }
            catch(e: Exception) { update { it.copy(formError = "Không thể lưu. Vui lòng thử lại.") } }
            finally { update { it.copy(saving = false) } }
        }
    }
}

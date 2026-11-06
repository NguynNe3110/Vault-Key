package com.example.feature.editor

import com.example.domain.model.*

sealed interface EditorUiEvent {
    data class Open(val id: String?, val kind: RecordKind) : EditorUiEvent
    data class TitleChanged(val value: String) : EditorUiEvent
    data class UsernameChanged(val value: String) : EditorUiEvent
    data class CategoryChanged(val value: String) : EditorUiEvent
    data class LevelChanged(val id: Int) : EditorUiEvent
    data class RuleChanged(val id: String) : EditorUiEvent
    data class NumberChanged(val value: String) : EditorUiEvent
    data class KindChanged(val kind: RecordKind) : EditorUiEvent
    data class FieldChanged(val field: ExtraField) : EditorUiEvent
    data class RemoveField(val id: String) : EditorUiEvent
    data object AddField : EditorUiEvent
    data object ToggleNumberVisibility : EditorUiEvent
    data object ToggleFavorite : EditorUiEvent
    data object Save : EditorUiEvent
    data object Close : EditorUiEvent
    data object ConfirmDiscard : EditorUiEvent
    data object KeepEditing : EditorUiEvent
}

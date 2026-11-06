package com.example.feature.settings

import com.example.domain.model.*

sealed interface SettingsUiEvent {
    data class EditRule(val rule: MaskingRule?) : SettingsUiEvent
    data class EditLevel(val level: SecurityLevel) : SettingsUiEvent
    data class NameChanged(val value: String) : SettingsUiEvent
    data class DescriptionChanged(val value: String) : SettingsUiEvent
    data class PatternChanged(val value: String) : SettingsUiEvent
    data class LastFourChanged(val value: Boolean) : SettingsUiEvent
    data class SampleChanged(val value: String) : SettingsUiEvent
    data class LevelCountChanged(val count: Int) : SettingsUiEvent
    data class DeleteCategory(val name: String) : SettingsUiEvent
    data object AddCategory : SettingsUiEvent
    data object SaveCategory : SettingsUiEvent
    data object SaveRule : SettingsUiEvent
    data object SaveLevel : SettingsUiEvent
    data object AskDeleteRule : SettingsUiEvent
    data object ConfirmDeleteRule : SettingsUiEvent
    data object ConfirmReduceLevels : SettingsUiEvent
    data object AskSignOut : SettingsUiEvent
    data object ConfirmSignOut : SettingsUiEvent
    data object CloseDialog : SettingsUiEvent
    data object Retry : SettingsUiEvent
}

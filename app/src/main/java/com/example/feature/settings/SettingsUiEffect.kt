package com.example.feature.settings

import com.example.domain.model.*

sealed interface SettingsUiEffect {
    data class Message(val value: String) : SettingsUiEffect
    data object SignOut : SettingsUiEffect
}

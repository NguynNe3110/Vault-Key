package com.example.feature.settings

import com.example.domain.model.*

enum class CloudState { Current, Updating, Waiting, Attention }
data class SettingsUiState(
    val contents: VaultContents = VaultContents(), val categories: List<String> = emptyList(),
    val user: UserSession? = null, val cloud: CloudState = CloudState.Waiting, val googleAvailable: Boolean = false,
    val editingRule: MaskingRule? = null, val editingLevel: SecurityLevel? = null, val addingCategory: Boolean = false,
    val deletingRule: MaskingRule? = null, val reducingTo: Int? = null, val showSignOut: Boolean = false,
    val name: String = "", val description: String = "", val pattern: String = "", val lastFour: Boolean = false,
    val sample: String = "123456", val error: String? = null, val saving: Boolean = false,
    val rulePreviews: Map<String, String> = emptyMap(), val draftPreview: String = "—"
)

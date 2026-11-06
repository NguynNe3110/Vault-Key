package com.example.feature.editor

import com.example.domain.model.*
import java.util.UUID

data class ExtraField(val id: String = UUID.randomUUID().toString(), val name: String = "", val value: String = "")
data class EditorUiState(
    val initialized: Boolean = false, val original: AccountItem? = null, val kind: RecordKind = RecordKind.ACCOUNT,
    val title: String = "", val username: String = "", val category: String = "", val levelId: Int = 1,
    val ruleId: String? = null, val rawIdentifier: String = "", val numberVisible: Boolean = false,
    val favorite: Boolean = false, val fields: List<ExtraField> = emptyList(), val preview: String = "",
    val levels: List<SecurityLevel> = emptyList(), val rules: List<MaskingRule> = emptyList(), val categories: List<String> = emptyList(),
    val submitted: Boolean = false, val saving: Boolean = false, val discard: Boolean = false,
    val numberError: String? = null, val fieldsError: String? = null, val formError: String? = null,
    val dirty: Boolean = false
)

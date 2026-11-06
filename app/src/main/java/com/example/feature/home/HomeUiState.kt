package com.example.feature.home

import com.example.domain.model.*

data class HomeUiState(
    val records: List<AccountItem> = emptyList(), val levels: List<SecurityLevel> = emptyList(),
    val counts: Map<String, Int> = emptyMap(), val total: Int = 0, val categories: List<String> = emptyList(),
    val query: String = "", val category: String = "Tất cả", val kind: String = "ALL", val tab: Int = 0,
    val displayName: String? = null
)

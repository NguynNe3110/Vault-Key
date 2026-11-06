package com.example.feature.main

import com.example.domain.model.UserSession
import com.example.domain.model.VaultContents

data class MainUiState(
    val contents: VaultContents = VaultContents(), val ready: Boolean = false,
    val user: UserSession? = null, val error: String? = null
)

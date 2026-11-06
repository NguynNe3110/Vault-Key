package com.example.feature.auth.login

data class LoginUiState(val available: Boolean = false, val clientId: String = "", val busy: Boolean = false, val error: String? = null)

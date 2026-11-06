package com.example.feature.auth.login



sealed interface LoginUiEvent {
    data object Start : LoginUiEvent
    data object Cancel : LoginUiEvent
    data class TokenReceived(val token: String) : LoginUiEvent
    data class Failed(val noAccount: Boolean = false) : LoginUiEvent
}

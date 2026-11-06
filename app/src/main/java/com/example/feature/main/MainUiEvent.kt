package com.example.feature.main


sealed interface MainUiEvent {
    data object Foreground : MainUiEvent
    data object SignOut : MainUiEvent
    data class DeleteRecord(val id: String) : MainUiEvent
}

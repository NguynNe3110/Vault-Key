package com.example.feature.main


sealed interface MainUiEffect { data class Message(val value: String) : MainUiEffect }

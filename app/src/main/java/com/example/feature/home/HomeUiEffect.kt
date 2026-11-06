package com.example.feature.home

import com.example.domain.model.*

sealed interface HomeUiEffect { data class Message(val value: String) : HomeUiEffect }

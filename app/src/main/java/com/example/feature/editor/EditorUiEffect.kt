package com.example.feature.editor

import com.example.domain.model.*

sealed interface EditorUiEffect { data object Close : EditorUiEffect }

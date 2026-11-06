package com.example.feature.home

import com.example.domain.model.*

sealed interface HomeUiEvent {
    data class TabChanged(val tab: Int) : HomeUiEvent
    data class QueryChanged(val value: String) : HomeUiEvent
    data class CategoryChanged(val value: String) : HomeUiEvent
    data class KindChanged(val value: String) : HomeUiEvent
    data class FavoriteClicked(val account: AccountItem) : HomeUiEvent
    data object ClearFilters : HomeUiEvent
}
